package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_notes")
public class DoctorNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private Long doctorId;
    private String doctorName;

    @Column(columnDefinition = "TEXT")
    private String noteText;

    private LocalDateTime createdDate = LocalDateTime.now();

    public DoctorNote() {}

    public DoctorNote(String caseId, Long doctorId, String doctorName, String noteText) {
        this.caseId = caseId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.noteText = noteText;
        this.createdDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
