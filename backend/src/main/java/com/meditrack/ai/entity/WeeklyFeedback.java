package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_feedbacks")
public class WeeklyFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private Integer weekNumber;
    private LocalDateTime submissionDate = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String q1CurrentSymptoms;
    @Column(columnDefinition = "TEXT")
    private String q2SymptomsImproved;
    @Column(columnDefinition = "TEXT")
    private String q3NewSymptoms;
    @Column(columnDefinition = "TEXT")
    private String q4SymptomsWorse;
    @Column(columnDefinition = "TEXT")
    private String q5SideEffects;
    @Column(columnDefinition = "TEXT")
    private String q6FollowedSchedule;
    @Column(columnDefinition = "TEXT")
    private String q7MissedMedicines;
    @Column(columnDefinition = "TEXT")
    private String q8ImprovementsNoticed;
    @Column(columnDefinition = "TEXT")
    private String q9ChallengesFaced;
    @Column(columnDefinition = "TEXT")
    private String q10DoctorNotes;
    
    private String status = "SUBMITTED"; // DRAFT, SUBMITTED

    public WeeklyFeedback() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public String getQ1CurrentSymptoms() { return q1CurrentSymptoms; }
    public void setQ1CurrentSymptoms(String q1CurrentSymptoms) { this.q1CurrentSymptoms = q1CurrentSymptoms; }
    public String getQ2SymptomsImproved() { return q2SymptomsImproved; }
    public void setQ2SymptomsImproved(String q2SymptomsImproved) { this.q2SymptomsImproved = q2SymptomsImproved; }
    public String getQ3NewSymptoms() { return q3NewSymptoms; }
    public void setQ3NewSymptoms(String q3NewSymptoms) { this.q3NewSymptoms = q3NewSymptoms; }
    public String getQ4SymptomsWorse() { return q4SymptomsWorse; }
    public void setQ4SymptomsWorse(String q4SymptomsWorse) { this.q4SymptomsWorse = q4SymptomsWorse; }
    public String getQ5SideEffects() { return q5SideEffects; }
    public void setQ5SideEffects(String q5SideEffects) { this.q5SideEffects = q5SideEffects; }
    public String getQ6FollowedSchedule() { return q6FollowedSchedule; }
    public void setQ6FollowedSchedule(String q6FollowedSchedule) { this.q6FollowedSchedule = q6FollowedSchedule; }
    public String getQ7MissedMedicines() { return q7MissedMedicines; }
    public void setQ7MissedMedicines(String q7MissedMedicines) { this.q7MissedMedicines = q7MissedMedicines; }
    public String getQ8ImprovementsNoticed() { return q8ImprovementsNoticed; }
    public void setQ8ImprovementsNoticed(String q8ImprovementsNoticed) { this.q8ImprovementsNoticed = q8ImprovementsNoticed; }
    public String getQ9ChallengesFaced() { return q9ChallengesFaced; }
    public void setQ9ChallengesFaced(String q9ChallengesFaced) { this.q9ChallengesFaced = q9ChallengesFaced; }
    public String getQ10DoctorNotes() { return q10DoctorNotes; }
    public void setQ10DoctorNotes(String q10DoctorNotes) { this.q10DoctorNotes = q10DoctorNotes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
