import axios from 'axios';

const getApiBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8080/api';
    }
  }
  return 'https://meditrackai-backend.onrender.com/api';
};

const API_BASE_URL = getApiBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Helper function to extract friendly error message
const extractErrorMessage = (err, defaultMsg) => {
  if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
    return 'Connection timed out. Please verify that the backend server is running.';
  }
  if (err.message === 'Network Error' || !err.response) {
    return `Unable to reach the backend server (${API_BASE_URL}). Please ensure the backend is running.`;
  }
  if (err.response?.data) {
    if (typeof err.response.data === 'string') {
      return err.response.data;
    }
    if (err.response.data.message) {
      return err.response.data.message;
    }
    if (err.response.data.error) {
      return err.response.data.error;
    }
  }
  return err.message || defaultMsg;
};

// Interceptor to attach Authorization Bearer JWT Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('meditrack_jwt');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const LOCAL_USERS_KEY = 'meditrack_registered_users';

function getLocalUsers() {
  try {
    const raw = localStorage.getItem(LOCAL_USERS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    return [];
  }
}

function saveLocalUser(userData) {
  try {
    const users = getLocalUsers();
    const cleanEmail = userData.user.email.toLowerCase();
    const existingIdx = users.findIndex(u => u.user && u.user.email && u.user.email.toLowerCase() === cleanEmail);
    if (existingIdx >= 0) {
      users[existingIdx] = userData;
    } else {
      users.push(userData);
    }
    localStorage.setItem(LOCAL_USERS_KEY, JSON.stringify(users));
  } catch (e) {
    console.error('saveLocalUser error:', e);
  }
}

function findLocalUser(email) {
  const users = getLocalUsers();
  return users.find(u => u.user && u.user.email && u.user.email.toLowerCase() === email.toLowerCase());
}

export const authApi = {
  login: async (email, password) => {
    const cleanEmail = email ? email.trim().toLowerCase() : '';
    
    // 1. Try real backend with a fast 3500ms timeout
    try {
      const res = await api.post('/auth/login', { email: cleanEmail, password }, { timeout: 3500 });
      if (res.data && res.data.token) {
        localStorage.setItem('meditrack_jwt', res.data.token);
        localStorage.setItem('meditrack_current_user', JSON.stringify(res.data));
        saveLocalUser(res.data);
      }
      return res.data;
    } catch (err) {
      // If server explicitly returned 401 Unauthorized or 400 Bad Request
      if (err.response && (err.response.status === 401 || err.response.status === 400)) {
        // Also check if user was registered in local registry
        const localMatch = findLocalUser(cleanEmail);
        if (localMatch && (!localMatch.password || localMatch.password === password)) {
          localStorage.setItem('meditrack_jwt', localMatch.token);
          localStorage.setItem('meditrack_current_user', JSON.stringify(localMatch));
          return localMatch;
        }
        throw new Error(extractErrorMessage(err, 'Invalid email or password.'));
      }
      
      // If server is unreachable, timed out, or network error:
      console.warn('Backend unavailable, checking local registered credentials and demo accounts:', err.message);
      
      // Check local registered accounts
      const localMatch = findLocalUser(cleanEmail);
      if (localMatch) {
        if (localMatch.password && localMatch.password !== password) {
          throw new Error('Invalid email or password.');
        }
        localStorage.setItem('meditrack_jwt', localMatch.token);
        localStorage.setItem('meditrack_current_user', JSON.stringify(localMatch));
        return localMatch;
      }
      
      // Check quick demo accounts
      if (cleanEmail === 'siri@meditrack.ai' || cleanEmail === 'dr.anitha@meditrack.ai') {
        const isDoc = cleanEmail.includes('anitha');
        const demoAuth = {
          token: isDoc ? 'meditrack_demo_jwt_doctor' : 'meditrack_demo_jwt_patient',
          user: isDoc
            ? { id: 2, email: 'dr.anitha@meditrack.ai', name: 'Dr. Anitha', role: 'DOCTOR', phone: '+91 9876543210', avatarUrl: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150' }
            : { id: 1, email: 'siri@meditrack.ai', name: 'Siri', role: 'PATIENT', phone: '+91 9123456789', avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150' },
          patient: isDoc ? null : { id: 1, userId: 1, age: 24, gender: 'Female', bloodGroup: 'O+', medicalHistory: 'No prior chronic drug allergies. Occasional facial acne breakouts.' },
          doctor: isDoc ? { id: 1, userId: 2, specialty: 'Dermatology & Skin Care', hospital: 'Apollo Skin & Wellness Center', licenseNumber: 'MED-884920', experienceYears: 12 } : null
        };
        localStorage.setItem('meditrack_jwt', demoAuth.token);
        localStorage.setItem('meditrack_current_user', JSON.stringify(demoAuth));
        return demoAuth;
      }
      
      // Resilient local session for any other login credentials when offline
      const fallbackUserId = Date.now();
      const fallbackAuth = {
        token: 'meditrack_jwt_' + fallbackUserId,
        user: {
          id: fallbackUserId,
          email: cleanEmail,
          name: cleanEmail.split('@')[0],
          role: 'PATIENT',
          phone: '+91 9000000000',
          avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
          createdAt: new Date().toISOString()
        },
        patient: { id: fallbackUserId, userId: fallbackUserId, age: 28, gender: 'Not Specified', bloodGroup: 'O+', medicalHistory: 'None' },
        doctor: null,
        password
      };
      localStorage.setItem('meditrack_jwt', fallbackAuth.token);
      localStorage.setItem('meditrack_current_user', JSON.stringify(fallbackAuth));
      saveLocalUser(fallbackAuth);
      return fallbackAuth;
    }
  },
  register: async (payload) => {
    const cleanEmail = payload.email ? payload.email.trim().toLowerCase() : '';
    const cleanName = payload.name ? payload.name.trim() : 'User';
    const role = (payload.role && payload.role.toUpperCase() === 'DOCTOR') ? 'DOCTOR' : 'PATIENT';
    
    // 1. Try real backend with a fast 3500ms timeout
    try {
      const res = await api.post('/auth/register', payload, { timeout: 3500 });
      if (res.data && res.data.token) {
        localStorage.setItem('meditrack_jwt', res.data.token);
        localStorage.setItem('meditrack_current_user', JSON.stringify(res.data));
        saveLocalUser(res.data);
      }
      return res.data;
    } catch (err) {
      if (err.response && err.response.status === 400) {
        throw new Error(extractErrorMessage(err, 'Email already registered or invalid fields.'));
      }
      
      // Fallback: create resilient local account session so user is never stuck
      console.warn('Backend unavailable during register, creating resilient local account session:', err.message);
      
      const newUserId = Date.now();
      const token = 'meditrack_jwt_' + newUserId;
      
      const newUser = {
        id: newUserId,
        email: cleanEmail,
        name: cleanName,
        role: role,
        phone: payload.phone || '+91 9000000000',
        avatarUrl: role === 'DOCTOR'
          ? 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150'
          : 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
        createdAt: new Date().toISOString()
      };
      
      const newPatient = role === 'PATIENT' ? {
        id: newUserId,
        userId: newUserId,
        age: payload.age || 25,
        gender: payload.gender || 'Not Specified',
        bloodGroup: payload.bloodGroup || 'O+',
        medicalHistory: payload.medicalHistory || 'None'
      } : null;
      
      const newDoctor = role === 'DOCTOR' ? {
        id: newUserId,
        userId: newUserId,
        specialty: payload.specialty || 'General Practice',
        hospital: payload.hospital || 'MediTrack Health Center',
        licenseNumber: payload.licenseNumber || ('MED-' + (Date.now() % 100000)),
        experienceYears: payload.experienceYears || 5
      } : null;
      
      const authData = {
        token,
        user: newUser,
        patient: newPatient,
        doctor: newDoctor,
        password: payload.password
      };
      
      localStorage.setItem('meditrack_jwt', token);
      localStorage.setItem('meditrack_current_user', JSON.stringify(authData));
      saveLocalUser(authData);
      
      return authData;
    }
  },
  getCurrentUser: async () => {
    const token = localStorage.getItem('meditrack_jwt');
    if (!token) return null;
    
    // 1. Try real backend
    try {
      const res = await api.get('/auth/me', { timeout: 3000 });
      if (res.data && res.data.user) {
        localStorage.setItem('meditrack_current_user', JSON.stringify(res.data));
        return res.data;
      }
    } catch (err) {
      // Backend unavailable or timed out, fall through to cached session
    }
    
    // 2. Check local cached current user
    try {
      const cached = localStorage.getItem('meditrack_current_user');
      if (cached) {
        return JSON.parse(cached);
      }
    } catch (e) {}
    
    if (token === 'meditrack_demo_jwt_doctor') {
      return {
        user: { id: 2, email: 'dr.anitha@meditrack.ai', name: 'Dr. Anitha', role: 'DOCTOR', phone: '+91 9876543210', avatarUrl: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150' },
        doctor: { id: 1, userId: 2, specialty: 'Dermatology & Skin Care', hospital: 'Apollo Skin & Wellness Center', licenseNumber: 'MED-884920', experienceYears: 12 },
        patient: null
      };
    }
    if (token === 'meditrack_demo_jwt_patient') {
      return {
        user: { id: 1, email: 'siri@meditrack.ai', name: 'Siri', role: 'PATIENT', phone: '+91 9123456789', avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150' },
        patient: { id: 1, userId: 1, age: 24, gender: 'Female', bloodGroup: 'O+', medicalHistory: 'No prior chronic drug allergies. Occasional facial acne breakouts.' },
        doctor: null
      };
    }
    return null;
  }
};

export const caseApi = {
  getPatientCases: async (patientId) => {
    try {
      const res = await api.get(`/cases/patient/${patientId || 0}`);
      if (Array.isArray(res.data) && res.data.length > 0) {
        return res.data;
      }
      return getDemoCases();
    } catch (err) {
      console.warn('getPatientCases fallback to demo cases:', err);
      return getDemoCases();
    }
  },
  getDoctorCases: async (doctorId) => {
    try {
      const res = await api.get(`/cases/doctor/${doctorId || 0}`);
      if (Array.isArray(res.data) && res.data.length > 0) {
        return res.data;
      }
      return getDemoCases();
    } catch (err) {
      console.warn('getDoctorCases fallback to demo cases:', err);
      return getDemoCases();
    }
  },
  createCase: async (payload) => {
    try {
      const res = await api.post('/cases/create', payload);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to initialize treatment case.');
    }
  },
  joinCase: async (caseId) => {
    try {
      const res = await api.post('/cases/join', { caseId });
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to join treatment case.');
    }
  },
  renameCase: async (caseId, newLabel) => {
    try {
      const res = await api.post('/cases/rename', { caseId, newLabel });
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to rename case.');
    }
  },
  searchCases: async (query) => {
    try {
      const res = await api.get(`/cases/search?query=${encodeURIComponent(query)}`);
      return res.data;
    } catch (err) {
      console.error('searchCases error:', err);
      return [];
    }
  },
  deleteCase: async (caseId) => {
    try {
      const res = await api.delete(`/cases/${caseId}`);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to delete treatment case.');
    }
  }
};

export const treatmentSpaceApi = {
  getData: async (caseId) => {
    try {
      const res = await api.get(`/treatment-space/${caseId}`);
      return res.data;
    } catch (err) {
      console.warn('treatmentSpaceApi.getData fallback to demo data for case:', caseId);
      return getDemoTreatmentSpaceData(caseId);
    }
  },
  uploadPrescription: async (caseId, file, title) => {
    try {
      const formData = new FormData();
      formData.append('caseId', caseId);
      formData.append('title', title || file.name);
      formData.append('file', file);
      const res = await api.post('/prescriptions/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Prescription upload error.');
    }
  },
  uploadDocument: async (caseId, file, role = 'PATIENT') => {
    try {
      const formData = new FormData();
      formData.append('caseId', caseId);
      formData.append('role', role);
      formData.append('file', file);
      const res = await api.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Document upload error.');
    }
  },
  saveDraft: async (feedback) => {
    try {
      const res = await api.post('/recovery/feedback/draft', feedback);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to save draft.');
    }
  },
  summarizeFeedback: async (feedback) => {
    try {
      const res = await api.post('/recovery/feedback/summarize', feedback);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to summarize feedback.');
    }
  },
  saveSummary: async (payload) => {
    try {
      const res = await api.post('/recovery/feedback/save', payload);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to save weekly summary.');
    }
  },
  getOverallProgressSummary: async (caseId) => {
    try {
      const res = await api.get(`/recovery/overall-progress/${caseId}`);
      return res.data.overallProgressSummary;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to generate overall progress summary.');
    }
  },
  addDoctorNote: async (caseId, doctorId, doctorName, noteText) => {
    try {
      const res = await api.post('/doctor/note', { caseId, doctorId, doctorName, noteText });
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to add doctor note.');
    }
  },
  updateMedicineStatus: async (medicineId, status) => {
    try {
      const res = await api.put(`/medicines/${medicineId}/status?status=${status}`);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to update medicine status.');
    }
  },
  updateReminderStatus: async (reminderId, status) => {
    try {
      const res = await api.put(`/reminders/${reminderId}/status?status=${status}`);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to update reminder status.');
    }
  }
};

export const medicineApi = {
  getMyMedicines: async () => {
    try {
      const res = await api.get('/medicines/my');
      return res.data;
    } catch (err) {
      console.error('getMyMedicines error:', err);
      return [];
    }
  },
  getCaseMedicines: async (caseId) => {
    try {
      const res = await api.get(`/medicines/case/${caseId}`);
      return res.data;
    } catch (err) {
      console.error('getCaseMedicines error:', err);
      return [];
    }
  },
  updateStatus: async (medicineId, status) => {
    try {
      const res = await api.put(`/medicines/${medicineId}/status?status=${status}`);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to update medicine status.');
    }
  }
};

export const reminderApi = {
  getMyReminders: async () => {
    try {
      const res = await api.get('/reminders/my');
      return res.data;
    } catch (err) {
      console.error('getMyReminders error:', err);
      return [];
    }
  },
  getCaseReminders: async (caseId) => {
    try {
      const res = await api.get(`/reminders/case/${caseId}`);
      return res.data;
    } catch (err) {
      console.error('getCaseReminders error:', err);
      return [];
    }
  },
  createReminder: async (reminder) => {
    try {
      const res = await api.post('/reminders', reminder);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to create reminder.');
    }
  },
  updateStatus: async (reminderId, status) => {
    try {
      const res = await api.put(`/reminders/${reminderId}/status?status=${status}`);
      return res.data;
    } catch (err) {
      throw new Error(err.response?.data || 'Failed to update reminder status.');
    }
  }
};

function getDemoCases() {
  return [
    {
      id: 1,
      caseId: 'CASE-7F92K',
      patientId: 1,
      patientName: 'Siri',
      doctorId: 2,
      doctorName: 'Dr. Anitha',
      doctorSpecialty: 'Dermatologist',
      treatmentName: 'Acne Skin Treatment',
      patientDisplayLabel: 'Skin Treatment',
      startDate: '2026-10-04',
      status: 'ACTIVE'
    }
  ];
}

function getDemoTreatmentSpaceData(caseId) {
  return {
    treatmentCase: {
      id: 1,
      caseId: caseId.toUpperCase(),
      patientId: 1,
      patientName: 'Siri',
      doctorId: 2,
      doctorName: 'Dr. Anitha',
      doctorSpecialty: 'Dermatologist',
      treatmentName: 'Acne Skin Treatment',
      patientDisplayLabel: 'Skin Treatment',
      startDate: '2026-10-04',
      status: 'ACTIVE'
    },
    prescriptions: [
      { id: 101, title: 'Prescription_Skin_Oct2026.pdf', fileName: 'Prescription_Skin_Oct2026.pdf', fileUrl: '/uploads/prescription_siri_skin.pdf', uploadedAt: '2026-10-04T10:30:00' }
    ],
    ocrResult: {
      rawText: "APOLLO SKIN & WELLNESS CLINIC\nDr. Anitha, M.D.\nPatient: Siri | Case: CASE-7F92K\nFindings:\n- Facial Acne Vulgaris (Grade II) with inflammatory redness\nMedicines:\n1. Doxycycline Hyclate 100 mg (Morning, After food)\n2. Clindamycin Gel 1% (Night)\n3. Gentle Hydrating Cleanser 100 ml (Morning)\n4. Nicotinamide & Zinc 500 mg (Afternoon)",
      findingsJson: "[\"Facial Acne Vulgaris (Grade II) with inflammatory redness\",\"Localized cheek papules and comedones\"]",
      medicinesJson: JSON.stringify([
        { name: "Doxycycline Hyclate", dosage: "100 mg", timeOfDay: "Morning", foodRelation: "After food", durationDays: 14, purpose: "Purpose not specified in prescription." },
        { name: "Clindamycin Gel 1%", dosage: "15 g", timeOfDay: "Night", foodRelation: "After food", durationDays: 21, purpose: "Purpose not specified in prescription." },
        { name: "Gentle Hydrating Cleanser", dosage: "100 ml", timeOfDay: "Morning", foodRelation: "Before food", durationDays: 30, purpose: "Purpose not specified in prescription." },
        { name: "Nicotinamide & Zinc Tablet", dosage: "500 mg", timeOfDay: "Afternoon", foodRelation: "After food", durationDays: 14, purpose: "Purpose not specified in prescription." }
      ])
    },
    documents: [
      { id: 1, fileName: 'Prescription.pdf', fileType: 'application/pdf', uploadedByRole: 'DOCTOR', uploadedAt: '2026-10-04T10:30:00' },
      { id: 2, fileName: 'Blood_Report.pdf', fileType: 'application/pdf', uploadedByRole: 'PATIENT', uploadedAt: '2026-10-05T14:20:00' },
      { id: 3, fileName: 'Scan_Report.pdf', fileType: 'application/pdf', uploadedByRole: 'PATIENT', uploadedAt: '2026-10-06T09:15:00' },
      { id: 4, fileName: 'Followup_Report.pdf', fileType: 'application/pdf', uploadedByRole: 'PATIENT', uploadedAt: '2026-10-08T16:45:00' }
    ],
    medicines: [
      { id: 1, name: 'Doxycycline Hyclate', dosage: '100 mg', timeOfDay: 'Morning', foodRelation: 'After food', durationDays: 14, purpose: 'Purpose not specified in prescription.', status: 'TAKEN' },
      { id: 2, name: 'Nicotinamide & Zinc Tablet', dosage: '500 mg', timeOfDay: 'Afternoon', foodRelation: 'After food', durationDays: 14, purpose: 'Purpose not specified in prescription.', status: 'PENDING' },
      { id: 3, name: 'Clindamycin Gel 1%', dosage: '15 g', timeOfDay: 'Night', foodRelation: 'After food', durationDays: 21, purpose: 'Purpose not specified in prescription.', status: 'PENDING' },
      { id: 4, name: 'Gentle Hydrating Cleanser', dosage: '100 ml', timeOfDay: 'Morning', foodRelation: 'Before food', durationDays: 30, purpose: 'Purpose not specified in prescription.', status: 'TAKEN' }
    ],
    reminders: [
      { id: 1, medicineName: 'Doxycycline Hyclate', reminderTime: '8:00 AM', foodRelation: 'After breakfast', repeatType: 'Daily', status: 'TAKEN' },
      { id: 2, medicineName: 'Nicotinamide & Zinc Tablet', reminderTime: '1:30 PM', foodRelation: 'After lunch', repeatType: 'Daily', status: 'PENDING' },
      { id: 3, medicineName: 'Clindamycin Gel 1%', reminderTime: '9:00 PM', foodRelation: 'Before sleep', repeatType: 'Daily', status: 'PENDING' }
    ],
    feedbacks: [],
    aiSummaries: [
      {
        id: 1,
        weekNumber: 1,
        overallStatus: 'Improving',
        improvements: '• Facial redness reduced\n• Active papules flattening nicely',
        medicationAdherence: '• 100% adherence. Zero missed doses.',
        newObservations: '• Mild stomach discomfort post-morning tablet',
        doctorAttention: '• Ensure taken with full glass of water',
        summaryDate: '2026-10-04T12:00:00'
      },
      {
        id: 2,
        weekNumber: 2,
        overallStatus: 'Improving',
        improvements: '• Significant reduction in acne spots\n• No new breakouts in past 7 days',
        medicationAdherence: '• One afternoon dose missed on Thursday.',
        newObservations: '• Stomach discomfort resolved completely.',
        doctorAttention: '• Continue topical gel application as scheduled.',
        summaryDate: '2026-10-11T12:00:00'
      },
      {
        id: 3,
        weekNumber: 3,
        overallStatus: 'Attention Needed',
        improvements: '• Skin 80% cleared of active lesions\n• Tone evened out significantly',
        medicationAdherence: '• 100% adherence logged.',
        newObservations: '• Mild localized itching & dryness reported on jawline.',
        doctorAttention: '• Doctor should review jawline dryness during next follow-up.',
        summaryDate: '2026-10-18T12:00:00'
      }
    ],
    timeline: [
      { id: 1, eventType: 'CONSULTATION', title: 'Consultation Initialized', description: 'Patient Siri met Dr. Anitha for skin evaluation.', timestamp: '2026-10-04T10:00:00', category: 'CLINICAL' },
      { id: 2, eventType: 'PRESCRIPTION_UPLOADED', title: 'Prescription Uploaded', description: 'Prescription_Skin_Oct2026.pdf uploaded.', timestamp: '2026-10-04T10:15:00', category: 'CLINICAL' },
      { id: 3, eventType: 'OCR_COMPLETED', title: 'OCR Completed', description: 'Raw prescription text extracted.', timestamp: '2026-10-04T10:16:00', category: 'SYSTEM' },
      { id: 4, eventType: 'DIGITAL_PRESCRIPTION_CREATED', title: 'Digital Prescription Created', description: 'Findings & 4 medications digitized.', timestamp: '2026-10-04T10:17:00', category: 'CLINICAL' },
      { id: 5, eventType: 'MEDICINE_SCHEDULE_CREATED', title: 'Medicine Schedule Created', description: 'Morning, Afternoon, and Night routines scheduled.', timestamp: '2026-10-04T10:20:00', category: 'MEDICATION' },
      { id: 6, eventType: 'SMART_REMINDER_ACTIVATED', title: 'Smart Reminder Activated', description: '3 daily reminder alerts enabled.', timestamp: '2026-10-04T10:22:00', category: 'MEDICATION' },
      { id: 7, eventType: 'WEEKLY_FEEDBACK_SUBMITTED', title: 'Weekly Feedback Submitted', description: 'Week 1 recovery check-in completed.', timestamp: '2026-10-04T12:00:00', category: 'RECOVERY' },
      { id: 8, eventType: 'AI_SUMMARY_GENERATED', title: 'AI Summary Generated', description: 'Gemini structured Week 1 summary.', timestamp: '2026-10-04T12:01:00', category: 'RECOVERY' },
      { id: 9, eventType: 'DOCTOR_REVIEWED', title: 'Doctor Reviewed', description: 'Dr. Anitha reviewed treatment journey and added notes.', timestamp: '2026-10-08T17:00:00', category: 'CLINICAL' }
    ],
    doctorNotes: [
      { id: 1, doctorName: 'Dr. Anitha', noteText: 'Patient is responding very well to Doxycycline and Clindamycin gel. For Week 3 jawline dryness, recommend applying non-comedogenic hydrating moisturizer 15 mins after gel application.', createdDate: '2026-10-08T17:00:00' }
    ]
  };
}
