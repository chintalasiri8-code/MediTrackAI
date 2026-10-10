package com.meditrack.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private String uploadedByRole; // PATIENT or DOCTOR
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public DocumentEntity() {}

    public DocumentEntity(String caseId, String fileName, String fileType, String fileUrl, String uploadedByRole) {
        this.caseId = caseId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.uploadedByRole = uploadedByRole;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getUploadedByRole() { return uploadedByRole; }
    public void setUploadedByRole(String uploadedByRole) { this.uploadedByRole = uploadedByRole; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
