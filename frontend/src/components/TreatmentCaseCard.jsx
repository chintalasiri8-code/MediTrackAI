import React, { useState } from 'react';
import { Stethoscope, User, Calendar, Edit2, Check, ArrowRight, ShieldCheck, Trash2 } from 'lucide-react';
import { useTreatment } from '../context/TreatmentContext';

export const TreatmentCaseCard = ({ caseItem, onOpenTreatment, onDeleteCase, isDeleting }) => {
  const { renameCase, deleteCase } = useTreatment();
  const [localDeleting, setLocalDeleting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [labelInput, setLabelInput] = useState(caseItem.patientDisplayLabel || caseItem.treatmentName);

  const handleDelete = async (e) => {
    e.stopPropagation();
    if (onDeleteCase) {
      onDeleteCase(caseItem.caseId);
      return;
    }
    const confirmed = window.confirm(`Are you sure you want to delete Treatment Case ${caseItem.caseId}? This will permanently remove all associated prescriptions, medicines, and recovery history.`);
    if (!confirmed) return;

    setLocalDeleting(true);
    try {
      await deleteCase(caseItem.caseId);
    } catch (err) {
      alert('Failed to delete case: ' + err.message);
    } finally {
      setLocalDeleting(false);
    }
  };

  const handleSaveRename = (e) => {
    e.stopPropagation();
    if (labelInput.trim()) {
      renameCase(caseItem.caseId, labelInput.trim());
      setIsEditing(false);
    }
  };

  const getSpecialtyIcon = (specialty) => {
    if (specialty?.toLowerCase().includes('ortho')) return '🦴';
    if (specialty?.toLowerCase().includes('cardio')) return '🫀';
    return '🩺';
  };

  const deletingActive = isDeleting || localDeleting;

  return (
    <div className="card card-hover" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', height: '100%', border: '1px solid var(--border)' }}>
      <div>
        {/* Specialty & Case ID Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8125rem', fontWeight: 700, color: 'var(--primary)' }}>
            <span>{getSpecialtyIcon(caseItem.doctorSpecialty)}</span>
            <span>{caseItem.doctorSpecialty || 'General Treatment'}</span>
          </div>
          <span className="badge badge-active">{caseItem.status || 'Active'}</span>
        </div>

        {/* Treatment Label with Edit Rename capability */}
        <div style={{ marginBottom: '0.75rem' }}>
          {!isEditing ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-main)' }}>
                {caseItem.patientDisplayLabel || caseItem.treatmentName}
              </h3>
              <button 
                title="Rename Treatment Label"
                onClick={() => setIsEditing(true)} 
                style={{ border: 'none', background: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}
              >
                <Edit2 size={15} />
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <input 
                type="text" 
                className="form-control" 
                value={labelInput} 
                onChange={(e) => setLabelInput(e.target.value)} 
                style={{ padding: '0.3rem 0.5rem', fontSize: '0.9rem' }}
                autoFocus
              />
              <button className="btn btn-sm btn-primary" onClick={handleSaveRename}>
                <Check size={14} />
              </button>
            </div>
          )}
        </div>

        {/* Doctor & Case Metadata */}
        <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.4rem', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={15} color="var(--primary)" />
            <span>Doctor: <strong>{caseItem.doctorName}</strong></span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShieldCheck size={15} color="var(--accent)" />
            <span>Case ID: <strong style={{ color: 'var(--text-main)', letterSpacing: '0.05em' }}>{caseItem.caseId}</strong></span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Calendar size={15} color="var(--text-light)" />
            <span>Started: {caseItem.startDate || '04 Oct 2026'}</span>
          </div>
        </div>
      </div>

      {/* Action Footer */}
      <div style={{ paddingTop: '0.75rem', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
        <span style={{ fontSize: '0.75rem', color: 'var(--text-light)' }}>Last Update: Today</span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button 
            type="button"
            className="btn btn-sm btn-outline"
            onClick={handleDelete}
            disabled={deletingActive}
            style={{ 
              color: 'var(--danger)', 
              borderColor: 'var(--danger-light)', 
              display: 'inline-flex', 
              alignItems: 'center', 
              gap: '0.35rem',
              cursor: deletingActive ? 'not-allowed' : 'pointer'
            }}
            title="Delete Treatment Case"
          >
            <Trash2 size={14} /> {deletingActive ? 'Deleting...' : 'Delete'}
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => onOpenTreatment(caseItem.caseId)}>
            Open Treatment <ArrowRight size={14} />
          </button>
        </div>
      </div>
    </div>
  );
};
