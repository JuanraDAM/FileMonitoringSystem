// src/routes/AppRouter.jsx
import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import Dashboard from '../pages/Dashboard';
import LogsPage from '../pages/LogsPage';
import MainLayout from '../layouts/MainLayout';

/**
 * Comprueba si hay token en localStorage.
 * Si no existe, redirige a /login.
 * Se usa useLocation para forzar que React Router entienda la navegación.
 */
const RequireAuth = ({ children }) => {
  const token = localStorage.getItem('access_token');
  const location = useLocation();

  if (!token) {
    // Redirige a /login, y guardamos la ubicación actual en state por si queremos “volver”
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return children;
};

const AppRouter = () => (
  <Routes>
    {/* ----------------- RUTAS PÚBLICAS ----------------- */}
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />

    {/* ----------------- RUTAS PRIVADAS ----------------- */}
    <Route
      path="/"
      element={
        <RequireAuth>
          <MainLayout />
        </RequireAuth>
      }
    >
      {/* Si alguien va a “/” con token, le llevamos a “/dashboard” */}
      <Route index element={<Navigate to="/dashboard" replace />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="logs" element={<LogsPage />} />
    </Route>

    {/* Cualquier otra ruta lleva al login */}
    <Route path="*" element={<Navigate to="/login" replace />} />
  </Routes>
);

export default AppRouter;
