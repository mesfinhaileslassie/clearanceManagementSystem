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
import java.util.*;

public class StudentDashboardController implements Initializable {

    // Status and main controls
    @FXML private Label lblStatusMessage;
    @FXML private Button btnSubmitRequest;
    @FXML private Button btnGenerateCertificate;
    @FXML private Button btnSubmitRequestInline;
    @FXML private Button btnReapplyInline;
    @FXML private Button btnProfileSubmitRequest;
    @FXML private Button btnProfileReapply;
    @FXML private Button btnReapplyClearance;
    
    // Table controls
    @FXML private TableView<ApprovalStatus> tableApprovals;
    @FXML private TableColumn<ApprovalStatus, String> colDepartment;
    @FXML private TableColumn<ApprovalStatus, String> colStatus;
    @FXML private TableColumn<ApprovalStatus, String> colRemarks;
    @FXML private TableColumn<ApprovalStatus, String> colOfficer;
    @FXML private TableColumn<ApprovalStatus, String> colDate;
    
    // Profile labels
    @FXML private Label lblProfileId;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileDepartment;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblProfileYearLevel;
    @FXML private Label lblProfilePhone;
    @FXML private Label lblClearanceStatus;
    @FXML private Label lblClearanceStatusProfile;
    
    // User info labels
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblUserNameSidebar;
    @FXML private Label lblUserRoleSidebar;
    
    // Statistics labels
    @FXML private Label lblTotalApprovals;
    @FXML private Label lblApprovedCount;
    @FXML private Label lblPendingCount;
    @FXML private Label lblRejectedCount;
    
    // Progress bar components
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgressText;
    @FXML private Label lblProgressPercentage;
    
    @FXML private HBox statusBanner;
    
    private User currentUser;
    private int currentRequestId = -1;
    private ObservableList<ApprovalStatus> approvalData = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;
    private boolean autoRefreshEnabled = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        
        if (btnGenerateCertificate != null) {
            btnGenerateCertificate.setDisable(true);
        }
        
        if (statusBanner != null) {
            statusBanner.setVisible(false);
        }
        
        // Initialize progress bar
        if (progressBar != null) {
            progressBar.setProgress(0.0);
        }
        if (lblProgressPercentage != null) {
            lblProgressPercentage.setText("0%");
        }
        if (lblProgressText != null) {
            lblProgressText.setText("No approvals yet");
        }
        
