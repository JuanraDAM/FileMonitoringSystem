import React from 'react';
import { Outlet, Link } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';

const navStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: '1rem',
  borderBottom: '1px solid #ccc',
};

const logoutButtonStyle = {
  background: '#e67e22',
  color: '#fff',
  border: 'none',
  padding: '0.5rem 1rem',
  borderRadius: '4px',
  cursor: 'pointer',
};

const linksContainerStyle = {
  display: 'flex',
  alignItems: 'center',
};

const linkStyle = {
  marginRight: '1rem',
  textDecoration: 'none',
  color: '#333',
  fontWeight: 'bold',
};


const MainLayout = () => {
  const { logout } = useContext(AuthContext);

  return (
    <div>
      <nav style={navStyle}>
        <div style={linksContainerStyle}>
          <Link to="/dashboard" style={linkStyle}>
            Dashboard
          </Link>
          <Link to="/logs" style={linkStyle}>
            Logs
          </Link>
        </div>
        <button onClick={logout} style={logoutButtonStyle}>
          Logout
        </button>
      </nav>
      <main style={{ padding: '1rem' }}>
        <Outlet />
      </main>
    </div>
  );
};

export default MainLayout;
