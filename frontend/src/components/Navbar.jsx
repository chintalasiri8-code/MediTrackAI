import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTreatment } from '../context/TreatmentContext';
import { 
  Activity, Search, Home, FolderHeart, Pill, Bell, 
  User, Stethoscope, Users, Calendar, ArrowRightLeft, Plus, LogOut
} from 'lucide-react';

export const Navbar = ({ activeTab, setActiveTab, onOpenJoinModal, onOpenCreateModal }) => {
  const { user, logout, isDoctor } = useAuth();
  const { searchQuery, setSearchQuery } = useTreatment();

  return (
    <header className="glass-panel" style={{ position: 'sticky', top: 0, zIndex: 100, borderBottom: '1px solid var(--border)' }}>
      {/* Top Banner with User Status and Logout */}
      <div style={{ backgroundColor: isDoctor ? '#0F172A' : '#0284C7', color: '#FFF', padding: '0.35rem 1.5rem', fontSize: '0.8125rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ fontWeight: 600 }}>AUTHENTICATED:</span>
          <span>Logged in as <strong>{user?.name}</strong> ({user?.email}) — <span style={{ textTransform: 'uppercase', fontWeight: 700 }}>{isDoctor ? 'Doctor Portal' : 'Patient Portal'}</span></span>
        </div>
        <button 
          onClick={logout}
          style={{ background: 'rgba(239, 68, 68, 0.25)', border: '1px solid rgba(255,255,255,0.3)', color: '#FFF', padding: '0.2rem 0.6rem', borderRadius: '4px', fontSize: '0.75rem', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '0.3rem', fontWeight: 600 }}
          title="Sign out of MediTrack AI"
        >
          <LogOut size={13} /> Log Out
        </button>
      </div>

      <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '0.75rem 1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        {/* Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', cursor: 'pointer' }} onClick={() => setActiveTab('home')}>
          <div style={{ width: '40px', height: '40px', borderRadius: '12px', background: 'linear-gradient(135deg, #0EA5E9, #6366F1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#FFF', boxShadow: '0 4px 10px rgba(14,165,233,0.3)' }}>
            <Activity size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)', lineHeight: 1.1 }}>MediTrack <span style={{ color: 'var(--primary)' }}>AI</span></h1>
            <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 500 }}>Treatment Journey Workspace</p>
          </div>
        </div>

        {/* Global Search Bar */}
        <div style={{ flex: 1, maxWidth: '420px', position: 'relative' }}>
          <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input 
            type="text" 
            className="form-control" 
            placeholder="Search doctors, specialties or treatment cases" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ width: '100%', paddingLeft: '38px', borderRadius: '20px', backgroundColor: 'var(--bg-main)' }}
          />
        </div>

        {/* Navigation Tabs */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {!isDoctor ? (
            <>
              <button className={`btn btn-sm ${activeTab === 'home' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('home')}>
                <Home size={16} /> Home
              </button>
              <button className={`btn btn-sm ${activeTab === 'cases' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('cases')}>
                <FolderHeart size={16} /> Treatment Cases
              </button>
              <button className={`btn btn-sm ${activeTab === 'tracker' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('tracker')}>
                <Pill size={16} /> Medicine Tracker
              </button>
              <button className={`btn btn-sm ${activeTab === 'notifications' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('notifications')}>
                <Bell size={16} /> Notifications
              </button>
              <button className={`btn btn-sm ${activeTab === 'profile' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('profile')}>
                <User size={16} /> Profile
              </button>
              <button className="btn btn-sm btn-secondary" onClick={onOpenJoinModal} style={{ marginLeft: '0.5rem' }}>
                <Plus size={16} /> Join Case
              </button>
            </>
          ) : (
            <>
              <button className={`btn btn-sm ${activeTab === 'home' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('home')}>
                <Stethoscope size={16} /> Dashboard
              </button>
              <button className={`btn btn-sm ${activeTab === 'patients' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('patients')}>
                <Users size={16} /> Patients
              </button>
              <button className={`btn btn-sm ${activeTab === 'cases' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('cases')}>
                <FolderHeart size={16} /> Treatment Cases
              </button>
              <button className={`btn btn-sm ${activeTab === 'followups' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('followups')}>
                <Calendar size={16} /> Follow-ups
              </button>
              <button className={`btn btn-sm ${activeTab === 'profile' ? 'btn-primary' : 'btn-outline'}`} onClick={() => setActiveTab('profile')}>
                <User size={16} /> Profile
              </button>
              <button className="btn btn-sm btn-primary" onClick={onOpenCreateModal} style={{ marginLeft: '0.5rem' }}>
                <Plus size={16} /> New Case
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
};
