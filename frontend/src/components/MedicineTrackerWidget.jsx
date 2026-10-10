import React, { useState, useEffect } from 'react';
import { Pill, CheckCircle2, Clock, AlertCircle, Sun, Sunset, Moon, Coffee } from 'lucide-react';
import { medicineApi } from '../services/api';

export const MedicineTrackerWidget = ({ medicines, onUpdateStatus }) => {
  const [medList, setMedList] = useState(Array.isArray(medicines) ? medicines : []);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (medicines !== undefined) {
      setMedList(Array.isArray(medicines) ? medicines : []);
      return;
    }

    let isMounted = true;
    setLoading(true);
    medicineApi.getMyMedicines().then(data => {
      if (isMounted && Array.isArray(data)) {
        setMedList(data);
      }
    }).catch(err => {
      console.error('Failed to load medicines:', err);
    }).finally(() => {
      if (isMounted) {
        setLoading(false);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [medicines]);

  const toggleStatus = async (id, newStatus) => {
    setMedList(prev => prev.map(m => m.id === id ? { ...m, status: newStatus } : m));
    try {
      await medicineApi.updateStatus(id, newStatus);
    } catch (err) {
      console.error('Failed to persist medicine status:', err);
    }
    if (onUpdateStatus) onUpdateStatus(id, newStatus);
  };

  const times = [
    { name: 'Morning', icon: <Sun size={18} color="#F59E0B" />, bg: '#FEF3C7' },
    { name: 'Afternoon', icon: <Coffee size={18} color="#0EA5E9" />, bg: '#E0F2FE' },
    { name: 'Evening', icon: <Sunset size={18} color="#6366F1" />, bg: '#EEF2FF' },
    { name: 'Night', icon: <Moon size={18} color="#8B5CF6" />, bg: '#F3E8FF' }
  ];

  const totalCount = medList.length;
  const takenCount = medList.filter(m => m.status === 'TAKEN').length;
  const progressPercent = totalCount > 0 ? Math.round((takenCount / totalCount) * 100) : 0;

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
        <div>
          <h2 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Pill color="var(--primary)" /> Today's Medicine Tracker
          </h2>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Daily schedule & food relation compliance</p>
        </div>

        {/* Progress Bar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--primary)' }}>{takenCount} of {totalCount} Taken</span>
            <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>{progressPercent}% completed</span>
          </div>
          <div style={{ width: '80px', height: '10px', backgroundColor: 'var(--border)', borderRadius: '5px', overflow: 'hidden' }}>
            <div style={{ width: `${progressPercent}%`, height: '100%', background: 'linear-gradient(90deg, #0EA5E9, #10B981)', transition: 'width 0.3s ease' }} />
          </div>
        </div>
      </div>

      {/* Structured Time Table */}
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th style={{ width: '15%' }}>TIME</th>
              <th style={{ width: '35%' }}>BEFORE FOOD</th>
              <th style={{ width: '35%' }}>AFTER FOOD</th>
              <th style={{ width: '15%', textAlign: 'center' }}>ACTION</th>
            </tr>
          </thead>
          <tbody>
            {times.map(t => {
              const beforeMeds = medList.filter(m => m.timeOfDay === t.name && m.foodRelation.toLowerCase().includes('before'));
              const afterMeds = medList.filter(m => m.timeOfDay === t.name && m.foodRelation.toLowerCase().includes('after'));

              return (
                <tr key={t.name}>
                  <td style={{ fontWeight: 700, verticalAlign: 'top' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.35rem 0.6rem', borderRadius: '8px', backgroundColor: t.bg, width: 'fit-content' }}>
                      {t.icon} <span>{t.name}</span>
                    </div>
                  </td>

                  {/* Before Food Column */}
                  <td style={{ verticalAlign: 'top' }}>
                    {beforeMeds.length > 0 ? beforeMeds.map(m => (
                      <div key={m.id} style={{ padding: '0.5rem', borderRadius: '8px', backgroundColor: 'var(--bg-main)', marginBottom: '0.5rem', borderLeft: '3px solid var(--primary)' }}>
                        <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{m.name}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{m.dosage} • {m.durationDays} days</div>
                        <span className={`badge badge-${m.status.toLowerCase()}`} style={{ marginTop: '0.35rem' }}>
                          {m.status}
                        </span>
                      </div>
                    )) : <span style={{ color: 'var(--text-light)', fontSize: '0.8125rem' }}>No medicines before food</span>}
                  </td>

                  {/* After Food Column */}
                  <td style={{ verticalAlign: 'top' }}>
                    {afterMeds.length > 0 ? afterMeds.map(m => (
                      <div key={m.id} style={{ padding: '0.5rem', borderRadius: '8px', backgroundColor: 'var(--bg-main)', marginBottom: '0.5rem', borderLeft: '3px solid var(--accent)' }}>
                        <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{m.name}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{m.dosage} • {m.durationDays} days</div>
                        <span className={`badge badge-${m.status.toLowerCase()}`} style={{ marginTop: '0.35rem' }}>
                          {m.status}
                        </span>
                      </div>
                    )) : <span style={{ color: 'var(--text-light)', fontSize: '0.8125rem' }}>No medicines after food</span>}
                  </td>

                  {/* Interactive Status Actions */}
                  <td style={{ verticalAlign: 'top', textAlign: 'center' }}>
                    {[...beforeMeds, ...afterMeds].map(m => (
                      <div key={m.id} style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', marginBottom: '0.5rem' }}>
                        {m.status !== 'TAKEN' ? (
                          <button className="btn btn-sm btn-primary" onClick={() => toggleStatus(m.id, 'TAKEN')}>
                            <CheckCircle2 size={12} /> Mark Taken
                          </button>
                        ) : (
                          <button className="btn btn-sm btn-outline" onClick={() => toggleStatus(m.id, 'PENDING')}>
                            Reset Pending
                          </button>
                        )}
                        <button className="btn btn-sm btn-outline" onClick={() => toggleStatus(m.id, 'MISSED')} style={{ color: 'var(--danger)' }}>
                          Mark Missed
                        </button>
                      </div>
                    ))}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
