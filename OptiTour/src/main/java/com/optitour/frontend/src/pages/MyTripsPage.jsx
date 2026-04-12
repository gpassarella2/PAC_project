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
//   tripName → nome del viaggio da mostrare nel messaggio
//   onConfirm → funzione chiamata se l'utente conferma
//   onClose → funzione chiamata se l'utente annulla o clicca fuori
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
// Mostra uno stato con colore diverso a seconda del valore.
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
  const totalVisitMin = (trip.stages || []).reduce(
    (acc, s) => acc + (s.visitDurationMinutes || 0), 0,
  );
  const isStarred    = trip.status === 'STARRED';
  const isCompleted  = trip.status === 'COMPLETED';

  return (
    <div
      key={trip.id}
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
        {totalVisitMin > 0 && (
          <span className="trip-stat">
            {Math.floor(totalVisitMin / 60)}h {totalVisitMin % 60}min visita
          </span>
        )}
        {trip.totalDurationSeconds != null && (
          <span className="trip-stat">{formatDuration(trip.totalDurationSeconds)} percorso</span>
        )}
      </div>

      {/* Azioni inline */}
      {!isCompleted && (
        <div className="trip-card-actions" onClick={e => e.stopPropagation()}>
          {/* Preferiti */}
          <button
            id={`btn-fav-${trip.id}`}
            className={`btn btn-sm ${isStarred ? 'btn-star-active' : 'btn-star'}`}
            title={isStarred ? 'Rimuovi dai preferiti' : 'Aggiungi ai preferiti'}
            onClick={() => onToggleFavorite(trip)}
          >
            {isStarred ? '★ Preferito' : '☆ Preferiti'}
          </button>

          {/* Completa viaggio */}
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
	  
	  {/* Bottone ripristina — visibile solo nella tab Storico */}
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

      <div className="trip-card-footer">
        <span className="trip-card-date">{formatDate(trip.createdAt)}</span>
        <button
          id={`btn-delete-${trip.id}`}
          className="btn btn-danger btn-sm"
          onClick={e => { e.stopPropagation(); onDelete(trip); }}
        >
          Elimina
        </button>
      </div>
    </div>
  );
}

// ---- Componente principale: Pagina "I miei viaggi" -----------------------------
export default function MyTripsPage() {

  const { user } = useAuth(); // Prende l'utente loggato dal context (contiene l'id necessario per caricare i suoi viaggi)
  
  const navigate = useNavigate(); // per navigare verso altre pagine
  
  //--- Dati ---

  const [trips, setTrips] = useState([]);   // Lista completa dei viaggi caricati dal backend
  const [history, setHistory] = useState([]);

  // --- UI state ---

  const [loading, setLoading] = useState(true); // true mentre i dati stanno arrivando dal backend, false quando sono pronti
  const [error, setError] = useState(''); // Messaggio di errore da mostrare se la chiamata API fallisce
  const [search, setSearch] = useState(''); // Testo digitato nella barra di ricerca
  const [statusFilter, setStatusFilter] = useState(''); // Valore selezionato nel filtro per stato (es. "DRAFT", "SAVED" o "" per tutti)
  const [activeTab,     setActiveTab]     = useState('all'); // 'all' | 'favorites' | 'history'
  const [deleteModal, setDeleteModal] = useState(null); // Viaggio selezionato per eliminazione — null se finestra è chiusa, oggetto trip se è aperta
  
  // --- Caricamento dati all'apertura della pagina --------------------------------------
  // useEffect con [user] significa: esegui quando il componente appare
  // e ogni volta che cambia l'oggetto user
  
  const loadData = useCallback(async () => {
	// Se l'utente non è ancora disponibile (es. durante il login) non fare nulla
    if (!user?.id) return;
    setLoading(true);
    try {
      const [tripsRes, historyRes] = await Promise.all([
        getTripsByUser(user.id),
        getTripHistory(),
      ]);
      setTrips(tripsRes.data.filter(t => t.status !== 'COMPLETED')); // salva i viaggi nello stato
      setHistory(historyRes.data);
    } catch {
      setError('Errore nel caricamento dei viaggi'); // mostra errore se fallisce
    } finally {
      setLoading(false); // in ogni caso, smetti di mostrare il loader
    }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  // --- Filtraggio lato client --------------------------------------
  // Non chiama il backend ogni volta — filtra l'array già in memoria.
  // Viene ricalcolato automaticamente ogni volta che cambia trips, search o statusFilter.

  const applyFilters = (list) =>
    list.filter(t => {
	  // La ricerca funziona sia sul nome che sulla città
      const matchSearch =
        t.name.toLowerCase().includes(search.toLowerCase()) ||
        t.city.toLowerCase().includes(search.toLowerCase());
	  // Se statusFilter è vuoto ("") mostra tutti, altrimenti filtra per stato	
      const matchStatus = !statusFilter || t.status === statusFilter;
      return matchSearch && matchStatus;
    });
  
	// Vista "Tutti" mostra tutti i viaggi inclusi i completati (trips + history)
	const allTrips = applyFilters([...trips, ...history]);

	// Preferiti e Storico applicano solo il filtro di ricerca (per nome/città),
	// ignorando il filtro per stato — così starred e completed sono sempre visibili
	const favoriteTrips = trips.filter(t => t.status === 'STARRED').filter(t =>
	  t.name.toLowerCase().includes(search.toLowerCase()) ||
	  t.city.toLowerCase().includes(search.toLowerCase())
	);
	const historyTrips  = history.filter(t =>
	  t.name.toLowerCase().includes(search.toLowerCase()) ||
	  t.city.toLowerCase().includes(search.toLowerCase())
	);

  const currentList =
    activeTab === 'favorites' ? favoriteTrips :
    activeTab === 'history'   ? historyTrips  : allTrips;

  // ---- AZIONI ---------------------------------------------------
  
  // Eliminazione viaggio ----
  // Chiamata quando l'utente conferma nel modal di eliminazione
  const handleDelete = async () => {
    try {
      await deleteTrip(deleteModal.id); // chiama il backend per eliminare
      // Aggiorna lo stato locale rimuovendo il viaggio eliminato, senza ricaricare tutto
      setTrips(prev => prev.filter(t => t.id !== deleteModal.id));
	  setHistory(prev => prev.filter(t => t.id !== deleteModal.id));
    } catch { /* se fallisce ignora silenziosamente — il viaggio rimane in lista */ }
    setDeleteModal(null); // chiude il modal in ogni caso
  };
  
  // Aggiunge o rimuove il viaggio dai preferiti in base allo stato corrente.
  // Se il viaggio è già STARRED -> chiama removeTripFromFavorites (status torna a SAVED).
  // Se non lo è -> chiama saveTripToFavorites (status diventa STARRED).
  // In entrambi i casi aggiorna la card nella lista locale senza ricaricare tutto dal backend.
  const handleToggleFavorite = async (trip) => {
    try {
      const res = trip.status === 'STARRED'
        ? await removeTripFromFavorites(trip.id)
        : await saveTripToFavorites(trip.id);
      const updated = res.data;
      // Sostituisce il viaggio aggiornato nell'array mantenendo l'ordine degli altri
      setTrips(prev => prev.map(t => t.id === updated.id ? updated : t));
    } catch { /* ignora errori di rete: lo stato visivo rimane invariato */ }
  };

  // Marca il viaggio come COMPLETED chiamando il backend.
  // Una volta completato, il viaggio viene rimosso dalla lista principale (trips)
  // e aggiunto in cima allo storico (history), così appare subito nella tab "Storico".
  const handleComplete = async (trip) => {
    try {
      const res = await completeTrip(trip.id);
      const updated = res.data;
      // Rimuove il viaggio dalla lista "Tutti" perchè non è più attivo
      setTrips(prev => prev.filter(t => t.id !== updated.id));
      // Aggiunge il viaggio in cima allo storico (ordine cronologico inverso)
      setHistory(prev => [updated, ...prev]);
    } catch { /* ignora errori di rete: lo stato visualizzato rimane invariato */ }
  };
  
  const handleRestore = async (trip) => {
      try {
        const res = await restoreTrip(trip.id);
        const updated = res.data;
        // Rimuove il viaggio dallo storico
        setHistory(prev => prev.filter(t => t.id !== updated.id));
        // Lo reinserisce nella lista principale
        setTrips(prev => [updated, ...prev]);
      } catch { /* ignora errori di rete */ }
  };

  // Raccoglie tutti gli stati unici presenti nei viaggi per popolare il filtro a tendina.
  // Set() elimina i duplicati, filter(Boolean) rimuove i valori null/undefined
  const statuses = [
    { value: 'SAVED',     label: 'In programma' },
    { value: 'STARRED',   label: 'Preferiti' },
    { value: 'COMPLETED', label: 'Completati' },
  ];

  // --- Rendering --------------------------------------------------------
  return (
    <div className="trips-page">
      <Header />

      <div className="page-content">

        {/* Intestazione pagina con titolo e pulsante nuovo viaggio */}
        <div className="trips-top">
          <div>
            <h1 className="page-title">I miei viaggi</h1>
            <p className="page-subtitle">{trips.length} itinerari creati</p>
          </div>
          {/* Naviga alla home dove si sceglie la città per creare un nuovo viaggio */}
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            + Nuovo viaggio
          </button>
        </div>
		
		{/* Tab navigation */}
		<div className="tab-nav">
		  <button
		    className={`tab-btn ${activeTab === 'all'       ? 'tab-active' : ''}`}
		    onClick={() => setActiveTab('all')}
		  >
		    Tutti ({activeTab === 'all' ? currentList.length : trips.length + history.length})
		  </button>
		  <button
		    className={`tab-btn ${activeTab === 'favorites' ? 'tab-active' : ''}`}
		    onClick={() => setActiveTab('favorites')}
		  >
		    ★ Preferiti ({trips.filter(t => t.status === 'STARRED').length})
		  </button>
		  <button
		    className={`tab-btn ${activeTab === 'history'   ? 'tab-active' : ''}`}
		    onClick={() => setActiveTab('history')}
		  >
		    Storico ({history.length})
		  </button>
		</div>

        {/* Filtri dello stato del viaggio */}
        <div className="trips-filters">
          <input
            id="input-search-trip"
            type="text"
            className="form-input"
            placeholder="Cerca per nome o città..."
            value={search}
            onChange={e => setSearch(e.target.value)} // aggiorna lo stato ad ogni tasto premuto
            style={{ flex: 1 }}
          />
		   {activeTab === 'all' && ( // i filtri sono applicabili solo nella pagina all
		      <select
		        id="select-status-filter"
		        className="form-input"
		        value={statusFilter}
		        onChange={e => setStatusFilter(e.target.value)}
		        style={{ maxWidth: 180, appearance: 'none', cursor: 'pointer' }}
		      >
		        <option value="">Tutti</option>
		        {statuses.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
		      </select>
		    )}
		  </div>

        {/* Spinner di caricamento — visibile solo mentre loading è true */}
        {loading && (
          <div className="loading-center"><div className="spinner" /><span>Caricamento...</span></div>
        )}

        {/* Messaggio di errore — visibile solo se error è non vuoto */}
        {error && <div className="error-msg">{error}</div>}

        {/* Stato vuoto — mostrato quando il caricamento è finito ma non ci sono risultati */}
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
		    <p style={{ fontSize: '0.875rem' }}>
		      {activeTab === 'favorites'
		        ? 'Clicca ☆ su un viaggio per aggiungerlo ai preferiti'
		        : activeTab === 'history'
		        ? 'Completa un viaggio per vederlo qui'
		        : 'Crea il tuo primo itinerario dalla home'}
		    </p>
		    {activeTab === 'all' && (
		      <button
		        className="btn btn-primary"
		        style={{ marginTop: 14 }}
		        onClick={() => navigate('/')}
		      >
		        Inizia ora
		      </button>
		    )}
		  </div>
		)}

        {/* Griglia di card — una per ogni viaggio filtrato */}
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
			  activeTab={activeTab}  // ← aggiungi questa
			/>
		    ))}
		  </div>
		</div>

      {/* Modal di conferma eliminazione — renderizzato solo se deleteModal non è null */}
      {deleteModal && (
        <ConfirmDeleteModal
          tripName={deleteModal.name}
          onConfirm={handleDelete}
          onClose={() => setDeleteModal(null)}
        />
      )}

      {/* Stili CSS specifici di questa pagina */}
      <style>{`
		/* --- Pagina --- */
        .trips-page { min-height: 100vh; background: var(--bg); }
        .trips-top {
          display: flex; justify-content: space-between;
          align-items: flex-start; margin-bottom: 20px;
          gap: 16px; flex-wrap: wrap;
        }
		
		/* --- Tab --- */
		.tab-nav {
		  display: flex; gap: 4px;
		  border-bottom: 2px solid var(--border);
		  margin-bottom: 20px;
		}
		.tab-btn {
		  padding: 8px 18px;
		  border: none; border-bottom: 2px solid transparent;
		  background: transparent; cursor: pointer;
		  font-size: 0.875rem; font-weight: 500;
		  color: var(--text-muted);
		  margin-bottom: -2px;
		  transition: color 0.15s, border-color 0.15s;
		}
		.tab-btn:hover { color: var(--text); }
		.tab-active {
		  color: #2563eb !important;
		  border-bottom-color: #2563eb !important;
		}
		
		/* --- Filtri --- */
        .trips-filters { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }
		
		/* --- Griglia --- */
        /* Griglia responsive: le card vanno a capo automaticamente, minimo 280px ciascuna */
        .trips-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: 14px;
        }
		
        /* --- Card --- */
        .trip-card {
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius);
          padding: 18px;
          cursor: pointer;
          transition: all 0.15s;
          display: flex; flex-direction: column; gap: 10px;
        }
        .trip-card:hover {
          border-color: #bfdbfe;
          box-shadow: 0 2px 12px rgba(37,99,235,0.1);
        }
        .trip-card-header { display: flex; justify-content: space-between; align-items: center; }
        .trip-card-city   { font-size: 0.78rem; color: var(--text-muted); font-weight: 500; }
        .trip-card-name   { font-size: 0.975rem; font-weight: 700; line-height: 1.3; }
        .trip-card-stats  { display: flex; gap: 12px; flex-wrap: wrap; }
        .trip-stat        { font-size: 0.8rem; color: var(--text-muted); }
        .trip-card-footer {
          display: flex; justify-content: space-between; align-items: center;
          margin-top: 2px; padding-top: 10px; border-top: 1px solid var(--border);
        }
        .trip-card-date { font-size: 0.75rem; color: var(--text-dim); }

        /* --- Azioni inline --- */
        .trip-card-actions {
          display: flex; gap: 8px; flex-wrap: wrap;
        }

        /* --- Badge star --- */
        .badge-star {
          background: #fef3c7; color: #d97706;
          border: 1px solid #fcd34d;
          padding: 2px 8px; border-radius: 999px;
          font-size: 0.72rem; font-weight: 600;
        }

        /* --- Bottoni extra --- */
        .btn-star {
          background: transparent;
          border: 1px solid var(--border);
          color: var(--text-muted);
          border-radius: var(--radius);
          padding: 4px 10px; font-size: 0.78rem; cursor: pointer;
          transition: all 0.15s;
        }
        .btn-star:hover { border-color: #fcd34d; color: #d97706; }

        .btn-star-active {
          background: #fef3c7;
          border: 1px solid #fcd34d;
          color: #d97706;
          border-radius: var(--radius);
          padding: 4px 10px; font-size: 0.78rem; cursor: pointer;
          transition: all 0.15s;
        }
        .btn-star-active:hover { background: #fde68a; }

        .btn-success {
          background: #16a34a; color: white;
          border: none; border-radius: var(--radius);
          padding: 4px 10px; font-size: 0.78rem; cursor: pointer;
          transition: background 0.15s;
        }
        .btn-success:hover { background: #15803d; }
      `}</style>
    </div>
  );
}
