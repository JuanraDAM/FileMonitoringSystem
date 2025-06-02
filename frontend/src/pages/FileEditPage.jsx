// src/pages/FileEditPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getConfigById, updateConfig } from '../api/files';

const delimiterOptions = [
  { label: 'Comma (,)', value: ',' },
  { label: 'Semicolon (;)', value: ';' },
  { label: 'Tab (\t)', value: '\t' },
  { label: 'Pipe (|)', value: '|' }
];
const quoteOptions = [
  { label: 'Double Quote (")', value: '"' },
  { label: "Single Quote (')", value: "'" },
  { label: 'None', value: '' }
];
const escapeOptions = [
  { label: 'Backslash (\\)', value: '\\' },
  { label: 'Double Quote (")', value: '"' },
  { label: 'None', value: '' }
];
const dateFormatOptions = [
  { label: 'YYYY-MM-DD', value: 'yyyy-MM-dd' },
  { label: 'DD/MM/YYYY', value: 'dd/MM/yyyy' },
  { label: 'MM-DD-YYYY', value: 'MM-dd-yyyy' }
];
const timestampOptions = [
  { label: 'YYYY-MM-DD HH:mm:ss', value: 'yyyy-MM-dd HH:mm:ss' },
  { label: 'ISO 8601', value: "yyyy-MM-dd'T'HH:mm:ss" },
  { label: 'Timestamp MS', value: 'epoch_millis' }
];

const FileEditPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [config, setConfig] = useState(null);
  const [formData, setFormData] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchConfig = async () => {
      setLoading(true);
      try {
        const { data } = await getConfigById(id);
        setConfig(data);
        const {
          delimiter,
          quote_char,
          escape_char,
          has_header,
          date_format,
          timestamp_format,
          partition_columns
        } = data;
        setFormData({
          delimiter,
          quote_char,
          escape_char,
          has_header,
          date_format,
          timestamp_format,
          partition_columns: partition_columns || ''
        });
      } catch (err) {
        console.error('Error al cargar configuración:', err);
        setError('No se pudo cargar la configuración.');
      } finally {
        setLoading(false);
      }
    };
    fetchConfig();
  }, [id]);

  const handleChange = e => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await updateConfig(id, formData);
      alert('Configuración actualizada correctamente.');
      navigate('/dashboard');
    } catch (err) {
      console.error('Error al actualizar:', err);
      setError('Error al actualizar la configuración.');
    } finally {
      setLoading(false);
    }
  };

  if (loading || !config) return <p>Cargando...</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <div className="file-edit-page">
      <h1>Editar Configuración #{id}</h1>
      <form onSubmit={handleSubmit} className="edit-form">
        <div>
          <label>Delimiter</label>
          <select name="delimiter" value={formData.delimiter} onChange={handleChange} required>
            {delimiterOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label>Quote Character</label>
          <select name="quote_char" value={formData.quote_char} onChange={handleChange} required>
            {quoteOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label>Escape Character</label>
          <select name="escape_char" value={formData.escape_char} onChange={handleChange} required>
            {escapeOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div className="checkbox-group">
          <label>
            <input
              type="checkbox"
              name="has_header"
              checked={formData.has_header}
              onChange={handleChange}
            />{' '}
            Has Header
          </label>
        </div>
        <div>
          <label>Date Format</label>
          <select name="date_format" value={formData.date_format} onChange={handleChange}>
            {dateFormatOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label>Timestamp Format</label>
          <select name="timestamp_format" value={formData.timestamp_format} onChange={handleChange}>
            {timestampOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label>Partition Columns (comma separated)</label>
          <input
            name="partition_columns"
            value={formData.partition_columns}
            onChange={handleChange}
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Actualizando...' : 'Guardar Cambios'}
        </button>
      </form>
    </div>
  );
};

export default FileEditPage;
