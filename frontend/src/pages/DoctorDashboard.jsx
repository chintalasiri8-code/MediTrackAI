import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTreatment } from '../context/TreatmentContext';
import { Stethoscope, Search, User, ShieldCheck, ArrowRight, Plus, CheckCircle2, Copy, Check, Trash2, AlertCircle, X } from 'lucide-react';

export const DoctorDashboard = ({ onOpenTreatmentSpace, onOpenCreateModal }) => {
  const { user } = useAuth();
  const { cases, searchQuery, setSearchQuery, deleteCase } = useTreatment();
  const [copiedId, setCopiedId] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const filteredCases = cases.filter(c => 
    !searchQuery ||
    c.patientName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.treatmentName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.caseId?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleCopy = (caseId, e) => {
    e.stopPropagation();
    navigator.clipboard.writeText(caseId);
    setCopiedId(caseId);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDeleteCase = async (caseId, e) => {
    if (e) e.stopPropagation();
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

      {/* Doctor Hero Welcome Banner */}
      <div className="card" style={{ background: 'linear-gradient(135deg, #0F172A, #1E293B)', color: '#FFF', border: 'none', boxShadow: 'var(--shadow-lg)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#38BDF8', fontWeight: 700 }}>
              DOCTOR CLINICAL PORTAL
            </span>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#FFF', marginTop: '0.2rem' }}>
              Welcome, {user?.name || 'Dr. Anitha'}
            </h1>
            <p style={{ fontSize: '0.9rem', color: '#94A3B8', marginTop: '0.3rem' }}>
              Specialty: <strong>{user?.specialty || 'Dermatology'}</strong> • Apollo Skin & Wellness Center
            </p>
          </div>

          <button className="btn btn-primary" onClick={onOpenCreateModal} style={{ boxShadow: '0 4px 14px rgba(14,165,233,0.4)' }}>
            <Plus size={18} /> Initialize New Treatment Case
          </button>
        </div>
      </div>

      {/* Patient Search Bar */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Search size={20} color="var(--primary)" />
          <input 
            type="text" 
            className="form-control" 
            placeholder="Search Patient Name or Patient Case ID (e.g. Siri or CASE-7F92K)..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            style={{ fontSize: '1rem', border: 'none', background: 'transparent' }}
          />
        </div>
      </div>

      {/* Patient Case Cards Grid */}
      <div>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Stethoscope color="var(--primary)" /> Connected Patient Treatment Cases ({filteredCases.length})
        </h2>

        {filteredCases.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--text-muted)' }}>
            <Stethoscope size={40} style={{ opacity: 0.5, marginBottom: '0.75rem' }} />
            <p style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-main)' }}>No Connected Treatment Cases</p>
            <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>You have not created any treatment cases yet. Click "Initialize New Treatment Case" above to start a case.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
            {filteredCases.map(c => (
              <div key={c.id || c.caseId} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', border: '1px solid var(--border)' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                    <span className={`badge badge-${c.status === 'ACTIVE' ? 'active' : 'warning'}`}>
                      {c.status || 'Active'}
                    </span>
                    <button 
                      className="btn btn-sm btn-outline" 
                      onClick={(e) => handleCopy(c.caseId, e)}
                      style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                      title="Copy Case ID for Patient"
                    >
                      {copiedId === c.caseId ? <Check size={12} color="var(--success)" /> : <Copy size={12} />}
                      <span>{c.caseId}</span>
                    </button>
                  </div>

                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.2rem' }}>
                    {c.patientName || 'Awaiting Patient'}
                  </h3>
                  <p style={{ fontSize: '0.9rem', color: 'var(--primary-dark)', fontWeight: 600, marginBottom: '0.75rem' }}>
                    {c.treatmentName || 'Skin Treatment'}
                  </p>

                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.35rem', marginBottom: '1rem' }}>
                    <div>Doctor Specialty: {c.doctorSpecialty || 'Dermatology'}</div>
                    <div>Started: {c.startDate || '04 Oct 2026'}</div>
                    <div>Patient Status: {c.patientId ? 'Connected' : 'Awaiting Patient Join'}</div>
                  </div>
                </div>

                <div style={{ paddingTop: '0.75rem', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-light)' }}>Role: Attending Doctor</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <button 
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={(e) => handleDeleteCase(c.caseId, e)}
                      disabled={deletingId === c.caseId}
                      style={{ 
                        color: 'var(--danger)', 
                        borderColor: 'var(--danger-light)', 
                        display: 'inline-flex', 
                        alignItems: 'center', 
                        gap: '0.35rem',
                        cursor: deletingId === c.caseId ? 'not-allowed' : 'pointer'
                      }}
                      title="Delete Treatment Case"
                    >
                      <Trash2 size={14} /> {deletingId === c.caseId ? 'Deleting...' : 'Delete'}
                    </button>
                    <button className="btn btn-primary btn-sm" onClick={() => onOpenTreatmentSpace(c.caseId)}>
                      Open Case <ArrowRight size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
