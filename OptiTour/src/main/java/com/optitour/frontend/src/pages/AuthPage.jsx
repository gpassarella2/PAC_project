import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function AuthPage() {
  // Estrae le funzioni di login e registrazione dal contesto di autenticazione
  const { login, register } = useAuth();

  // Per cambiare pagina da codice senza premere link
  const navigate = useNavigate();

  // Tab attivo: 'login' o 'register'
  const [tab, setTab] = useState('login');

  // Stato di caricamento per disabilitare i pulsanti e mostrare il caricamento
  const [loading, setLoading] = useState(false);

  // Messaggio di errore da mostrare all’utente
  const [error, setError] = useState('');

  // Stato del form di login
  const [loginForm, setLoginForm] = useState({
    usernameOrEmail: '',
    password: ''
  });

  // Stato del form di registrazione
  const [regForm, setRegForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });

  // Gestione submit login
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Chiama la funzione login
      await login(loginForm);

      // Reindirizza alla home dopo login riuscito
      navigate('/');
    } catch (err) {
      // Mostra messaggio di errore proveniente dal backend
      setError(err.response?.data?.message || 'Credenziali non valide');
    } finally {
      setLoading(false);
    }
  };

  // Gestione submit registrazione
  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');

    // Validazione lato client: password uguali
    if (regForm.password !== regForm.confirmPassword) {
      setError('Le password non coincidono');
      return;
    }

    setLoading(true);

    try {
      // Chiama la funzione register
      await register({
        username: regForm.username,
        email: regForm.email,
        password: regForm.password
      });

      // Reindirizza alla home dopo registrazione riuscita
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Errore durante la registrazione');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">

        {/* Logo e tagline */}
        <div className="auth-logo">
          <span className="auth-logo-text">OptiTour</span>
        </div>
        <p className="auth-tagline">Crea itinerari ottimizzati in ogni città</p>

        {/* Card contenente tab e form */}
        <div className="auth-card">

          {/* Tab di selezione login/registrazione */}
          <div className="auth-tabs">
            <button
              id="tab-login"
              className={`auth-tab ${tab === 'login' ? 'active' : ''}`}
              onClick={() => { setTab('login'); setError(''); }}
            >
              Accedi
            </button>

            <button
              id="tab-register"
              className={`auth-tab ${tab === 'register' ? 'active' : ''}`}
              onClick={() => { setTab('register'); setError(''); }}
            >
              Registrati
            </button>
          </div>

          {/* Messaggio di errore */}
          {error && <div className="error-msg">{error}</div>}

          {/* FORM LOGIN */}
          {tab === 'login' ? (
            <form id="form-login" onSubmit={handleLogin} className="auth-form">

              {/* Campo username/email */}
              <div className="form-group">
                <label className="form-label">Username o Email</label>
                <input
                  id="login-username"
                  className="form-input"
                  type="text"
                  placeholder="Username"
                  required
                  value={loginForm.usernameOrEmail}
                  onChange={(e) =>
                    setLoginForm({ ...loginForm, usernameOrEmail: e.target.value })
                  }
                />
              </div>

              {/* Campo password */}
              <div className="form-group">
                <label className="form-label">Password</label>
                <input
                  id="login-password"
                  className="form-input"
                  type="password"
                  placeholder="Password"
                  required
                  value={loginForm.password}
                  onChange={(e) =>
                    setLoginForm({ ...loginForm, password: e.target.value })
                  }
                />
              </div>

              {/* Pulsante login */}
              <button
                id="btn-login"
                type="submit"
                className="btn btn-primary btn-full btn-lg"
                disabled={loading}
              >
                {loading ? <span className="btn-spinner" /> : 'Accedi'}
              </button>
            </form>
          ) : (

            /* FORM REGISTRAZIONE */
            <form id="form-register" onSubmit={handleRegister} className="auth-form">

              {/* Username */}
              <div className="form-group">
                <label className="form-label">Username</label>
                <input
                  id="reg-username"
                  className="form-input"
                  type="text"
                  placeholder="Username"
                  required
                  value={regForm.username}
                  onChange={(e) =>
                    setRegForm({ ...regForm, username: e.target.value })
                  }
                />
              </div>

              {/* Email */}
              <div className="form-group">
                <label className="form-label">Email</label>
                <input
                  id="reg-email"
                  className="form-input"
                  type="email"
                  placeholder="username@email.com"
                  required
                  value={regForm.email}
                  onChange={(e) =>
                    setRegForm({ ...regForm, email: e.target.value })
                  }
                />
              </div>

              {/* Password */}
              <div className="form-group">
                <label className="form-label">Password</label>
                <input
                  id="reg-password"
                  className="form-input"
                  type="password"
                  placeholder="Minimo 4 caratteri"
                  required
                  value={regForm.password}
                  onChange={(e) =>
                    setRegForm({ ...regForm, password: e.target.value })
                  }
                />
              </div>

              {/* Conferma password */}
              <div className="form-group">
                <label className="form-label">Conferma Password</label>
                <input
                  id="reg-confirm-password"
                  className="form-input"
                  type="password"
                  placeholder="Ripeti la password"
                  required
                  value={regForm.confirmPassword}
                  onChange={(e) =>
                    setRegForm({ ...regForm, confirmPassword: e.target.value })
                  }
                />
              </div>

              {/* Pulsante registrazione */}
              <button
                id="btn-register"
                type="submit"
                className="btn btn-primary btn-full btn-lg"
                disabled={loading}
              >
                {loading ? <span className="btn-spinner" /> : 'Crea account'}
              </button>
            </form>
          )}
        </div>
      </div>

      {/* Stili inline del componente */}
      <style>{`
        .auth-page {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 24px;
          background: var(--bg);
        }
        .auth-container {
          width: 100%;
          max-width: 400px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
        }
        .auth-logo { margin-bottom: 2px; }
        .auth-logo-text {
          font-size: 1.8rem;
          font-weight: 700;
          color: var(--accent);
          letter-spacing: -0.03em;
        }
        .auth-tagline {
          color: var(--text-muted);
          font-size: 0.9rem;
          text-align: center;
          margin-bottom: 4px;
        }
        .auth-card {
          width: 100%;
          background: #fff;
          border: 1px solid var(--border);
          border-radius: var(--radius-lg);
          padding: 28px;
          box-shadow: var(--shadow);
        }
        .auth-tabs {
          display: flex;
          background: var(--bg);
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          padding: 3px;
          margin-bottom: 20px;
          gap: 3px;
        }
        .auth-tab {
          flex: 1;
          padding: 7px;
          border: none;
          border-radius: 4px;
          background: transparent;
          color: var(--text-muted);
          font-family: inherit;
          font-size: 0.875rem;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.15s ease;
        }
        .auth-tab.active {
          background: #fff;
          color: var(--text);
          box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .auth-form {
          display: flex;
          flex-direction: column;
          gap: 14px;
        }
        .btn-spinner {
          display: inline-block;
          width: 16px;
          height: 16px;
          border: 2px solid rgba(255,255,255,0.4);
          border-top-color: white;
          border-radius: 50%;
          animation: spin 0.6s linear infinite;
        }
      `}</style>
    </div>
  );
}
