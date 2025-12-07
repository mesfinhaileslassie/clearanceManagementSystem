package com.university.clearance.util;

import java.util.regex.Pattern;

public class ValidationService {
    
    // Patterns
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^DBU\\d{7}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\s.'-]{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(09|07)\\d{8}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,}$");
    
    // UPDATED: Allow numbers (1-99) for block numbers
    private static final Pattern BLOCK_PATTERN = Pattern.compile("^[1-9][0-9]?$");
    
    // UPDATED: Allow any number for room numbers (1-9999)
    private static final Pattern ROOM_PATTERN = Pattern.compile("^[1-9][0-9]{0,3}$");
    
    // Validation Methods
    public static ValidationResult validateStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ValidationResult(false, "Student ID is required");
        }
        
        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            return new ValidationResult(false, 
                "Student ID must be exactly 7 digits after DBU\n" +
                "Format: DBU1234567");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ValidationResult(false, "Full name is required");
        }
        
        if (!NAME_PATTERN.matcher(fullName.trim()).matches()) {
            return new ValidationResult(false, 
                "Full name must be 2-50 characters\n" +
                "Only letters, spaces, dots, apostrophes and hyphens allowed");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        
        if (password.length() < 6) {
            return new ValidationResult(false, "Password must be at least 6 characters");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, 
                "Password must contain:\n" +
                "• At least 6 characters\n" +
                "• At least one letter\n" +
                "• At least one number");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(true, "Optional"); // Email is optional
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return new ValidationResult(false, "Invalid email format");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationResult(false, "Phone number is required");
        }
        
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            return new ValidationResult(false, 
                "Phone number must be:\n" +
                "• 10 digits total\n" +
                "• Start with 09 or 07\n" +
                "• Format: 0912345678 or 0712345678");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    public static ValidationResult validateDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return new ValidationResult(false, "Please select a department");
        }
        
        // List of valid departments
        String[] validDepartments = {
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration",
            "Accounting", "Economics", "Mathematics", "Food Engineering", 
            "Chemistry", "Biology"
        };
        
        for (String validDept : validDepartments) {
            if (validDept.equals(department)) {
                return new ValidationResult(true, "Valid");
            }
        }
        
        return new ValidationResult(false, "Invalid department selected");
    }
    
    public static ValidationResult validateYearLevel(String yearLevel) {
        if (yearLevel == null || yearLevel.trim().isEmpty()) {
            return new ValidationResult(false, "Please select year level");
        }
        
        String[] validYears = {"1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"};
        for (String validYear : validYears) {
            if (validYear.equals(yearLevel)) {
                return new ValidationResult(true, "Valid");
            }
        }
        
        return new ValidationResult(false, "Invalid year level");
    }
    
    // UPDATED: Validate block number as number (1-99)
    public static ValidationResult validateBlockNumber(String blockNumber) {
        if (blockNumber == null || blockNumber.trim().isEmpty()) {
            return new ValidationResult(true, "Optional"); // Block number is optional
        }
        
        if (!BLOCK_PATTERN.matcher(blockNumber.trim()).matches()) {
            return new ValidationResult(false, 
                "Block number must be:\n" +
                "• A number from 1 to 99\n" +
                "• Examples: 1, 5, 10, 25, 99");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    // UPDATED: Validate room number as any number (1-9999)
    public static ValidationResult validateRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return new ValidationResult(true, "Optional"); // Room number is optional
        }
        
        if (!ROOM_PATTERN.matcher(roomNumber.trim()).matches()) {
            return new ValidationResult(false, 
                "Room number must be:\n" +
                "• A number from 1 to 9999\n" +
                "• Examples: 101, 205, 1001, 9999");
        }
        
        return new ValidationResult(true, "Valid");
    }
    
    // Validate complete student registration
    public static ValidationResult validateStudentRegistration(
            String studentId, String fullName, String password, String email,
            String phone, String department, String yearLevel) {
        
        ValidationResult result;
        
        result = validateStudentId(studentId);
        if (!result.isValid()) return result;
        
        result = validateFullName(fullName);
        if (!result.isValid()) return result;
        
        result = validatePassword(password);
        if (!result.isValid()) return result;
        
        result = validateEmail(email);
        if (!result.isValid()) return result;
        
        result = validatePhone(phone);
        if (!result.isValid()) return result;
        
        result = validateDepartment(department);
        if (!result.isValid()) return result;
        
        result = validateYearLevel(yearLevel);
        if (!result.isValid()) return result;
        
        return new ValidationResult(true, "All fields are valid");
    }
    
    // Validation Result Class
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
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