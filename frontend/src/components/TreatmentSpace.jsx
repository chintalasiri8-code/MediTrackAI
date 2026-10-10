import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, User, Stethoscope, Calendar, FileText, Camera, Upload, 
  Sparkles, Clock, CheckCircle2, AlertCircle, Plus, ChevronRight, MessageSquare, Save, History, Activity
} from 'lucide-react';
import { treatmentSpaceApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

export const TreatmentSpace = ({ caseId = 'CASE-7F92K', onBack }) => {
  const { isDoctor, user } = useAuth();
  const [activeSubTab, setActiveSubTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);

  // Upload modal & File state
  const [showUploadRxModal, setShowUploadRxModal] = useState(false);
  const [rxTitle, setRxTitle] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);

  // Weekly Recovery Feedback Form state
  const [weekNumber, setWeekNumber] = useState(4);
  const [feedbackForm, setFeedbackForm] = useState({
    q1CurrentSymptoms: '',
    q2SymptomsImproved: 'Yes',
    q3NewSymptoms: '',
    q4SymptomsWorse: 'No',
    q5SideEffects: '',
    q6FollowedSchedule: 'Yes',
    q7MissedMedicines: 'None',
    q8ImprovementsNoticed: '',
    q9ChallengesFaced: '',
    q10DoctorNotes: ''
  });

  const [aiPreviewSummary, setAiPreviewSummary] = useState(null);
  const [overallProgressText, setOverallProgressText] = useState('');
  const [doctorNoteInput, setDoctorNoteInput] = useState('');

  const loadSpaceData = async () => {
    setLoading(true);
    try {
      const spaceRes = await treatmentSpaceApi.getData(caseId);
      setData(spaceRes);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSpaceData();
  }, [caseId]);

  useEffect(() => {
    if (data?.feedbacks) {
      const existing = data.feedbacks.find(f => f.weekNumber === weekNumber);
      if (existing) {
        setFeedbackForm({
          q1CurrentSymptoms: existing.q1CurrentSymptoms || '',
          q2SymptomsImproved: existing.q2SymptomsImproved || 'Yes',
          q3NewSymptoms: existing.q3NewSymptoms || '',
          q4SymptomsWorse: existing.q4SymptomsWorse || 'No',
          q5SideEffects: existing.q5SideEffects || '',
          q6FollowedSchedule: existing.q6FollowedSchedule || 'Yes',
          q7MissedMedicines: existing.q7MissedMedicines || 'None',
          q8ImprovementsNoticed: existing.q8ImprovementsNoticed || '',
          q9ChallengesFaced: existing.q9ChallengesFaced || '',
          q10DoctorNotes: existing.q10DoctorNotes || ''
        });
      } else {
        setFeedbackForm({
          q1CurrentSymptoms: '',
          q2SymptomsImproved: 'Yes',
          q3NewSymptoms: '',
          q4SymptomsWorse: 'No',
          q5SideEffects: '',
          q6FollowedSchedule: 'Yes',
          q7MissedMedicines: 'None',
          q8ImprovementsNoticed: '',
          q9ChallengesFaced: '',
          q10DoctorNotes: ''
        });
      }
    }
  }, [weekNumber, data]);

  const handleUploadPrescription = async (e) => {
    e.preventDefault();
    if (!selectedFile) return;
    setLoading(true);
    await treatmentSpaceApi.uploadPrescription(caseId, selectedFile, rxTitle);
    setShowUploadRxModal(false);
    setSelectedFile(null);
    setRxTitle('');
    await loadSpaceData();
    setActiveSubTab('prescription');
  };

  const handleUploadDocument = async (e) => {
    const file = e.target.files[0];
    if (file) {
      setLoading(true);
      await treatmentSpaceApi.uploadDocument(caseId, file, isDoctor ? 'DOCTOR' : 'PATIENT');
      await loadSpaceData();
      setActiveSubTab('documents');
    }
  };

  const handleSaveDraft = async () => {
    setLoading(true);
    try {
      await treatmentSpaceApi.saveDraft({
        caseId,
        weekNumber,
        ...feedbackForm
      });
      await loadSpaceData();
      alert(`Week ${weekNumber} draft saved successfully!`);
    } catch (err) {
      alert('Error saving draft: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSummarizeFeedback = async () => {
    setLoading(true);
    try {
      const result = await treatmentSpaceApi.summarizeFeedback({ caseId, weekNumber, ...feedbackForm });
      setAiPreviewSummary(result.aiSummary);
    } catch (err) {
      alert('Error summarizing feedback: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSummary = async () => {
    if (!aiPreviewSummary) return;
    setLoading(true);
    try {
      await treatmentSpaceApi.saveSummary({
        caseId,
        weekNumber,
        ...feedbackForm
      });
      setAiPreviewSummary(null);
      setFeedbackForm({
        q1CurrentSymptoms: '', q2SymptomsImproved: 'Yes', q3NewSymptoms: '', q4SymptomsWorse: 'No',
        q5SideEffects: '', q6FollowedSchedule: 'Yes', q7MissedMedicines: 'None', q8ImprovementsNoticed: '',
        q9ChallengesFaced: '', q10DoctorNotes: ''
      });
      await loadSpaceData();
      setActiveSubTab('ai-summary');
    } catch (err) {
      alert('Error saving summary: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateOverallProgress = async () => {
    setLoading(true);
    const text = await treatmentSpaceApi.getOverallProgressSummary(caseId);
    setOverallProgressText(text);
    setLoading(false);
  };

  const handleAddDoctorNote = async (e) => {
    e.preventDefault();
    if (!doctorNoteInput.trim()) return;
    setLoading(true);
    await treatmentSpaceApi.addDoctorNote(caseId, user.id, user.name, doctorNoteInput);
    setDoctorNoteInput('');
    await loadSpaceData();
  };

  if (loading && !data) {
    return (
      <div style={{ padding: '4rem', textAlign: 'center' }}>
        <Activity className="animate-spin" size={40} color="var(--primary)" />
        <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Loading Central Treatment Space...</p>
      </div>
    );
  }

  const tc = data?.treatmentCase || { caseId, patientName: 'Siri', doctorName: 'Dr. Anitha', treatmentName: 'Skin Treatment', startDate: '2026-10-04', status: 'ACTIVE' };
  const prescriptions = data?.prescriptions || [];
  const ocr = data?.ocrResult || null;
  const docs = data?.documents || [];
  const medicines = data?.medicines || [];
  const aiSummaries = data?.aiSummaries || [];
  const timeline = data?.timeline || [];
  const doctorNotes = data?.doctorNotes || [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* 1. CENTRAL TREATMENT SPACE HEADER */}
      <div className="card" style={{ background: 'linear-gradient(135deg, #0F172A, #1E293B)', color: '#FFF', border: 'none' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span style={{ backgroundColor: 'var(--primary)', color: '#FFF', padding: '0.2rem 0.6rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 700 }}>
                TREATMENT SPACE
              </span>
              <span className="badge badge-active">{tc.status}</span>
            </div>

            <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#FFF' }}>
              {tc.patientDisplayLabel || tc.treatmentName}
            </h1>

            <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.75rem', fontSize: '0.875rem', color: '#94A3B8', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Stethoscope size={16} color="#38BDF8" /> Doctor: <strong style={{ color: '#FFF' }}>{tc.doctorName}</strong> ({tc.doctorSpecialty || 'Dermatologist'})
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <User size={16} color="#38BDF8" /> Patient: <strong style={{ color: '#FFF' }}>{tc.patientName}</strong>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <ShieldCheck size={16} color="#818CF8" /> Case ID: <strong style={{ color: '#818CF8', letterSpacing: '0.05em' }}>{tc.caseId}</strong>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Calendar size={16} color="#94A3B8" /> Started: {tc.startDate}
              </div>
            </div>
          </div>

          <button className="btn btn-outline" onClick={onBack} style={{ color: '#FFF', borderColor: '#334155' }}>
            ← Back to Dashboard
          </button>
        </div>

        {/* Core Product Statement Banner */}
        <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid #334155', fontSize: '0.8125rem', color: '#94A3B8', fontStyle: 'italic' }}>
          "Instead of simply storing what the doctor prescribed, MediTrack tracks what happens after the prescription."
        </div>
      </div>

      {/* 2. TREATMENT SPACE NAVIGATION TABS */}
      <div style={{ display: 'flex', gap: '0.5rem', overflowX: 'auto', paddingBottom: '0.25rem', borderBottom: '1px solid var(--border)' }}>
        {[
          { id: 'overview', label: 'Overview', icon: <Activity size={16} /> },
          { id: 'timeline', label: 'Timeline', icon: <Clock size={16} /> },
          { id: 'prescription', label: 'Prescription & OCR', icon: <FileText size={16} /> },
          { id: 'documents', label: 'Documents', icon: <Upload size={16} /> },
          { id: 'medicines', label: 'Medicines', icon: <CheckCircle2 size={16} /> },
          { id: 'recovery', label: 'Recovery Check-in', icon: <MessageSquare size={16} /> },
          { id: 'ai-summary', label: 'AI Summary & History', icon: <Sparkles size={16} /> }
        ].map(tab => (
          <button 
            key={tab.id}
            className={`btn btn-sm ${activeSubTab === tab.id ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setActiveSubTab(tab.id)}
            style={{ borderRadius: '20px', whiteSpace: 'nowrap' }}
          >
            {tab.icon} {tab.label}
          </button>
        ))}
      </div>

      {/* 3. TAB CONTENTS */}

      {/* TAB: OVERVIEW */}
      {activeSubTab === 'overview' && (
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.25rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Quick Summary Card */}
            <div className="card">
              <h3 style={{ fontSize: '1.1rem', marginBottom: '0.75rem', color: 'var(--primary)' }}>Treatment Space Status</h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                This central workspace manages all prescriptions, digitized OCR findings, document files, medication compliance, and AI weekly recovery feedback for treatment case <strong>{tc.caseId}</strong>.
              </p>
              
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginTop: '1rem' }}>
                <div style={{ backgroundColor: 'var(--bg-main)', padding: '0.75rem', borderRadius: '10px', textAlign: 'center' }}>
                  <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>{medicines.length}</span>
                  <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>Active Medicines</span>
                </div>
                <div style={{ backgroundColor: 'var(--bg-main)', padding: '0.75rem', borderRadius: '10px', textAlign: 'center' }}>
                  <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent)' }}>{aiSummaries.length}</span>
                  <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>Weekly Summaries</span>
                </div>
                <div style={{ backgroundColor: 'var(--bg-main)', padding: '0.75rem', borderRadius: '10px', textAlign: 'center' }}>
                  <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)' }}>{docs.length}</span>
                  <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)' }}>Uploaded Files</span>
                </div>
              </div>
            </div>

            {/* Latest Doctor Notes */}
            <div className="card">
              <h3 style={{ fontSize: '1.1rem', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Stethoscope color="var(--primary)" /> Doctor Clinical Notes & Review
              </h3>
              {doctorNotes.length > 0 ? doctorNotes.map(n => (
                <div key={n.id} style={{ backgroundColor: 'var(--primary-light)', padding: '1rem', borderRadius: '12px', marginBottom: '0.75rem', borderLeft: '4px solid var(--primary)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem', fontWeight: 700, fontSize: '0.875rem' }}>
                    <span>{n.doctorName}</span>
                    <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '0.75rem' }}>{n.createdDate ? new Date(n.createdDate).toLocaleDateString() : 'Recent'}</span>
                  </div>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-main)', lineHeight: 1.5 }}>{n.noteText}</p>
                </div>
              )) : <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No clinical notes added yet.</p>}

              {/* Add Note if Doctor */}
              {isDoctor && (
                <form onSubmit={handleAddDoctorNote} style={{ marginTop: '1rem' }}>
                  <textarea 
                    className="form-control" 
                    placeholder="Add doctor review / follow-up notes for patient..." 
                    value={doctorNoteInput} 
                    onChange={e => setDoctorNoteInput(e.target.value)} 
                    rows={3} 
                  />
                  <button type="submit" className="btn btn-primary btn-sm" style={{ marginTop: '0.5rem' }}>
                    Save Doctor Review
                  </button>
                </form>
              )}
            </div>
          </div>

          {/* Right Column: Quick Timeline */}
          <div className="card">
            <h3 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Recent Timeline Events</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {timeline.slice(-4).map(e => (
                <div key={e.id} style={{ fontSize: '0.8125rem', borderLeft: '2px solid var(--primary)', paddingLeft: '0.6rem' }}>
                  <div style={{ fontWeight: 700, color: 'var(--text-main)' }}>{e.title}</div>
                  <div style={{ color: 'var(--text-muted)' }}>{e.description}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB: TIMELINE */}
      {activeSubTab === 'timeline' && (
        <div className="card">
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock color="var(--primary)" /> Chronological Treatment Timeline
          </h2>
          <div className="timeline">
            {timeline.map((ev, i) => (
              <div key={ev.id || i} className="timeline-item">
                <div className="timeline-marker">
                  <CheckCircle2 size={12} />
                </div>
                <div className="timeline-content">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-main)' }}>{ev.title}</span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{ev.timestamp ? new Date(ev.timestamp).toLocaleString() : 'Date'}</span>
                  </div>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>{ev.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB: PRESCRIPTION & OCR */}
      {activeSubTab === 'prescription' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <FileText color="var(--primary)" /> Prescription & Digitized OCR
              </h2>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Extract structured findings and medicines from prescription documents</p>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button className="btn btn-outline btn-sm" onClick={() => setShowUploadRxModal(true)}>
                <Camera size={14} /> 📷 Camera
              </button>
              <button className="btn btn-primary btn-sm" onClick={() => setShowUploadRxModal(true)}>
                <Upload size={14} /> Upload Prescription
              </button>
            </div>
          </div>

          {/* Digital Prescription Presentation */}
          <div className="card" style={{ border: '2px solid var(--primary-light)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.75rem' }}>
              <h3 style={{ fontSize: '1.1rem', color: 'var(--primary-dark)', letterSpacing: '0.04em' }}>DIGITAL PRESCRIPTION</h3>
              <span className="badge badge-active">OCR Processed</span>
            </div>

            {/* 1. FINDINGS */}
            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ fontSize: '0.95rem', color: 'var(--text-muted)', textTransform: 'uppercase', tracking: '0.05em', marginBottom: '0.5rem' }}>1. FINDINGS</h4>
              <div style={{ backgroundColor: 'var(--bg-main)', padding: '1rem', borderRadius: '10px' }}>
                {ocr?.findingsJson ? JSON.parse(ocr.findingsJson).map((f, idx) => (
                  <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem', fontSize: '0.9rem', color: 'var(--text-main)' }}>
                    <CheckCircle2 size={16} color="var(--primary)" /> <span>{f}</span>
                  </div>
                )) : (
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Mild Acne Vulgaris with localized facial inflammation & erythema on cheek region.</p>
                )}
              </div>
            </div>

            {/* 2. MEDICINES */}
            <div>
              <h4 style={{ fontSize: '0.95rem', color: 'var(--text-muted)', textTransform: 'uppercase', tracking: '0.05em', marginBottom: '0.5rem' }}>2. MEDICINES</h4>
              <div className="table-container">
                <table className="table">
                  <thead>
                    <tr>
                      <th>MEDICINE</th>
                      <th>DOSAGE</th>
                      <th>TIME</th>
                      <th>DURATION</th>
                      <th>PURPOSE</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ocr?.medicinesJson ? JSON.parse(ocr.medicinesJson).map((m, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 700 }}>{m.name}</td>
                        <td>{m.dosage}</td>
                        <td>{m.timeOfDay} ({m.foodRelation})</td>
                        <td>{m.durationDays} days</td>
                        <td style={{ fontStyle: m.purpose?.includes('not specified') ? 'italic' : 'normal', color: 'var(--text-muted)' }}>
                          {m.purpose || 'Purpose not specified in prescription.'}
                        </td>
                      </tr>
                    )) : medicines.map((m, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 700 }}>{m.name}</td>
                        <td>{m.dosage}</td>
                        <td>{m.timeOfDay} ({m.foodRelation})</td>
                        <td>{m.durationDays} days</td>
                        <td style={{ fontStyle: 'italic', color: 'var(--text-muted)' }}>
                          {m.purpose || 'Purpose not specified in prescription.'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB: DOCUMENTS */}
      {activeSubTab === 'documents' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <div>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Upload color="var(--primary)" /> Case Medical Documents
              </h2>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>All uploaded PDF reports, scan reports and follow-ups for {tc.caseId}</p>
            </div>
            <label className="btn btn-primary btn-sm" style={{ cursor: 'pointer' }}>
              <Plus size={16} /> Upload File
              <input type="file" onChange={handleUploadDocument} style={{ display: 'none' }} />
            </label>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '1rem' }}>
            {docs.map(d => (
              <div key={d.id} style={{ border: '1px solid var(--border)', borderRadius: '12px', padding: '1rem', backgroundColor: 'var(--bg-main)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
                  <FileText size={28} color="var(--primary)" />
                  <div>
                    <h4 style={{ fontSize: '0.9rem', fontWeight: 700, wordBreak: 'break-all' }}>{d.fileName}</h4>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Role: {d.uploadedByRole}</span>
                  </div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-light)', marginTop: '0.5rem' }}>
                  Case: {tc.caseId}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB: MEDICINES */}
      {activeSubTab === 'medicines' && (
        <div className="card">
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckCircle2 color="var(--primary)" /> Prescribed Medications Schedule
          </h2>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>MEDICINE</th>
                  <th>DOSAGE</th>
                  <th>TIMING</th>
                  <th>FOOD RELATION</th>
                  <th>DURATION</th>
                  <th>STATUS</th>
                </tr>
              </thead>
              <tbody>
                {medicines.map(m => (
                  <tr key={m.id}>
                    <td style={{ fontWeight: 700 }}>{m.name}</td>
                    <td>{m.dosage}</td>
                    <td>{m.timeOfDay}</td>
                    <td>{m.foodRelation}</td>
                    <td>{m.durationDays} days</td>
                    <td><span className={`badge badge-${m.status.toLowerCase()}`}>{m.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB: RECOVERY CHECK-IN */}
      {activeSubTab === 'recovery' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.75rem' }}>
            <div>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <MessageSquare color="var(--primary)" /> WEEKLY RECOVERY CHECK-IN
              </h2>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Weekly recovery progress form & Gemini AI summary generator</p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <label style={{ fontSize: '0.875rem', fontWeight: 600 }}>Select Week:</label>
              <select className="form-control" value={weekNumber} onChange={e => setWeekNumber(Number(e.target.value))} style={{ width: '110px' }}>
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(w => (
                  <option key={w} value={w}>Week {w}</option>
                ))}
              </select>
            </div>
          </div>

          {/* 10 Required Questionnaire Questions */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">1. What symptoms are you currently experiencing?</label>
              <input type="text" className="form-control" placeholder="e.g. Mild redness on facial cheeks" value={feedbackForm.q1CurrentSymptoms} onChange={e => setFeedbackForm({ ...feedbackForm, q1CurrentSymptoms: e.target.value })} />
            </div>

            <div className="form-group">
              <label className="form-label">2. Have your symptoms improved?</label>
              <select className="form-control" value={feedbackForm.q2SymptomsImproved} onChange={e => setFeedbackForm({ ...feedbackForm, q2SymptomsImproved: e.target.value })}>
                <option>Yes</option>
                <option>Slightly</option>
                <option>No</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">3. Have you noticed any new symptoms?</label>
              <input type="text" className="form-control" placeholder="e.g. Mild itching on jawline" value={feedbackForm.q3NewSymptoms} onChange={e => setFeedbackForm({ ...feedbackForm, q3NewSymptoms: e.target.value })} />
            </div>

            <div className="form-group">
              <label className="form-label">4. Has anything become worse?</label>
              <select className="form-control" value={feedbackForm.q4SymptomsWorse} onChange={e => setFeedbackForm({ ...feedbackForm, q4SymptomsWorse: e.target.value })}>
                <option>No</option>
                <option>Yes</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">5. Have you experienced any side effects?</label>
              <input type="text" className="form-control" placeholder="e.g. Mild dryness after gel application" value={feedbackForm.q5SideEffects} onChange={e => setFeedbackForm({ ...feedbackForm, q5SideEffects: e.target.value })} />
            </div>

            <div className="form-group">
              <label className="form-label">6. Were you able to follow your medication schedule?</label>
              <select className="form-control" value={feedbackForm.q6FollowedSchedule} onChange={e => setFeedbackForm({ ...feedbackForm, q6FollowedSchedule: e.target.value })}>
                <option>Yes</option>
                <option>Mostly</option>
                <option>No</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">7. Did you miss any medicines?</label>
              <input type="text" className="form-control" placeholder="e.g. None" value={feedbackForm.q7MissedMedicines} onChange={e => setFeedbackForm({ ...feedbackForm, q7MissedMedicines: e.target.value })} />
            </div>

            <div className="form-group">
              <label className="form-label">8. What improvement did you notice?</label>
              <input type="text" className="form-control" placeholder="e.g. Acne pimples are flattening" value={feedbackForm.q8ImprovementsNoticed} onChange={e => setFeedbackForm({ ...feedbackForm, q8ImprovementsNoticed: e.target.value })} />
            </div>

            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label className="form-label">9. What challenges did you face?</label>
              <input type="text" className="form-control" placeholder="e.g. Cold weather dryness" value={feedbackForm.q9ChallengesFaced} onChange={e => setFeedbackForm({ ...feedbackForm, q9ChallengesFaced: e.target.value })} />
            </div>

            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label className="form-label">10. Is there anything else your doctor should know?</label>
              <textarea className="form-control" placeholder="Additional notes for Dr. Anitha" value={feedbackForm.q10DoctorNotes} onChange={e => setFeedbackForm({ ...feedbackForm, q10DoctorNotes: e.target.value })} rows={2} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
            <button className="btn btn-outline" onClick={handleSaveDraft}>
              Save Draft
            </button>
            <button className="btn btn-ai" onClick={handleSummarizeFeedback}>
              <Sparkles size={16} /> Summarize (Gemini AI)
            </button>
          </div>

          {/* AI Structured Preview & Save Summary */}
          {aiPreviewSummary && (
            <div className="card animate-fade-in" style={{ marginTop: '1.5rem', backgroundColor: 'var(--accent-light)', border: '2px solid var(--accent)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.1rem', color: 'var(--accent)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Sparkles size={20} /> WEEKLY RECOVERY SUMMARY (Week {aiPreviewSummary.weekNumber})
                </h3>
                <span className={`badge badge-${aiPreviewSummary.overallStatus?.toLowerCase().replace(' ', '-')}`}>
                  Overall: {aiPreviewSummary.overallStatus}
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.9rem' }}>
                <div><strong>Improvements:</strong> <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap' }}>{aiPreviewSummary.improvements}</pre></div>
                <div><strong>Medication Adherence:</strong> <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap' }}>{aiPreviewSummary.medicationAdherence}</pre></div>
                <div><strong>New Observations:</strong> <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap' }}>{aiPreviewSummary.newObservations}</pre></div>
                <div><strong>Doctor Attention:</strong> <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap' }}>{aiPreviewSummary.doctorAttention}</pre></div>
              </div>

              <button className="btn btn-primary" onClick={handleSaveSummary} style={{ marginTop: '1.25rem' }}>
                <Save size={16} /> Save Summary to Treatment Space
              </button>
            </div>
          )}
        </div>
      )}

      {/* TAB: AI SUMMARIES & RECOVERY HISTORY */}
      {activeSubTab === 'ai-summary' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Sparkles color="var(--accent)" /> Stored Weekly Summaries & History
              </h2>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Maintained recovery history for patient Siri under case {tc.caseId}</p>
            </div>
            <button className="btn btn-ai" onClick={handleGenerateOverallProgress}>
              <Sparkles size={16} /> Summarize Progress
            </button>
          </div>

          {/* Overall Progress Synthesis Result */}
          {overallProgressText && (
            <div className="card animate-fade-in" style={{ backgroundColor: 'var(--primary-light)', border: '2px solid var(--primary)' }}>
              <h3 style={{ fontSize: '1.1rem', color: 'var(--primary-dark)', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Activity size={20} /> OVERALL RECOVERY PROGRESS SUMMARY
              </h3>
              <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap', fontSize: '0.9rem', lineHeight: 1.6, color: 'var(--text-main)' }}>
                {overallProgressText}
              </pre>
            </div>
          )}

          {/* List of Stored Weekly Summaries */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {aiSummaries.map(s => (
              <div key={s.id} className="card" style={{ borderLeft: '4px solid var(--accent)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 700 }}>WEEK {s.weekNumber} SUMMARY</h3>
                  <span className={`badge badge-${s.overallStatus?.toLowerCase().replace(' ', '-')}`}>
                    Overall: {s.overallStatus}
                  </span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', fontSize: '0.875rem' }}>
                  <div>
                    <strong>Improvements:</strong>
                    <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap', color: 'var(--text-muted)' }}>{s.improvements}</pre>
                  </div>
                  <div>
                    <strong>Doctor Attention:</strong>
                    <pre style={{ fontFamily: 'inherit', whiteSpace: 'pre-wrap', color: 'var(--text-muted)' }}>{s.doctorAttention}</pre>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Upload Prescription Modal */}
      {showUploadRxModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 200, padding: '1rem' }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '440px' }}>
            <h3 style={{ fontSize: '1.15rem', marginBottom: '1rem' }}>Upload Prescription Document</h3>
            <form onSubmit={handleUploadPrescription}>
              <div className="form-group">
                <label className="form-label">Prescription Title</label>
                <input type="text" className="form-control" placeholder="e.g. Skin Prescription Oct 2026" value={rxTitle} onChange={e => setRxTitle(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Select File (JPG, PNG, PDF)</label>
                <input type="file" className="form-control" onChange={e => setSelectedFile(e.target.files[0])} required />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowUploadRxModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Process OCR & Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
