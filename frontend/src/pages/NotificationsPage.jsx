import React from 'react';
import { Bell, CheckCircle2, Clock, Calendar } from 'lucide-react';
import { useNotification } from '../context/NotificationContext';

export const NotificationsPage = () => {
  const { notifications } = useNotification();

  return (
    <div className="card">
      <h2 style={{ fontSize: '1.25rem', marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Bell color="var(--primary)" /> Smart Notifications & Alert Logs
      </h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {notifications.map(n => (
          <div key={n.id} style={{ padding: '1rem', borderRadius: '12px', border: '1px solid var(--border)', backgroundColor: n.unread ? 'var(--primary-light)' : 'var(--bg-main)', display: 'flex', alignItems: 'flex-start', gap: '1rem' }}>
            <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: 'var(--primary)', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Bell size={18} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 700 }}>{n.title}</h4>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{n.time}</span>
              </div>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>{n.message}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
