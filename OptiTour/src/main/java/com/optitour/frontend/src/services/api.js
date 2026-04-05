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

// GET /api/monuments?city=...
// Recupera la lista dei monumenti filtrati per città
export const getMonumentsByCity = (city) =>
  api.get('/api/monuments', { params: { city } });