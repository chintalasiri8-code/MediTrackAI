package com.meditrack.ai.service;

import com.meditrack.ai.config.UserPrincipal;
import com.meditrack.ai.entity.Doctor;
import com.meditrack.ai.entity.Patient;
import com.meditrack.ai.entity.TreatmentCase;
import com.meditrack.ai.repository.DoctorRepository;
import com.meditrack.ai.repository.PatientRepository;
import com.meditrack.ai.repository.TreatmentCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CaseAccessValidator {

    @Autowired private TreatmentCaseRepository caseRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;

    public UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public Patient getAuthenticatedPatient() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null || !"PATIENT".equalsIgnoreCase(principal.getRole())) {
            return null;
        }
        return patientRepository.findByUserId(principal.getId()).orElse(null);
    }

    public Doctor getAuthenticatedDoctor() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null || !"DOCTOR".equalsIgnoreCase(principal.getRole())) {
            return null;
        }
        return doctorRepository.findByUserId(principal.getId()).orElse(null);
    }

    public boolean isUserAuthorizedForCase(String caseId) {
        if (caseId == null || caseId.isBlank()) return false;
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null) return false;

        Optional<TreatmentCase> caseOpt = caseRepository.findByCaseId(caseId.trim().toUpperCase());
        if (caseOpt.isEmpty()) return false;

        TreatmentCase tc = caseOpt.get();
        if ("PATIENT".equalsIgnoreCase(principal.getRole())) {
            Patient p = getAuthenticatedPatient();
            Long pId = (p != null) ? p.getId() : principal.getId();
            return tc.getPatientId() != null && tc.getPatientId().equals(pId);
        } else if ("DOCTOR".equalsIgnoreCase(principal.getRole())) {
            Doctor d = getAuthenticatedDoctor();
            Long dId = (d != null) ? d.getId() : principal.getId();
            return tc.getDoctorId() != null && tc.getDoctorId().equals(dId);
        }
        return false;
    }
}
