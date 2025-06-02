// src/api/logs.js
import api from '../services/api';

/**
 * Obtiene todos los logs de validación.
 * Puedes pasar filtros opcionales: { file_config_id, environment, date_from, date_to }
 */
export const getLogs = (filters = {}) => {
  return api.get('/files/logs', { params: filters });
};
