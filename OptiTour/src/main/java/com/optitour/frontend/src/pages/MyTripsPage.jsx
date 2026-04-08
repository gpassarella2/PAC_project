import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; // per navigare tra le pagine
import { useAuth } from '../context/AuthContext'; // per ottenere l'utente loggato
import Header from '../components/Header';
import { getTripsByUser, deleteTrip } from '../services/api'; // chiamate al backend

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
    DRAFT:     { cls: 'badge-muted',  label: 'Bozza' },
    SAVED: { cls: 'badge-accent', label: 'Ottimizzato' },
    COMPLETED: { cls: 'badge-green',  label: 'Completato' },
  };
  // Se lo stato non è nella mappa, usa badge grigio con il valore grezzo
  const s = map[status] || { cls: 'badge-muted', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

// ─── Componente principale: Pagina "I miei viaggi" ─────────────────────
export default function MyTripsPage() {

  // Prende l'utente loggato dal context (contiene l'id necessario per caricare i suoi viaggi)
  const { user } = useAuth();

  // per navigare verso altre pagine
  const navigate = useNavigate();

  // Lista completa dei viaggi caricati dal backend
  const [trips, setTrips] = useState([]);

  // true mentre i dati stanno arrivando dal backend, false quando sono pronti
  const [loading, setLoading] = useState(true);

  // Messaggio di errore da mostrare se la chiamata API fallisce
  const [error, setError] = useState('');

  // Testo digitato nella barra di ricerca
  const [search, setSearch] = useState('');

  // Valore selezionato nel filtro per stato (es. "DRAFT", "SAVED" o "" per tutti)
  const [statusFilter, setStatusFilter] = useState('');

  // Viaggio selezionato per eliminazione — null se finestra è chiusa, oggetto trip se è aperta
  const [deleteModal, setDeleteModal] = useState(null);

  // ─── Caricamento dati all'apertura della pagina ──────────────────────
  // useEffect con [user] significa: esegui quando il componente appare
  // e ogni volta che cambia l'oggetto user
  useEffect(() => {
    // Se l'utente non è ancora disponibile (es. durante il login) non fare nulla
    if (!user?.id) return;

    getTripsByUser(user.id)
      .then(res => setTrips(res.data))       // salva i viaggi nello stato
      .catch(() => setError('Errore nel caricamento dei viaggi')) // mostra errore se fallisce
      .finally(() => setLoading(false));     // in ogni caso, smetti di mostrare il loader
  }, [user]);

  // ─── Filtraggio lato client ──────────────────────────────────────────
  // Non chiama il backend ogni volta — filtra l'array già in memoria.
  // Viene ricalcolato automaticamente ogni volta che cambia trips, search o statusFilter.
  const filtered = trips.filter(t => {
    // La ricerca funziona sia sul nome che sulla città
    const matchSearch =
      t.name.toLowerCase().includes(search.toLowerCase()) ||
      t.city.toLowerCase().includes(search.toLowerCase());
    // Se statusFilter è vuoto ("") mostra tutti, altrimenti filtra per stato
    const matchStatus = !statusFilter || t.status === statusFilter;
    return matchSearch && matchStatus;
  });

  // ─── Eliminazione viaggio ────────────────────────────────────────────
  // Chiamata quando l'utente conferma nel modal di eliminazione
  const handleDelete = async () => {
    try {
      await deleteTrip(deleteModal.id); // chiama il backend per eliminare
      // Aggiorna lo stato locale rimuovendo il viaggio eliminato, senza ricaricare tutto
      setTrips(prev => prev.filter(t => t.id !== deleteModal.id));
    } catch { /* se fallisce ignora silenziosamente — il viaggio rimane in lista */ }
    setDeleteModal(null); // chiude il modal in ogni caso
  };

  // Raccoglie tutti gli stati unici presenti nei viaggi per popolare il filtro a tendina.
  // Set() elimina i duplicati, filter(Boolean) rimuove i valori null/undefined
  const statuses = [...new Set(trips.map(t => t.status).filter(Boolean))];

  // ─── Rendering ───────────────────────────────────────────────────────
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

        {/* Barra ricerca + filtro per stato */}
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
          <select
            id="select-status-filter"
            className="form-input"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ maxWidth: 180, appearance: 'none', cursor: 'pointer' }}
          >
            <option value="">Tutti gli stati</option>
            {/* Genera un'opzione per ogni stato trovato nei viaggi */}
            {statuses.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>

        {/* Spinner di caricamento — visibile solo mentre loading è true */}
        {loading && (
          <div className="loading-center"><div className="spinner" /><span>Caricamento...</span></div>
        )}

        {/* Messaggio di errore — visibile solo se error è non vuoto */}
        {error && <div className="error-msg">{error}</div>}

        {/* Stato vuoto — mostrato quando il caricamento è finito ma non ci sono risultati */}
        {!loading && !error && filtered.length === 0 && (
          <div className="empty-state" style={{ marginTop: 40 }}>
            <div className="empty-icon">—</div>
            <p style={{ fontSize: '1rem', fontWeight: 600 }}>Nessun viaggio trovato</p>
            <p style={{ fontSize: '0.875rem' }}>Crea il tuo primo itinerario dalla home</p>
            <button className="btn btn-primary" style={{ marginTop: 14 }} onClick={() => navigate('/')}>
              Inizia ora
            </button>
          </div>
        )}

        {/* Griglia di card — una per ogni viaggio filtrato */}
        <div className="trips-grid">
          {filtered.map(trip => {
            // Calcola il tempo totale di visita sommando i minuti di ogni tappa
            const totalVisitMin = (trip.stages || []).reduce((acc, s) => acc + (s.visitDurationMinutes || 0), 0);
            return (
              // Cliccando sulla card si naviga alla pagina itinerario di quel viaggio
              <div
                key={trip.id}
                id={`trip-card-${trip.id}`}
                className="trip-card"
                onClick={() => navigate(`/itinerary/${trip.id}`)}
              >
                {/* Riga superiore: città + badge stato */}
                <div className="trip-card-header">
                  <span className="trip-card-city">{trip.city}</span>
                  <StatusBadge status={trip.status} />
                </div>

                {/* Nome del viaggio */}
                <h3 className="trip-card-name">{trip.name}</h3>

                {/* Statistiche: numero tappe, tempo visita, durata percorso */}
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

                {/* Footer card: data creazione + pulsante elimina */}
                <div className="trip-card-footer">
                  <span className="trip-card-date">{formatDate(trip.createdAt)}</span>
                  <button
                    id={`btn-delete-${trip.id}`}
                    className="btn btn-danger btn-sm"
                    // stopPropagation evita che il click sul pulsante attivi anche il click della card
                    onClick={e => { e.stopPropagation(); setDeleteModal(trip); }}
                  >
                    Elimina
                  </button>
                </div>
              </div>
            );
          })}
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
        .trips-page { min-height: 100vh; background: var(--bg); }
        .trips-top {
          display: flex; justify-content: space-between;
          align-items: flex-start; margin-bottom: 20px;
          gap: 16px; flex-wrap: wrap;
        }
        .trips-filters { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }
        /* Griglia responsive: le card vanno a capo automaticamente, minimo 280px ciascuna */
        .trips-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: 14px;
        }
        .trip-card {
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius);
          padding: 18px;
          cursor: pointer;
          transition: all 0.15s;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .trip-card:hover {
          border-color: #bfdbfe;
          box-shadow: 0 2px 12px rgba(37,99,235,0.1);
        }
        .trip-card-header { display: flex; justify-content: space-between; align-items: center; }
        .trip-card-city { font-size: 0.78rem; color: var(--text-muted); font-weight: 500; }
        .trip-card-name { font-size: 0.975rem; font-weight: 700; line-height: 1.3; }
        .trip-card-stats { display: flex; gap: 12px; flex-wrap: wrap; }
        .trip-stat { font-size: 0.8rem; color: var(--text-muted); }
        .trip-card-footer {
          display: flex; justify-content: space-between;
          align-items: center; margin-top: 2px;
          padding-top: 10px; border-top: 1px solid var(--border);
        }
        .trip-card-date { font-size: 0.75rem; color: var(--text-dim); }
      `}</style>
    </div>
  );
}
