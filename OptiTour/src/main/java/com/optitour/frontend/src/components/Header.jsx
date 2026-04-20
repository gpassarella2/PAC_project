import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Header principale dell'app: mostra logo, navigazione e controlli utente
export default function Header() {

  const { user, logout } = useAuth();
  const navigate = useNavigate();

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
              {/* Username cliccabile apre la pagina profilo */}
              <Link
                to="/profile"
                style={{
                  fontSize: '0.82rem',
                  color: '#2563eb', /* Nome user in blu */
                  textDecoration: 'none',
                  padding: '4px 8px',
                  borderRadius: 'var(--radius)',
                  transition: 'background 0.15s, color 0.15s',
                }}
                onMouseEnter={e => { e.currentTarget.style.background = '#2563eb'; e.currentTarget.style.color = '#ffff'; }}
                onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#2563eb'; }}
                title="Gestisci il tuo profilo"
              >
                {user.username}
              </Link>

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
