package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.service.PDFCertificateService;
import com.university.clearance.service.SimpleCertificateService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {

    @FXML private Label lblStatusMessage;
    @FXML private Button btnSubmitRequest;
    @FXML private Button btnGenerateCertificate;
    
    @FXML private TableView<ApprovalStatus> tableApprovals;
    @FXML private TableColumn<ApprovalStatus, String> colDepartment;
    @FXML private TableColumn<ApprovalStatus, String> colStatus;
    @FXML private TableColumn<ApprovalStatus, String> colRemarks;
    
    // Profile display labels
    @FXML private Label lblProfileId;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileDepartment;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblProfileYearLevel;
    @FXML private Label lblProfilePhone;

    private User currentUser;
    private int currentRequestId = -1;
    private ObservableList<ApprovalStatus> approvalData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        btnGenerateCertificate.setDisable(true);
    }
    
    /**
     * Sets the currently logged-in user and initiates loading of clearance status and profile info.
     * @param user The logged-in User object.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadClearanceStatus();
        loadProfileInformation();
    }
    
    /**
     * Configures the TableView columns and adds a custom cell factory for color-coded status.
     */
    private void setupTableColumns() {
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        colStatus.setCellFactory(column -> new TableCell<ApprovalStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color-code the status
                    if (item.contains("Approved")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Green
                    } else if (item.contains("Rejected")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Red
                    } else {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Orange/Yellow
                    }
                }
            }
        });
    }

    /**
     * Handles the student's request to begin the clearance process.
     */
    @FXML
    private void submitClearanceRequest() {
        if (hasActiveRequest()) {
            showAlert("Active Request", "You already have an active clearance request. Please wait for approvals or address rejections.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Submit Request");
        confirmation.setHeaderText("Start Clearance Process");
        confirmation.setContentText("Submit clearance request to all departments?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                createClearanceRequest();
            }
        });
    }

    /**
     * Checks the database to see if the student already has an active request 
     * (status not FULLY_CLEARED or REJECTED).
     * @return true if an active request exists.
     */
    private boolean hasActiveRequest() {
        if (currentUser == null) return false;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM clearance_requests WHERE student_id = ? AND status NOT IN ('FULLY_CLEARED', 'REJECTED')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new clearance request and corresponding approval entries for all departments.
     */
    private void createClearanceRequest() {
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Create the clearance request
            String requestSql = "INSERT INTO clearance_requests (student_id, request_date, status) VALUES (?, NOW(), 'IN_PROGRESS')";
            PreparedStatement requestStmt = conn.prepareStatement(requestSql, Statement.RETURN_GENERATED_KEYS);
            requestStmt.setInt(1, currentUser.getId());
            requestStmt.executeUpdate();
            
            ResultSet rs = requestStmt.getGeneratedKeys();
            if (rs.next()) {
                currentRequestId = rs.getInt(1);
            }
            
            // 2. Create approvals for all required departments/officer roles
            // NOTE: Must match the roles expected in the database and by the officer dashboard.
            String[] departments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
            String approvalSql = "INSERT INTO clearance_approvals (request_id, officer_role, status) VALUES (?, ?, 'PENDING')";
            PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
            
            for (String dept : departments) {
                approvalStmt.setInt(1, currentRequestId);
                approvalStmt.setString(2, dept);
                approvalStmt.addBatch();
            }
            
            approvalStmt.executeBatch();
            conn.commit();
            
            showMessage("Request submitted successfully! Check progress below.", "success");
            loadClearanceStatus();
            
        } catch (Exception e) {
            showMessage("Failed to submit request: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Refreshes the clearance status displayed on the dashboard.
     */
    @FXML
    private void refreshStatus() {
        loadClearanceStatus();
        showMessage("Status refreshed.", "info");
    }

    /**
     * Handles the generation of the final clearance certificate in the user's chosen format.
     */
    @FXML
    private void generateCertificate() {
        if (currentUser == null) {
            showAlert("Session", "Please log in again.");
            return;
        }
        
        if (!isClearanceFullyApproved()) {
            showAlert("Not Ready", "Your clearance is not fully approved yet. Please refresh the status.");
            return;
        }
        
        // Let user choose format
        ChoiceDialog<String> dialog = new ChoiceDialog<>("PDF", "PDF", "HTML", "TEXT");
        dialog.setTitle("Choose Certificate Format");
        dialog.setHeaderText("Select Certificate Format");
        dialog.setContentText("Choose your preferred format:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(format -> {
            try {
                showMessage("Generating " + format + " certificate...", "info");
                
                String filePath = null;
                
                // Determine which service and format to use
                switch (format) {
                    case "PDF":
                        PDFCertificateService pdfService = new PDFCertificateService();
                        filePath = pdfService.generatePDFCertificate(currentUser.getId(), currentRequestId);
                        break;
                    case "HTML":
                        SimpleCertificateService htmlService = new SimpleCertificateService();
                        // CORRECTED: The service is now relied upon to return the correct .html path.
                        // We remove the redundant/incorrect path manipulation logic.
                        filePath = htmlService.generateHTMLCertificate(currentUser.getId(), currentRequestId);
                        break;
                    case "TEXT":
                        SimpleCertificateService textService = new SimpleCertificateService();
                        filePath = textService.generateClearanceCertificate(currentUser.getId(), currentRequestId);
                        break;
                }
                
                // Check if file was created successfully
                if (filePath != null && new File(filePath).exists()) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Certificate Ready");
                    success.setHeaderText(format + " Certificate Generated!");
                    success.setContentText("Certificate saved to:\n" + filePath);
                    success.showAndWait();
                    
                 
                } else {
                    showMessage("Failed to generate " + format + " certificate. File not created or path is invalid.", "error");
                }
                
            } catch (Exception e) {
                showMessage("Error generating certificate: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        });
    }

    /**
     * Determines if the current clearance request has been fully approved by all required departments.
     * @return true if all 5 required approvals are 'APPROVED' and there are no 'REJECTED' statuses.
     */
    private boolean isClearanceFullyApproved() {
        if (currentRequestId == -1) return false;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if all departments approved (expecting 5 approvals)
            String sql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
                    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected
                FROM clearance_approvals 
                WHERE request_id = ?
                AND officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentRequestId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                int approved = rs.getInt("approved");
                int rejected = rs.getInt("rejected");
                
                // Must have 5 departments initialized, 5 approved, and 0 rejected
                return total == 5 && approved == 5 && rejected == 0;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Loads the student's latest clearance request status and approval details into the TableView.
     */
    private void loadClearanceStatus() {
        if (currentUser == null) return;
        
        approvalData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get latest request
            String requestSql = "SELECT id, status FROM clearance_requests WHERE student_id = ? ORDER BY request_date DESC LIMIT 1";
            PreparedStatement requestStmt = conn.prepareStatement(requestSql);
            requestStmt.setInt(1, currentUser.getId());
            ResultSet requestRs = requestStmt.executeQuery();
            
            if (!requestRs.next()) {
                showMessage("No clearance request found. Click 'Submit Request' to start.", "info");
                btnSubmitRequest.setDisable(false);
                btnGenerateCertificate.setDisable(true);
                return;
            }
            
            currentRequestId = requestRs.getInt("id");
            String overallStatus = requestRs.getString("status");
            
            // Check if fully approved and update overall status if necessary
            boolean isFullyApproved = isClearanceFullyApproved();
            btnGenerateCertificate.setDisable(!isFullyApproved);
            
            if (isFullyApproved && !"FULLY_CLEARED".equals(overallStatus)) {
                updateOverallStatus(conn, currentRequestId);
                showMessage("Your clearance is fully approved! You can now download your certificate.", "success");
            } else if (isFullyApproved || "FULLY_CLEARED".equals(overallStatus)) {
                 showMessage("Your clearance is fully approved! You can now download your certificate.", "success");
            } else {
                showMessage("Clearance in progress...", "info");
            }
            
            // Load approval details
            String approvalSql = """
                SELECT 
                    ca.officer_role,
                    CASE 
                        WHEN ca.status = 'APPROVED' THEN 'Approved'
                        WHEN ca.status = 'REJECTED' THEN 'Rejected'
                        ELSE 'Pending'
                    END as display_status,
                    COALESCE(ca.remarks, 'No remarks') as remarks
                FROM clearance_approvals ca
                WHERE ca.request_id = ?
                AND ca.officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                ORDER BY FIELD(ca.officer_role, 'LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
                
            PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
            approvalStmt.setInt(1, currentRequestId);
            ResultSet approvalRs = approvalStmt.executeQuery();
            
            while (approvalRs.next()) {
                ApprovalStatus status = new ApprovalStatus(
                    formatDepartmentName(approvalRs.getString("officer_role")),
                    approvalRs.getString("display_status"),
                    approvalRs.getString("remarks")
                );
                approvalData.add(status);
            }
            
            tableApprovals.setItems(approvalData);
            
        } catch (Exception e) {
            showMessage("Error loading status: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Updates the clearance request status to 'FULLY_CLEARED' in the database.
     */
    private void updateOverallStatus(Connection conn, int requestId) throws SQLException {
        String updateSql = "UPDATE clearance_requests SET status = 'FULLY_CLEARED', completion_date = NOW() WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(updateSql);
        ps.setInt(1, requestId);
        ps.executeUpdate();
    }

    /**
     * Loads the student's profile information from the database and updates the labels.
     */
    private void loadProfileInformation() {
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                lblProfileId.setText(rs.getString("username"));
                lblProfileName.setText(rs.getString("full_name"));
                lblProfileDepartment.setText(rs.getString("department"));
                lblProfileEmail.setText(rs.getString("email"));
                lblProfileYearLevel.setText(rs.getString("year_level") != null ? rs.getString("year_level") : "Not set");
                lblProfilePhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "Not provided");
            }
        } catch (Exception e) {
            showMessage("Error loading profile: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Displays a dialog for the user to change their password.
     */
    @FXML
    private void changePassword() {
        // Create a custom dialog for password change
        Dialog<PasswordPair> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter your new password");

        // Set the button types
        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        // Create the password fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("Current Password");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New Password");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm New Password");

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(oldPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPassword, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                return new PasswordPair(
                    oldPassword.getText(),
                    newPassword.getText(),
                    confirmPassword.getText()
                );
            }
            return null;
        });

        Optional<PasswordPair> result = dialog.showAndWait();

        result.ifPresent(passwordPair -> {
            String oldPass = passwordPair.getOldPassword();
            String newPass = passwordPair.getNewPassword();
            String confirmPass = passwordPair.getConfirmPassword();
            
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                showAlert("Error", "All password fields are required");
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                showAlert("Error", "New passwords do not match");
                return;
            }
            
            if (newPass.length() < 6) {
                showAlert("Error", "Password must be at least 6 characters long");
                return;
            }
            
            // Verify current password first
            if (!verifyCurrentPassword(oldPass)) {
                showAlert("Error", "Current password is incorrect");
                return;
            }
            
            // Update password in database
            updatePassword(newPass);
        });
    }

    /**
     * Checks the provided current password against the stored password in the database.
     * @param oldPassword The password entered by the user.
     * @return true if the password matches the stored password.
     */
    private boolean verifyCurrentPassword(String oldPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT password FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // NOTE: In a real app, use a proper hashing and comparison library (e.g., BCrypt).
                return storedPassword.equals(oldPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates the user's password in the database.
     * @param newPassword The new password to save.
     */
    private void updatePassword(String newPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword); // NOTE: In a real app, hash this password
            ps.setInt(2, currentUser.getId());
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                showAlert("Success", "Password changed successfully!");
            } else {
                showAlert("Error", "Failed to change password");
            }
        } catch (Exception e) {
            showAlert("Error", "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refreshes the profile view (optional, since loadProfileInformation runs on startup).
     */
    @FXML
    private void viewProfile() {
        loadProfileInformation();
    }

    /**
     * Handles user logout and navigates back to the Login scene.
     */
    @FXML
    private void handleLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Switch to login scene
                Parent root = FXMLLoader.load(getClass().getResource("/com/university/clearance/view/Login.fxml"));
                Stage stage = (Stage) lblStatusMessage.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("University Clearance System - Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage());
            }
        }
    }

    /**
     * Formats the database role (e.g., LIBRARIAN) into a user-friendly name (e.g., Library).
     * @param role The officer role string.
     * @return The formatted department name.
     */
    private String formatDepartmentName(String role) {
        return switch (role) {
            case "LIBRARIAN" -> "Library";
            case "CAFETERIA" -> "Cafeteria";
            case "DORMITORY" -> "Dormitory";
            case "REGISTRAR" -> "Registrar";
            case "DEPARTMENT_HEAD" -> "Department Head";
            default -> role;
        };
    }

    /**
     * Displays messages in the status label with appropriate styling.
     */
    private void showMessage(String message, String type) {
        lblStatusMessage.setText(message);
        switch (type) {
            case "success":
                lblStatusMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                break;
            case "error":
                lblStatusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                break;
            default:
                lblStatusMessage.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        }
    }

    /**
     * Displays a standard JavaFX information alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for password change dialog result
    private static class PasswordPair {
        private final String oldPassword;
        private final String newPassword;
        private final String confirmPassword;

        public PasswordPair(String oldPassword, String newPassword, String confirmPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
            this.confirmPassword = confirmPassword;
        }

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
    }

    // Inner class for approval status used by the TableView
    public static class ApprovalStatus {
        private final String department;
        private final String status;
        private final String remarks;

        public ApprovalStatus(String department, String status, String remarks) {
            this.department = department;
            this.status = status;
            this.remarks = remarks;
        }

        public String getDepartment() { return department; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
    }
}