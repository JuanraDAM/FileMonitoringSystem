// src/contexts/AuthContext.jsx
import React, { createContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('access_token'));
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  // Cuando token cambia, guardamos o borramos en localStorage
  useEffect(() => {
    if (token) {
      localStorage.setItem('access_token', token);
      setUser({}); // opcional: aquí podrías decodificar el token para extraer datos de usuario
    } else {
      localStorage.removeItem('access_token');
      setUser(null);
    }
  }, [token]);

  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    setToken(response.data.access_token);
    navigate('/dashboard');
  };

  const register = async (email, password) => {
    await api.post('/auth/register', { email, password });
    navigate('/login');
  };

  const logout = () => {
    setToken(null);
    navigate('/login');
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
