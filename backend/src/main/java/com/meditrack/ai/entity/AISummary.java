package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_summaries")
public class AISummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private Long weeklyFeedbackId;
    private Integer weekNumber;
    private LocalDateTime summaryDate = LocalDateTime.now();
    private String overallStatus; // Improving, Stable, Attention Needed

    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Column(columnDefinition = "TEXT")
    private String medicationAdherence;

    @Column(columnDefinition = "TEXT")
    private String newObservations;

    @Column(columnDefinition = "TEXT")
    private String doctorAttention;

    @Column(columnDefinition = "TEXT")
    private String fullRawSummary;

    public AISummary() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getWeeklyFeedbackId() { return weeklyFeedbackId; }
    public void setWeeklyFeedbackId(Long weeklyFeedbackId) { this.weeklyFeedbackId = weeklyFeedbackId; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public LocalDateTime getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDateTime summaryDate) { this.summaryDate = summaryDate; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }
    public String getMedicationAdherence() { return medicationAdherence; }
    public void setMedicationAdherence(String medicationAdherence) { this.medicationAdherence = medicationAdherence; }
    public String getNewObservations() { return newObservations; }
    public void setNewObservations(String newObservations) { this.newObservations = newObservations; }
    public String getDoctorAttention() { return doctorAttention; }
    public void setDoctorAttention(String doctorAttention) { this.doctorAttention = doctorAttention; }
    public String getFullRawSummary() { return fullRawSummary; }
    public void setFullRawSummary(String fullRawSummary) { this.fullRawSummary = fullRawSummary; }
}
