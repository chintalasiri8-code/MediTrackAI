package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "timeline_events")
public class TimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private String eventType; // CONSULTATION, PRESCRIPTION_UPLOADED, OCR_COMPLETED, DIGITAL_PRESCRIPTION_CREATED, MEDICINE_SCHEDULE_CREATED, SMART_REMINDER_ACTIVATED, WEEKLY_FEEDBACK_SUBMITTED, AI_SUMMARY_GENERATED, DOCTOR_REVIEWED
    private String title;
    private String description;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String category; // CLINICAL, MEDICATION, RECOVERY, SYSTEM

    public TimelineEvent() {}

    public TimelineEvent(String caseId, String eventType, String title, String description, String category) {
        this.caseId = caseId;
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.category = category;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
