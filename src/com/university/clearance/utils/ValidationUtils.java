package com.university.clearance.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    // Student ID validation
    public static boolean isValidStudentId(String studentId) {
        return Pattern.matches("^DBU\\d{7}$", studentId);
    }
    
    // Username validation
    public static boolean isValidUsername(String username) {
        return Pattern.matches("^[a-z0-9_]{3,50}$", username);
    }
    
    // Full name validation
    public static boolean isValidFullName(String fullName) {
        return Pattern.matches("^[A-Za-z\\s\\-]{2,100}$", fullName);
    }
    
    // Email validation
    public static boolean isValidEmail(String email) {
        return Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", email);
    }
    
    // Phone validation (Ethiopian format)
    public static boolean isValidEthiopianPhone(String phone) {
        return Pattern.matches("^(09|07)\\d{8}$", phone);
    }
    
    // Password strength validation
    public static boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        if (!Pattern.matches(".*[A-Z].*", password)) return false;
        if (!Pattern.matches(".*[a-z].*", password)) return false;
        if (!Pattern.matches(".*\\d.*", password)) return false;
        return true;
    }
    
    // Dormitory block validation
    public static boolean isValidBlockNumber(String block) {
        return Pattern.matches("^[A-Za-z1-9]{1,3}$", block);
    }
    
    // Dormitory room validation
    public static boolean isValidRoomNumber(String room) {
        return Pattern.matches("^\\d{3,4}$", room);
    }
    
    // Department validation
    public static boolean isValidDepartment(String department) {
        String[] validDepartments = {
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration",
            "Accounting", "Economics", "Mathematics", "Food Engineering", 
            "Chemistry", "Biology"
        };
        for (String validDept : validDepartments) {
            if (validDept.equals(department)) {
                return true;
            }
        }
        return false;
    }
    
    // Year level validation
    public static boolean isValidYearLevel(String year) {
        String[] validYears = {"1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"};
        for (String validYear : validYears) {
            if (validYear.equals(year)) {
                return true;
            }
        }
        return false;
    }
}