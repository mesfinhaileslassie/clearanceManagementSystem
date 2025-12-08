package com.university.clearance.utils;

public class ValidationHelper {
    
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
    
    // ==================== STUDENT ID VALIDATION ====================
    
    public static ValidationResult validateStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        String id = studentId.trim().toUpperCase();
        
        // Format: DBU + 7 digits
        if (!id.matches("^DBU\\d{7}$")) {
            return new ValidationResult(false, "Format: DBU + 7 digits");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    public static String generateUsername(String studentId) {
        if (studentId == null || !studentId.trim().toUpperCase().startsWith("DBU")) {
            return "";
        }
        
        // Convert DBU1601111 to dbu1601111
        return studentId.trim().toLowerCase();
    }
    
    // ==================== FULL NAME VALIDATION ====================
    
    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        String name = fullName.trim();
        
        // Check for at least first and last name
        String[] parts = name.split("\\s+");
        if (parts.length < 2) {
            return new ValidationResult(false, "Enter first and last name");
        }
        
        // Check for only letters and spaces
        if (!name.matches("^[A-Za-z\\s.'-]+$")) {
            return new ValidationResult(false, "Only letters and spaces allowed");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== PASSWORD VALIDATION ====================
    
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        if (password.length() < 6) {
            return new ValidationResult(false, "Min 6 characters");
        }
        
        // Check for at least one letter and one number
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        
        if (!hasLetter || !hasNumber) {
            return new ValidationResult(false, "Letters and numbers required");
        }
        
        return new ValidationResult(true, "Strong ✓");
    }
    
 // ==================== EMAIL VALIDATION ====================
    
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Required"); // Changed from optional to required
        }
        
        String emailStr = email.trim().toLowerCase();
        