        // Initialize auto-refresh (every 30 seconds)
        initializeAutoRefresh();
    }
    
    private void initializeAutoRefresh() {
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(30), event -> {
                if (autoRefreshEnabled && currentUser != null) {
                    Platform.runLater(() -> {
                        loadClearanceStatus();
                        updateProfileFromDatabase();
                    });
                }
            })
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("DEBUG: setCurrentUser called with: " + (user != null ? user.getFullName() : "null"));
        updateUserDisplay();
        loadClearanceStatus();
        loadProfileInformation();
        autoRefreshEnabled = true;
    }
    
    private void updateUserDisplay() {
        if (currentUser == null) return;
        
        Platform.runLater(() -> {
            System.out.println("DEBUG: Updating user display for: " + currentUser.getFullName());
            
            if (lblUserName != null) {
                lblUserName.setText(currentUser.getFullName());
            }
            if (lblUserRole != null) {
                lblUserRole.setText("Student - " + currentUser.getDepartment());
            }
            if (lblUserNameSidebar != null) {
                lblUserNameSidebar.setText(currentUser.getFullName());
            }
            if (lblUserRoleSidebar != null) {
                lblUserRoleSidebar.setText("Student - " + currentUser.getDepartment());
            }
            
            // Also set profile name initially
            if (lblProfileName != null) {
                lblProfileName.setText(currentUser.getFullName());
            }
            if (lblProfileDepartment != null) {
                lblProfileDepartment.setText(currentUser.getDepartment());
            }
        });
    }
    
    private void setupTableColumns() {
        if (colDepartment == null || colStatus == null || colOfficer == null || colDate == null || colRemarks == null) {
            System.out.println("DEBUG: Table columns not initialized properly");
            return;
        }
        
        // Link table columns to ApprovalStatus properties
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officer"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Custom cell factory for status column to add colors and icons
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
        
        System.out.println("DEBUG: Table columns setup complete");
    }

    @FXML
    private void submitClearanceRequest() {
        System.out.println("DEBUG: submitClearanceRequest called");
        if (currentUser == null) {
            showMessage("Please log in again.", "error");
            return;
        }

        if (hasActiveRequest()) {
            showAlert("Active Request", "You already have an active clearance request. Please wait for approvals or address rejections.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Submit Clearance Request");
        confirmation.setHeaderText("Start Clearance Process");
        confirmation.setContentText("Submit clearance request to all departments?\n\n" +
                                  "This will send your request to:\n" +
                                  "• Library\n• Cafeteria\n• Dormitory\n• Registrar\n• Department Head");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            createClearanceRequest();
        }
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

    private void createClearanceRequest() {
        if (currentUser == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Update can_reapply to false for any previous requests
            String updateSql = "UPDATE clearance_requests SET can_reapply = FALSE WHERE student_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, currentUser.getId());
            updateStmt.executeUpdate();
            
            // Create the clearance request
            String requestSql = "INSERT INTO clearance_requests (student_id, request_date, status, can_reapply) VALUES (?, NOW(), 'IN_PROGRESS', FALSE)";
            PreparedStatement requestStmt = conn.prepareStatement(requestSql, Statement.RETURN_GENERATED_KEYS);
            requestStmt.setInt(1, currentUser.getId());
            requestStmt.executeUpdate();
            
            ResultSet rs = requestStmt.getGeneratedKeys();
            if (rs.next()) {
                currentRequestId = rs.getInt(1);
            }
            
            // Create approvals for all required departments
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
        System.out.println("DEBUG: refreshStatus called");
        loadClearanceStatus();
        showMessage("Status refreshed.", "info");
    }
    
    @FXML
    private void refreshProfile() {
        System.out.println("DEBUG: refreshProfile called");
        loadProfileInformation();
        showMessage("Profile information refreshed.", "info");
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
        System.out.println("DEBUG: loadClearanceStatus called for user: " + (currentUser != null ? currentUser.getFullName() : "null"));
        if (currentUser == null) return;
        
        if (approvalData != null) {
            approvalData.clear();
        }
        
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
            
            String overallStatus = "NO_REQUEST";
            boolean canReapply = false;
            
            if (requestRs.next()) {
                currentRequestId = requestRs.getInt("id");
                overallStatus = requestRs.getString("status");
                canReapply = requestRs.getBoolean("can_reapply");
                System.out.println("DEBUG: Found clearance request - ID: " + currentRequestId + ", Status: " + overallStatus);
            } else {
                // No request found
                System.out.println("DEBUG: No clearance request found for user");
                showMessage("No clearance request found. Click 'Submit Request' to start.", "info");
                if (btnSubmitRequest != null) btnSubmitRequest.setDisable(false);
                if (btnGenerateCertificate != null) btnGenerateCertificate.setDisable(true);
                
                String statusText = "Status: No Request";
                if (lblClearanceStatus != null) lblClearanceStatus.setText(statusText);
                if (lblClearanceStatusProfile != null) lblClearanceStatusProfile.setText(statusText);
                
                // Update progress bar for no request
                updateProgressBar(0, 0, 0);
                updateStatistics(5, 0, 5, 0);
                updateButtonStates(false, 0, 5, 0);
                return;
            }
            
            // Update clearance status label
            String statusText = "Status: " + formatStatus(overallStatus);
            System.out.println("DEBUG: Setting clearance status to: " + statusText);
            
            Platform.runLater(() -> {
                if (lblClearanceStatus != null) {
                    lblClearanceStatus.setText(statusText);
                }
                if (lblClearanceStatusProfile != null) {
                    lblClearanceStatusProfile.setText(statusText);
                }
            });
            
            // Load approval details
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
            int totalDepartments = 5;
            
            // Create default department list if no records found
            if (!approvalRs.isBeforeFirst()) {
                System.out.println("DEBUG: No approval records found, creating default entries");
                // No approval records found, create default pending entries
                String[] departments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
                for (String dept : departments) {
                    String department = formatDepartmentName(dept);
                    String displayStatus = "⏳ Pending";
                    String officer = "Not Assigned";
                    String date = "";
                    String remarks = "Waiting for review...";
                    
                    ApprovalStatus status = new ApprovalStatus(
                        department, displayStatus, officer, date, remarks
                    );
                    if (approvalData != null) {
                        approvalData.add(status);
                    }
                    pendingCount++;
                }
            } else {
                // Process existing approval records
                System.out.println("DEBUG: Processing approval records");
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
                    if (approvalData != null) {
                        approvalData.add(status);
                    }
                }
            }
            
            System.out.println("DEBUG: Counts - Approved: " + approvedCount + ", Pending: " + pendingCount + ", Rejected: " + rejectedCount);
            
            // Update statistics labels
            updateStatistics(totalDepartments, approvedCount, pendingCount, rejectedCount);
            
            // Update progress bar
            updateProgressBar(totalDepartments, approvedCount, rejectedCount);
            
            // Update table with data
            Platform.runLater(() -> {
                if (tableApprovals != null && approvalData != null) {
                    tableApprovals.setItems(approvalData);
                }
            });
            
            // Check if fully approved
            boolean isFullyApproved = approvedCount == totalDepartments && rejectedCount == 0;
            Platform.runLater(() -> {
                if (btnGenerateCertificate != null) {
                    btnGenerateCertificate.setDisable(!isFullyApproved);
                    if (isFullyApproved) {
                        btnGenerateCertificate.setTooltip(new Tooltip("Click to download clearance certificate"));
                        btnGenerateCertificate.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    } else {
                        btnGenerateCertificate.setTooltip(new Tooltip("This button is inactive until all departments approve your request"));
                        btnGenerateCertificate.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                    }
                }
            });
            
            // Update button states based on status
            updateButtonStates(canReapply, approvedCount, pendingCount, rejectedCount);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error in loadClearanceStatus: " + e.getMessage());
            e.printStackTrace();
            showMessage("Error loading status: " + e.getMessage(), "error");
        }
    }
    
    private void updateProgressBar(int total, int approved, int rejected) {
        Platform.runLater(() -> {
            if (progressBar == null || lblProgressPercentage == null || lblProgressText == null) {
                System.out.println("DEBUG: Progress bar components not initialized");
                return;
            }
            
            if (total == 0) {
                progressBar.setProgress(0.0);
                lblProgressPercentage.setText("0%");
                lblProgressText.setText("No request submitted");
                progressBar.setStyle("-fx-accent: #95a5a6;");
                return;
            }
            
            // Calculate progress percentage (approved / total)
            double progress = (double) approved / total;
            progressBar.setProgress(progress);
            
            // Format percentage
            int percentage = (int) (progress * 100);
            lblProgressPercentage.setText(percentage + "%");
            
            // Update progress text based on status
            if (rejected > 0) {
                lblProgressText.setText(approved + "/" + total + " approved • " + rejected + " rejected");
                progressBar.setStyle("-fx-accent: #e74c3c;");
            } else if (approved == total) {
                lblProgressText.setText("All departments approved! Ready for certificate.");
                progressBar.setStyle("-fx-accent: #27ae60;");
            } else {
                lblProgressText.setText(approved + "/" + total + " approved • " + (total - approved) + " pending");
                progressBar.setStyle("-fx-accent: #3498db;");
            }
        });
    }
    
    private void updateStatistics(int total, int approved, int pending, int rejected) {
        Platform.runLater(() -> {
            if (lblTotalApprovals != null) {
                lblTotalApprovals.setText(String.valueOf(total));
            }
            
            if (lblApprovedCount != null) {
                lblApprovedCount.setText(String.valueOf(approved));
                if (approved > 0) {
                    lblApprovedCount.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
            
            if (lblPendingCount != null) {
                lblPendingCount.setText(String.valueOf(pending));
                if (pending > 0) {
                    lblPendingCount.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
            
            if (lblRejectedCount != null) {
                lblRejectedCount.setText(String.valueOf(rejected));
                if (rejected > 0) {
                    lblRejectedCount.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void updateButtonStates(boolean canReapply, int approved, int pending, int rejected) {
        Platform.runLater(() -> {
            // Update reapply buttons
            boolean showReapply = canReapply && pending == 0;
            System.out.println("DEBUG: showReapply = " + showReapply + " (canReapply=" + canReapply + ", pending=" + pending + ")");
            
            if (btnReapplyClearance != null) {
                btnReapplyClearance.setVisible(showReapply);
                btnReapplyClearance.setDisable(!showReapply);
            }
            
            if (btnReapplyInline != null) {
                btnReapplyInline.setVisible(showReapply);
                btnReapplyInline.setDisable(!showReapply);
            }
            
            if (btnProfileReapply != null) {
                btnProfileReapply.setVisible(showReapply);
                btnProfileReapply.setDisable(!showReapply);
            }
            
            // Update submit buttons
            boolean hasActive = pending > 0 || (approved > 0 && approved < 5 && rejected == 0);
            boolean canSubmit = !hasActive && !showReapply;
            System.out.println("DEBUG: canSubmit = " + canSubmit + " (hasActive=" + hasActive + ", showReapply=" + showReapply + ")");
            
            if (btnSubmitRequest != null) {
                btnSubmitRequest.setDisable(!canSubmit);
                btnSubmitRequest.setStyle(canSubmit ? 
                    "-fx-background-color: #3498db; -fx-text-fill: white;" :
                    "-fx-background-color: #95a5a6; -fx-text-fill: white;");
            }
            
            if (btnSubmitRequestInline != null) {
                btnSubmitRequestInline.setVisible(canSubmit);
                btnSubmitRequestInline.setDisable(!canSubmit);
            }
            
            if (btnProfileSubmitRequest != null) {
                btnProfileSubmitRequest.setVisible(canSubmit);
                btnProfileSubmitRequest.setDisable(!canSubmit);
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
            case "NO_REQUEST": return "📝 No Request";
            default: return "❓ " + status;
        }
    }

    private String formatDepartmentName(String role) {
        switch (role) {
            case "LIBRARIAN": return "Library";
            case "CAFETERIA": return "Cafeteria";
            case "DORMITORY": return "Dormitory";
            case "REGISTRAR": return "Registrar";
            case "DEPARTMENT_HEAD": return "Department Head";
            default: return role;
        }
    }

    private void loadProfileInformation() {
        System.out.println("DEBUG: loadProfileInformation called");
        if (currentUser == null) {
            System.out.println("DEBUG: currentUser is null");
            return;
        }
        
        // First update from current user object (fast)
        Platform.runLater(() -> {
            if (lblProfileName != null) {
                lblProfileName.setText(currentUser.getFullName());
            }
            if (lblProfileDepartment != null) {
                lblProfileDepartment.setText(currentUser.getDepartment());
            }
        });
        
        // Then update from database for complete information
        updateProfileFromDatabase();
    }
    
    private void updateProfileFromDatabase() {
        if (currentUser == null) return;
        
        System.out.println("DEBUG: updateProfileFromDatabase called for user ID: " + currentUser.getId());
        
        // Create a new thread to handle database operations
        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT username, full_name, department, email, year_level, phone FROM users WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, currentUser.getId());
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    // Extract data from ResultSet BEFORE closing it
                    String username = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    String department = rs.getString("department");
                    String email = rs.getString("email");
                    String yearLevel = rs.getString("year_level");
                    String phone = rs.getString("phone");
                    
                    System.out.println("DEBUG: Retrieved from database - Username: " + username + 
                                      ", Full Name: " + fullName + 
                                      ", Department: " + department + 
                                      ", Email: " + email + 
                                      ", Year Level: " + yearLevel + 
                                      ", Phone: " + phone);
                    
                    // Now update UI on JavaFX thread
                    Platform.runLater(() -> {
                        System.out.println("DEBUG: Updating UI with profile data");
                        
                        // Update profile labels with null checks
                        if (lblProfileId != null) {
                            lblProfileId.setText(username != null ? username : "N/A");
                            System.out.println("DEBUG: Set profile ID to: " + username);
                        }
                        
                        if (lblProfileName != null) {
                            lblProfileName.setText(fullName != null ? fullName : "N/A");
                            System.out.println("DEBUG: Set profile name to: " + fullName);
                        }
                        
                        if (lblProfileDepartment != null) {
                            lblProfileDepartment.setText(department != null ? department : "N/A");
                            System.out.println("DEBUG: Set profile department to: " + department);
                        }
                        
                        if (lblProfileEmail != null) {
                            lblProfileEmail.setText(email != null ? email : "N/A");
                            System.out.println("DEBUG: Set profile email to: " + email);
                        }
                        
                        if (lblProfileYearLevel != null) {
                            lblProfileYearLevel.setText(yearLevel != null ? "Year " + yearLevel : "Not set");
                            System.out.println("DEBUG: Set profile year level to: " + yearLevel);
                        }
                        
                        if (lblProfilePhone != null) {
                            lblProfilePhone.setText(phone != null ? phone : "Not provided");
                            System.out.println("DEBUG: Set profile phone to: " + phone);
                        }
                        
                        // Also update the user display in sidebar
                        if (lblUserNameSidebar != null) {
                            lblUserNameSidebar.setText(fullName != null ? fullName : "N/A");
                        }
                        
                        if (lblUserRoleSidebar != null) {
                            lblUserRoleSidebar.setText("Student - " + (department != null ? department : "N/A"));
                        }
                        
                        System.out.println("DEBUG: Profile update complete");
                    });
                } else {
                    System.out.println("DEBUG: User profile not found in database for ID: " + currentUser.getId());
                    Platform.runLater(() -> {
                        showMessage("User profile not found in database.", "error");
                    });
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Exception in updateProfileFromDatabase: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showMessage("Error loading profile: " + e.getMessage(), "error");
                });
            }
        }).start();
    }

    @FXML
    private void viewProfile() {
        System.out.println("DEBUG: viewProfile called");
        loadProfileInformation();
        showMessage("Profile information loaded.", "info");
    }

    private void showMessage(String message, String type) {
        Platform.runLater(() -> {
            if (lblStatusMessage != null) {
                lblStatusMessage.setText(message);
            }
            if (statusBanner != null) {
                statusBanner.setVisible(true);
                
                switch (type) {
                    case "success":
                        statusBanner.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 0 0 5 5;");
                        break;
                    case "error":
                        statusBanner.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 0 0 5 5;");
                        break;
                    case "info":
                        statusBanner.setStyle("-fx-background-color: #3498db; -fx-background-radius: 0 0 5 5;");
                        break;
                    default:
                        statusBanner.setStyle("-fx-background-color: #f39c12; -fx-background-radius: 0 0 5 5;");
                }
                
                // Hide banner after 5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        Platform.runLater(() -> {
                            if (statusBanner != null) {
                                statusBanner.setVisible(false);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                // Fallback to alert if status banner is not available
                showAlert("Message", message);
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
                loadClearanceStatus();
                
                // Show success message
                showMessage("Reapplication submitted successfully! Status: IN PROGRESS", "success");
                
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

    @FXML
    private void handleLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Stop auto-refresh
                autoRefreshEnabled = false;
                if (autoRefreshTimeline != null) {
                    autoRefreshTimeline.stop();
                }
                
                Stage currentStage = (Stage) (lblStatusMessage != null ? 
                    lblStatusMessage.getScene().getWindow() : 
                    (btnSubmitRequest != null ? btnSubmitRequest.getScene().getWindow() : null));
                
                if (currentStage != null) {
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
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage());
                Stage stage = (Stage) (lblStatusMessage != null ? 
                    lblStatusMessage.getScene().getWindow() : 
                    (btnSubmitRequest != null ? btnSubmitRequest.getScene().getWindow() : null));
                if (stage != null) {
                    stage.close();
                }
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
    
    // Cleanup method to be called when controller is destroyed
    public void cleanup() {
        autoRefreshEnabled = false;
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }
}