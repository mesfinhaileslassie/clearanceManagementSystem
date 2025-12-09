package com.university.clearance.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClearanceRequest {
    private final StringProperty studentId;
    private final StringProperty fullName;
    private final StringProperty department;
    private final StringProperty status;
    private final StringProperty requestDate;
    private final IntegerProperty approvedCount;
    private final BooleanProperty canReapply;
    private int requestId;
    
    // Formatter for parsing date strings
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    public ClearanceRequest(String studentId, String fullName, String department,
                           String status, String requestDate, int approvedCount) {
        this.studentId = new SimpleStringProperty(studentId);
        this.fullName = new SimpleStringProperty(fullName);
        this.department = new SimpleStringProperty(department);
        this.status = new SimpleStringProperty(status);
        this.requestDate = new SimpleStringProperty(requestDate);
        this.approvedCount = new SimpleIntegerProperty(approvedCount);
        this.canReapply = new SimpleBooleanProperty(false);
    }

    // Property getters for JavaFX TableView
    public StringProperty studentIdProperty() { return studentId; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty departmentProperty() { return department; }
    public StringProperty statusProperty() { return status; }
    public StringProperty requestDateProperty() { return requestDate; }
    public IntegerProperty approvedCountProperty() { return approvedCount; }
    public BooleanProperty canReapplyProperty() { return canReapply; }

    // Regular getters
    public String getStudentId() { return studentId.get(); }
    public String getFullName() { return fullName.get(); }
    public String getDepartment() { return department.get(); }
    public String getStatus() { return status.get(); }
    public String getRequestDate() { return requestDate.get(); }
    public int getApprovedCount() { return approvedCount.get(); }
    public boolean isCanReapply() { return canReapply.get(); }
    
    // Setters
    public void setStudentId(String studentId) { this.studentId.set(studentId); }
    public void setFullName(String fullName) { this.fullName.set(fullName); }
    public void setDepartment(String department) { this.department.set(department); }
    public void setStatus(String status) { this.status.set(status); }
    public void setRequestDate(String requestDate) { this.requestDate.set(requestDate); }
    public void setApprovedCount(int approvedCount) { this.approvedCount.set(approvedCount); }
    public void setCanReapply(boolean canReapply) { this.canReapply.set(canReapply); }
    
    // Request ID methods
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    
    // Utility methods for the Allow Resubmission feature
    
    /**
     * Check if this clearance request is rejected
     * @return true if status is "REJECTED"
     */
    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(getStatus());
    }
    
    /**
     * Check if this clearance request is expired (older than 30 days)
     * @return true if request is older than 30 days
     */
    public boolean isExpired() {
        try {
            String dateStr = getRequestDate();
            if (dateStr == null || dateStr.isEmpty()) {
                return false;
            }
            
            LocalDateTime requestDateTime = parseDateTime(dateStr);
            LocalDateTime now = LocalDateTime.now();
            
            // Check if request is older than 30 days
            boolean expired = requestDateTime.plusDays(30).isBefore(now);
            
            System.out.println("DEBUG isExpired: " + 
                             "Request date: " + requestDateTime + 
                             ", Now: " + now + 
                             ", Expired: " + expired +
                             ", Days difference: " + java.time.Duration.between(requestDateTime, now).toDays());
            
            return expired;
        } catch (Exception e) {
            System.err.println("Error in isExpired for request " + getRequestId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if this request is eligible for resubmission
     * (Rejected and expired)
     * @return true if eligible for resubmission
     */
    public boolean isEligibleForResubmission() {
        return isRejected() && isExpired() && !isCanReapply();
    }
    
    /**
     * Check if the Allow Resubmission button should be shown
     * @return true if button should be shown
     */
    public boolean shouldShowResubmitButton() {
        return isEligibleForResubmission();
    }
    
    /**
     * Parse date string to LocalDateTime
     * @param dateStr the date string to parse
     * @return LocalDateTime object
     */
    private LocalDateTime parseDateTime(String dateStr) {
        // Try different date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // Try to parse as LocalDateTime first
                if (dateStr.length() > 10) { // Has time component
                    return LocalDateTime.parse(dateStr.replace(" ", "T"), 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else { // Just date
                    return LocalDateTime.parse(dateStr + "T00:00:00", 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        // If all parsers fail, throw exception
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
    
    /**
     * Get formatted request date
     * @return formatted date string
     */
    public String getFormattedDate() {
        try {
            LocalDateTime dateTime = parseDateTime(getRequestDate());
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return getRequestDate();
        }
    }
    
    /**
     * Get days since request was made
     * @return number of days
     */
    public int getDaysSinceRequest() {
        try {
            LocalDateTime requestDateTime = parseDateTime(getRequestDate());
            LocalDateTime now = LocalDateTime.now();
            return (int) java.time.Duration.between(requestDateTime, now).toDays();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get days until expiration (negative if already expired)
     * @return days until expiration
     */
    public int getDaysUntilExpiration() {
        try {
            LocalDateTime requestDateTime = parseDateTime(getRequestDate());
            LocalDateTime expirationDate = requestDateTime.plusDays(30);
            LocalDateTime now = LocalDateTime.now();
            
            long days = java.time.Duration.between(now, expirationDate).toDays();
            return (int) days;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get expiration status message
     * @return expiration status message
     */
    public String getExpirationStatus() {
        if (!isRejected()) {
            return "Not applicable";
        }
        
        int daysUntilExpiration = getDaysUntilExpiration();
        if (daysUntilExpiration > 0) {
            return "Expires in " + daysUntilExpiration + " days";
        } else if (daysUntilExpiration == 0) {
            return "Expires today";
        } else {
            return "Expired " + Math.abs(daysUntilExpiration) + " days ago";
        }
    }
    
    /**
     * Get resubmission eligibility message
     * @return eligibility message
     */
    public String getResubmissionEligibility() {
        if (!isRejected()) {
            return "Not eligible (not rejected)";
        }
        
        if (isCanReapply()) {
            return "Already allowed to reapply";
        }
        
        if (isExpired()) {
            return "Eligible for resubmission";
        } else {
            int daysLeft = getDaysUntilExpiration();
            return "Eligible in " + daysLeft + " days";
        }
    }
    
    @Override
    public String toString() {
        return String.format("ClearanceRequest{studentId='%s', fullName='%s', " +
                           "department='%s', status='%s', requestDate='%s', " +
                           "approvedCount=%d, canReapply=%b, requestId=%d}",
                           getStudentId(), getFullName(), getDepartment(),
                           getStatus(), getFormattedDate(), getApprovedCount(),
                           isCanReapply(), getRequestId());
    }
}