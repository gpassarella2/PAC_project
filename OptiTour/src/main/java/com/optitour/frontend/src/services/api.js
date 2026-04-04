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

export default api;

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

// PUT /api/trips/{id}/status?status=DRAFT|OPTIMIZED|COMPLETED
export const updateTripStatus = (id, status) =>
  api.put(`/api/trips/${id}/status`, null, { params: { status } });

