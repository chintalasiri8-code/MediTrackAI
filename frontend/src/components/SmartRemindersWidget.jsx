import React, { useState, useEffect } from 'react';
import { Bell, Plus, Clock, Calendar, Check, X, AlertCircle } from 'lucide-react';
import { useNotification } from '../context/NotificationContext';
import { reminderApi } from '../services/api';

export const SmartRemindersWidget = () => {
  const { activeAlert, triggerReminderAlert, dismissAlert } = useNotification();
  const [showAddModal, setShowAddModal] = useState(false);
  const [reminders, setReminders] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    reminderApi.getMyReminders().then(data => {
      if (Array.isArray(data)) {
        setReminders(data);
      }
    }).catch(err => {
      console.error('Failed to load reminders:', err);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  const [formData, setFormData] = useState({
    medicine: '',
    time: '08:00 AM',
    foodRelation: 'After breakfast',
    repeat: 'Daily',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0]
  });

  const handleCreateReminder = async (e) => {
    e.preventDefault();
    if (!formData.medicine) return;
    try {
      const payload = {
        medicineName: formData.medicine,
        reminderTime: formData.time,
        foodRelation: formData.foodRelation,
        repeatType: formData.repeat,
        startDate: formData.startDate,
        endDate: formData.endDate,
        status: 'PENDING'
      };
      const saved = await reminderApi.createReminder(payload);
      setReminders(prev => [saved, ...prev]);
      triggerReminderAlert(formData.medicine, formData.foodRelation);
      setShowAddModal(false);
      setFormData({ medicine: '', time: '08:00 AM', foodRelation: 'After breakfast', repeat: 'Daily', startDate: new Date().toISOString().split('T')[0], endDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0] });
    } catch (err) {
      alert('Failed to save reminder: ' + err.message);
    }
  };

  const handleReminderAction = async (id, actionStatus) => {
    setReminders(prev => prev.map(r => r.id === id ? { ...r, status: actionStatus } : r));
    try {
      await reminderApi.updateStatus(id, actionStatus);
    } catch (err) {
      console.error('Failed to update reminder status:', err);
    }
    dismissAlert();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Live Reminder Notification Banner */}
      {activeAlert && activeAlert.show && (
        <div className="card animate-fade-in" style={{ background: 'linear-gradient(135deg, #0EA5E9, #0284C7)', color: '#FFF', border: 'none', position: 'relative', overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.875rem' }}>
              <div style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Bell className="animate-bounce" size={24} />
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', tracking: '0.05em', opacity: 0.9, fontWeight: 700 }}>SMART MEDICATION REMINDER</span>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: '0.2rem 0' }}>"{activeAlert.message}"</h3>
                <p style={{ fontSize: '0.8125rem', opacity: 0.85 }}>Scheduled Time: {activeAlert.reminderTime || 'Now'} • {activeAlert.foodRelation}</p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button className="btn btn-sm" onClick={() => handleReminderAction(1, 'TAKEN')} style={{ backgroundColor: '#10B981', color: '#FFF', border: 'none', fontWeight: 700 }}>
                <Check size={14} /> Taken
              </button>
              <button className="btn btn-sm" onClick={() => dismissAlert()} style={{ backgroundColor: 'rgba(255,255,255,0.25)', color: '#FFF', border: 'none' }}>
                Remind Later
              </button>
              <button className="btn btn-sm" onClick={() => handleReminderAction(1, 'MISSED')} style={{ backgroundColor: '#EF4444', color: '#FFF', border: 'none' }}>
                <X size={14} /> Missed
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reminder List Header */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <div>
            <h2 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Bell color="var(--primary)" /> Smart Reminders
            </h2>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Configured automated push & in-app alerts</p>
          </div>
          <button className="btn btn-primary btn-sm" onClick={() => setShowAddModal(true)}>
            <Plus size={16} /> Create Reminder
          </button>
        </div>

        {reminders.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)' }}>
            <p>No active smart reminders yet. Click "Create Reminder" above to set a scheduled alert.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1rem' }}>
            {reminders.map(r => {
              const medName = r.medicineName || r.medicine || 'Medication';
              const remTime = r.reminderTime || r.time || '08:00 AM';
              const repType = r.repeatType || r.repeat || 'Daily';
              const statusStr = (r.status || 'PENDING').toLowerCase();
              return (
                <div key={r.id} style={{ border: '1px solid var(--border)', borderRadius: '12px', padding: '1rem', backgroundColor: 'var(--bg-card)', position: 'relative' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)' }}>{medName}</h4>
                    <span className={`badge badge-${statusStr}`}>{r.status || 'PENDING'}</span>
                  </div>

                  <div style={{ marginTop: '0.75rem', fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Clock size={14} color="var(--primary)" /> <span>{remTime} ({r.foodRelation || 'With food'})</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Calendar size={14} color="var(--accent)" /> <span>{r.startDate || 'Today'} to {r.endDate || 'Ongoing'} ({repType})</span>
                    </div>
                  </div>

                  <div style={{ marginTop: '0.875rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border)', display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                    <button className="btn btn-sm btn-outline" onClick={() => handleReminderAction(r.id, 'TAKEN')}>
                      Mark Taken
                    </button>
                    <button className="btn btn-sm btn-outline" onClick={() => triggerReminderAlert(medName, r.foodRelation || 'With food')}>
                      Test Alert
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Add Reminder Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 200, padding: '1rem' }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '480px', backgroundColor: 'var(--bg-card)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.15rem' }}>Create Smart Reminder</h3>
              <button style={{ border: 'none', background: 'none', cursor: 'pointer' }} onClick={() => setShowAddModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateReminder}>
              <div className="form-group">
                <label className="form-label">Medicine Name</label>
                <input type="text" className="form-control" placeholder="e.g. Doxycycline 100mg" value={formData.medicine} onChange={e => setFormData({ ...formData, medicine: e.target.value })} required />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Time</label>
                  <input type="text" className="form-control" placeholder="e.g. 08:00 AM" value={formData.time} onChange={e => setFormData({ ...formData, time: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Food Relation</label>
                  <select className="form-control" value={formData.foodRelation} onChange={e => setFormData({ ...formData, foodRelation: e.target.value })}>
                    <option>After breakfast</option>
                    <option>Before breakfast</option>
                    <option>After lunch</option>
                    <option>After dinner</option>
                    <option>Before sleep</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Start Date</label>
                  <input type="date" className="form-control" value={formData.startDate} onChange={e => setFormData({ ...formData, startDate: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="form-label">End Date</label>
                  <input type="date" className="form-control" value={formData.endDate} onChange={e => setFormData({ ...formData, endDate: e.target.value })} />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Repeat</label>
                <select className="form-control" value={formData.repeat} onChange={e => setFormData({ ...formData, repeat: e.target.value })}>
                  <option>Daily</option>
                  <option>Every 2 Days</option>
                  <option>Weekly</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Reminder</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
