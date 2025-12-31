package com.university.clearance.model;

//Inner class for table data - UPDATE THIS CLASS
public  class RegistrarClearanceRequest {
 private final String studentId;
 private final String studentName;
 private final String department;
 private final String yearLevel;
 private final String requestDate;
 private final String academicStatus;
 private final int requestId;
 private final int approvedDepartmentsCount;
 private final boolean allDepartmentsApproved;

 public RegistrarClearanceRequest(String studentId, String studentName, String department, 
                                String yearLevel, String requestDate, String academicStatus, 
                                int requestId, int approvedDepartmentsCount) {
     this.studentId = studentId;
     this.studentName = studentName;
     this.department = department;
     this.yearLevel = yearLevel;
     this.requestDate = requestDate;
     this.academicStatus = academicStatus;
     this.requestId = requestId;
     this.approvedDepartmentsCount = approvedDepartmentsCount;
     this.allDepartmentsApproved = (approvedDepartmentsCount == 4);
 }

 // Getters
 public String getStudentId() { return studentId; }
 public String getStudentName() { return studentName; }
 public String getDepartment() { return department; }
 public String getYearLevel() { return yearLevel; }
 public String getRequestDate() { return requestDate; }
 public String getAcademicStatus() { return academicStatus; }
 public int getRequestId() { return requestId; }
 public int getApprovedDepartmentsCount() { return approvedDepartmentsCount; }
 public boolean isAllDepartmentsApproved() { return allDepartmentsApproved; }
 
 // Helper method for display
 public String getDepartmentStatus() {
     return approvedDepartmentsCount + "/4 Departments Approved";
 }
}