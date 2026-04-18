import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import { getRandomCatalogTrip, generateRandomTrip } from '../services/api';

// ─── SurprisePage ─────────────────────────────────────────────────────────────
// Permette all'utente di generare un viaggio casuale in due modi:
//  Modalità 1 – dal catalogo: pesca un viaggio pubblico già esistente
//  Modalità 2 – genera: crea un viaggio nuovo con monumenti casuali
//               per la città e il tempo indicati
export default function SurprisePage() {
  const navigate = useNavigate();

  const [catalogCity, setCatalogCity] = useState('');

  // ── Stato condiviso ────────────────────────────────────────────────
  const [loading, setLoading]   = useState(false);
  const [error,   setError]     = useState('');

  // ── Stato modalità 2 (genera) ──────────────────────────────────────
  const [city,    setCity]      = useState('');
  const [hours,   setHours]     = useState(3);   // ore disponibili

  // ── Modalità 1: viaggio casuale dal catalogo ──────────────────────
  const handleCatalog = async () => {
    setError('');
    setLoading(true);
    try {
        const res = await getRandomCatalogTrip(catalogCity.trim() || null);
        navigate(`/itinerary/${res.data.id}`);
    } catch (e) {
        const status = e.response?.status;
        if (status === 404) setError(
            catalogCity.trim()
                ? `Nessun viaggio disponibile per "${catalogCity}". Prova un'altra città.`
                : 'Il catalogo è ancora vuoto. Torna più tardi!'
        );
        else setError('Errore nel caricamento. Riprova.');
    } finally {
        setLoading(false);
    }
};

  // ── Modalità 2: genera viaggio casuale ────────────────────────────
  const handleGenerate = async (e) => {
    e.preventDefault();
    if (!city.trim()) return;
    setError('');
    setLoading(true);
    try {
      const availableMinutes = hours * 60;
      const res = await generateRandomTrip(city.trim(), availableMinutes);
      // Il trip è DRAFT: naviga all'itinerario dove l'utente può ottimizzarlo
      navigate(`/itinerary/${res.data.id}`);
    } catch (e) {
      const msg = e.response?.data?.message || e.message || '';
      if (msg.includes('Nessun monumento'))
        setError(`Nessun monumento trovato per "${city}". Prova un'altra città.`);
      else if (msg.includes('insufficiente'))
        setError('Tempo troppo breve. Prova ad aumentare le ore disponibili.');
      else
        setError('Errore nella generazione. Riprova.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="surprise-page">
      <Header />

      <div className="surprise-body">
        <div className="surprise-content">

          {/* Intestazione */}
          <h1 className="surprise-title">Viaggio a sorpresa</h1>
          <p className="surprise-sub">
            Lascia decidere al caso. Scegli una modalità.
          </p>

          {/* Errore globale */}
          {error && <div className="error-msg" style={{ marginBottom: 20 }}>{error}</div>}

          {/* ── Card modalità 1: dal catalogo ── */}
          <div className="surprise-card">
            <div className="surprise-card-header">
              <div>
                <h2 className="surprise-card-title">Dal catalogo</h2>
                <p className="surprise-card-desc">
                  Ti viene mostrato un viaggio già creato e condiviso da un altro utente.
                </p>
              </div>
            </div>
            <input
    type="text"
    className="form-input"
    placeholder="Qualsiasi città (opzionale)"
    value={catalogCity}
    onChange={e => setCatalogCity(e.target.value)}
    disabled={loading}
    style={{ marginTop: 14 }}
/>

            <button
              className="btn btn-primary"
              style={{ width: '100%', marginTop: 14 }}
              onClick={handleCatalog}
              disabled={loading}
            >
              {loading ? 'Estrazione in corso…' : 'Pesca un viaggio dal catalogo'}
            </button>
          </div>

          {/* ── Divisore ── */}
          <div className="surprise-divider">
            <span>oppure</span>
          </div>

          {/* ── Card modalità 2: genera ── */}
          <div className="surprise-card">
            <div className="surprise-card-header">
              <div>
                <h2 className="surprise-card-title">Genera un itinerario</h2>
                <p className="surprise-card-desc">
                  Scegli la città e il tempo disponibile: selezioniamo noi i monumenti.
                </p>
              </div>
            </div>

            <form onSubmit={handleGenerate} style={{ marginTop: 14 }}>
              <div className="surprise-form-row">
                <div className="surprise-field">
                  <label className="surprise-label">Città</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Es. Roma, Firenze, Venezia…"
                    value={city}
                    onChange={e => setCity(e.target.value)}
                    disabled={loading}
                  />
                </div>
                <div className="surprise-field surprise-field-sm">
                  <label className="surprise-label">
                    Ore disponibili: <strong>{hours}h</strong>
                  </label>
                  <input
                    type="range"
                    min={1}
                    max={12}
                    step={1}
                    value={hours}
                    onChange={e => setHours(Number(e.target.value))}
                    disabled={loading}
                    style={{ width: '100%', cursor: 'pointer' }}
                  />
                  <div className="surprise-range-labels">
                    <span>1h</span><span>6h</span><span>12h</span>
                  </div>
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                style={{ width: '100%', marginTop: 14 }}
                disabled={loading || !city.trim()}
              >
                {loading ? 'Generazione in corso…' : 'Genera itinerario'}
              </button>
            </form>
          </div>

          {/* Link indietro */}
          <button
            className="btn btn-ghost btn-sm"
            style={{ marginTop: 20, width: '100%' }}
            onClick={() => navigate('/')}
          >
            Indietro
          </button>

        </div>
      </div>

      <style>{`
        .surprise-page { min-height: 100vh; display: flex; flex-direction: column; background: var(--bg); }
        .surprise-body {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 48px 24px;
        }
        .surprise-content {
          width: 100%;
          max-width: 520px;
        }
        .surprise-title {
          font-size: 1.8rem;
          font-weight: 700;
          letter-spacing: -0.03em;
          margin-bottom: 8px;
          color: var(--text);
          text-align: center;
        }
        .surprise-sub {
          color: var(--text-muted);
          font-size: 0.95rem;
          margin-bottom: 28px;
          text-align: center;
          line-height: 1.6;
        }
        .surprise-card {
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius);
          padding: 20px;
          box-shadow: var(--shadow);
        }
        .surprise-card-header {
          display: flex;
          gap: 14px;
          align-items: flex-start;
        }
        .surprise-card-title {
          font-size: 1rem;
          font-weight: 700;
          margin-bottom: 4px;
          color: var(--text);
        }
        .surprise-card-desc {
          font-size: 0.85rem;
          color: var(--text-muted);
          line-height: 1.5;
        }
        .surprise-divider {
          display: flex;
          align-items: center;
          gap: 12px;
          margin: 20px 0;
          color: var(--text-dim);
          font-size: 0.8rem;
        }
        .surprise-divider::before, .surprise-divider::after {
          content: '';
          flex: 1;
          height: 1px;
          background: var(--border);
        }
        .surprise-form-row {
          display: flex;
          flex-direction: column;
          gap: 14px;
        }
        .surprise-field {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .surprise-label {
          font-size: 0.78rem;
          font-weight: 500;
          color: var(--text-muted);
        }
        .surprise-range-labels {
          display: flex;
          justify-content: space-between;
          font-size: 0.72rem;
          color: var(--text-dim);
          margin-top: 2px;
        }
      `}</style>
    </div>
  );
}
