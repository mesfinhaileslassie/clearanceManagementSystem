package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import com.university.clearance.service.PDFCertificateService;
import com.university.clearance.service.SimpleCertificateService;

public class StudentDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblStatusMessage;
    @FXML private Button btnSubmitRequest;
    @FXML private Button btnRefresh;
    
    @FXML private TableView<ApprovalStatus> tableApprovals;
    @FXML private TableColumn<ApprovalStatus, String> colDepartment;
    @FXML private TableColumn<ApprovalStatus, String> colOfficer;
    @FXML private TableColumn<ApprovalStatus, String> colStatus;
    @FXML private TableColumn<ApprovalStatus, String> colRemarks;
    @FXML private TableColumn<ApprovalStatus, String> colDate;

    private User currentUser;
    private int currentRequestId = -1;
    private ObservableList<ApprovalStatus> approvalData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupTableStyle();
    }
    
    
    
    
    @FXML
    private void generatePDFCertificate() {
        if (currentRequestId == -1) {
            showAlert("No Clearance", "You don't have a completed clearance request to generate a certificate.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if clearance is fully approved
            String checkSql = "SELECT status FROM clearance_requests WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, currentRequestId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                
                if (!"FULLY_CLEARED".equals(status)) {
                    showAlert("Not Ready", "Your clearance is not fully approved yet. Current status: " + status);
                    return;
                }
                
                // Generate PDF certificate
                PDFCertificateService pdfService = new PDFCertificateService();
                String filePath = pdfService.generatePDFCertificate(currentUser.getId(), currentRequestId);
                
                if (filePath != null) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("PDF Certificate Generated");
                    success.setHeaderText("🎉 PDF Clearance Certificate Ready!");
                    success.setContentText("Your professional PDF clearance certificate has been generated successfully!\n\n" +
                                        "Saved to: " + filePath + "\n\n" +
                                        "The PDF certificate has been saved to your Downloads folder.");
                    success.showAndWait();
                    
                    // Open the PDF file
                    try {
                        java.awt.Desktop.getDesktop().open(new File(filePath));
                    } catch (Exception e) {
                        System.out.println("Could not open PDF automatically");
                    }
                } else {
                    showAlert("Error", "Failed to generate PDF certificate. Please try again.");
                }
            }
            
        } catch (Exception e) {
            showAlert("Error", "PDF certificate generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    
    
    
    @FXML
    private void generateCertificate() {
        if (currentRequestId == -1) {
            showAlert("No Clearance", "You don't have a completed clearance request to generate a certificate.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if clearance is fully approved
            String checkSql = "SELECT status FROM clearance_requests WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, currentRequestId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                
                if (!"FULLY_CLEARED".equals(status)) {
                    showAlert("Not Ready", "Your clearance is not fully approved yet. Current status: " + status);
                    return;
                }
                
                // Ask user for format choice - NOW INCLUDES PDF
                ChoiceDialog<String> formatDialog = new ChoiceDialog<>("PDF", "PDF", "HTML", "Text");
                formatDialog.setTitle("Certificate Format");
                formatDialog.setHeaderText("Choose Certificate Format");
                formatDialog.setContentText("Select your preferred certificate format:");
                
                Optional<String> formatResult = formatDialog.showAndWait();
                if (formatResult.isPresent()) {
                    String format = formatResult.get();
                    String filePath;
                    
                    if ("PDF".equals(format)) {
                        // Generate PDF certificate
                        PDFCertificateService pdfService = new PDFCertificateService();
                        filePath = pdfService.generatePDFCertificate(currentUser.getId(), currentRequestId);
                    } else {
                        SimpleCertificateService certificateService = new SimpleCertificateService();
                        if ("HTML".equals(format)) {
                            filePath = certificateService.generateHTMLCertificate(currentUser.getId(), currentRequestId);
                        } else {
                            filePath = certificateService.generateClearanceCertificate(currentUser.getId(), currentRequestId);
                        }
                    }
                    
                    if (filePath != null) {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Certificate Generated");
                        success.setHeaderText("🎉 Clearance Certificate Ready!");
                        success.setContentText("Your clearance certificate has been generated successfully!\n\n" +
                                            "Format: " + format + "\n" +
                                            "Saved to: " + filePath + "\n\n" +
                                            "The certificate has been saved to your Downloads folder.");
                        success.showAndWait();
                        
                        // Open the file
                        try {
                            java.awt.Desktop.getDesktop().open(new File(filePath));
                        } catch (Exception e) {
                            System.out.println("Could not open file automatically");
                        }
                    } else {
                        showAlert("Error", "Failed to generate " + format + " certificate. Please try again.");
                    }
                }
            }
            
        } catch (Exception e) {
            showAlert("Error", "Certificate generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " (" + user.getUsername() + ")");
        loadClearanceStatus();
    }

    private void setupTableColumns() {
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officer"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("approvalDate"));
        
        // Custom status column with colors
        colStatus.setCellFactory(column -> new TableCell<ApprovalStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "✅ APPROVED":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "❌ REJECTED":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "⏳ PENDING":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });
    }

    private void setupTableStyle() {
        tableApprovals.setStyle("-fx-font-size: 14px;");
    }

    @FXML
    private void submitClearanceRequest() {
        // Check if already has active request
        if (hasActiveRequest()) {
            showAlert("Clearance Request", "You already have an active clearance request in progress!");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Submit Clearance Request");
        confirmation.setHeaderText("Start Clearance Process");
        confirmation.setContentText("Are you sure you want to submit a clearance request?\n\nThis will initiate the approval process with all departments.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                createClearanceRequest();
            }
        });
    }

    private boolean hasActiveRequest() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM clearance_requests WHERE student_id = ? AND status NOT IN ('FULLY_CLEARED', 'REJECTED', 'EXPIRED')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createClearanceRequest() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Create clearance request
                String requestSql = "INSERT INTO clearance_requests (student_id, request_date, status) VALUES (?, NOW(), 'IN_PROGRESS')";
                PreparedStatement requestStmt = conn.prepareStatement(requestSql, Statement.RETURN_GENERATED_KEYS);
                requestStmt.setInt(1, currentUser.getId());
                requestStmt.executeUpdate();
                
                // Get the generated request ID
                ResultSet rs = requestStmt.getGeneratedKeys();
                if (rs.next()) {
                    currentRequestId = rs.getInt(1);
                }
                
                // Create approval entries for all departments
                String[] departments = {
                    "LIBRARIAN", "CAFETERIA", "DORMITORY", 
                    "ASSOCIATION", "REGISTRAR", "DEPARTMENT_HEAD"
                };
                
                String approvalSql = "INSERT INTO clearance_approvals (request_id, officer_role, status) VALUES (?, ?, 'PENDING')";
                PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
                
                for (String department : departments) {
                    approvalStmt.setInt(1, currentRequestId);
                    approvalStmt.setString(2, department);
                    approvalStmt.addBatch();
                }
                
                approvalStmt.executeBatch();
                conn.commit();
                
                showSuccessMessage("Clearance request submitted successfully! 🎉\nYour request is now being processed by all departments.");
                loadClearanceStatus();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showErrorMessage("Failed to submit clearance request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshStatus() {
        loadClearanceStatus();
        showInfoMessage("Status refreshed successfully!");
    }

    private void loadClearanceStatus() {
        approvalData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if student has any clearance request
            String requestSql = "SELECT id, status, request_date FROM clearance_requests WHERE student_id = ? ORDER BY request_date DESC LIMIT 1";
            PreparedStatement requestStmt = conn.prepareStatement(requestSql);
            requestStmt.setInt(1, currentUser.getId());
            ResultSet requestRs = requestStmt.executeQuery();
            
            if (!requestRs.next()) {
                // No clearance request found
                showInfoMessage("No clearance request found. Click 'Submit Clearance Request' to start the process.");
                btnSubmitRequest.setDisable(false);
                tableApprovals.setItems(FXCollections.observableArrayList());
                return;
            }
            
            currentRequestId = requestRs.getInt("id");
            String overallStatus = requestRs.getString("status");
            Date requestDate = requestRs.getDate("request_date");
            
            // Update UI based on overall status
            updateOverallStatusUI(overallStatus, requestDate);
            
            // Load approval details - FIXED QUERY (removed approval_date)
            String approvalSql = """
                SELECT 
                    ca.officer_role,
                    u.full_name as officer_name,
                    ca.status,
                    ca.remarks,
                    CASE 
                        WHEN ca.status = 'APPROVED' THEN '✅ Approved'
                        WHEN ca.status = 'REJECTED' THEN '❌ Rejected'
                        ELSE '⏳ Pending'
                    END as display_status
                FROM clearance_approvals ca
                LEFT JOIN users u ON ca.officer_id = u.id
                WHERE ca.request_id = ?
                ORDER BY FIELD(ca.officer_role, 'LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'ASSOCIATION', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
                
            PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
            approvalStmt.setInt(1, currentRequestId);
            ResultSet approvalRs = approvalStmt.executeQuery();
            
            while (approvalRs.next()) {
                String officerName = approvalRs.getString("officer_name");
                String status = approvalRs.getString("display_status");
                String remarks = approvalRs.getString("remarks");
                
                // Handle null values gracefully
                if (officerName == null) officerName = "Not Assigned Yet";
                if (remarks == null) remarks = "No remarks yet";
                
                ApprovalStatus approvalStatus = new ApprovalStatus(
                    formatDepartmentName(approvalRs.getString("officer_role")),
                    officerName,
                    status,
                    remarks,
                    getApprovalDateText(approvalRs.getString("status"))
                );
                approvalData.add(approvalStatus);
            }
            
            tableApprovals.setItems(approvalData);
            
        } catch (Exception e) {
            showErrorMessage("Error loading clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getApprovalDateText(String status) {
        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
            return "Today"; // You can modify this to show actual date if available
        }
        return "Not approved yet";
    }

    private void updateOverallStatusUI(String overallStatus, Date requestDate) {
        String statusText = "";
        String style = "";
        
        switch (overallStatus) {
            case "FULLY_CLEARED":
                statusText = "🎉 CONGRATULATIONS! Your clearance is fully approved!";
                style = "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;";
                btnSubmitRequest.setDisable(true);
                break;
            case "REJECTED":
                statusText = "❌ Your clearance request was rejected.";
                style = "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;";
                btnSubmitRequest.setDisable(false);
                break;
            case "IN_PROGRESS":
                statusText = "⏳ Clearance in progress... (Submitted: " + requestDate + ")";
                style = "-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 16px;";
                btnSubmitRequest.setDisable(true);
                break;
            case "PENDING":
                statusText = "📋 Clearance request submitted. Waiting for processing.";
                style = "-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 16px;";
                btnSubmitRequest.setDisable(true);
                break;
            default:
                statusText = "❓ Unknown status";
                style = "-fx-text-fill: #7f8c8d; -fx-font-size: 16px;";
        }
        
        lblStatusMessage.setText(statusText);
        lblStatusMessage.setStyle(style);
    }

    private String formatDepartmentName(String role) {
        return switch (role) {
            case "LIBRARIAN" -> "📚 Library";
            case "CAFETERIA" -> "🍽️ Cafeteria";
            case "DORMITORY" -> "🏠 Dormitory";
            case "ASSOCIATION" -> "👥 Student Association";
            case "REGISTRAR" -> "📄 Registrar Office";
            case "DEPARTMENT_HEAD" -> "🎓 Department Head";
            default -> role;
        };
    }

    private void showSuccessMessage(String message) {
        lblStatusMessage.setText(message);
        lblStatusMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void showErrorMessage(String message) {
        lblStatusMessage.setText(message);
        lblStatusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void showInfoMessage(String message) {
        lblStatusMessage.setText(message);
        lblStatusMessage.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for approval status table
    public static class ApprovalStatus {
        private final String department;
        private final String officer;
        private final String status;
        private final String remarks;
        private final String approvalDate;

        public ApprovalStatus(String department, String officer, String status, String remarks, String approvalDate) {
            this.department = department;
            this.officer = officer;
            this.status = status;
            this.remarks = remarks;
            this.approvalDate = approvalDate;
        }

        public String getDepartment() { return department; }
        public String getOfficer() { return officer; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
        public String getApprovalDate() { return approvalDate; }
    }
}