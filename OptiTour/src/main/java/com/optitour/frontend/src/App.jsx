import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AuthPage from './pages/AuthPage';
import HomePage from './pages/HomePage';
import MonumentsPage from './pages/MonumentsPage';
import ItineraryPage from './pages/ItineraryPage';
import MyTripsPage from './pages/MyTripsPage';
import ExplorePage from './pages/ExplorePage';
import SurprisePage from './pages/SurprisePage';
import EditTripPage from './pages/EditTripPage';
import ProfilePage from './pages/ProfilePage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>

          {/* Pagina pubblica: login/registrazione */}
          <Route path="/auth" element={<AuthPage />} />

          {/* Home protetta */}
          <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />

          {/* Pagina dei monumenti */}
          <Route path="/monuments" element={<ProtectedRoute><MonumentsPage /></ProtectedRoute>} />

          {/* Pagina dell'itinerario */}
          <Route path="/itinerary/:tripId" element={<ProtectedRoute><ItineraryPage /></ProtectedRoute>} />

          {/* Pagina dei viaggi salvati */}
          <Route path="/my-trips" element={<ProtectedRoute><MyTripsPage /></ProtectedRoute>} />

          {/* Catalogo dei viaggi pubblici */}
          <Route path="/explore" element={<ProtectedRoute><ExplorePage /></ProtectedRoute>} />

          {/* Pagina viaggio a sorpresa */}
          <Route path="/surprise" element={<ProtectedRoute><SurprisePage /></ProtectedRoute>} />

          {/* Pagina modifica viaggio */}
          <Route path="/edit-trip/:id" element={<ProtectedRoute><EditTripPage /></ProtectedRoute>} />

          {/* Pagina profilo utente */}
          <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />

          {/* Qualsiasi route non valida reindirizza alla home */}
          <Route path="*" element={<Navigate to="/" replace />} />

        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
