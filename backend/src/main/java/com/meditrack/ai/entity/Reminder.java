package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reminders")
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private String medicineName;
    private String reminderTime; // e.g. "8:00 AM"
    private String foodRelation; // e.g. "After breakfast"
    private String repeatType;   // "Daily", "Weekly"
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active = true;
    private String status = "PENDING"; // PENDING, TAKEN, REMIND_LATER, MISSED

    public Reminder() {}

    public Reminder(String caseId, String medicineName, String reminderTime, String foodRelation, String repeatType, LocalDate startDate, LocalDate endDate) {
        this.caseId = caseId;
        this.medicineName = medicineName;
        this.reminderTime = reminderTime;
        this.foodRelation = foodRelation;
        this.repeatType = repeatType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    public String getFoodRelation() { return foodRelation; }
    public void setFoodRelation(String foodRelation) { this.foodRelation = foodRelation; }
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
