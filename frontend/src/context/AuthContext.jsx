import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [patient, setPatient] = useState(null);
  const [doctor, setDoctor] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('meditrack_jwt');
      if (token) {
        const data = await authApi.getCurrentUser();
        if (data && data.user) {
          setUser(data.user);
          setPatient(data.patient);
          setDoctor(data.doctor);
        } else {
          logout();
        }
      } else {
        setUser(null);
        setPatient(null);
        setDoctor(null);
      }
    } catch (err) {
      console.error(err);
      logout();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUser();
  }, []);

  const login = async (email, password) => {
    const data = await authApi.login(email, password);
    setUser(data.user);
    setPatient(data.patient);
    setDoctor(data.doctor);
    return data;
  };

  const register = async (payload) => {
    const data = await authApi.register(payload);
    setUser(data.user);
    setPatient(data.patient);
    setDoctor(data.doctor);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('meditrack_jwt');
    setUser(null);
    setPatient(null);
    setDoctor(null);
  };

  const switchRole = async (role) => {
    const email = role === 'DOCTOR' ? 'dr.anitha@meditrack.ai' : 'siri@meditrack.ai';
    return await login(email, 'password');
  };

  return (
    <AuthContext.Provider value={{
      user,
      patient,
      doctor,
      loading,
      login,
      register,
      logout,
      switchRole,
      isDoctor: user?.role === 'DOCTOR'
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
