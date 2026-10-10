package com.meditrack.ai.dto;

public class JoinCaseRequest {
    private String caseId;
    private Long patientId;

    public JoinCaseRequest() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
}
