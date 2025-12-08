package com.university.clearance.utils;

import java.util.regex.Pattern;

public class ValidationHelper {
    
    // Student ID validation
    public static ValidationResult validateStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ValidationResult(false, "Student ID is required");
        }
        
        String trimmedId = studentId.trim().toUpperCase();
        
        if (!trimmedId.startsWith("DBU")) {
            return new ValidationResult(false, "Must start with DBU");
        }
        
        if (trimmedId.length() != 10) {
            return new ValidationResult(false, "Must be DBU + 7 digits (10 total)");
        }
        
        String digits = trimmedId.substring(3);
        if (!Pattern.matches("^\\d{7}$", digits)) {
            return new ValidationResult(false, "After DBU must be 7 digits");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Generate username from student ID
    public static String generateUsername(String studentId) {
        if (studentId == null || studentId.trim().length() < 3) return "";
        String id = studentId.trim().toUpperCase();
        if (!id.startsWith("DBU")) return "";
        if (id.length() != 10) return "";
        return "dbu" + id.substring(3);
    }
    
    // Full name validation
    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ValidationResult(false, "Full name is required");
        }
        
        String name = fullName.trim();
        
        if (name.length() < 2) {
            return new ValidationResult(false, "At least 2 characters");
        }
        
        if (name.length() > 100) {
            return new ValidationResult(false, "Maximum 100 characters");
        }
        
        if (!Pattern.matches("^[A-Za-z\\s\\-']+$", name)) {
            return new ValidationResult(false, "Only letters, spaces, hyphens, apostrophes");
        }
        
        // Check for at least first and last name
        String[] nameParts = name.split("\\s+");
        if (nameParts.length < 2) {
            return new ValidationResult(false, "Include first and last name");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Password strength validation
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        
        if (password.length() < 6) {
            return new ValidationResult(false, "At least 6 characters");
        }
        
        if (!Pattern.matches(".*[A-Za-z].*", password)) {
            return new ValidationResult(false, "Must contain letters");
        }
        
        if (!Pattern.matches(".*\\d.*", password)) {
            return new ValidationResult(false, "Must contain numbers");
        }
        
        return new ValidationResult(true, "Strong ✓");
    }
    
    // Email validation
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(true, "Optional - can be empty");
        }
        
        String trimmedEmail = email.trim();
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        if (!Pattern.matches(emailRegex, trimmedEmail)) {
            return new ValidationResult(false, "Invalid email format");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Phone number validation
    public static ValidationResult validatePhoneNumber(String prefix, String suffix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ValidationResult(false, "Select 09 or 07");
        }
        
        if (suffix == null || suffix.isEmpty()) {
            return new ValidationResult(false, "Enter 8 digits");
        }
        
        String fullPhone = prefix + suffix;
        
        if (!Pattern.matches("^(09|07)\\d{8}$", fullPhone)) {
            return new ValidationResult(false, "Format: 09/07 + 8 digits");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Block number validation (must be number 1-10)
    public static ValidationResult validateBlockNumber(String block) {
        if (block == null || block.trim().isEmpty()) {
            return new ValidationResult(true, "Optional - can be empty");
        }
        
        String trimmedBlock = block.trim();
        
        if (!Pattern.matches("^[1-9]|10$", trimmedBlock)) {
            return new ValidationResult(false, "Must be number 1-10");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Dorm number validation (must be 3-4 digits)
    public static ValidationResult validateDormNumber(String dorm) {
        if (dorm == null || dorm.trim().isEmpty()) {
            return new ValidationResult(true, "Optional - can be empty");
        }
        
        String trimmedDorm = dorm.trim();
        
        if (!Pattern.matches("^\\d{3,4}$", trimmedDorm)) {
            return new ValidationResult(false, "3-4 digits only");
        }
        
        int dormNum = Integer.parseInt(trimmedDorm);
        if (dormNum < 100 || dormNum > 4999) {
            return new ValidationResult(false, "Must be between 100-4999");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Department validation
    public static ValidationResult validateDepartment(String department) {
        if (department == null || department.isEmpty()) {
            return new ValidationResult(false, "Select a department");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Year level validation
    public static ValidationResult validateYearLevel(String year) {
        if (year == null || year.isEmpty()) {
            return new ValidationResult(false, "Select year level");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Validation result class
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}