import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
/**
 * homePage
 * pagina iniziale dopo l'accesso
 * permette all'utente di:
 * inserire una città a piacimento
 * cerca monumenti di quella città
 * passare alla pagina successiva, cioè MonumentsPage
 * 
 */


export default function HomePage() {
  const [city, setCity] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    const trimmed = city.trim();
    if (!trimmed) return;
    navigate(`/monuments?city=${encodeURIComponent(trimmed)}`);
  };

  const popularCities = ['Roma', 'Firenze', 'Venezia', 'Milano', 'Napoli', 'Torino'];

  return (
    <div className="home-page">
      <Header />
      <div className="home-body">
        <div className="home-content">
          <h1 className="home-title">Pianifica il tuo itinerario</h1>
          <p className="home-sub">
            Inserisci una città, scegli i monumenti e ottimizza il percorso di visita.
          </p>

          <form id="form-city-search" onSubmit={handleSearch} className="home-search-wrapper">
            <div className="home-search-box">
              <input
                id="input-city"
                type="text"
                className="home-search-input"
                placeholder="Es. Roma, Firenze, Venezia..."
                value={city}
                onChange={(e) => setCity(e.target.value)}
                autoFocus
              />
              <button
                id="btn-search-city"
                type="submit"
                className="btn btn-primary"
                disabled={!city.trim()}
              >
                Cerca
              </button>
            </div>
          </form>

          <div className="home-chips-label">Città popolari</div>
          <div className="home-chips">
            {popularCities.map(c => (
              <button
                key={c}
                className="home-chip"
                onClick={() => navigate(`/monuments?city=${c}`)}
              >
                {c}
              </button>
            ))}
          </div>
        </div>
      </div>

      <style>{`
        .home-page { min-height: 100vh; display: flex; flex-direction: column; }
        .home-body {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 60px 24px;
        }
        .home-content {
          width: 100%;
          max-width: 560px;
          text-align: center;
        }
        .home-title {
          font-size: 2rem;
          font-weight: 700;
          letter-spacing: -0.03em;
          margin-bottom: 10px;
          color: var(--text);
        }
        .home-sub {
          color: var(--text-muted);
          font-size: 1rem;
          margin-bottom: 32px;
          line-height: 1.6;
        }
        .home-search-wrapper { margin-bottom: 24px; }
        .home-search-box {
          display: flex;
          align-items: center;
          gap: 8px;
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius);
          padding: 6px 6px 6px 14px;
          box-shadow: var(--shadow);
          transition: border-color var(--transition);
        }
        .home-search-box:focus-within { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
        .home-search-input {
          flex: 1;
          background: transparent;
          border: none;
          outline: none;
          color: var(--text);
          font-family: inherit;
          font-size: 0.95rem;
        }
        .home-search-input::placeholder { color: var(--text-dim); }
        .home-chips-label {
          font-size: 0.78rem;
          font-weight: 500;
          color: var(--text-dim);
          text-transform: uppercase;
          letter-spacing: 0.06em;
          margin-bottom: 10px;
        }
        .home-chips {
          display: flex;
          flex-wrap: wrap;
          justify-content: center;
          gap: 8px;
        }
        .home-chip {
          background: #fff;
          border: 1px solid var(--border);
          border-radius: 100px;
          color: var(--text-muted);
          font-family: inherit;
          font-size: 0.825rem;
          padding: 4px 14px;
          cursor: pointer;
          transition: all 0.15s;
        }
        .home-chip:hover {
          border-color: var(--accent);
          color: var(--accent);
          background: var(--accent-soft);
        }
      `}</style>
    </div>
  );
}
