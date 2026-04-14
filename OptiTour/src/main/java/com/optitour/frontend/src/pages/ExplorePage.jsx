import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import { getPublicTrips } from '../services/api';

// Converte secondi in formato leggibile
function formatDuration(seconds) {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}min` : `${m} min`;
}

// Converte una data ISO in formato italiano
function formatDate(isoString) {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
}

// ─── Componente principale: Catalogo viaggi pubblici ──────────────────
export default function ExplorePage() {
  const navigate = useNavigate();

  // Lista di tutti i viaggi pubblici
  const [trips, setTrips] = useState([]);

  // true mentre i dati arrivano dal backend
  const [loading, setLoading] = useState(true);

  // Eventuale messaggio di errore
  const [error, setError] = useState('');

  // Testo nella barra di ricerca
  const [search, setSearch] = useState('');

  // Filtro per città
  const [cityFilter, setCityFilter] = useState('');

  // Carica i viaggi pubblici all'apertura della pagina
  useEffect(() => {
    getPublicTrips()
      .then(res => setTrips(res.data))
      .catch(() => setError('Errore nel caricamento del catalogo'))
      .finally(() => setLoading(false));
  }, []);

  // Filtraggio lato client su nome, città e filtro città selezionato
  const filtered = trips.filter(t => {
    const matchSearch =
      t.name.toLowerCase().includes(search.toLowerCase()) ||
      t.city.toLowerCase().includes(search.toLowerCase());
    const matchCity = !cityFilter || t.city === cityFilter;
    return matchSearch && matchCity;
  });

  // Città uniche per il filtro a tendina
  const cities = [...new Set(trips.map(t => t.city).filter(Boolean))].sort();

  return (
    <div className="explore-page">
      <Header />

      <div className="page-content">

        {/* Intestazione */}
        <div className="trips-top">
          <div>
            <h1 className="page-title">Esplora itinerari</h1>
            <p className="page-subtitle">
              {trips.length} viaggi condivisi dalla community
            </p>
          </div>
        </div>

        {/* Barra ricerca + filtro città */}
        <div className="trips-filters">
          <input
            id="input-explore-search"
            type="text"
            className="form-input"
            placeholder="Cerca per nome o città..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ flex: 1 }}
          />
          <select
            id="select-city-filter"
            className="form-input"
            value={cityFilter}
            onChange={e => setCityFilter(e.target.value)}
            style={{ maxWidth: 180, appearance: 'none', cursor: 'pointer' }}
          >
            <option value="">Tutte le città</option>
            {cities.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        {/* Spinner */}
        {loading && (
          <div className="loading-center">
            <div className="spinner" /><span>Caricamento...</span>
          </div>
        )}

        {/* Errore */}
        {error && <div className="error-msg">{error}</div>}

        {/* Stato vuoto */}
        {!loading && !error && filtered.length === 0 && (
          <div className="empty-state" style={{ marginTop: 40 }}>
            <div className="empty-icon">🗺️</div>
            <p style={{ fontSize: '1rem', fontWeight: 600 }}>Nessun itinerario trovato</p>
            <p style={{ fontSize: '0.875rem' }}>
              {search || cityFilter
                ? 'Prova a cambiare i filtri di ricerca'
                : 'Non ci sono ancora itinerari pubblici. Sii il primo a condividere il tuo!'}
            </p>
            <button
              className="btn btn-primary"
              style={{ marginTop: 14 }}
              onClick={() => navigate('/my-trips')}
            >
              I miei viaggi
            </button>
          </div>
        )}

        {/* Griglia di card */}
        <div className="trips-grid">
          {filtered.map(trip => {
            const totalVisitMin = (trip.stages || []).reduce(
              (acc, s) => acc + (s.visitDurationMinutes || 0), 0
            );
            return (
              <div
                key={trip.id}
                id={`explore-card-${trip.id}`}
                className="trip-card explore-card"
                onClick={() => navigate(`/itinerary/${trip.id}`)}
              >
                {/* Riga superiore: città + badge pubblico */}
                <div className="trip-card-header">
                  <span className="trip-card-city">{trip.city}</span>
                  <span className="badge badge-accent" style={{ fontSize: '0.7rem' }}>
                    🌐 Pubblico
                  </span>
                </div>

                {/* Nome del viaggio */}
                <h3 className="trip-card-name">{trip.name}</h3>

                {/* Autore */}
                {trip.authorUsername && (
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                    di <strong style={{ color: 'var(--text-muted)' }}>{trip.authorUsername}</strong>
                  </span>
                )}

                {/* Statistiche */}
                <div className="trip-card-stats">
                  <span className="trip-stat">
                    {(trip.stages || []).length} tappe
                  </span>
                  {totalVisitMin > 0 && (
                    <span className="trip-stat">
                      {Math.floor(totalVisitMin / 60)}h {totalVisitMin % 60}min visita
                    </span>
                  )}
                  {trip.totalDurationSeconds != null && (
                    <span className="trip-stat">
                      {formatDuration(trip.totalDurationSeconds)} percorso
                    </span>
                  )}
                </div>

                {/* Footer: data pubblicazione */}
                <div className="trip-card-footer">
                  <span className="trip-card-date">
                    Pubblicato {formatDate(trip.publishedAt || trip.createdAt)}
                  </span>
                  <span className="btn btn-ghost btn-sm" style={{ pointerEvents: 'none' }}>
                    Visualizza →
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <style>{`
        .explore-page { min-height: 100vh; background: var(--bg); }
        .trips-top {
          display: flex; justify-content: space-between;
          align-items: flex-start; margin-bottom: 20px;
          gap: 16px; flex-wrap: wrap;
        }
        .trips-filters { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }
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
        .explore-card:hover {
          border-color: #a5f3c3;
          box-shadow: 0 2px 12px rgba(16,185,129,0.1);
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
