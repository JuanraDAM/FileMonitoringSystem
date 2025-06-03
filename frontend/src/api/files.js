import api from '../services/api';

/**
 * Sube un fichero al backend.
 * POST /files/upload
 * @param {FormData} formData - Debe contener el campo "file" con el fichero.
 */
export const uploadFile = (formData) => {
  return api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

/**
 * Obtiene todas las configuraciones de ficheros.
 * GET /files/
 */
export const getConfigs = () => {
  return api.get('/files/');
};

/**
 * Obtiene la configuración de un fichero por ID.
 * GET /files/{config_id}
 * @param {number|string} configId
 */
export const getConfigById = (configId) => {
  return api.get(`/files/${configId}`);
};

/**
 * Actualiza los campos de configuración permitidos.
 * PATCH /files/{config_id}
 * @param {number|string} configId
 * @param {object} data - Campos a actualizar.
 */
export const updateConfig = (configId, data) => {
  return api.patch(`/files/${configId}`, data);
};

/**
 * Elimina la configuración con el ID dado.
 * DELETE /files/{config_id}
 * @param {number|string} configId
 */
export const deleteConfig = (configId) => {
  return api.delete(`/files/${configId}`);
};

/**
 * Envía a HDFS el fichero indicado.
 * POST /files/push/{file_name}
 * @param {string} fileName
 */
export const pushToHDFS = (fileName) => {
  return api.post(`/files/push/${fileName}`);
};

/**
 * Construye la URL para descargar el fichero.
 * GET /files/download/{file_name}
 * @param {string} fileName
 */
export const downloadFileUrl = (fileName) => {
  return `${api.defaults.baseURL}/files/download/${fileName}`;
};
