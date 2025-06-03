import React, { useState } from 'react';
import { updateConfig, pushToHDFS, deleteConfig, downloadFileUrl } from '../api/files';

const overlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1000,
};

const modalStyle = {
  background: '#fff',
  borderRadius: '8px',
  width: '90%',
  maxWidth: '600px',
  padding: '1.5rem',
  boxShadow: '0 2px 10px rgba(0,0,0,0.3)',
  position: 'relative',
};

const inputStyle = {
  width: '100%',
  padding: '0.5rem',
  marginBottom: '1rem',
  border: '1px solid #ccc',
  borderRadius: '4px',
};

const buttonStyle = (bg) => ({
  background: bg,
  color: '#fff',
  padding: '0.5rem 1rem',
  border: 'none',
  borderRadius: '4px',
  cursor: 'pointer',
});

const footerStyle = {
  display: 'flex',
  justifyContent: 'flex-end',
  gap: '0.5rem',
  marginTop: '1rem',
};

const FileDetailModal = ({ config, onClose, onRefresh }) => {
  const [formData, setFormData] = useState({
    delimiter: config.delimiter,
    quote_char: config.quote_char,
    escape_char: config.escape_char,
    has_header: config.has_header,
    date_format: config.date_format,
    timestamp_format: config.timestamp_format,
    partition_columns: config.partition_columns || '',
  });
  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const save = async () => {
    setSaving(true);
    try {
      await updateConfig(config.id, formData);
      onRefresh();
      alert('Configuración actualizada');
    } catch (err) {
      console.error('Error updating:', err);
      alert('Error al actualizar');
    } finally {
      setSaving(false);
    }
  };

  const validate = async () => {
    setValidating(true);
    try {
      await pushToHDFS(config.file_name);
      onRefresh();
      alert('Enviado a validar');
    } catch (err) {
      console.error('Error validating:', err);
      alert('Error al validar');
    } finally {
      setValidating(false);
    }
  };

  const remove = async () => {
    if (!window.confirm('¿Estás seguro de que quieres eliminar esta configuración?')) return;
    setDeleting(true);
    try {
      await deleteConfig(config.id);
      onRefresh();
      onClose();
      alert('Configuración eliminada');
    } catch (err) {
      console.error('Error deleting:', err);
      alert('Error al eliminar');
    } finally {
      setDeleting(false);
    }
  };

  const downloadFile = async () => {
    try {
      const response = await fetch(downloadFileUrl(config.file_name), {
        headers: {
          Authorization: localStorage.getItem('access_token')
            ? `Bearer ${localStorage.getItem('access_token')}`
            : '',
        },
      });
      if (!response.ok) throw new Error('Error en la descarga');
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = config.file_name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error descargando:', err);
      alert('No se pudo descargar el fichero.');
    }
  };

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>Detalles de {config.file_name}</h2>
          <button
            type="button"
            onClick={onClose}
            style={{ background: 'transparent', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}
          >
            &times;
          </button>
        </div>

        {/* Body */}
        <div style={{ marginTop: '1rem' }}>
          <label>Delimiter</label>
          <select name="delimiter" value={formData.delimiter} onChange={handleChange} style={inputStyle}>
            <option value=",">Comma (,)</option>
            <option value=";">Semicolon (;)</option>
            <option value={'\t'}>Tab (↹)</option>
            <option value="|">Pipe (|)</option>
          </select>

          <label>Quote Character</label>
          <select name="quote_char" value={formData.quote_char} onChange={handleChange} style={inputStyle}>
            <option value={'"'}>Double Quote (")</option>
            <option value={"'"}>Single Quote (')</option>
            <option value="">None</option>
          </select>

          <label>Escape Character</label>
          <select name="escape_char" value={formData.escape_char} onChange={handleChange} style={inputStyle}>
            <option value={'\\'}>Backslash (\)</option>
            <option value={'"'}>Double Quote (")</option>
            <option value="">None</option>
          </select>

          <label style={{ display: 'flex', alignItems: 'center' }}>
            <input
              type="checkbox"
              name="has_header"
              checked={formData.has_header}
              onChange={handleChange}
              style={{ marginRight: '0.5rem' }}
            />
            Has Header
          </label>

          <label>Date Format</label>
          <select name="date_format" value={formData.date_format} onChange={handleChange} style={inputStyle}>
            <option value="yyyy-MM-dd">YYYY-MM-DD</option>
            <option value="dd/MM/yyyy">DD/MM/YYYY</option>
            <option value="MM-dd-yyyy">MM-DD-YYYY</option>
          </select>

          <label>Timestamp Format</label>
          <select name="timestamp_format" value={formData.timestamp_format} onChange={handleChange} style={inputStyle}>
            <option value="yyyy-MM-dd HH:mm:ss">YYYY-MM-DD HH:mm:ss</option>
            <option value={"yyyy-MM-dd'T'HH:mm:ss"}>ISO 8601</option>
            <option value="epoch_millis">Timestamp MS</option>
          </select>

          <label>Partition Columns</label>
          <input
            type="text"
            name="partition_columns"
            value={formData.partition_columns}
            onChange={handleChange}
            style={inputStyle}
          />
        </div>

        {/* Footer */}
        <div style={footerStyle}>
          <button type="button" onClick={save} disabled={saving} style={buttonStyle('#27ae60')}>
            {saving ? 'Guardando...' : 'Guardar'}
          </button>
          <button type="button" onClick={validate} disabled={validating} style={buttonStyle('#3498db')}>
            {validating ? 'Enviando…' : 'Validar'}
          </button>
          <button type="button" onClick={downloadFile} style={buttonStyle('#3498db')}>
            Descargar
          </button>
          <button type="button" onClick={remove} disabled={deleting} style={buttonStyle('#e74c3c')}>
            {deleting ? 'Eliminando…' : 'Eliminar'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FileDetailModal;
