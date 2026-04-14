import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom'; // per navigare tra le pagine
import { useAuth } from '../context/AuthContext'; // per ottenere l'utente loggato
import Header from '../components/Header';
import {
  getTripsByUser,
  deleteTrip,
  saveTripToFavorites,
  removeTripFromFavorites,
  completeTrip,
  getTripHistory,
  restoreTrip,
} from '../services/api'; // chiamate al backend

// ─── Funzioni di utilità ───────────────────────────────────────────────

// Converte secondi in formato leggibile (es. 3700 → "1h 1min")
function formatDuration(seconds) {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}min` : `${m} min`;
}

// Converte una data ISO in formato italiano (es. "2024-03-01" → "01 mar 2024")
function formatDate(isoString) {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
}

// ─── Componente: finestra di conferma eliminazione ────────────────────────
// Appare quando l'utente clicca "Elimina" su un viaggio.
function ConfirmDeleteModal({ tripName, onConfirm, onClose }) {
  return (
    // Cliccando sull'overlay scuro (fuori dal box) si chiude il modal
    <div className="modal-overlay" onClick={onClose}>
      {/* stopPropagation evita che il click sul box si propaghi all'overlay e chiuda il modal */}
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <h2 className="modal-title">Elimina viaggio</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Sei sicuro di voler eliminare <strong style={{ color: 'var(--text)' }}>{tripName}</strong>?
          <br />Questa azione non può essere annullata.
        </p>
        <div className="modal-footer">
          <button className="btn btn-ghost" onClick={onClose}>Annulla</button>
          <button id="btn-confirm-delete" className="btn btn-danger" onClick={onConfirm}>Elimina</button>
        </div>
      </div>
    </div>
  );
}

// ─── Componente: Badge colorato per lo stato del viaggio ───────────────
function StatusBadge({ status }) {
  // Mappa ogni stato al nome della classe CSS e all'etichetta da mostrare
  const map = {
    SAVED:     { cls: 'badge-accent',  label: 'In programma' },
    STARRED:   { cls: 'badge-star',    label: '★ Preferito' },
    COMPLETED: { cls: 'badge-green',   label: 'Completato' },
  };
  // Se lo stato non è nella mappa, usa badge grigio con il valore grezzo
  const s = map[status] || { cls: 'badge-muted', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

/**
 * Card di un singolo viaggio.
 * Mostra il bottone "★ Preferito" / "☆ Preferito" e "Completa viaggio"
 * solo se il viaggio non è già COMPLETED.
 */
function TripCard({ trip, onDelete, onToggleFavorite, onComplete, onRestore, onClick, activeTab }) {
  const navigate = useNavigate();
  
  // Calcola il tempo totale di visita sommando i minuti di ogni tappa
  const totalVisitMin = (trip.stages || []).reduce(
    (acc, s) => acc + (s.visitDurationMinutes || 0), 0,
  );
  const isStarred    = trip.status === 'STARRED';
  const isCompleted  = trip.status === 'COMPLETED';

  return (
    <div
      id={`trip-card-${trip.id}`}
      className="trip-card"
      onClick={onClick}
    >
      <div className="trip-card-header">
        <span className="trip-card-city">{trip.city}</span>
        <StatusBadge status={trip.status} />
      </div>

      <h3 className="trip-card-name">{trip.name}</h3>

      <div className="trip-card-stats">
        <span className="trip-stat">{(trip.stages || []).length} tappe</span>
        {/* Mostra il tempo di visita solo se è maggiore di 0 */}
        {totalVisitMin > 0 && (
          <span className="trip-stat">
            {Math.floor(totalVisitMin / 60)}h {totalVisitMin % 60}min visita
          </span>
        )}
        {/* Mostra la durata del percorso solo se il backend l'ha calcolata (dopo ottimizzazione) */}
        {trip.totalDurationSeconds != null && (
          <span className="trip-stat">{formatDuration(trip.totalDurationSeconds)} percorso</span>
        )}
      </div>

      {/* Azioni inline: Preferiti e Completa (solo se non completato) */}
      {!isCompleted && (
        <div className="trip-card-actions" onClick={e => e.stopPropagation()}>
          <button
            id={`btn-fav-${trip.id}`}
            className={`btn btn-sm ${isStarred ? 'btn-star-active' : 'btn-star'}`}
            title={isStarred ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'}
            onClick={() => onToggleFavorite(trip)}
          >
            {isStarred ? '★ Preferito' : '☆ Preferiti'}
          </button>

          <button
            id={`btn-complete-${trip.id}`}
            className="btn btn-sm btn-success"
            title="Segna come completato"
            onClick={() => onComplete(trip)}
          >
            ✓ Completa
          </button>
        </div>
      )}
      
      {/* Bottone ripristina — visibile solo nella tab Storico per i viaggi completati */}
      {isCompleted && activeTab === 'history' && (
        <div className="trip-card-actions" onClick={e => e.stopPropagation()}>
          <button
            className="btn btn-sm btn-star"
            onClick={() => onRestore(trip)}
          >
            ↩ Ripristina
          </button>
        </div>
      )}

      {/* Footer card: data creazione + pulsanti elimina/modifica */}
      <div className="trip-card-footer">
        <span className="trip-card-date">{formatDate(trip.createdAt)}</span>
        <div style={{ display: 'flex', gap: '6px' }} onClick={e => e.stopPropagation()}>
          {/* Pulsante Modifica: visibile solo se non completato */}
          {!isCompleted && (
            <button
              id={`btn-edit-${trip.id}`}
              className="btn btn-edit btn-sm"
              onClick={() => navigate(`/edit-trip/${trip.id}`)}
            >
              Modifica
            </button>
          )}
          
          {/* Pulsante Elimina */}
          <button
            id={`btn-delete-${trip.id}`}
            className="btn btn-danger btn-sm"
            onClick={() => onDelete(trip)}
          >
            Elimina
          </button>
        </div>
      </div>
    </div>
  );
}

// ---- Componente principale: Pagina "I miei viaggi" -----------------------------
export default function MyTripsPage() {

  const { user } = useAuth(); // Prende l'utente loggato dal context
  const navigate = useNavigate(); // per navigare verso altre pagine
  
  //--- Dati ---
  const [trips, setTrips] = useState([]);   // Lista completa dei viaggi attivi
  const [history, setHistory] = useState([]); // Lista dei viaggi completati

  // --- UI state ---
  const [loading, setLoading] = useState(true); // true mentre i dati stanno arrivando
  const [error, setError] = useState(''); // Messaggio di errore
  const [search, setSearch] = useState(''); // Testo digitato nella barra di ricerca
  const [statusFilter, setStatusFilter] = useState(''); // Filtro per stato ("" per tutti)
  const [activeTab, setActiveTab] = useState('all'); // 'all' | 'favorites' | 'history'
  const [deleteModal, setDeleteModal] = useState(null); // Viaggio selezionato per eliminazione
  
  // --- Caricamento dati all'apertura della pagina --------------------------------------
  const loadData = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const [tripsRes, historyRes] = await Promise.all([
        getTripsByUser(user.id),
        getTripHistory(),
      ]);
      // Separiamo i viaggi attivi da quelli completati nello stato locale
      setTrips(tripsRes.data.filter(t => t.status !== 'COMPLETED'));
      setHistory(historyRes.data);
    } catch {
      setError('Errore nel caricamento dei viaggi');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  // --- Filtraggio lato client --------------------------------------
  const applyFilters = (list) =>
    list.filter(t => {
      // La ricerca funziona sia sul nome che sulla città
      const matchSearch =
        t.name.toLowerCase().includes(search.toLowerCase()) ||
        t.city.toLowerCase().includes(search.toLowerCase());
      // Se statusFilter è vuoto mostra tutti, altrimenti filtra per stato
      const matchStatus = !statusFilter || t.status === statusFilter;
      return matchSearch && matchStatus;
    });
  
  // Vista "Tutti" mostra tutti i viaggi (attivi + storico)
  const allTrips = applyFilters([...trips, ...history]);

  // Preferiti e Storico applicano solo il filtro di ricerca
  const favoriteTrips = trips.filter(t => t.status === 'STARRED').filter(t =>
    t.name.toLowerCase().includes(search.toLowerCase()) ||
    t.city.toLowerCase().includes(search.toLowerCase())
  );
  const historyTrips = history.filter(t =>
    t.name.toLowerCase().includes(search.toLowerCase()) ||
    t.city.toLowerCase().includes(search.toLowerCase())
  );

  const currentList =
    activeTab === 'favorites' ? favoriteTrips :
    activeTab === 'history'   ? historyTrips  : allTrips;

  // ---- AZIONI ---------------------------------------------------
  
  // Eliminazione viaggio
  const handleDelete = async () => {
    try {
      await deleteTrip(deleteModal.id);
      // Aggiorna lo stato locale rimuovendo il viaggio eliminato
      setTrips(prev => prev.filter(t => t.id !== deleteModal.id));
      setHistory(prev => prev.filter(t => t.id !== deleteModal.id));
    } catch { /* errore silenziato */ }
    setDeleteModal(null);
  };
  
  // Gestione preferiti
  const handleToggleFavorite = async (trip) => {
    try {
      const res = trip.status === 'STARRED'
        ? await removeTripFromFavorites(trip.id)
        : await saveTripToFavorites(trip.id);
      const updated = res.data;
      setTrips(prev => prev.map(t => t.id === updated.id ? updated : t));
    } catch { }
  };

  // Marca come completato
  const handleComplete = async (trip) => {
    try {
      const res = await completeTrip(trip.id);
      const updated = res.data;
      setTrips(prev => prev.filter(t => t.id !== updated.id));
      setHistory(prev => [updated, ...prev]);
    } catch { }
  };
  
  // Ripristina un viaggio dallo storico a "SAVED"
  const handleRestore = async (trip) => {
      try {
        const res = await restoreTrip(trip.id);
        const updated = res.data;
        setHistory(prev => prev.filter(t => t.id !== updated.id));
        setTrips(prev => [updated, ...prev]);
      } catch { }
  };

  const statuses = [
    { value: 'SAVED',     label: 'In programma' },
    { value: 'STARRED',   label: 'Preferiti' },
    { value: 'COMPLETED', label: 'Completati' },
  ];

  return (
    <div className="trips-page">
      <Header />

      <div className="page-content">

        {/* Intestazione pagina con titolo e pulsante nuovo viaggio */}
        <div className="trips-top">
          <div>
            <h1 className="page-title">I miei viaggi</h1>
            <p className="page-subtitle">{trips.length + history.length} itinerari creati</p>
          </div>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            + Nuovo viaggio
          </button>
        </div>
    
        {/* Tab navigation */}
        <div className="tab-nav">
          <button
            className={`tab-btn ${activeTab === 'all' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('all')}
          >
            Tutti ({trips.length + history.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'favorites' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('favorites')}
          >
            ★ Preferiti ({trips.filter(t => t.status === 'STARRED').length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'history' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            Storico ({history.length})
          </button>
        </div>

        {/* Barra di ricerca e Filtri */}
        <div className="trips-filters">
          <input
            id="input-search-trip"
            type="text"
            className="form-input"
            placeholder="Cerca per nome o città..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ flex: 1 }}
          />
          {activeTab === 'all' && (
            <select
              id="select-status-filter"
              className="form-input"
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              style={{ maxWidth: 180, appearance: 'none', cursor: 'pointer' }}
            >
              <option value="">Tutti gli stati</option>
              {statuses.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          )}
        </div>

        {loading && (
          <div className="loading-center"><div className="spinner" /><span>Caricamento...</span></div>
        )}

        {error && <div className="error-msg">{error}</div>}

        {/* Stato vuoto */}
        {!loading && !error && currentList.length === 0 && (
          <div className="empty-state" style={{ marginTop: 40 }}>
            <div className="empty-icon">
              {activeTab === 'favorites' ? '★' : activeTab === 'history' ? '📋' : '—'}
            </div>
            <p style={{ fontSize: '1rem', fontWeight: 600 }}>
              {activeTab === 'favorites' ? 'Nessun preferito ancora'
               : activeTab === 'history' ? 'Nessun viaggio completato'
               : 'Nessun viaggio trovato'}
            </p>
            {activeTab === 'all' && (
              <button className="btn btn-primary" style={{ marginTop: 14 }} onClick={() => navigate('/')}>
                Inizia ora
              </button>
            )}
          </div>
        )}

        {/* Griglia di card */}
        <div className="trips-grid">
          {currentList.map(trip => (
            <TripCard
              key={trip.id}
              trip={trip}
              onDelete={t => setDeleteModal(t)}
              onToggleFavorite={handleToggleFavorite}
              onComplete={handleComplete}
              onRestore={handleRestore}
              onClick={() => navigate(`/itinerary/${trip.id}`)}
              activeTab={activeTab}
            />
          ))}
        </div>
      </div>

      {/* Modal di conferma eliminazione */}
      {deleteModal && (
        <ConfirmDeleteModal
          tripName={deleteModal.name}
          onConfirm={handleDelete}
          onClose={() => setDeleteModal(null)}
        />
      )}

      {/* Stili CSS integrati */}
      <style>{`
        .trips-page { min-height: 100vh; background: var(--bg); }
        .trips-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; gap: 16px; flex-wrap: wrap; }
        
        /* Tab navigation */
        .tab-nav { display: flex; gap: 4px; border-bottom: 2px solid var(--border); margin-bottom: 20px; }
        .tab-btn { padding: 8px 18px; border: none; border-bottom: 2px solid transparent; background: transparent; cursor: pointer; font-size: 0.875rem; font-weight: 500; color: var(--text-muted); margin-bottom: -2px; transition: color 0.15s, border-color 0.15s; }
        .tab-btn:hover { color: var(--text); }
        .tab-active { color: #2563eb !important; border-bottom-color: #2563eb !important; }
        
        .trips-filters { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }
        .trips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
        
        /* Card Styles */
        .trip-card { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); padding: 18px; cursor: pointer; transition: all 0.15s; display: flex; flex-direction: column; gap: 10px; }
        .trip-card:hover { border-color: #bfdbfe; box-shadow: 0 2px 12px rgba(37,99,235,0.1); }
        .trip-card-header { display: flex; justify-content: space-between; align-items: center; }
        .trip-card-city { font-size: 0.78rem; color: var(--text-muted); font-weight: 500; }
        .trip-card-name { font-size: 0.975rem; font-weight: 700; line-height: 1.3; }
        .trip-card-stats { display: flex; gap: 12px; flex-wrap: wrap; }
        .trip-stat { font-size: 0.8rem; color: var(--text-muted); }
        
        .trip-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 2px; padding-top: 10px; border-top: 1px solid var(--border); }
        .trip-card-date { font-size: 0.75rem; color: var(--text-dim); }

        .trip-card-actions { display: flex; gap: 8px; flex-wrap: wrap; }

        /* Status & Buttons */
        .badge-star { background: #fef3c7; color: #d97706; border: 1px solid #fcd34d; padding: 2px 8px; border-radius: 999px; font-size: 0.72rem; font-weight: 600; }
        .btn-star { background: transparent; border: 1px solid var(--border); color: var(--text-muted); border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }
        .btn-star-active { background: #fef3c7; border: 1px solid #fcd34d; color: #d97706; border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }
        .btn-success { background: #16a34a; color: white; border: none; border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }
        
        /* Stile Modifica (da branch develop) */
        .btn-edit { background-color: #f7f7f7; color: #0011ff; border: 1px solid #0011ff; transition: all 0.2s; }
        .btn-edit:hover { background-color: #0011ff; color: #ffffff; border-color: #0011ff; }
      `}</style>
    </div>
  );
}