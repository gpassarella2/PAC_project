import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AuthPage from './pages/AuthPage';
import HomePage from './pages/HomePage';
import MonumentsPage from './pages/MonumentsPage';
import ItineraryPage from './pages/ItineraryPage';
import MyTripsPage from './pages/MyTripsPage';



// Componente che serve a definire il routing e contesto globale(AuthContext)
// Contesto globale -> tutti i componenti in AuthProvider possono accedere allo stato di autenticazione

export default function App() {
  return (
    // Rende disponibile lo stato di autenticazione a tutta l'app
    <AuthProvider>

      <BrowserRouter>
        <Routes>

          {/* Pagina pubblica: login/registrazione */}
          <Route path="/auth" element={<AuthPage />} />

          {/* Home protetta: accessibile solo se l'utente è autenticato */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            }
          />

          {/* Pagina dei monumenti, protetta */}
          <Route
            path="/monuments"
            element={
              <ProtectedRoute>
                <MonumentsPage />
              </ProtectedRoute>
            }
          />

          {/* Pagina dell'itinerario, protetta e con parametro dinamico(id dell'itinerario) */}
          <Route
            path="/itinerary/:tripId"
            element={
              <ProtectedRoute>
                <ItineraryPage />
              </ProtectedRoute>
            }
          />

          {/* Pagina dei viaggi salvati, protetta */}
          <Route
            path="/my-trips"
            element={
              <ProtectedRoute>
                <MyTripsPage />
              </ProtectedRoute>
            }
          />

          {/* Qualsiasi route non valida reindirizza alla home */}
          <Route path="*" element={<Navigate to="/" replace />} />

        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}