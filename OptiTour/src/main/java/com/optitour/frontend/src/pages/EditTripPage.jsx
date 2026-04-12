import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import {
  getTripById,
  getMonumentsByCity,
  updateTrip,
  optimizeTrip,
} from '../services/api';

// ─── Modal modifica tempo ─────────────────────
function EditTimeModal({ item, onConfirm, onClose }) {
  const [minutes, setMinutes] = useState(item.visitDurationMinutes);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <h2 className="modal-title">Modifica tempo di visita</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: 16, fontSize: '0.875rem' }}>
          Quanti minuti vuoi dedicare a <strong style={{ color: 'var(--text)' }}>{item.monument.name}</strong>?
        </p>
        <div className="form-group">
          <label className="form-label">Minuti di visita</label>
          <input
            type="number"
            min="5"
            max="480"
            step="5"
            className="form-input"
            value={minutes}
            onChange={e => setMinutes(Number(e.target.value))}
          />
        </div>
        <div className="visit-presets">
          {[30, 60, 90, 120].map(m => (
            <button
              key={m}
              className={`preset-btn ${minutes === m ? 'active' : ''}`}
              onClick={() => setMinutes(m)}
            >
              {m} min
            </button>
          ))}
        </div>
        <div className="modal-footer">
          <button className="btn btn-ghost" onClick={onClose}>Annulla</button>
          <button className="btn btn-primary" onClick={() => onConfirm(minutes)}>Salva</button>
        </div>
      </div>
    </div>
  );
}

// ─── Type badge ─────────────────────
function TypeBadge({ type }) {
  return (
    <span className="type-badge">
      {type || 'altro'}
    </span>
  );
}

