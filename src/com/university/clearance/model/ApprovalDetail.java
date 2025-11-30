// src/com/university/clearance/model/ApprovalDetail.java
package com.university.clearance.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ApprovalDetail {

    private final StringProperty department = new SimpleStringProperty();
    private final StringProperty officerName = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty remarks = new SimpleStringProperty();

    public ApprovalDetail(String department, String officerName, String status, String remarks) {
        setDepartment(department);
        setOfficerName(officerName != null ? officerName : "Not Assigned");
        setStatus(status);
        setRemarks(remarks != null ? remarks : "-");
    }

    public StringProperty departmentProperty() { return department; }
    public StringProperty officerNameProperty() { return officerName; }
    public StringProperty statusProperty() { return status; }
    public StringProperty remarksProperty() { return remarks; }

    public void setDepartment(String v) { department.set(v); }
    public void setOfficerName(String v) { officerName.set(v); }
    public void setStatus(String v) { status.set(v); }
    public void setRemarks(String v) { remarks.set(v); }

    public String getDepartment() { return department.get(); }
    public String getOfficerName() { return officerName.get(); }
    public String getStatus() { return status.get(); }
    public String getRemarks() { return remarks.get(); }
}