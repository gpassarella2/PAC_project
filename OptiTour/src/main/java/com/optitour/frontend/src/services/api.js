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


export default api;