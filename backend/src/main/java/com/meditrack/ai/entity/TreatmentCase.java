package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_cases")
public class TreatmentCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String caseId; // e.g., CASE-7F92K

    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String treatmentName; // e.g., Skin Treatment
    private String patientDisplayLabel; // Editable by patient
    private LocalDate startDate;
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, PAUSED
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastUpdate = LocalDateTime.now();

    public TreatmentCase() {}

    public TreatmentCase(String caseId, Long patientId, String patientName, Long doctorId, String doctorName, String doctorSpecialty, String treatmentName, LocalDate startDate) {
        this.caseId = caseId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorSpecialty = doctorSpecialty;
        this.treatmentName = treatmentName;
        this.patientDisplayLabel = treatmentName;
        this.startDate = startDate;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }
    public String getTreatmentName() { return treatmentName; }
    public void setTreatmentName(String treatmentName) { this.treatmentName = treatmentName; }
    public String getPatientDisplayLabel() { return patientDisplayLabel; }
    public void setPatientDisplayLabel(String patientDisplayLabel) { this.patientDisplayLabel = patientDisplayLabel; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
}
