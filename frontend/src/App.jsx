import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { TreatmentProvider, useTreatment } from './context/TreatmentContext';
import { NotificationProvider } from './context/NotificationContext';
import { Navbar } from './components/Navbar';
import { PatientDashboard } from './pages/PatientDashboard';
import { DoctorDashboard } from './pages/DoctorDashboard';
import { TreatmentSpace } from './components/TreatmentSpace';
import { MedicineTrackerPage } from './pages/MedicineTrackerPage';
import { SmartRemindersPage } from './pages/SmartRemindersPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { ProfilePage } from './pages/ProfilePage';
import { Login } from './pages/Login';
import { X, Plus, Key, Copy, CheckCircle2, AlertCircle, ShieldCheck } from 'lucide-react';

const MainContent = () => {
  const { user, isDoctor } = useAuth();
  const { joinCase, createCase } = useTreatment();

  const [activeTab, setActiveTab] = useState('home');
  const [selectedCaseId, setSelectedCaseId] = useState(null);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Modal Inputs & Success Feedback
  const [inputCaseId, setInputCaseId] = useState('');
  const [joinError, setJoinError] = useState('');
  const [joinSuccess, setJoinSuccess] = useState(null);

  const [newCaseData, setNewCaseData] = useState({
    treatmentName: 'Skin Treatment',
    doctorSpecialty: 'Dermatology'
  });

  const [createdCaseResult, setCreatedCaseResult] = useState(null);
  const [copied, setCopied] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user) {
    return <Login onLoginSuccess={() => setActiveTab('home')} />;
  }

  const handleOpenTreatmentSpace = (caseId) => {
    setSelectedCaseId(caseId);
    setActiveTab('space');
  };

  const handleJoinCaseSubmit = async (e) => {
    e.preventDefault();
    setJoinError('');
    setJoinSuccess(null);
    if (!inputCaseId.trim()) return;
    setIsSubmitting(true);
    try {
      const linkedCase = await joinCase(inputCaseId.trim().toUpperCase());
      setJoinSuccess(linkedCase);
      setTimeout(() => {
        setShowJoinModal(false);
        setJoinSuccess(null);
        setInputCaseId('');
        handleOpenTreatmentSpace(linkedCase.caseId);
      }, 1500);
    } catch (err) {
      setJoinError(err.message || 'Failed to join Treatment Case ID.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateCaseSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const created = await createCase({
        treatmentName: newCaseData.treatmentName || 'Skin Treatment',
        doctorSpecialty: newCaseData.doctorSpecialty || 'Dermatology'
      });
      setCreatedCaseResult(created);
    } catch (err) {
      alert('Error creating case: ' + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const copyCreatedCaseId = () => {
    if (createdCaseResult?.caseId) {
      navigator.clipboard.writeText(createdCaseResult.caseId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--bg-main)' }}>
      <Navbar 
        activeTab={activeTab} 
        setActiveTab={setActiveTab} 
        onOpenJoinModal={() => { setJoinError(''); setJoinSuccess(null); setShowJoinModal(true); }}
        onOpenCreateModal={() => { setCreatedCaseResult(null); setShowCreateModal(true); }}
      />

      <main style={{ flex: 1, maxWidth: '1280px', width: '100%', margin: '0 auto', padding: '1.5rem' }}>
        {activeTab === 'space' && selectedCaseId && (
          <TreatmentSpace caseId={selectedCaseId} onBack={() => setActiveTab('home')} />
        )}

        {activeTab === 'home' && (!isDoctor ? (
          <PatientDashboard 
            onOpenTreatmentSpace={handleOpenTreatmentSpace} 
            onOpenJoinModal={() => { setJoinError(''); setJoinSuccess(null); setShowJoinModal(true); }} 
          />
        ) : (
          <DoctorDashboard 
            onOpenTreatmentSpace={handleOpenTreatmentSpace} 
            onOpenCreateModal={() => { setCreatedCaseResult(null); setShowCreateModal(true); }} 
          />
        ))}

        {activeTab === 'cases' && (!isDoctor ? (
          <PatientDashboard 
            onOpenTreatmentSpace={handleOpenTreatmentSpace} 
            onOpenJoinModal={() => { setJoinError(''); setJoinSuccess(null); setShowJoinModal(true); }} 
          />
        ) : (
          <DoctorDashboard 
            onOpenTreatmentSpace={handleOpenTreatmentSpace} 
            onOpenCreateModal={() => { setCreatedCaseResult(null); setShowCreateModal(true); }} 
          />
        ))}

        {activeTab === 'patients' && (
          <DoctorDashboard 
            onOpenTreatmentSpace={handleOpenTreatmentSpace} 
            onOpenCreateModal={() => { setCreatedCaseResult(null); setShowCreateModal(true); }} 
          />
        )}

        {activeTab === 'tracker' && <MedicineTrackerPage />}
        {activeTab === 'notifications' && <NotificationsPage />}
        {activeTab === 'profile' && <ProfilePage />}
        {activeTab === 'followups' && <DoctorDashboard onOpenTreatmentSpace={handleOpenTreatmentSpace} onOpenCreateModal={() => { setCreatedCaseResult(null); setShowCreateModal(true); }} />}
      </main>

      {/* Footer Core Statement */}
      <footer style={{ borderTop: '1px solid var(--border)', backgroundColor: 'var(--bg-card)', padding: '1.5rem', textAlign: 'center', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
        <p style={{ fontWeight: 600 }}>MediTrack AI — Smart Treatment Journey Management System</p>
        <p style={{ marginTop: '0.25rem' }}>"MediTrack AI transforms fragmented treatment information into a continuous, structured and trackable treatment journey."</p>
      </footer>

      {/* Join Case Modal */}
      {showJoinModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 200, padding: '1rem' }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '420px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Key color="var(--primary)" size={20} /> Connect to Doctor / Join Treatment Case
              </h3>
              <button style={{ border: 'none', background: 'none', cursor: 'pointer' }} onClick={() => setShowJoinModal(false)}>
                <X size={20} />
              </button>
            </div>

            {joinError && (
              <div style={{ padding: '0.75rem', backgroundColor: 'var(--danger-light)', color: '#B91C1C', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <AlertCircle size={16} /> {joinError}
              </div>
            )}

            {joinSuccess ? (
              <div style={{ padding: '1rem', backgroundColor: 'var(--success-light)', color: '#047857', borderRadius: '10px', textAlign: 'center' }}>
                <CheckCircle2 size={32} style={{ margin: '0 auto 0.5rem' }} />
                <h4 style={{ fontWeight: 700 }}>Successfully Joined Case!</h4>
                <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>
                  Connected to <strong>{joinSuccess.treatmentName}</strong> with <strong>{joinSuccess.doctorName}</strong> ({joinSuccess.doctorSpecialty}). Opening Treatment Space...
                </p>
              </div>
            ) : (
              <form onSubmit={handleJoinCaseSubmit}>
                <div className="form-group">
                  <label className="form-label">Enter Unique Treatment Case ID</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="e.g. CASE-7F92K" 
                    value={inputCaseId} 
                    onChange={e => setInputCaseId(e.target.value)} 
                    required 
                    style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 700 }}
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Enter the fresh Case ID generated by your attending doctor.</span>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                  <button type="button" className="btn btn-outline" onClick={() => setShowJoinModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                    {isSubmitting ? 'Verifying Case ID...' : 'Join Treatment Case'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Create Case Modal (Doctor) */}
      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 200, padding: '1rem' }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '460px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Plus color="var(--primary)" size={20} /> Initialize New Treatment Case
              </h3>
              <button style={{ border: 'none', background: 'none', cursor: 'pointer' }} onClick={() => setShowCreateModal(false)}>
                <X size={20} />
              </button>
            </div>

            {!createdCaseResult ? (
              <form onSubmit={handleCreateCaseSubmit}>
                <div className="form-group">
                  <label className="form-label">1. Treatment Name</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="e.g. Skin Treatment or Knee Treatment" 
                    value={newCaseData.treatmentName} 
                    onChange={e => setNewCaseData({ ...newCaseData, treatmentName: e.target.value })} 
                    required 
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">2. Doctor's Specialty</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="e.g. Dermatology or Orthopedics" 
                    value={newCaseData.doctorSpecialty} 
                    onChange={e => setNewCaseData({ ...newCaseData, doctorSpecialty: e.target.value })} 
                    required 
                  />
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
                  <button type="button" className="btn btn-outline" onClick={() => setShowCreateModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                    {isSubmitting ? 'Generating Case ID...' : 'Generate Case ID and Create'}
                  </button>
                </div>
              </form>
            ) : (
              /* Display Generated Case ID Result with Copy Button */
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{ padding: '1rem', backgroundColor: 'var(--primary-light)', border: '2px solid var(--primary)', borderRadius: '12px', textAlign: 'center' }}>
                  <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', tracking: '0.05em', color: 'var(--primary-dark)', fontWeight: 700 }}>
                    GENERATED TREATMENT CASE ID
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.75rem', margin: '0.5rem 0' }}>
                    <span style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary-dark)', letterSpacing: '0.08em' }}>
                      {createdCaseResult.caseId}
                    </span>
                    <button className="btn btn-sm btn-primary" onClick={copyCreatedCaseId}>
                      {copied ? <CheckCircle2 size={16} /> : <Copy size={16} />}
                      {copied ? 'Copied!' : 'Copy ID'}
                    </button>
                  </div>
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Share this Case ID with your patient so they can join the Treatment Space.</span>
                </div>

                <div style={{ fontSize: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.4rem', backgroundColor: 'var(--bg-main)', padding: '0.875rem', borderRadius: '8px' }}>
                  <div>Treatment Name: <strong>{createdCaseResult.treatmentName}</strong></div>
                  <div>Doctor Name: <strong>{createdCaseResult.doctorName}</strong></div>
                  <div>Specialty: <strong>{createdCaseResult.doctorSpecialty}</strong></div>
                  <div>Case Status: <span className="badge badge-active">{createdCaseResult.status || 'AWAITING_PATIENT'}</span></div>
                  <div>Patient Status: <span>Awaiting Patient Join</span></div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                  <button className="btn btn-outline" onClick={() => setShowCreateModal(false)}>Close</button>
                  <button className="btn btn-primary" onClick={() => { setShowCreateModal(false); handleOpenTreatmentSpace(createdCaseResult.caseId); }}>
                    Open Treatment Space
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <TreatmentProvider>
        <NotificationProvider>
          <MainContent />
        </NotificationProvider>
      </TreatmentProvider>
    </AuthProvider>
  );
}
