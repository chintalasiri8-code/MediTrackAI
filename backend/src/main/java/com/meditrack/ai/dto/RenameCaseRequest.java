package com.meditrack.ai.dto;

public class RenameCaseRequest {
    private String caseId;
    private String newLabel;

    public RenameCaseRequest() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getNewLabel() { return newLabel; }
    public void setNewLabel(String newLabel) { this.newLabel = newLabel; }
}