        // Basic email format validation
        if (!emailStr.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return new ValidationResult(false, "Invalid email format");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== PHONE NUMBER VALIDATION ====================
    
    public static ValidationResult validatePhoneNumber(String prefix, String suffix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ValidationResult(false, "Select 09 or 07 prefix");
        }
        
        if (!prefix.equals("09") && !prefix.equals("07")) {
            return new ValidationResult(false, "Prefix must be 09 or 07");
        }
        
        if (suffix == null || suffix.isEmpty()) {
            return new ValidationResult(false, "Enter 7 digits");
        }
        
        if (!suffix.matches("\\d{7}")) {
            return new ValidationResult(false, "Must be exactly 7 digits");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // Alternative single field phone validation
    public static ValidationResult validatePhoneSingleField(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        // Remove spaces and any non-digits
        String cleanPhone = phone.replaceAll("\\s+", "").replaceAll("[^\\d]", "");
        
        if (!cleanPhone.matches("^(09|07)\\d{7}$")) {
            return new ValidationResult(false, "Format: 09xxxxxxx or 07xxxxxxx");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== DEPARTMENT VALIDATION ====================
    
    public static ValidationResult validateDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return new ValidationResult(false, "Select a department");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== YEAR LEVEL VALIDATION ====================
    
    public static ValidationResult validateYearLevel(String yearLevel) {
        if (yearLevel == null || yearLevel.trim().isEmpty()) {
            return new ValidationResult(false, "Select a year level");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== BLOCK NUMBER VALIDATION ====================
    
    public static ValidationResult validateBlockNumber(String blockNumber) {
        if (blockNumber == null || blockNumber.trim().isEmpty()) {
            return new ValidationResult(true, "Optional - can be empty");
        }
        
        try {
            int block = Integer.parseInt(blockNumber.trim());
            if (block >= 1 && block <= 45) {
                return new ValidationResult(true, "Valid ✓");
            } else {
                return new ValidationResult(false, "Must be 1-45");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Must be a number");
        }
    }
    
    // ==================== DORM NUMBER VALIDATION ====================
    
    public static ValidationResult validateDormNumber(String dormNumber) {
        if (dormNumber == null || dormNumber.trim().isEmpty()) {
            return new ValidationResult(true, "Optional - can be empty");
        }
        
        try {
            int dorm = Integer.parseInt(dormNumber.trim());
            if (dorm >= 100 && dorm <= 4999) {
                return new ValidationResult(true, "Valid ✓");
            } else {
                return new ValidationResult(false, "Must be 100-4999");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Must be a number");
        }
    }
    
    // ==================== USERNAME VALIDATION (for officers) ====================
    
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        String user = username.trim().toLowerCase();
        
        if (user.equals("admin")) {
            return new ValidationResult(false, "Cannot use 'admin'");
        }
        
        if (!user.matches("^[a-z0-9_]{3,50}$")) {
            return new ValidationResult(false, "3-50 chars, lowercase, numbers, underscores");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== OFFICER EMAIL VALIDATION ====================
    
    public static ValidationResult validateOfficerEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        String emailStr = email.trim().toLowerCase();
        
        if (!emailStr.matches("^[A-Za-z0-9+_.-]+@dbu\\.edu\\.et$")) {
            return new ValidationResult(false, "Must end with @dbu.edu.et");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }
    
    // ==================== OFFICER PASSWORD VALIDATION ====================
    
    public static ValidationResult validateOfficerPassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        if (password.length() < 8) {
            return new ValidationResult(false, "Min 8 characters");
        }
        
        // Check for uppercase, lowercase, and number
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        
        if (!hasUpper || !hasLower || !hasNumber) {
            return new ValidationResult(false, "Uppercase, lowercase, and number required");
        }
        
        return new ValidationResult(true, "Strong ✓");
    }
    
    // ==================== CLEARANCE STATUS VALIDATION ====================
    
    public static ValidationResult validateClearanceStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        String[] validStatuses = {"PENDING", "IN_PROGRESS", "APPROVED", "REJECTED", "FULLY_CLEARED"};
        
        for (String validStatus : validStatuses) {
            if (validStatus.equals(status.toUpperCase())) {
                return new ValidationResult(true, "Valid ✓");
            }
        }
        
        return new ValidationResult(false, "Invalid status");
    }
    
    // ==================== DATE VALIDATION ====================
    
    public static ValidationResult validateDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        try {
            java.sql.Date.valueOf(dateStr);
            return new ValidationResult(true, "Valid ✓");
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, "Invalid date format (YYYY-MM-DD)");
        }
    }
    
    // ==================== AMOUNT VALIDATION ====================
    
    public static ValidationResult validateAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        try {
            double amount = Double.parseDouble(amountStr.trim());
            if (amount < 0) {
                return new ValidationResult(false, "Cannot be negative");
            }
            return new ValidationResult(true, "Valid ✓");
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Must be a number");
        }
    }
    
    // ==================== GENERATE RANDOM PASSWORD ====================
    
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    // ==================== FORMAT PHONE NUMBER ====================
    
    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        
        // Remove all non-digits
        String digits = phone.replaceAll("[^\\d]", "");
        
        // Format: 09 123 4567
        if (digits.length() == 9) {
            return digits.substring(0, 2) + " " + 
                   digits.substring(2, 5) + " " + 
                   digits.substring(5);
        }
        
        // Return as-is if not 9 digits
        return phone;
    }
    
    // ==================== VALIDATE SESSION NAME ====================
    
    public static ValidationResult validateSessionName(String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return new ValidationResult(false, "Required");
        }
        
        if (sessionName.trim().length() < 3) {
            return new ValidationResult(false, "Min 3 characters");
        }
        
        return new ValidationResult(true, "Valid ✓");
    }













//Add these methods to ValidationHelper.java

public static ValidationResult validatePhoneWithProvider(String phone, String provider) {
	if (phone == null || phone.trim().isEmpty()) {
        return new ValidationResult(false, "Required");
    }
 
String cleanPhone = phone.replaceAll("[^\\d]", "");
    
    // Check total length
    if (cleanPhone.length() != 10) {
        return new ValidationResult(false, "Must be exactly 10 digits");
    }
    
    if ("Provider A".equals(provider) && !cleanPhone.startsWith("09")) {
        return new ValidationResult(false, "Provider A must start with 09");
    } else if ("Provider B".equals(provider) && !cleanPhone.startsWith("07")) {
        return new ValidationResult(false, "Provider B must start with 07");
    }
    
    
    
    if (!cleanPhone.matches("^(09|07)\\d{8}$")) {
        return new ValidationResult(false, "Format: prefix (09/07) + 8 digits");
    }
    
    
 // Check total length
 if (cleanPhone.length() != 10) {
     return new ValidationResult(false, "Must be 10 digits total");
 }
 
 // Check prefix (2 digits) + 8 digits
 if (!cleanPhone.matches("^(09|07)\\d{8}$")) {
     return new ValidationResult(false, "Format: prefix (09/07) + 8 digits");
 }
 
 return new ValidationResult(true, "Valid ✓");
}

//For student registration
public static ValidationResult validateStudentPhone(String phone, String provider) {
 return validatePhoneWithProvider(phone, provider);
}

//For officer registration
public static ValidationResult validateOfficerPhone(String phone) {
 if (phone == null || phone.trim().isEmpty()) {
     return new ValidationResult(false, "Required");
 }
 
 String cleanPhone = phone.replaceAll("\\s+", "").replaceAll("[^\\d]", "");
 
 if (!cleanPhone.matches("^(09|07)\\d{8}$")) {
     return new ValidationResult(false, "Format: 09xxxxxxx or 07xxxxxxx");
 }
 
 return new ValidationResult(true, "Valid ✓");
}}