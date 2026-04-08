import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Componente che protegge una route: permette l'accesso solo se l'utente è autenticato
export default function ProtectedRoute({ children }) {

  // Recupera l'utente dal contesto di autenticazione
  const { user } = useAuth();

  // Se non c'è un utente loggato, reindirizza alla pagina di login
  // "replace" evita che l'utente possa tornare indietro con il tasto "back"
  if (!user) return <Navigate to="/auth" replace />;

  // Se l'utente è autenticato, mostra il contenuto della route protetta
  return children;
}
