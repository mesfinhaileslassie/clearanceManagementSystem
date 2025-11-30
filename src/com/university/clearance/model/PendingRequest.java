// src/com/university/clearance/model/PendingRequest.java
package com.university.clearance.model;

import javafx.beans.property.SimpleStringProperty;

public class PendingRequest {
    private final SimpleStringProperty studentId = new SimpleStringProperty();
    private final SimpleStringProperty fullName = new SimpleStringProperty();
    private final SimpleStringProperty department = new SimpleStringProperty();
    private final SimpleStringProperty date = new SimpleStringProperty();
    private final SimpleStringProperty myStatus = new SimpleStringProperty();
    private final int requestId;

    public PendingRequest(int requestId, String studentId, String fullName, String department, String date, String myStatus) {
        this.requestId = requestId;
        this.studentId.set(studentId);
        this.fullName.set(fullName != null ? fullName : "Unknown");
        this.department.set(department != null ? department : "N/A");
        this.date.set(date);
        this.myStatus.set(myStatus);
    }

    public int getRequestId() { return requestId; }

    // GETTERS FOR DISPLAY IN MESSAGES
    public String getStudentId() { return studentId.get(); }
    public String getFullName() { return fullName.get(); }        // THIS WAS MISSING!
    public String getDepartment() { return department.get(); }
    public String getDate() { return date.get(); }
    public String getMyStatus() { return myStatus.get(); }

    // PROPERTY METHODS (for TableView)
    public SimpleStringProperty studentIdProperty() { return studentId; }
    public SimpleStringProperty fullNameProperty() { return fullName; }
    public SimpleStringProperty departmentProperty() { return department; }
    public SimpleStringProperty dateProperty() { return date; }
    public SimpleStringProperty myStatusProperty() { return myStatus; }
}