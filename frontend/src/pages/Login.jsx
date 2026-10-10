import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Activity, UserCheck, Stethoscope, ArrowRight, UserPlus, AlertCircle } from 'lucide-react';

export const Login = ({ onLoginSuccess }) => {
  const { login, register, switchRole } = useAuth();
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [roleMode, setRoleMode] = useState('PATIENT'); // PATIENT or DOCTOR
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  
  // Registration fields
  const [specialty, setSpecialty] = useState('Dermatology');
  const [hospital, setHospital] = useState('Apollo Skin & Wellness');
  const [age, setAge] = useState(25);
  const [bloodGroup, setBloodGroup] = useState('O+');

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      onLoginSuccess();
    } catch (err) {
      setError(err.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({
        email,
        password,
        name,
        role: roleMode,
        phone,
        specialty: roleMode === 'DOCTOR' ? specialty : null,
        hospital: roleMode === 'DOCTOR' ? hospital : null,
        age: roleMode === 'PATIENT' ? age : null,
        bloodGroup: roleMode === 'PATIENT' ? bloodGroup : null
      });
      onLoginSuccess();
    } catch (err) {
      setError(err.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoSelect = async (role) => {
    setError('');
    setLoading(true);
    try {
      await switchRole(role);
      onLoginSuccess();
    } catch (err) {
      setError('Demo login failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-main)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
      <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2.5rem 2rem', border: '1px solid var(--border)', boxShadow: 'var(--shadow-lg)' }}>
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '16px', background: 'linear-gradient(135deg, #0EA5E9, #6366F1)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: '#FFF', boxShadow: '0 6px 16px rgba(14,165,233,0.3)', marginBottom: '0.75rem' }}>
            <Activity size={32} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800 }}>MediTrack <span style={{ color: 'var(--primary)' }}>AI</span></h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Smart Treatment Journey Management System</p>
        </div>

        {/* Quick Demo Role Selectors */}
        <div style={{ backgroundColor: 'var(--primary-light)', padding: '0.875rem', borderRadius: '12px', marginBottom: '1.25rem', border: '1px solid #BAE6FD' }}>
          <span style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', tracking: '0.05em', color: 'var(--primary-dark)', fontWeight: 700, marginBottom: '0.5rem', textAlign: 'center' }}>
            QUICK DEMO ACCESSIBILITY
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <button className="btn btn-sm btn-primary" onClick={() => handleDemoSelect('PATIENT')} disabled={loading}>
              <UserCheck size={14} /> Siri (Patient)
            </button>
            <button className="btn btn-sm btn-ai" onClick={() => handleDemoSelect('DOCTOR')} disabled={loading}>
              <Stethoscope size={14} /> Dr. Anitha (Doctor)
            </button>
          </div>
        </div>

        {/* Toggle Login / Register */}
        <div style={{ display: 'flex', borderBottom: '1px solid var(--border)', marginBottom: '1.25rem' }}>
          <button 
            style={{ flex: 1, padding: '0.6rem', border: 'none', background: 'none', borderBottom: !isRegisterMode ? '2px solid var(--primary)' : 'none', fontWeight: !isRegisterMode ? 700 : 500, color: !isRegisterMode ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
            onClick={() => { setIsRegisterMode(false); setError(''); }}
          >
            Sign In
          </button>
          <button 
            style={{ flex: 1, padding: '0.6rem', border: 'none', background: 'none', borderBottom: isRegisterMode ? '2px solid var(--primary)' : 'none', fontWeight: isRegisterMode ? 700 : 500, color: isRegisterMode ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
            onClick={() => { setIsRegisterMode(true); setError(''); }}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div style={{ padding: '0.75rem', backgroundColor: 'var(--danger-light)', color: '#B91C1C', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertCircle size={16} /> {error}
          </div>
        )}

        {!isRegisterMode ? (
          /* Login Form */
          <form onSubmit={handleLoginSubmit}>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input type="email" className="form-control" placeholder="siri@meditrack.ai" value={email} onChange={e => setEmail(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input type="password" className="form-control" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '0.5rem' }} disabled={loading}>
              {loading ? 'Authenticating...' : 'Sign In with Credentials'}
            </button>
          </form>
        ) : (
          /* Registration Form */
          <form onSubmit={handleRegisterSubmit}>
            <div className="form-group">
              <label className="form-label">Account Role</label>
              <select className="form-control" value={roleMode} onChange={e => setRoleMode(e.target.value)}>
                <option value="PATIENT">Patient Account</option>
                <option value="DOCTOR">Doctor Account</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input type="text" className="form-control" placeholder={roleMode === 'DOCTOR' ? 'Dr. Sarah Jenkins' : 'Jane Doe'} value={name} onChange={e => setName(e.target.value)} required />
            </div>

            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input type="email" className="form-control" placeholder="user@meditrack.ai" value={email} onChange={e => setEmail(e.target.value)} required />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input type="password" className="form-control" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} required minLength={4} />
            </div>

            {roleMode === 'DOCTOR' ? (
              <>
                <div className="form-group">
                  <label className="form-label">Specialty</label>
                  <input type="text" className="form-control" placeholder="e.g. Dermatology" value={specialty} onChange={e => setSpecialty(e.target.value)} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Hospital / Clinic</label>
                  <input type="text" className="form-control" placeholder="e.g. Apollo Skin Center" value={hospital} onChange={e => setHospital(e.target.value)} />
                </div>
              </>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div className="form-group">
                  <label className="form-label">Age</label>
                  <input type="number" className="form-control" value={age} onChange={e => setAge(Number(e.target.value))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Blood Group</label>
                  <input type="text" className="form-control" value={bloodGroup} onChange={e => setBloodGroup(e.target.value)} />
                </div>
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '0.75rem' }} disabled={loading}>
              {loading ? 'Creating Account...' : `Register as ${roleMode}`}
            </button>
          </form>
        )}

        <p style={{ fontSize: '0.75rem', color: 'var(--text-light)', textAlign: 'center', marginTop: '1.25rem', fontStyle: 'italic' }}>
          "MediTrack AI transforms fragmented treatment information into a continuous, structured and trackable treatment journey."
        </p>
      </div>
    </div>
  );
};
