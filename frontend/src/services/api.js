import axios from 'axios';
import { getToken } from './storage';

const api = axios.create({
  baseURL: 'http://localhost:8000'
});

// Interceptor: añade Authorization si hay token
api.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
