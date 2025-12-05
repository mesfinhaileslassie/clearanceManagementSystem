package com.university.clearance.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ClearanceRequest {
    private final StringProperty studentId;
    private final StringProperty fullName;
    private final StringProperty department;
    private final StringProperty status;
    private final StringProperty requestDate;
    private final IntegerProperty approvedCount;
    private int requestId;

    public ClearanceRequest(String studentId, String fullName, String department,
                           String status, String requestDate, int approvedCount) {
        this.studentId = new SimpleStringProperty(studentId);
        this.fullName = new SimpleStringProperty(fullName);
        this.department = new SimpleStringProperty(department);
        this.status = new SimpleStringProperty(status);
        this.requestDate = new SimpleStringProperty(requestDate);
        this.approvedCount = new SimpleIntegerProperty(approvedCount);
    }

    // Property getters for JavaFX TableView
    public StringProperty studentIdProperty() { return studentId; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty departmentProperty() { return department; }
    public StringProperty statusProperty() { return status; }
    public StringProperty requestDateProperty() { return requestDate; }
    public IntegerProperty approvedCountProperty() { return approvedCount; }

    // Regular getters
    public String getStudentId() { return studentId.get(); }
    public String getFullName() { return fullName.get(); }
    public String getDepartment() { return department.get(); }
    public String getStatus() { return status.get(); }
    public String getRequestDate() { return requestDate.get(); }
    public int getApprovedCount() { return approvedCount.get(); }
    
    // New method
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
}