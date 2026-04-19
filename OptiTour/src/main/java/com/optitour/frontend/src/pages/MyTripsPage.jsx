import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Header from '../components/Header';
import {
  getTripsByUser,
  deleteTrip,
  publishTrip,
  unpublishTrip,
  saveTripToFavorites,
  removeTripFromFavorites,
  completeTrip,
  getTripHistory,
  restoreTrip,
} from '../services/api';

// --- Funzioni di utilità ------------------------------------------------

function formatDuration(seconds) {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}min` : `${m} min`;
}

function formatDate(isoString) {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
}

// --- Modal conferma eliminazione  ------------------------------------------------

function ConfirmDeleteModal({ tripName, onConfirm, onClose }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
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

// --- Badge stato  ------------------------------------------------

function StatusBadge({ status }) {
  const map = {
    SAVED:     { cls: 'badge-accent',  label: 'In programma' },
    STARRED:   { cls: 'badge-star',    label: '★ Preferito' },
    COMPLETED: { cls: 'badge-green',   label: 'Completato' },
    DRAFT:     { cls: 'badge-muted',   label: 'Bozza' },
  };
  const s = map[status] || { cls: 'badge-muted', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

// --- Menu a 3 punti ------------------------------------------------

function ThreeDotMenu({ trip, onDelete, onEdit, isCompleted }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  // Chiude il menu cliccando fuori
  useEffect(() => {
    const handleClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  return (
    <div ref={ref} style={{ position: 'relative' }} onClick={e => e.stopPropagation()}>
      <button
        className="btn-three-dot"
        onClick={() => setOpen(v => !v)}
        title="Opzioni"
        aria-label="Opzioni viaggio"
		onMouseEnter={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#2563eb';}}
      >
        ≡
      </button>

      {open && (
        <div className="three-dot-dropdown">
          {/* Modifica: solo per viaggi non completati */}
          {!isCompleted && (
            <button
              className="three-dot-item"
              onClick={() => { setOpen(false); onEdit(trip); }}
            >
              ✎ Modifica
            </button>
          )}
          <button
            className="three-dot-item three-dot-item--danger"
            onClick={() => { setOpen(false); onDelete(trip); }}
          >
            ✖ Elimina
          </button>
        </div>
      )}
    </div>
  );
}

// --- Card singolo viaggio  ------------------------------------------------

function TripCard({ trip, onDelete, onToggleFavorite, onComplete, onRestore, onPublish, onClick, activeTab }) {
  const navigate = useNavigate();

  const totalVisitMin = (trip.stages || []).reduce(
    (acc, s) => acc + (s.visitDurationMinutes || 0), 0,
  );
  const isStarred   = trip.status === 'STARRED';
  const isCompleted = trip.status === 'COMPLETED';

  return (
    <div
      id={`trip-card-${trip.id}`}
      className="trip-card"
      onClick={onClick}
    >
      {/* Header card: città  + badge + menu 3 punti */}
      <div className="trip-card-header">
        <span className="trip-card-city">{trip.city}</span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <StatusBadge status={trip.status} />
          <ThreeDotMenu
            trip={trip}
            isCompleted={isCompleted}
            onDelete={onDelete}
            onEdit={t => navigate(`/edit-trip/${t.id}`)}
          />
        </div>
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

      {trip.isPublic && (
        <div style={{ fontSize: '0.75rem', color: 'var(--accent)', fontWeight: 500 }}>
          🌐Pubblicato nel catalogo
        </div>
      )}

      {/* Azioni inline: Preferiti e Completa */}
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

      {/* Ripristina solo storico */}
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

      {/* Footer: data + pubblica */}
      <div className="trip-card-footer">
        <span className="trip-card-date">{formatDate(trip.createdAt)}</span>
        <div style={{ display: 'flex', gap: '6px' }} onClick={e => e.stopPropagation()}>
          {trip.status !== 'DRAFT' && (
			<button
			  id={`btn-publish-${trip.id}`}
			  className={`btn btn-sm ${trip.isPublic ? 'btn-secondary' : 'btn-ghost'}`}
			  title={trip.isPublic ? 'Rimuovi dal catalogo pubblico' : 'Pubblica nel catalogo pubblico'}
			  onClick={() => onPublish(trip)}
			>
			  {trip.isPublic ? (
			    <span className="btn-unpublish-text">🚫 Annulla pubblicazione </span>
			  ) : (
			    <span className="btn-publish-text">🌐 Pubblica viaggio </span>
			  )}
			</button>
          )}
        </div>
      </div>
    </div>
  );
}

// --- Pagina principale  ------------------------------------------------

export default function MyTripsPage() {

  const { user } = useAuth();
  const navigate = useNavigate();

  const [trips,   setTrips]   = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [publishError, setPublishError] = useState('');
  const [search,       setSearch]       = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [activeTab,    setActiveTab]    = useState('all');
  const [deleteModal,  setDeleteModal]  = useState(null);

  // --- Caricamento dati ------------------------------------------------
  const loadData = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const [tripsRes, historyRes] = await Promise.all([
        getTripsByUser(user.id),
        getTripHistory(),
      ]);
      setTrips(tripsRes.data.filter(t => t.status !== 'COMPLETED'));
      setHistory(historyRes.data);
    } catch {
      setError('Errore nel caricamento dei viaggi');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  // --- Filtraggio ------------------------------------------------
  const applyFilters = (list) =>
    list.filter(t => {
      const matchSearch =
        t.name.toLowerCase().includes(search.toLowerCase()) ||
        t.city.toLowerCase().includes(search.toLowerCase());
      const matchStatus = !statusFilter || t.status === statusFilter;
      return matchSearch && matchStatus;
    });

  const allTrips      = applyFilters([...trips, ...history]);
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

  // --- Azioni ------------------------------------------------
  const handleDelete = async () => {
    try {
      await deleteTrip(deleteModal.id);
      setTrips(prev => prev.filter(t => t.id !== deleteModal.id));
      setHistory(prev => prev.filter(t => t.id !== deleteModal.id));
    } catch { }
    setDeleteModal(null);
  };

  const handleToggleFavorite = async (trip) => {
    try {
      const res = trip.status === 'STARRED'
        ? await removeTripFromFavorites(trip.id)
        : await saveTripToFavorites(trip.id);
      const updated = res.data;
      setTrips(prev => prev.map(t => t.id === updated.id ? updated : t));
    } catch { }
  };

  const handleComplete = async (trip) => {
    try {
      const res = await completeTrip(trip.id);
      const updated = res.data;
      setTrips(prev => prev.filter(t => t.id !== updated.id));
      setHistory(prev => [updated, ...prev]);
    } catch { }
  };

  const handleRestore = async (trip) => {
    try {
      const res = await restoreTrip(trip.id);
      const updated = res.data;
      setHistory(prev => prev.filter(t => t.id !== updated.id));
      setTrips(prev => [updated, ...prev]);
    } catch { }
  };

  const handlePublish = async (trip) => {
    setPublishError('');
    try {
      const res = trip.isPublic
        ? await unpublishTrip(trip.id)
        : await publishTrip(trip.id);
      setTrips(prev => prev.map(t => t.id === trip.id
        ? { ...t, isPublic: res.data.isPublic, publishedAt: res.data.publishedAt } : t));
      setHistory(prev => prev.map(t => t.id === trip.id
        ? { ...t, isPublic: res.data.isPublic, publishedAt: res.data.publishedAt } : t));
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore durante la pubblicazione';
      setPublishError(msg);
      setTimeout(() => setPublishError(''), 4000);
    }
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
          <button className={`tab-btn ${activeTab === 'all'       ? 'tab-active' : ''}`} onClick={() => setActiveTab('all')}>
            Tutti ({trips.length + history.length})
          </button>
          <button className={`tab-btn ${activeTab === 'favorites' ? 'tab-active' : ''}`} onClick={() => setActiveTab('favorites')}>
            ★ Preferiti ({trips.filter(t => t.status === 'STARRED').length})
          </button>
          <button className={`tab-btn ${activeTab === 'history'   ? 'tab-active' : ''}`} onClick={() => setActiveTab('history')}>
            Storico ({history.length})
          </button>
        </div>

        {/* Filtri */}
        <div className="trips-filters">
          <input
            id="input-search-trip"
            type="text"
            className="form-input"
            placeholder="Cerca per nome o città ..."
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

        {loading && <div className="loading-center"><div className="spinner" /><span>Caricamento...</span></div>}
        {error        && <div className="error-msg">{error}</div>}
        {publishError && <div className="error-msg">{publishError}</div>}

        {/* Stato vuoto */}
        {!loading && !error && currentList.length === 0 && (
          <div className="empty-state" style={{ marginTop: 40 }}>
            <div className="empty-icon">
              {activeTab === 'favorites' ? '☆' : activeTab === 'history' ? '🕒 ' : ' ✖ '}
            </div>
            <p style={{ fontSize: '1rem', fontWeight: 600 }}>
              {activeTab === 'favorites' ? 'Nessun viaggio nei preferiti'
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

        {/* Griglia card */}
        <div className="trips-grid">
          {currentList.map(trip => (
            <TripCard
              key={trip.id}
              trip={trip}
              onDelete={t => setDeleteModal(t)}
              onToggleFavorite={handleToggleFavorite}
              onComplete={handleComplete}
              onRestore={handleRestore}
              onPublish={handlePublish}
              onClick={() => navigate(`/itinerary/${trip.id}`)}
              activeTab={activeTab}
            />
          ))}
        </div>
      </div>

      {/* Modal eliminazione */}
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

        /* Tab */
        .tab-nav { display: flex; gap: 4px; border-bottom: 2px solid var(--border); margin-bottom: 20px; }
        .tab-btn { padding: 8px 18px; border: none; border-bottom: 2px solid transparent; background: transparent; cursor: pointer; font-size: 0.875rem; font-weight: 500; color: var(--text-muted); margin-bottom: -2px; transition: color 0.15s, border-color 0.15s; }
        .tab-btn:hover { color: var(--text); }
        .tab-active { color: #2563eb !important; border-bottom-color: #2563eb !important; }

        .trips-filters { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }
        .trips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }

        /* Card */
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

        /* Badges & Buttons */
        .badge-star { background: #fef3c7; color: #d97706; border: 1px solid #fcd34d; padding: 2px 8px; border-radius: 999px; font-size: 0.72rem; font-weight: 600; }
        .btn-star { background: transparent; border: 1px solid var(--border); color: var(--text-muted); border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }
        .btn-star-active { background: #fef3c7; border: 1px solid #fcd34d; color: #d97706; border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }
        .btn-success { background: #16a34a; color: white; border: none; border-radius: var(--radius); padding: 4px 10px; font-size: 0.78rem; cursor: pointer; }

		/* Testo azzurro per "Pubblica" */
		.btn-publish-text {
		  color: #2563eb !important;
		  font-weight: 600;
		}
		
		/* Testo rosso per "Annulla pubblicazione" */
		.btn-unpublish-text {
		  color: #dc2626 !important;
		  font-weight: 600;
		}
		
		.btn-publish-text {
		  color: #2563eb !important;
		  font-weight: 600;
		  border: 1px solid var(--border);
		  padding: 2px 8px;
		  border-radius: 5px;
		  display: inline-block; /* fondamentale */
		}


		/*  Menu 3 punti */
		.btn-three-dot {
		  background: transparent;
		  border: 1px solid var(--border);   /* ✔ aggiunto solo questo */
		  cursor: pointer;
		  font-size: 1.2rem;
		  line-height: 1;
		  color: #2563eb;
		  padding: 2px 6px;
		  border-radius: var(--radius);
		  transition: background 0.15s, color 0.15s, border-color 0.15s;
		}

		.btn-three-dot:hover {
		  background: var(--border);
		  color: var(--text);
		  border-color: var(--text-muted);   /* ✔ bordo più visibile in hover */
		}

		.three-dot-dropdown {
		  position: absolute;
		  top: calc(100% + 4px);
		  right: 0;
		  background: #fff;
		  border: 1px solid var(--border);
		  border-radius: var(--radius);
		  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
		  min-width: 140px;
		  z-index: 100;
		  overflow: hidden;
		}

		.three-dot-item {
		  display: block;
		  width: 100%;
		  padding: 9px 14px;
		  text-align: left;
		  background: transparent;
		  border: none;
		  cursor: pointer;
		  font-size: 0.85rem;
		  color: var(--text);
		  transition: background 0.12s;
		}

		.three-dot-item:hover {
		  background: #f1f5f9;
		}

		.three-dot-item--danger {
		  color: #dc2626;
		}

		.three-dot-item--danger:hover {
		  background: #fef2f2;
		}

      `}</style>
    </div>
  );
}
