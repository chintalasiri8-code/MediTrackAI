import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTreatment } from '../context/TreatmentContext';
import { TreatmentCaseCard } from '../components/TreatmentCaseCard';
import { MedicineTrackerWidget } from '../components/MedicineTrackerWidget';
import { FolderHeart, Plus, Activity, Bell, CheckCircle2, AlertCircle, X } from 'lucide-react';

export const PatientDashboard = ({ onOpenTreatmentSpace, onOpenJoinModal }) => {
  const { user } = useAuth();
  const { cases, searchQuery, deleteCase } = useTreatment();
  const [feedback, setFeedback] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const filteredCases = cases.filter(c => 
    !searchQuery || 
    c.treatmentName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.doctorName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.caseId?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.patientDisplayLabel?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleDeleteCase = async (caseId) => {
    const confirmed = window.confirm(`Are you sure you want to delete Treatment Case ${caseId}? This will permanently remove all associated prescriptions, medicines, and recovery history.`);
    if (!confirmed) return;

    setDeletingId(caseId);
    try {
      await deleteCase(caseId);
      setFeedback({ type: 'success', message: `Treatment case ${caseId} deleted successfully.` });
      setTimeout(() => setFeedback(null), 4000);
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || `Failed to delete treatment case ${caseId}.` });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Action Feedback Banner */}
      {feedback && (
        <div style={{ 
          padding: '0.85rem 1.25rem', 
          borderRadius: '10px', 
          backgroundColor: feedback.type === 'error' ? 'var(--danger-light)' : 'var(--success-light)',
          color: feedback.type === 'error' ? '#B91C1C' : '#047857',
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          border: `1px solid ${feedback.type === 'error' ? '#FCA5A5' : '#6EE7B7'}`,
          fontSize: '0.875rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600 }}>
            {feedback.type === 'error' ? <AlertCircle size={18} /> : <CheckCircle2 size={18} />}
            <span>{feedback.message}</span>
          </div>
          <button style={{ border: 'none', background: 'none', cursor: 'pointer', color: 'inherit' }} onClick={() => setFeedback(null)}>
            <X size={16} />
          </button>
        </div>
      )}

      {/* Patient Hero Welcome Banner */}
      <div className="card" style={{ background: 'linear-gradient(135deg, #0EA5E9, #0284C7)', color: '#FFF', border: 'none', boxShadow: 'var(--shadow-lg)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.9, fontWeight: 700 }}>
              PATIENT TREATMENT JOURNEY PORTAL
            </span>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#FFF', marginTop: '0.2rem' }}>
              Welcome back, {user?.name || 'Siri'}!
            </h1>
            <p style={{ fontSize: '0.9rem', opacity: 0.9, marginTop: '0.3rem' }}>
              You have <strong>{cases.length} Active Treatment Cases</strong>. Track your daily medication, submit recovery feedback, and access digitized prescriptions.
            </p>
          </div>

          <button className="btn" onClick={onOpenJoinModal} style={{ backgroundColor: '#FFF', color: 'var(--primary-dark)', fontWeight: 700 }}>
            <Plus size={18} /> Connect to Doctor / Join Treatment Case
          </button>
        </div>
      </div>

      {/* Treatment Case Cards Section */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <div>
            <h2 style={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <FolderHeart color="var(--primary)" /> Your Active Treatment Cases
            </h2>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Central structured workspaces connected with your doctors</p>
          </div>
        </div>

        {filteredCases.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--text-muted)' }}>
            <FolderHeart size={40} style={{ opacity: 0.5, marginBottom: '0.75rem' }} />
            <p style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-main)' }}>No Active Treatment Cases</p>
            <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>You have not joined any treatment cases yet. Click "Connect to Doctor / Join Treatment Case" above with your Case ID.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
            {filteredCases.map(c => (
              <TreatmentCaseCard 
                key={c.id || c.caseId} 
                caseItem={c} 
                onOpenTreatment={(cId) => onOpenTreatmentSpace(cId)} 
                onDeleteCase={handleDeleteCase}
                isDeleting={deletingId === c.caseId}
              />
            ))}
          </div>
        )}
      </div>

      {/* Prominent Medicine Tracker Widget */}
      <MedicineTrackerWidget />
    </div>
  );
};
