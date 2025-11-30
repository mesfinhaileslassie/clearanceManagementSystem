package com.university.clearance.model;

public class User {
    private int id;
    private String username;
    private String fullName;
    private String role;
    private String email;
    private String department;
    private String status;

    public User(int id, String username, String fullName, String role, String email, String department) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.department = department;
        this.status = "ACTIVE";
    }

    // Getters and setters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}