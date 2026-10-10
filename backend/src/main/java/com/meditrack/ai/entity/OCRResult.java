package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocr_results")
public class OCRResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private Long prescriptionId;
    
    @Column(columnDefinition = "TEXT")
    private String rawText;
    
    @Column(columnDefinition = "TEXT")
    private String findingsJson; // JSON array string
    
    @Column(columnDefinition = "TEXT")
    private String medicinesJson; // JSON array string
    
    private LocalDateTime processedAt = LocalDateTime.now();

    public OCRResult() {}

    public OCRResult(String caseId, Long prescriptionId, String rawText, String findingsJson, String medicinesJson) {
        this.caseId = caseId;
        this.prescriptionId = prescriptionId;
        this.rawText = rawText;
        this.findingsJson = findingsJson;
        this.medicinesJson = medicinesJson;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }
    public String getMedicinesJson() { return medicinesJson; }
    public void setMedicinesJson(String medicinesJson) { this.medicinesJson = medicinesJson; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
