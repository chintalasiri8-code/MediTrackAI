import React, { createContext, useContext, useState, useEffect } from 'react';
import { caseApi, treatmentSpaceApi } from '../services/api';
import { useAuth } from './AuthContext';

const TreatmentContext = createContext();

export const TreatmentProvider = ({ children }) => {
  const { user, isDoctor } = useAuth();
  const [cases, setCases] = useState([]);
  const [activeCaseId, setActiveCaseId] = useState(null);
  const [spaceData, setSpaceData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const loadCases = async () => {
    try {
      if (!user) {
        setCases([]);
        setActiveCaseId(null);
        return;
      }
      const data = isDoctor ? await caseApi.getDoctorCases(user.id) : await caseApi.getPatientCases(user.id);
      if (Array.isArray(data)) {
        setCases(data);
        if (data.length > 0) {
          if (!activeCaseId || !data.some(c => c.caseId === activeCaseId)) {
            setActiveCaseId(data[0].caseId);
          }
        } else {
          setActiveCaseId(null);
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const loadTreatmentSpace = async (caseId) => {
    const targetCaseId = caseId || activeCaseId;
    if (!targetCaseId) return;
    setLoading(true);
    try {
      const data = await treatmentSpaceApi.getData(targetCaseId);
      setSpaceData(data);
      setActiveCaseId(targetCaseId);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const renameCase = async (caseId, newLabel) => {
    await caseApi.renameCase(caseId, newLabel);
    setCases(prev => prev.map(c => c.caseId === caseId ? { ...c, patientDisplayLabel: newLabel } : c));
    if (spaceData && spaceData.treatmentCase.caseId === caseId) {
      setSpaceData(prev => ({
        ...prev,
        treatmentCase: { ...prev.treatmentCase, patientDisplayLabel: newLabel }
      }));
    }
  };

  const joinCase = async (caseId) => {
    const newCase = await caseApi.joinCase(caseId);
    await loadCases();
    return newCase;
  };

  const createCase = async (payload) => {
    const newCase = await caseApi.createCase(payload);
    await loadCases();
    return newCase;
  };

  const deleteCase = async (caseId) => {
    await caseApi.deleteCase(caseId);
    setCases(prev => prev.filter(c => c.caseId !== caseId));
    if (activeCaseId === caseId) {
      setActiveCaseId(null);
    }
    if (spaceData && spaceData.treatmentCase?.caseId === caseId) {
      setSpaceData(null);
    }
    await loadCases();
  };

  useEffect(() => {
    if (user?.id) {
      loadCases();
    }
  }, [user?.id, isDoctor]);

  return (
    <TreatmentContext.Provider value={{
      cases,
      activeCaseId,
      setActiveCaseId,
      spaceData,
      loading,
      searchQuery,
      setSearchQuery,
      loadCases,
      loadTreatmentSpace,
      renameCase,
      joinCase,
      createCase,
      deleteCase
    }}>
      {children}
    </TreatmentContext.Provider>
  );
};

export const useTreatment = () => useContext(TreatmentContext);
