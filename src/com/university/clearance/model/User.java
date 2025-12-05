package com.university.clearance.model;

public class User {
    private int id;
    private String username;
    private String fullName;
    private String role;
    private String email;
    private String department;
    private String status;
    private String yearLevel;
    private String phone;
    private String clearanceStatus;
    private boolean canReapply;
    private String password; // Added for completeness

    public User(int id, String username, String fullName, String role, String email, String department) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.department = department;
        this.status = "ACTIVE";
    }

    // Full constructor
    public User(int id, String username, String password, String fullName, String role, 
                String email, String department, String yearLevel, String phone, 
                String status, String clearanceStatus, boolean canReapply) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.department = department;
        this.yearLevel = yearLevel;
        this.phone = phone;
        this.status = status;
        this.clearanceStatus = clearanceStatus;
        this.canReapply = canReapply;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getClearanceStatus() { return clearanceStatus; }
    public void setClearanceStatus(String clearanceStatus) { this.clearanceStatus = clearanceStatus; }
    
    public boolean isCanReapply() { return canReapply; }
    public void setCanReapply(boolean canReapply) { this.canReapply = canReapply; }
}