import React, { useState, useEffect } from 'react';
import { getConfigs, uploadFile, pushToHDFS, deleteConfig } from '../api/files';
import FileDetailModal from '../components/FileDetailModal';

const Dashboard = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [selectedConfig, setSelectedConfig] = useState(null);
  const [actionLoading, setActionLoading] = useState({});

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const res = await getConfigs();
      setConfigs(res.data);
    } catch {
      alert('Error al cargar configuraciones');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleFileChange = e => setFile(e.target.files[0]);

  const handleUpload = async () => {
    if (!file) return alert('Selecciona un fichero.');
    setUploading(true);
    const form = new FormData();
    form.append('file', file);
    try {
      const res = await uploadFile(form);
      alert(`Fichero subido ID: ${res.data.file_config_id}`);
      setFile(null);
      fetchConfigs();
    } catch {
      alert('Error al subir fichero');
    } finally {
      setUploading(false);
    }
  };

  const handleValidate = async cfg => {
    setActionLoading(prev => ({ 
      ...prev, 
      [cfg.id]: { ...(prev[cfg.id]||{}), validating: true } 
    }));
    try {
      await pushToHDFS(cfg.file_name);
      alert(`"${cfg.file_name}" enviado a validar`);
      fetchConfigs();
    } catch {
      alert('Error al validar');
    } finally {
      setActionLoading(prev => ({ 
        ...prev, 
        [cfg.id]: { ...(prev[cfg.id]||{}), validating: false } 
      }));
    }
  };

  const handleDelete = async cfg => {
    if (!window.confirm('¿Estás seguro de que quieres eliminar esta configuración?')) return;
    setActionLoading(prev => ({ 
      ...prev, 
      [cfg.id]: { ...(prev[cfg.id]||{}), deleting: true } 
    }));
    try {
      await deleteConfig(cfg.id);
      alert('Configuración eliminada');
      fetchConfigs();
    } catch {
      alert('Error al eliminar');
    } finally {
      setActionLoading(prev => ({ 
        ...prev, 
        [cfg.id]: { ...(prev[cfg.id]||{}), deleting: false } 
      }));
    }
  };

  return (
    <div className="dashboard-page">
      <h1>Dashboard de Ficheros</h1>

      <div className="upload-section">
        <input type="file" onChange={handleFileChange} className="file-input" />
        <button onClick={handleUpload} disabled={uploading}>
          {uploading ? 'Subiendo...' : 'Subir Fichero'}
        </button>
      </div>

      {loading ? (
        <p>Cargando configuraciones…</p>
      ) : (
        <table className="config-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Path</th>
              <th>Has Header</th>
              <th>Delimiter</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {configs.map(cfg => {
              const { validating=false, deleting=false } = actionLoading[cfg.id] || {};
              return (
              <tr key={cfg.id}>
                <td>{cfg.id}</td>
                <td>{cfg.file_name}</td>
                <td>{cfg.path}</td>
                <td>{cfg.has_header ? 'Sí' : 'No'}</td>
                <td>{cfg.delimiter}</td>
                <td className="actions">
                  <button 
                    onClick={() => handleValidate(cfg)} 
                    disabled={validating}
                  >
                    {validating ? 'Enviando…' : 'Validar'}
                  </button>
                  <button 
                    onClick={() => handleDelete(cfg)} 
                    disabled={deleting}
                    style={{ marginLeft: '0.5rem' }}
                  >
                    {deleting ? 'Eliminando…' : 'Eliminar'}
                  </button>
                  <button
                    onClick={() => setSelectedConfig(cfg)}
                    style={{ marginLeft: '0.5rem' }}
                  >
                    Detalles
                  </button>
                </td>
              </tr>
              );
            })}
          </tbody>
        </table>
      )}

      {selectedConfig && (
        <FileDetailModal
          config={selectedConfig}
          onClose={() => setSelectedConfig(null)}
          onRefresh={fetchConfigs}
        />
      )}
    </div>
  );
};

export default Dashboard;
