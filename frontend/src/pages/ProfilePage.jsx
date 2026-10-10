import React from 'react';
import { useAuth } from '../context/AuthContext';
import { User, Mail, Phone, ShieldCheck, Stethoscope, Award, LogOut } from 'lucide-react';

export const ProfilePage = () => {
  const { user, isDoctor, logout } = useAuth();

  return (
    <div className="card" style={{ maxWidth: '640px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <img 
            src={user?.avatarUrl || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150'} 
            alt={user?.name} 
            style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover', border: '3px solid var(--primary)' }}
          />
          <div>
            <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>{user?.name}</h2>
            <span className="badge badge-active">{user?.role || 'PATIENT'}</span>
          </div>
        </div>

        <button className="btn btn-outline" onClick={logout} style={{ color: '#DC2626', borderColor: '#FCA5A5' }}>
          <LogOut size={16} /> Log Out
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Mail color="var(--primary)" size={18} /> <span>Email: <strong>{user?.email}</strong></span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Phone color="var(--primary)" size={18} /> <span>Phone: <strong>{user?.phone}</strong></span>
        </div>
        {isDoctor ? (
          <>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Stethoscope color="var(--accent)" size={18} /> <span>Specialty: <strong>{user?.specialty}</strong></span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Award color="var(--accent)" size={18} /> <span>Hospital: <strong>{user?.hospital}</strong></span>
            </div>
          </>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <ShieldCheck color="var(--success)" size={18} /> <span>Account Status: <strong>Verified Active Patient</strong></span>
          </div>
        )}
      </div>
    </div>
  );
};

