import React, { useState, useEffect } from 'react';
import { getLogs } from '../api/logs';

const LogsPage = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState('asc'); // 'asc' or 'desc'

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const response = await getLogs();
      setLogs(response.data);
    } catch (err) {
      console.error('Error al cargar logs:', err);
      setError('No se pudieron cargar los logs.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const handleSort = field => {
    if (sortField === field) {
      // Si ya estamos ordenando por este campo, invertimos el orden
      setSortOrder(prevOrder => (prevOrder === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const sortedLogs = React.useMemo(() => {
    if (!sortField) return logs;

    const sorted = [...logs].sort((a, b) => {
      let aValue = a[sortField];
      let bValue = b[sortField];

      // Si es fecha, convertimos a Date para comparar
      if (sortField === 'logged_at') {
        aValue = new Date(aValue);
        bValue = new Date(bValue);
      }

      // Si es número, comparamos directamente
      if (typeof aValue === 'number' && typeof bValue === 'number') {
        return sortOrder === 'asc' ? aValue - bValue : bValue - aValue;
      }

      const aStr = String(aValue).toLowerCase();
      const bStr = String(bValue).toLowerCase();
      if (aStr < bStr) return sortOrder === 'asc' ? -1 : 1;
      if (aStr > bStr) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    return sorted;
  }, [logs, sortField, sortOrder]);

  const renderArrow = field => {
    if (sortField !== field) return null;
    return sortOrder === 'asc' ? ' ▲' : ' ▼';
  };

  if (loading) return <p>Cargando logs…</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <div className="logs-page">
      <h1>Logs de Validación</h1>
      <table className="logs-table">
        <thead>
          <tr>
            <th onClick={() => handleSort('id')} style={{ cursor: 'pointer' }}>
              ID{renderArrow('id')}
            </th>
            <th onClick={() => handleSort('file_config_id')} style={{ cursor: 'pointer' }}>
              Config ID{renderArrow('file_config_id')}
            </th>
            <th onClick={() => handleSort('file_name')} style={{ cursor: 'pointer' }}>
              Fichero{renderArrow('file_name')}
            </th>
            <th onClick={() => handleSort('field_name')} style={{ cursor: 'pointer' }}>
              Campo{renderArrow('field_name')}
            </th>
            <th onClick={() => handleSort('environment')} style={{ cursor: 'pointer' }}>
              Entorno{renderArrow('environment')}
            </th>
            <th onClick={() => handleSort('validation_flag')} style={{ cursor: 'pointer' }}>
              Flag{renderArrow('validation_flag')}
            </th>
            <th onClick={() => handleSort('error_message')} style={{ cursor: 'pointer' }}>
              Mensaje{renderArrow('error_message')}
            </th>
            <th onClick={() => handleSort('logged_at')} style={{ cursor: 'pointer' }}>
              Fecha{renderArrow('logged_at')}
            </th>
          </tr>
        </thead>
        <tbody>
          {sortedLogs.map(l => {
            // Convertir a Date y mostrar en hora local de Madrid
            const localString = new Date(l.logged_at).toLocaleString('es-ES', {
              timeZone: 'Europe/Madrid',
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            });
            return (
              <tr key={l.id}>
                <td>{l.id}</td>
                <td>{l.file_config_id}</td>
                <td>{l.file_name}</td>
                <td>{l.field_name}</td>
                <td>{l.environment}</td>
                <td>{l.validation_flag}</td>
                <td>{l.error_message}</td>
                <td>{localString}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default LogsPage;
