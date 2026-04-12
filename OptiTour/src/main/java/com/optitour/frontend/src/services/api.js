import axios from 'axios';

const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ----- Auth ------------------------------------------------------

// POST /api/auth/register  { username, email, password }
export const registerUser = (data) =>
  api.post('/api/auth/register', data);

// POST /api/auth/login  { usernameOrEmail, password }
export const loginUser = (data) =>
  api.post('/api/auth/login', data);

// POST /api/auth/logout  (JWT nella Authorization header)
export const logoutUser = () => {
  const token = localStorage.getItem("token");

  return api.post(
    '/api/auth/logout',
    {},
    {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
};

// POST /api/auth/change-password  { currentPassword, newPassword }
export const changePassword = (data) =>
  api.post('/api/auth/change-password', data);

// ----- User Profile ------------------------------------------------------

// GET /api/user/profile
export const getUserProfile = () =>
  api.get('/api/user/profile');

// PATCH /api/user/profile  { firstName, lastName }
export const updateUserProfile = (data) =>
  api.patch('/api/user/profile', data);

// ----- Trips -------------------------------------------------------------

// POST /api/trips?userId=...  body: { name, city, startPoint, stages[] }
export const createTrip = (userId, data) =>
  api.post('/api/trips', data, { params: { userId } });

// GET /api/trips?userId=...
export const getTripsByUser = (userId) =>
  api.get('/api/trips', { params: { userId } });

// GET /api/trips/status?userId=...&status=...
export const getTripsByUserAndStatus = (userId, status) =>
  api.get('/api/trips/status', { params: { userId, status } });

// GET /api/trips/{id}
export const getTripById = (id) =>
  api.get(`/api/trips/${id}`);

// DELETE /api/trips/{id}
export const deleteTrip = (id) =>
  api.delete(`/api/trips/${id}`);

// PUT /api/trips/{id}/status?status= SAVED|COMPLETED|STARRED
export const updateTripStatus = (id, status) =>
  api.put(`/api/trips/${id}/status`, null, { params: { status } });

// ----- Endpoint: preferiti, storico, completa ----------------------

/**
 * POST /api/trips/{id}/save
 * Aggiunge il viaggio ai preferiti (status -> STARRED).
 * Il JWT nell'header identifica l'utente; nessun body richiesto.
 */
export const saveTripToFavorites = (id) =>
  api.post(`/api/trips/${id}/save`);

/**
 * DELETE /api/trips/{id}/save
 * Rimuove il viaggio dai preferiti (status -> SAVED).
 */
export const removeTripFromFavorites = (id) =>
  api.delete(`/api/trips/${id}/save`);

/**
 * GET /api/trips/history
 * Restituisce la lista dei viaggi COMPLETED dell'utente autenticato.
 */
export const getTripHistory = () =>
  api.get('/api/trips/history');

/**
 * PUT /api/trips/{id}/complete
 * Imposta il viaggio come COMPLETED.
 */
export const completeTrip = (id) =>
  api.put(`/api/trips/${id}/complete`);
/**
 * PUT /api/trips/{id}/restore
 * Riporta il viaggio dallo stato COMPLETED a SAVED
 */
export const restoreTrip = (id) =>
  api.put(`/api/trips/${id}/restore`);

// ----- Monuments ---------------------------------------------------------

// GET /api/monuments/{id}
export const getMonumentById = (id) =>
  api.get(`/api/monuments/${id}`);

// POST /api/trips/{id}/optimize
// Ottimizza il percorso e restituisce OptimizedTripResponse
// con le tappe complete (name, lat, lon, type, address)
export const optimizeTrip = (id) =>
  api.post(`/api/trips/${id}/optimize`);

// GET /api/monuments?city=...
// Recupera la lista dei monumenti filtrati per città
export const getMonumentsByCity = (city) =>
  api.get('/api/monuments', { params: { city } });

export default api;