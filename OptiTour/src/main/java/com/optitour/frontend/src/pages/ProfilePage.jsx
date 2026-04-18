import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Header from '../components/Header';
import { updateUserProfile, changePassword, deleteCurrentUser } from '../services/api';

export default function ProfilePage() {
  const { user, logout, login } = useAuth();
  const navigate = useNavigate();

  // Campi modifica profilo
  const [username, setUsername] = useState(user?.username || '');
  const [email, setEmail] = useState(user?.email || '');

  // Campi modifica password
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Stato UI
  const [profileMsg, setProfileMsg] = useState({ text: '', isError: false });
  const [passwordMsg, setPasswordMsg] = useState({ text: '', isError: false });
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);

  // --- Salva modifiche profilo -----------------------------------------------
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setProfileMsg({ text: '', isError: false });
    setProfileLoading(true);
    try {
      const updates = {};
      if (username.trim() && username !== user.username) updates.username = username.trim();
      if (email.trim()    && email    !== user.email)    updates.email    = email.trim();

      if (Object.keys(updates).length === 0) {
        setProfileMsg({ text: 'Nessuna modifica da salvare.', isError: false });
        return;
      }

      await updateUserProfile(updates);

      // Aggiorna il contesto locale con i nuovi dati
      const updatedUser = {
        ...user,
        username: updates.username || user.username,
        email:    updates.email    || user.email,
      };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      // Forza il refresh del contesto ricaricando la pagina
      window.location.reload();

    } catch (err) {
      const msg = err.response?.data?.message || 'Errore durante il salvataggio del profilo.';
      setProfileMsg({ text: msg, isError: true });
    } finally {
      setProfileLoading(false);
    }
  };

  // --- Cambia password -----------------------------------------------
  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordMsg({ text: '', isError: false });

    if (newPassword !== confirmPassword) {
      setPasswordMsg({ text: 'Le password non coincidono.', isError: true });
      return;
    }
    if (newPassword.length < 4) {
      setPasswordMsg({ text: 'La password deve essere di almeno 4 caratteri.', isError: true });
      return;
    }

    setPasswordLoading(true);
    try {
      await changePassword({ currentPassword, newPassword });
      setPasswordMsg({ text: 'Password aggiornata con successo!', isError: false });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const msg = err.response?.data?.message || 'Errore durante il cambio password.';
      setPasswordMsg({ text: msg, isError: true });
    } finally {
      setPasswordLoading(false);
    }
  };

  // --- Elimina account  -----------------------------------------------
  const handleDeleteAccount = async () => {
    setDeleteLoading(true);
    try {
      await deleteCurrentUser();
      await logout();
      navigate('/auth');
    } catch {
      setDeleteLoading(false);
      setShowDeleteConfirm(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      <Header />

      <div className="page-content" style={{ maxWidth: 540, margin: '0 auto', padding: '32px 16px' }}>
        <button
          onClick={() => navigate(-1)}
          style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.875rem', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 4 }}
        >
          ← Torna indietro
        </button>

        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: 8 }}>Il mio profilo</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: 32 }}>
          Gestisci le informazioni del tuo account.
        </p>

        {/* --- Sezione: dati profilo --- */}
        <section style={sectionStyle}>
          <h2 style={sectionTitleStyle}>Dati account</h2>
          <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <label style={labelStyle}>
              Username
              <input
                type="text"
                className="form-input"
                value={username}
                onChange={e => setUsername(e.target.value)}
                autoComplete="username"
                style={{ marginTop: 4 }}
              />
            </label>
            <label style={labelStyle}>
              Email
              <input
                type="email"
                className="form-input"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoComplete="email"
                style={{ marginTop: 4 }}
              />
            </label>

            {profileMsg.text && (
              <div style={{ fontSize: '0.85rem', color: profileMsg.isError ? '#dc2626' : '#16a34a', padding: '8px 12px', background: profileMsg.isError ? '#fef2f2' : '#f0fdf4', borderRadius: 6 }}>
                {profileMsg.text}
              </div>
            )}

            <button type="submit" className="btn btn-primary" disabled={profileLoading} style={{ alignSelf: 'flex-start' }}>
              {profileLoading ? 'Salvataggio...' : 'Salva modifiche'}
            </button>
          </form>
        </section>

        {/* --- Sezione: cambio password --- */}
        <section style={sectionStyle}>
          <h2 style={sectionTitleStyle}>Cambia password</h2>
          <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <label style={labelStyle}>
              Password attuale
              <input
                type="password"
                className="form-input"
                value={currentPassword}
                onChange={e => setCurrentPassword(e.target.value)}
                required
                autoComplete="current-password"
                style={{ marginTop: 4 }}
              />
            </label>
            <label style={labelStyle}>
              Nuova password
              <input
                type="password"
                className="form-input"
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                required
                autoComplete="new-password"
                style={{ marginTop: 4 }}
              />
            </label>
            <label style={labelStyle}>
              Conferma nuova password
              <input
                type="password"
                className="form-input"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                required
                autoComplete="new-password"
                style={{ marginTop: 4 }}
              />
            </label>

            {passwordMsg.text && (
              <div style={{ fontSize: '0.85rem', color: passwordMsg.isError ? '#dc2626' : '#16a34a', padding: '8px 12px', background: passwordMsg.isError ? '#fef2f2' : '#f0fdf4', borderRadius: 6 }}>
                {passwordMsg.text}
              </div>
            )}

            <button type="submit" className="btn btn-primary" disabled={passwordLoading} style={{ alignSelf: 'flex-start' }}>
              {passwordLoading ? 'Aggiornamento...' : 'Aggiorna password'}
            </button>
          </form>
        </section>

        {/* --- Sezione: zona pericolosa --- */}
        <section style={{ ...sectionStyle, borderColor: '#fecaca' }}>
          <h2 style={{ ...sectionTitleStyle, color: '#dc2626' }}>Attenzione!</h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: 16 }}>
            Eliminando il tuo account rimuoverai definitivamente tutti i tuoi dati e i tuoi viaggi. Questa operazione è irreversibile.
          </p>
          <button
            className="btn btn-danger"
            onClick={() => setShowDeleteConfirm(true)}
          >
            Elimina account
          </button>
        </section>
      </div>

      {/* --- Modal conferma eliminazione --- */}
      {showDeleteConfirm && (
        <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Elimina account</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              Sei sicuro di voler eliminare definitivamente il tuo account <strong style={{ color: 'var(--text)' }}>{user?.username}</strong>?
              <br /><br />
              Tutti i tuoi viaggi verranno cancellati. <strong>Questa azione è irreversibile.</strong>
            </p>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowDeleteConfirm(false)}>Annulla</button>
              <button className="btn btn-danger" onClick={handleDeleteAccount} disabled={deleteLoading}>
                {deleteLoading ? 'Eliminazione...' : 'Elimina definitivamente'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const sectionStyle = {
  background: '#fff',
  border: '1px solid var(--border)',
  borderRadius: 'var(--radius)',
  padding: '24px',
  marginBottom: 20,
};

const sectionTitleStyle = {
  fontSize: '1rem',
  fontWeight: 700,
  marginBottom: 16,
};

const labelStyle = {
  display: 'flex',
  flexDirection: 'column',
  fontSize: '0.85rem',
  fontWeight: 500,
  color: 'var(--text)',
};