// ─── Pagina principale ─────────────────────
export default function EditTripPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [trip, setTrip] = useState(null);
  const [monuments, setMonuments] = useState([]);
  const [selected, setSelected] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [editModal, setEditModal] = useState(null);

  // ─── Carica viaggio + monumenti ─────────────────────
  useEffect(() => {
    let tripData;

    getTripById(id).then(res => {
      tripData = res.data;
      setTrip(tripData);
      return getMonumentsByCity(tripData.city);
    })
    .then(res => {
      const allMonuments = res.data;
      setMonuments(allMonuments);

      setSelected(
        tripData.stages.map(s => {
          const monument = allMonuments.find(m => m.id === s.monumentId);
          return { monument, visitDurationMinutes: s.visitDurationMinutes };
        }).filter(s => s.monument != null)
      );
    })
    .finally(() => setLoading(false));
  }, [id]);

  // ─── Filtri ─────────────────────
  const types = [...new Set(monuments.map(m => m.type).filter(Boolean))];

  const filtered = monuments.filter(m => {
    const matchSearch = m.name.toLowerCase().includes(search.toLowerCase());
    const matchType = !typeFilter || m.type === typeFilter;
    return matchSearch && matchType;
  });

  // ─── Helpers ─────────────────────
  const isSelected = (monumentId) =>
    selected.some(s => s.monument.id === monumentId);

  const handleAdd = (monument) => {
    setSelected(prev => [...prev, { monument, visitDurationMinutes: 60 }]);
  };

  const handleRemove = (index) => {
    setSelected(prev => prev.filter((_, i) => i !== index));
  };

  const handleEditConfirm = (minutes) => {
    setSelected(prev =>
      prev.map((s, i) =>
        i === editModal.index ? { ...s, visitDurationMinutes: minutes } : s
      )
    );
    setEditModal(null);
  };

  const totalMinutes = selected.reduce((acc, s) => acc + s.visitDurationMinutes, 0);

  // ─── Salvataggio ─────────────────────
  const handleSave = async () => {
    setSaving(true);
    setSaveError('');
    try {
      const stages = selected.map(s => ({
        monumentId: s.monument.id,
        visitDurationMinutes: s.visitDurationMinutes,
      }));
      await updateTrip(id, { stages });
      await optimizeTrip(id);
      navigate(`/itinerary/${id}`);
    } catch (err) {
      setSaveError(err.response?.data?.message || 'Errore durante il salvataggio');
      setSaving(false);
    }
  };

  if (loading) return (
    <div className="mon-page">
      <Header />
      <div className="loading-center">
        <div className="spinner" />
        <span>Caricamento...</span>
      </div>
    </div>
  );

  return (
    <div className="mon-page">
      <Header />

      <div className="mon-layout">

        {/* ─── SINISTRA: lista monumenti ─── */}
        <div className="mon-left">
          <div className="mon-left-header">
            <div>
              <h1 className="page-title" style={{ marginBottom: 2 }}>{trip?.city}</h1>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: 14 }}>
                {monuments.length} monumenti disponibili
              </p>
            </div>
            <div className="mon-search-row">
              <input
                type="text"
                className="form-input"
                placeholder="Cerca monumento..."
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
              <select
                className="form-input type-select"
                value={typeFilter}
                onChange={e => setTypeFilter(e.target.value)}
              >
                <option value="">Tutti i tipi</option>
                {types.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <div className="mon-list">
            {filtered.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">?</div>
                <p>Nessun monumento trovato</p>
              </div>
            )}
            {filtered.map(m => (
              <div key={m.id} className={`mon-card ${isSelected(m.id) ? 'mon-card-selected' : ''}`}>
                <div className="mon-card-info">
                  <div className="mon-card-name">{m.name}</div>
                  <div className="mon-card-meta">
                    <TypeBadge type={m.type} />
                    {m.address && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>{m.address}</span>
                    )}
                  </div>
                  {m.estimatedVisitMinutes && (
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                      ~ {m.estimatedVisitMinutes} min suggeriti
                    </span>
                  )}
                </div>
                <button
                  className={`btn btn-sm ${isSelected(m.id) ? 'btn-secondary' : 'btn-primary'}`}
                  onClick={() => !isSelected(m.id) && handleAdd(m)}
                  disabled={isSelected(m.id)}
                >
                  {isSelected(m.id) ? 'Aggiunto' : '+ Aggiungi'}
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* ─── DESTRA: itinerario ─── */}
        <div className="mon-right">
          <div className="mon-right-header">
            <h2 className="mon-right-title">Modifica itinerario</h2>
            <span className="badge badge-accent">{selected.length} tappe</span>
          </div>

          <div className="mon-right-body">
            {selected.length === 0 ? (
              <div className="empty-state" style={{ padding: '40px 8px' }}>
                <div className="empty-icon">+</div>
                <p>Aggiungi monumenti dalla lista</p>
              </div>
            ) : (
              <div className="sel-list">
                {selected.map((s, i) => (
                  <div key={i} className="sel-card">
                    <div className="sel-order">{i + 1}</div>
                    <div className="sel-info">
                      <div className="sel-name">{s.monument.name}</div>
                      <div className="sel-time">{s.visitDurationMinutes} min</div>
                    </div>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => setEditModal({ ...s, index: i })}
                      >
                        Durata
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleRemove(i)}
                      >
                        rimuovi
                      </button>
                    </div>
                  </div>
                ))}

                <div className="sel-summary">
                  <div className="sel-total">
                    <span>Tempo totale:</span>
                    <span className="sel-total-val">
                      {Math.floor(totalMinutes / 60)}h {totalMinutes % 60}min
                    </span>
                  </div>
                </div>

                {saveError && (
                  <div className="error-msg" style={{ margin: '8px 0' }}>{saveError}</div>
                )}

                <button
                  className="btn btn-primary btn-full"
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving
                    ? <><span className="btn-spinner" /> Salvataggio e ottimizzazione...</>
                    : 'salva e ottimizza'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* MODAL */}
      {editModal && (
        <EditTimeModal
          item={editModal}
          onConfirm={handleEditConfirm}
          onClose={() => setEditModal(null)}
        />
      )}

      <style>{`
        .mon-page { min-height: 100vh; display: flex; flex-direction: column; }
        .mon-layout {
          flex: 1;
          display: grid;
          grid-template-columns: 1fr 360px;
          height: calc(100vh - 56px);
          overflow: hidden;
        }
        .mon-left {
          display: flex; flex-direction: column;
          border-right: 1px solid var(--border);
          overflow: hidden;
          background: #fff;
        }
        .mon-left-header {
          padding: 20px 24px 12px;
          border-bottom: 1px solid var(--border);
          background: #fff;
          position: sticky; top: 0; z-index: 10;
        }
        .mon-search-row { display: flex; gap: 8px; }
        .type-select { max-width: 150px; appearance: none; cursor: pointer; }
        .mon-list {
          flex: 1; overflow-y: auto; padding: 12px 24px;
          display: flex; flex-direction: column; gap: 8px;
        }
        .mon-card {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 12px;
          background: var(--bg);
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          padding: 12px 14px;
          transition: all 0.15s;
        }
        .mon-card:hover { border-color: #bfdbfe; background: var(--accent-soft); }
        .mon-card-selected { border-color: var(--accent) !important; background: var(--accent-soft) !important; }
        .mon-card-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
        .mon-card-name { font-weight: 600; font-size: 0.9rem; }
        .mon-card-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
        .type-badge {
          background: #f1f5f9;
          color: var(--text-muted);
          border: 1px solid var(--border);
          border-radius: 100px;
          padding: 1px 8px;
          font-size: 0.72rem;
          font-weight: 500;
        }
        .mon-right {
          display: grid;
          grid-template-rows: auto 1fr;
          overflow: hidden;
          background: var(--bg);
        }
        .mon-right-header {
          display: flex; align-items: center; justify-content: space-between;
          padding: 20px 20px 12px;
          border-bottom: 1px solid var(--border);
          background: var(--bg);
        }
        .mon-right-body {
          overflow-y: auto;
          padding: 12px 20px;
          min-height: 0;
        }
        .mon-right-title { font-size: 1rem; font-weight: 600; }
        .sel-list { display: flex; flex-direction: column; gap: 6px; }
        .sel-card {
          display: flex; align-items: center; gap: 10px;
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          padding: 10px 12px;
        }
        .sel-order {
          width: 24px; height: 24px;
          background: var(--accent);
          border-radius: 50%;
          display: flex; align-items: center; justify-content: center;
          font-size: 0.7rem; font-weight: 700; color: white;
          flex-shrink: 0;
        }
        .sel-info { flex: 1; }
        .sel-name { font-size: 0.85rem; font-weight: 600; }
        .sel-time { font-size: 0.75rem; color: var(--text-muted); margin-top: 1px; }
        .sel-summary {
          margin-top: 14px; padding-top: 14px;
          border-top: 1px solid var(--border);
          margin-bottom: 12px;
        }
        .sel-total {
          display: flex; justify-content: space-between;
          font-size: 0.875rem; color: var(--text-muted);
        }
        .sel-total-val { font-weight: 700; color: var(--text); }
        .visit-presets { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }
        .preset-btn {
          background: var(--bg);
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          color: var(--text-muted);
          font-family: inherit; font-size: 0.8rem;
          padding: 4px 10px; cursor: pointer; transition: all 0.15s;
        }
        .preset-btn.active {
          background: var(--accent-soft);
          border-color: var(--accent);
          color: var(--accent);
        }
        .btn-spinner {
          display: inline-block; width: 14px; height: 14px;
          border: 2px solid rgba(255,255,255,0.4);
          border-top-color: white; border-radius: 50%;
          animation: spin 0.6s linear infinite;
        }
        .loading-center {
          display: flex; flex-direction: column; align-items: center;
          justify-content: center; gap: 12px; height: 300px;
          color: var(--text-muted);
        }
        @media (max-width: 768px) {
          .mon-layout { grid-template-columns: 1fr; height: auto; }
          .mon-right { border-top: 1px solid var(--border); }
          .mon-list { max-height: 50vh; }
        }
      `}</style>
    </div>
  );
}