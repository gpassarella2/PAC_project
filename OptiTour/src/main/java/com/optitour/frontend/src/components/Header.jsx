import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Header principale dell'app: mostra logo, navigazione e controlli utente
export default function Header() {

  // Legge l'utente autenticato e permette di fare il logout
  const { user, logout } = useAuth();

  // Per cambiare pagina da codice senza premere link
  const navigate = useNavigate();

  // Gestisce il logout: chiama il contesto e poi reindirizza alla pagina di login
  const handleLogout = async () => {
    await logout();
    navigate('/auth');
  };

  return (
    <header className="header">
      <div className="header-inner">

        {/* Logo che rimanda alla home */}
        <Link to="/" className="header-logo">OptiTour</Link>

        {/* Navigazione visibile solo se l'utente è autenticato */}
        <nav className="header-nav">
          {user && (
            <>
              {/* Mostra username dell'utente loggato */}
              <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                {user.username}
              </span>

              {/* Link al catalogo pubblico */}
              <Link to="/explore" id="nav-explore" className="btn btn-ghost btn-sm">
                Esplora
              </Link>

              {/* Link viaggio a sorpresa */}
              <Link to="/surprise" id="nav-surprise" className="btn btn-ghost btn-sm">
                Sorpresa
              </Link>

              {/* Link alla pagina dei viaggi salvati */}
              <Link to="/my-trips" id="nav-my-trips" className="btn btn-ghost btn-sm">
                I miei viaggi
              </Link>

              {/* Pulsante di logout */}
              <button
                id="btn-logout"
                onClick={handleLogout}
                className="btn btn-secondary btn-sm"
              >
                Esci
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
