package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.service.PDFCertificateService;
import com.university.clearance.service.SimpleCertificateService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StudentDashboardController implements Initializable {

    @FXML private Label lblStatusMessage;
    @FXML private Button btnSubmitRequest;
    @FXML private Button btnGenerateCertificate;
    
    @FXML private TableView<ApprovalStatus> tableApprovals;
    @FXML private TableColumn<ApprovalStatus, String> colDepartment;
    @FXML private TableColumn<ApprovalStatus, String> colStatus;
    @FXML private TableColumn<ApprovalStatus, String> colRemarks;
    @FXML private TableColumn<ApprovalStatus, String> colOfficer;
    @FXML private TableColumn<ApprovalStatus, String> colDate;
    
    @FXML private Label lblProfileStatus;
    @FXML private Button btnReapplyInline;
    @FXML private Button btnSubmitRequestInline;
    @FXML private Button btnProfileSubmitRequest;
    @FXML private Label lblQuickStatus;
    
    // Profile display labels
    @FXML private Label lblProfileId;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileDepartment;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblProfileYearLevel;
    @FXML private Label lblProfilePhone;
    @FXML private Label lblClearanceStatus;
    
    @FXML private Label lblStatusIcon;
    @FXML private Button btnProfileReapply;
    
    // Statistics labels
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblTotalApprovals;
    @FXML private Label lblApprovedCount;
    @FXML private Label lblPendingCount;
    
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgressText;
    @FXML private Label lblRejectedCount;
    @FXML private Label lblProgressPercentage;
    @FXML private Button btnReapplyClearance;
    
    private boolean canReapply = false;
    private User currentUser;
    private int currentRequestId = -1;
    private ObservableList<ApprovalStatus> approvalData = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;
    private boolean autoRefreshEnabled = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        btnGenerateCertificate.setDisable(true);
        
        // Setup tooltip for download button
        Tooltip tooltip = new Tooltip("This button is inactive until all departments approve your request");
        tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: normal;");
        btnGenerateCertificate.setTooltip(tooltip);
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadClearanceStatus();
        loadProfileInformation();
        
        // Update user info in sidebar
        if (lblUserName != null && user != null) {
            lblUserName.setText(user.getFullName());
        }
        if (lblUserRole != null && user != null) {
            lblUserRole.setText("Student - " + user.getDepartment());
        }
        
        // Initialize UI states
        updateButtonStates();
    }
    
    private void setupTableColumns() {
        // Link table columns to ApprovalStatus properties
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officer"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Custom cell factory for status column to add icons
        colStatus.setCellFactory(column -> new TableCell<ApprovalStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("✅")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.contains("❌")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.contains("⏳")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Custom cell factory for department column to add icons
        colDepartment.setCellFactory(column -> new TableCell<ApprovalStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Add icons to department names
                    if (item.contains("Library")) {
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.contains("Cafeteria")) {
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.contains("Dormitory")) {
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.contains("Registrar")) {
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.contains("Department")) {
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    private void submitClearanceRequest() {
        if (hasActiveRequest()) {
            showAlert("Active Request", "You already have an active clearance request. Please wait for approvals or address rejections.");
            return;
        }

        if (hasRejectedRequestAndCannotReapply()) {
            showAlert("Request Rejected", 
                "Your previous clearance request was rejected.\n" +
                "Please contact the administrator to request permission to reapply.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Submit Request");
        confirmation.setHeaderText("Start Clearance Process");
        confirmation.setContentText("Submit clearance request to all departments?\n\n" +
                                  "This will send your request to:\n" +
                                  "• Library\n• Cafeteria\n• Dormitory\n• Registrar\n• Department Head");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                createClearanceRequest();
            }
        });
    }

    private boolean hasActiveRequest() {
        if (currentUser == null) return false;
        
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

    private boolean hasRejectedRequestAndCannotReapply() {
        if (currentUser == null) return false;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cr.status, cr.can_reapply 
                FROM clearance_requests cr 
                WHERE cr.student_id = ? 
                AND cr.status = 'REJECTED' 
                AND cr.can_reapply = FALSE
                ORDER BY cr.request_date DESC 
                LIMIT 1
                """;
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
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Update can_reapply to false for new request
            String updateSql = "UPDATE clearance_requests SET can_reapply = FALSE WHERE student_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, currentUser.getId());
            updateStmt.executeUpdate();
            
            // 1. Create the clearance request
            String requestSql = "INSERT INTO clearance_requests (student_id, request_date, status, can_reapply) VALUES (?, NOW(), 'IN_PROGRESS', FALSE)";
            PreparedStatement requestStmt = conn.prepareStatement(requestSql, Statement.RETURN_GENERATED_KEYS);
            requestStmt.setInt(1, currentUser.getId());
            requestStmt.executeUpdate();
            
            ResultSet rs = requestStmt.getGeneratedKeys();
            if (rs.next()) {
                currentRequestId = rs.getInt(1);
            }
            
            // 2. Create approvals for all required departments
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
    
    @FXML
    private void refreshStatus() {
        loadClearanceStatus();
        showMessage("Status refreshed.", "info");
    }
    
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
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("PDF", "PDF", "HTML", "TEXT");
        dialog.setTitle("Choose Certificate Format");
        dialog.setHeaderText("Select Certificate Format");
        dialog.setContentText("Choose your preferred format:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(format -> {
            try {
                showMessage("Generating " + format + " certificate...", "info");
                
                String filePath = null;
                
                switch (format) {
                    case "PDF":
                        PDFCertificateService pdfService = new PDFCertificateService();
                        filePath = pdfService.generatePDFCertificate(currentUser.getId(), currentRequestId);
                        break;
                    case "HTML":
                        SimpleCertificateService htmlService = new SimpleCertificateService();
                        filePath = htmlService.generateHTMLCertificate(currentUser.getId(), currentRequestId);
                        break;
                    case "TEXT":
                        SimpleCertificateService textService = new SimpleCertificateService();
                        filePath = textService.generateClearanceCertificate(currentUser.getId(), currentRequestId);
                        break;
                }
                
                if (filePath != null && new File(filePath).exists()) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Certificate Ready");
                    success.setHeaderText(format + " Certificate Generated!");
                    success.setContentText("Certificate saved to:\n" + filePath);
                    success.showAndWait();
                } else {
                    showMessage("Failed to generate " + format + " certificate.", "error");
                }
                
            } catch (Exception e) {
                showMessage("Error generating certificate: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        });
    }

    private boolean isClearanceFullyApproved() {
        if (currentRequestId == -1) return false;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
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
                
                return total == 5 && approved == 5 && rejected == 0;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private void loadClearanceStatus() {
        if (currentUser == null) return;
        
        approvalData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get latest request
            String requestSql = """
                SELECT id, status, can_reapply 
                FROM clearance_requests 
                WHERE student_id = ? 
                ORDER BY request_date DESC 
                LIMIT 1
                """;
            PreparedStatement requestStmt = conn.prepareStatement(requestSql);
            requestStmt.setInt(1, currentUser.getId());
            ResultSet requestRs = requestStmt.executeQuery();
            
            if (!requestRs.next()) {
                showMessage("No clearance request found. Click 'Submit Request' to start.", "info");
                btnSubmitRequest.setDisable(false);
                btnGenerateCertificate.setDisable(true);
                lblClearanceStatus.setText("Status: No Request");
                return;
            }
            
            currentRequestId = requestRs.getInt("id");
            String overallStatus = requestRs.getString("status");
            boolean canReapply = requestRs.getBoolean("can_reapply");
            
            // Update clearance status label
            String statusText = "Status: " + formatStatus(overallStatus);
            if ("REJECTED".equals(overallStatus) && !canReapply) {
                statusText += " (Cannot Reapply)";
            } else if ("REJECTED".equals(overallStatus) && canReapply) {
                statusText += " (Can Reapply)";
            }
            lblClearanceStatus.setText(statusText);
            
            // Check if fully approved
            boolean isFullyApproved = isClearanceFullyApproved();
            btnGenerateCertificate.setDisable(!isFullyApproved);
            
            // Update certificate button tooltip
            if (isFullyApproved) {
                btnGenerateCertificate.setTooltip(new Tooltip("Click to download clearance certificate"));
                showMessage("Your clearance is fully approved! You can now download your certificate.", "success");
            } else {
                btnGenerateCertificate.setTooltip(new Tooltip("This button is inactive until all departments approve your request"));
                showMessage("Clearance in progress...", "info");
            }
            
            // Load approval details for all 5 departments
            String approvalSql = """
                SELECT 
                    ca.officer_role,
                    ca.status as raw_status,
                    ca.remarks,
                    ca.approval_date,
                    u.full_name as officer_name,
                    u.department as officer_department
                FROM clearance_approvals ca
                LEFT JOIN users u ON ca.officer_id = u.id
                WHERE ca.request_id = ?
                AND ca.officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                ORDER BY FIELD(ca.officer_role, 'LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
                
            PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
            approvalStmt.setInt(1, currentRequestId);
            ResultSet approvalRs = approvalStmt.executeQuery();
            
            int approvedCount = 0;
            int pendingCount = 0;
            int rejectedCount = 0;
            
            // Create default department list if no records found
            if (!approvalRs.isBeforeFirst()) {
                // No approval records found, create default pending entries
                String[] departments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
                for (String dept : departments) {
                    String department = formatDepartmentName(dept);
                    String displayStatus = "⏳ Pending";
                    String officer = "Not Assigned";
                    String date = "";
                    String remarks = "Waiting for review...";
                    
                    approvalData.add(new ApprovalStatus(department, displayStatus, officer, date, remarks));
                    pendingCount++;
                }
            } else {
                // Process existing approval records
                while (approvalRs.next()) {
                    String officerRole = approvalRs.getString("officer_role");
                    String rawStatus = approvalRs.getString("raw_status");
                    String remarks = approvalRs.getString("remarks");
                    String officerName = approvalRs.getString("officer_name");
                    Timestamp approvalDate = approvalRs.getTimestamp("approval_date");
                    
                    // Format display values
                    String displayStatus = formatStatus(rawStatus);
                    String department = formatDepartmentName(officerRole);
                    String officer = (officerName != null && !officerName.isEmpty()) ? officerName : "Not Assigned";
                    String date = (approvalDate != null) ? 
                        new SimpleDateFormat("MMM dd, HH:mm").format(approvalDate) : "";
                    
                    // Build detailed remarks
                    String detailedRemarks = (remarks != null && !remarks.trim().isEmpty()) ? 
                        remarks : "Waiting for review...";
                    
                    // Count statuses
                    switch (rawStatus) {
                        case "APPROVED":
                            approvedCount++;
                            break;
                        case "REJECTED":
                            rejectedCount++;
                            break;
                        default:
                            pendingCount++;
                    }
                    
                    // Add to table data
                    ApprovalStatus status = new ApprovalStatus(
                        department, displayStatus, officer, date, detailedRemarks
                    );
                    approvalData.add(status);
                }
            }
            
            // Update statistics labels
            updateStatistics(5, approvedCount, pendingCount, rejectedCount);
            
            // Update table with data
            tableApprovals.setItems(approvalData);
            
            // Update button states based on status
            updateButtonStates();
            
        } catch (Exception e) {
            showMessage("Error loading status: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    private void updateButtonStates() {
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if student can reapply
            String checkSql = """
                SELECT 
                    CASE 
                        WHEN cr.status = 'REJECTED' AND cr.can_reapply = TRUE THEN TRUE
                        ELSE FALSE
                    END as can_reapply_now,
                    (SELECT COUNT(*) FROM clearance_requests cr2 
                     WHERE cr2.student_id = ? 
                     AND cr2.status IN ('IN_PROGRESS', 'PENDING')) as active_requests
                FROM clearance_requests cr
                WHERE cr.student_id = ?
                ORDER BY cr.request_date DESC 
                LIMIT 1
                """;
            
            PreparedStatement stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, currentUser.getId());
            stmt.setInt(2, currentUser.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                boolean canReapplyNow = rs.getBoolean("can_reapply_now");
                int activeRequests = rs.getInt("active_requests");
                
                // Update reapply button
                if (btnReapplyClearance != null) {
                    boolean showReapply = canReapplyNow && activeRequests == 0;
                    btnReapplyClearance.setVisible(showReapply);
                    btnReapplyClearance.setDisable(!showReapply);
                }
                
                if (btnReapplyInline != null) {
                    boolean showReapply = canReapplyNow && activeRequests == 0;
                    btnReapplyInline.setVisible(showReapply);
                    btnReapplyInline.setDisable(!showReapply);
                }
                
                if (btnProfileReapply != null) {
                    boolean showReapply = canReapplyNow && activeRequests == 0;
                    btnProfileReapply.setVisible(showReapply);
                    btnProfileReapply.setDisable(!showReapply);
                }
                
                // Update submit button
                boolean hasActive = activeRequests > 0;
                if (btnSubmitRequest != null) {
                    btnSubmitRequest.setDisable(hasActive || canReapplyNow);
                }
                
                if (btnSubmitRequestInline != null) {
                    btnSubmitRequestInline.setVisible(!hasActive && !canReapplyNow);
                    btnSubmitRequestInline.setDisable(hasActive || canReapplyNow);
                }
                
                if (btnProfileSubmitRequest != null) {
                    btnProfileSubmitRequest.setVisible(!hasActive && !canReapplyNow);
                    btnProfileSubmitRequest.setDisable(hasActive || canReapplyNow);
                }
                
                // Update quick status
                if (lblQuickStatus != null) {
                    String quickStatus = getQuickStatus();
                    lblQuickStatus.setText(quickStatus);
                }
                
                // Update profile status
                if (lblProfileStatus != null) {
                    lblProfileStatus.setText(getQuickStatus());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getQuickStatus() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT status 
                FROM clearance_requests 
                WHERE student_id = ? 
                ORDER BY request_date DESC 
                LIMIT 1
                """;
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentUser.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                return formatStatus(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "No Request";
    }
    
    private void updateStatistics(int total, int approved, int pending, int rejected) {
        Platform.runLater(() -> {
            if (lblTotalApprovals != null) lblTotalApprovals.setText(String.valueOf(total));
            if (lblApprovedCount != null) {
                lblApprovedCount.setText(String.valueOf(approved));
                // Update color based on count
                if (approved == total) {
                    lblApprovedCount.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
            if (lblPendingCount != null) {
                lblPendingCount.setText(String.valueOf(pending));
                if (pending > 0) {
                    lblPendingCount.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                }
            }
            if (lblRejectedCount != null) {
                lblRejectedCount.setText(String.valueOf(rejected));
                if (rejected > 0) {
                    lblRejectedCount.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
    }

    private String formatStatus(String status) {
        switch (status) {
            case "FULLY_CLEARED": return "✅ Fully Cleared";
            case "APPROVED": return "✅ Approved";
            case "REJECTED": return "❌ Rejected";
            case "IN_PROGRESS": return "🔄 In Progress";
            case "PENDING": return "⏳ Pending";
            default: return "❓ " + status;
        }
    }

    private String formatDepartmentName(String role) {
        switch (role) {
            case "LIBRARIAN": return "📚 Library";
            case "CAFETERIA": return "🍽️ Cafeteria";
            case "DORMITORY": return "🏠 Dormitory";
            case "REGISTRAR": return "📋 Registrar";
            case "DEPARTMENT_HEAD": return "👨‍🏫 Department Head";
            default: return role;
        }
    }

    private void loadProfileInformation() {
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Platform.runLater(() -> {
                    try {
						lblProfileId.setText(rs.getString("username"));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						lblProfileName.setText(rs.getString("full_name"));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						lblProfileDepartment.setText(rs.getString("department"));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						lblProfileEmail.setText(rs.getString("email"));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						lblProfileYearLevel.setText(rs.getString("year_level") != null ? rs.getString("year_level") : "Not set");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						lblProfilePhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "Not provided");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                });
            }
        } catch (Exception e) {
            showMessage("Error loading profile: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void showMessage(String message, String type) {
        Platform.runLater(() -> {
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
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    private void changePassword() {
        Dialog<PasswordPair> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter your new password");

        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

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
            
            if (!verifyCurrentPassword(oldPass)) {
                showAlert("Error", "Current password is incorrect");
                return;
            }
            
            updatePassword(newPass);
        });
    }

    private boolean verifyCurrentPassword(String oldPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT password FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword.equals(oldPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updatePassword(String newPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
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

    @FXML
    private void viewProfile() {
        loadProfileInformation();
    }
    
    @FXML
    private void handleReapplyClearance() {
        if (currentUser == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reapply for Clearance");
        confirm.setHeaderText("Submit New Clearance Request");
        confirm.setContentText("Are you sure you want to submit a new clearance request?\n\n" +
                             "Your previous request was rejected.\n" +
                             "This will create a new request with status: IN PROGRESS");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                submitReapplication();
            }
        });
    }

    private void submitReapplication() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Check if student can reapply (double-check)
                String checkSql = """
                    SELECT can_reapply 
                    FROM clearance_requests 
                    WHERE student_id = ? 
                    AND status = 'REJECTED' 
                    AND can_reapply = TRUE
                    ORDER BY id DESC 
                    LIMIT 1
                    """;
                
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, currentUser.getId());
                ResultSet rs = checkStmt.executeQuery();
                
                if (!rs.next() || !rs.getBoolean("can_reapply")) {
                    showMessage("Cannot Reapply", 
                        "You cannot reapply at this time.\n" +
                        "Please contact the administrator to allow reapplication.");
                    conn.rollback();
                    return;
                }
                
                // Update can_reapply to false for the old rejected request
                String updateOldRequestSql = """
                    UPDATE clearance_requests 
                    SET can_reapply = FALSE 
                    WHERE student_id = ? 
                    AND status = 'REJECTED' 
                    AND can_reapply = TRUE
                    """;
                
                PreparedStatement updateOldStmt = conn.prepareStatement(updateOldRequestSql);
                updateOldStmt.setInt(1, currentUser.getId());
                updateOldStmt.executeUpdate();
                
                // Create new clearance request with IN_PROGRESS status
                String requestSql = """
                    INSERT INTO clearance_requests (student_id, request_date, status, can_reapply)
                    VALUES (?, NOW(), 'IN_PROGRESS', FALSE)
                    """;
                
                PreparedStatement requestStmt = conn.prepareStatement(requestSql, Statement.RETURN_GENERATED_KEYS);
                requestStmt.setInt(1, currentUser.getId());
                requestStmt.executeUpdate();
                
                ResultSet generatedKeys = requestStmt.getGeneratedKeys();
                int requestId = -1;
                if (generatedKeys.next()) {
                    requestId = generatedKeys.getInt(1);
                }
                
                // Create pending approvals for each department
                String[] departments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
                String approvalSql = """
                    INSERT INTO clearance_approvals (request_id, officer_role, status)
                    VALUES (?, ?, 'PENDING')
                    """;
                
                PreparedStatement approvalStmt = conn.prepareStatement(approvalSql);
                
                for (String dept : departments) {
                    approvalStmt.setInt(1, requestId);
                    approvalStmt.setString(2, dept);
                    approvalStmt.addBatch();
                }
                
                approvalStmt.executeBatch();
                
                conn.commit();
                
                // Update UI state
                updateReapplyButtonState();
                
                // Show success message
                showMessage("Reapplication submitted successfully! Status: 🔄 IN PROGRESS", "success");
                
                // Refresh data to show new status
                loadClearanceStatus();
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showMessage("Failed to submit reapplication: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void updateReapplyButtonState() {
        if (currentUser == null || btnReapplyClearance == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = """
                SELECT 
                    CASE 
                        WHEN cr.status = 'REJECTED' AND cr.can_reapply = TRUE THEN TRUE
                        ELSE FALSE
                    END as can_reapply_now,
                    (SELECT COUNT(*) FROM clearance_requests cr2 
                     WHERE cr2.student_id = ? 
                     AND cr2.status IN ('IN_PROGRESS', 'PENDING')) as active_requests
                FROM clearance_requests cr
                WHERE cr.student_id = ?
                ORDER BY cr.request_date DESC 
                LIMIT 1
                """;
            
            PreparedStatement stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, currentUser.getId());
            stmt.setInt(2, currentUser.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                boolean canReapplyNow = rs.getBoolean("can_reapply_now");
                int activeRequests = rs.getInt("active_requests");
                
                // Can reapply if: previous request was rejected AND can_reapply=true AND no active requests
                canReapply = canReapplyNow && activeRequests == 0;
                
                // Update button state
                btnReapplyClearance.setDisable(!canReapply);
                
                // Update button text and style based on state
                if (canReapply) {
                    btnReapplyClearance.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
                    btnReapplyClearance.setTooltip(new Tooltip("Click to submit a new clearance request"));
                } else {
                    btnReapplyClearance.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                    if (activeRequests > 0) {
                        btnReapplyClearance.setTooltip(new Tooltip("You already have an active clearance request"));
                    } else {
                        btnReapplyClearance.setTooltip(new Tooltip("Reapplication not allowed at this time"));
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Stage currentStage = (Stage) lblStatusMessage.getScene().getWindow();
                Scene currentScene = currentStage.getScene();
                
                URL loginFxmlUrl = getClass().getResource("/com/university/clearance/resources/views/Login.fxml");
                
                if (loginFxmlUrl != null) {
                    Parent root = FXMLLoader.load(loginFxmlUrl);
                    Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
                    currentStage.setScene(newScene);
                    currentStage.setTitle("University Clearance System - Login");
                    currentStage.centerOnScreen();
                } else {
                    showAlert("Error", "Login screen not found. Closing application.");
                    currentStage.close();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage());
                Stage stage = (Stage) lblStatusMessage.getScene().getWindow();
                stage.close();
            }
        }
    }

    // Helper classes
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

    public static class ApprovalStatus {
        private final String department;
        private final String status;
        private final String remarks;
        private final String officer;
        private final String date;

        public ApprovalStatus(String department, String status, String officer, String date, String remarks) {
            this.department = department;
            this.status = status;
            this.remarks = remarks;
            this.officer = officer;
            this.date = date;
        }

        public String getDepartment() { return department; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
        public String getOfficer() { return officer; }
        public String getDate() { return date; }
    }
}