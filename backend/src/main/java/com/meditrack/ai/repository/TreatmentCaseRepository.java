package com.meditrack.ai.repository;

import com.meditrack.ai.entity.TreatmentCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TreatmentCaseRepository extends JpaRepository<TreatmentCase, Long> {
    Optional<TreatmentCase> findByCaseId(String caseId);
    List<TreatmentCase> findByPatientId(Long patientId);
    List<TreatmentCase> findByDoctorId(Long doctorId);
    List<TreatmentCase> findByPatientNameContainingIgnoreCaseOrTreatmentNameContainingIgnoreCaseOrCaseIdContainingIgnoreCase(String name, String treatment, String caseId);
    void deleteByCaseId(String caseId);
}
