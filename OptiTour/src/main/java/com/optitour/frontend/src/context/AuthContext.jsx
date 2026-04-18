import React, { createContext, useContext, useState, useEffect } from 'react';
import { loginUser, logoutUser, registerUser } from '../services/api';

// Crea un contesto per condividere stato e funzioni di autenticazione nell'app
const AuthContext = createContext(null);

export function AuthProvider({ children }) {

  // Stato dell'utente autenticato.
  // legge da localStorage -> per mantenere la sessione anche dopo un refresh della pagina
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });

  // Effettua login chiamando l'API backend e salva token + dati utente
  const login = async (credentials) => {
    const res = await loginUser(credentials);

	// serve per avere una struttura fissa dell'oggetto utente
    const u = {
      id: res.data.userId,
      username: res.data.username,
      email: res.data.email,
      token: res.data.token,
    };

    // Persistenza locale per sessione e richieste future
    localStorage.setItem('token', u.token);
    localStorage.setItem('user', JSON.stringify(u));

    // Aggiorna lo stato globale dell'utente
    setUser(u);

    return u;
  };

  // Registra un nuovo utente e poi effettua login automatico
  const register = async (data) => {
    await registerUser(data);
    return login({ usernameOrEmail: data.username, password: data.password });
  };

  // Logout: chiama il backend (se disponibile) e pulisce stato + storage
  const logout = async () => {
    try {
      await logoutUser();
    } catch (e) {
      // Ignora eventuali errori di rete: il logout locale è comunque valido
    }

    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  // Ritorna user + funzioni di autenticazione a tutti i componenti figli
  return (
    <AuthContext.Provider value={{ user, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
}

// Hook personalizzato per facilitare l'accesso all'autenticazione
export function useAuth() {
  return useContext(AuthContext);
}
