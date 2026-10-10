package com.meditrack.ai.repository;

import com.meditrack.ai.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByCaseIdOrderByUploadedAtDesc(String caseId);
    Optional<Prescription> findFirstByCaseIdOrderByUploadedAtDesc(String caseId);
    void deleteByCaseId(String caseId);
}
