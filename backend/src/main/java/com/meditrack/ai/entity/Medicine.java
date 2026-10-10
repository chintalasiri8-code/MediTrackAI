package com.meditrack.ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medicines")
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private Long prescriptionId;
    private String name;
    private String dosage; // e.g. "500 mg", "5 ml"
    private String unit;   // e.g. "mg", "ml", "tablet"
    private String timeOfDay; // "Morning", "Afternoon", "Evening", "Night"
    private String foodRelation; // "Before food", "After food"
    private Integer durationDays;
    private String purpose = "Purpose not specified in prescription.";
    private String status = "PENDING"; // TAKEN, PENDING, MISSED

    public Medicine() {}

    public Medicine(String caseId, Long prescriptionId, String name, String dosage, String unit, String timeOfDay, String foodRelation, Integer durationDays, String purpose, String status) {
        this.caseId = caseId;
        this.prescriptionId = prescriptionId;
        this.name = name;
        this.dosage = dosage;
        this.unit = unit;
        this.timeOfDay = timeOfDay;
        this.foodRelation = foodRelation;
        this.durationDays = durationDays;
        this.purpose = (purpose != null && !purpose.isBlank()) ? purpose : "Purpose not specified in prescription.";
        this.status = (status != null) ? status : "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    public String getFoodRelation() { return foodRelation; }
    public void setFoodRelation(String foodRelation) { this.foodRelation = foodRelation; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
