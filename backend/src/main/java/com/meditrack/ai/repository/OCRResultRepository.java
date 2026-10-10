package com.meditrack.ai.repository;

import com.meditrack.ai.entity.OCRResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OCRResultRepository extends JpaRepository<OCRResult, Long> {
    Optional<OCRResult> findByPrescriptionId(Long prescriptionId);
    Optional<OCRResult> findFirstByCaseIdOrderByProcessedAtDesc(String caseId);
    void deleteByCaseId(String caseId);
}
