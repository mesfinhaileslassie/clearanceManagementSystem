package com.university.clearance.model;

public class Approval {
    private String officerRole;
    private String status;
    private String remarks;

    public Approval(String officerRole, String status, String remarks) {
        this.officerRole = officerRole;
        this.status = status;
        this.remarks = (remarks == null || remarks.isEmpty()) ? "-" : remarks;
    }

    public String getOfficerRole() { return officerRole; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }
}