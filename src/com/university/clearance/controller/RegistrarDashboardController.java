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

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;



public class RegistrarDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private TabPane mainTabPane;
    
    
    @FXML private Label lblPendingCard;
    @FXML private Label lblApprovedCard;
    @FXML private Label lblHoldsCard;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colYearLevel;
    @FXML private TableColumn<ClearanceRequest, String> colAcademicStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<AcademicRecord> tableAcademicRecords;
    @FXML private TableColumn<AcademicRecord, String> colCourse;
    @FXML private TableColumn<AcademicRecord, String> colGrade;
    @FXML private TableColumn<AcademicRecord, String> colCredits;
    @FXML private TableColumn<AcademicRecord, String> colSemester;

    
    
    @FXML
    private Button logoutBtn;
    
    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<AcademicRecord> academicData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupAcademicTableColumns();
        
        // Add row selection listener
        tableRequests.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadStudentAcademicDetails(newSelection.getStudentId());
                    lblStudentInfo.setText("Viewing academic records for: " + 
                                          newSelection.getStudentName() + 
                                          " (" + newSelection.getStudentId() + ")");
                }
            });
    }

    
    
    
    
    @FXML
    private void handleLogout() {
        try {
            System.out.println("[DEBUG] Logout button clicked in Registrar Dashboard.");
            
            // Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout Confirmation");
            confirm.setHeaderText("Confirm Logout");
            confirm.setContentText("Are you sure you want to logout from the Registrar Dashboard?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("[DEBUG] User confirmed logout.");
                
                // Get current scene from welcome label to preserve window size
                Scene currentScene = lblWelcome.getScene();
                
                // Load the Login FXML
                String fxmlPath = "/com/university/clearance/resources/views/Login.fxml";
                System.out.println("[DEBUG] Trying FXML: " + fxmlPath);
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                
                if (loader.getLocation() == null) {
                    System.out.println("[DEBUG] ERROR: FXML not found at: " + fxmlPath);
                    showAlert("Error", "Login screen not found. Check FXML path.");
                    return;
                }
                
                Parent root = loader.load();
                System.out.println("[DEBUG] Login FXML loaded successfully.");
                
                // Get the current stage
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                
                // Preserve current window size
                Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
                stage.setScene(newScene);
                stage.setTitle("DBU Clearance System - Login");
                stage.centerOnScreen();
                
                System.out.println("[DEBUG] Successfully logged out and returned to login screen.");
            } else {
                System.out.println("[DEBUG] Logout cancelled by user.");
            }
            
        } catch (Exception e) {
            System.err.println("[DEBUG] Exception occurred during logout: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    
    
    
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("=== DEBUG: Setting current user for Registrar ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Full Name: " + user.getFullName());
        System.out.println("Role: " + user.getRole());
        
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Registrar Office");
        loadPendingRequests();
        
        // ADD THIS LINE: Update dashboard statistics
        updateDashboardStats();
    }

    // Add this method to update the card statistics
    private void updateDashboardStats() {
        System.out.println("=== DEBUG: Updating registrar dashboard statistics ===");
        
        int pendingCount = requestData.size();
        int approvedCount = countApprovedStudents();
        int holdsCount = countAcademicHolds();
        
        // Update card labels
        if (lblPendingCard != null) {
            lblPendingCard.setText(String.valueOf(pendingCount));
        }
        if (lblApprovedCard != null) {
            lblApprovedCard.setText(String.valueOf(approvedCount));
        }
        if (lblHoldsCard != null) {
            lblHoldsCard.setText(String.valueOf(holdsCount));
        }
        
        // Update card colors based on counts
        updateCardStyles(pendingCount, approvedCount, holdsCount);
        
        System.out.println("Dashboard stats - Pending: " + pendingCount + 
                          ", Approved: " + approvedCount + 
                          ", Holds: " + holdsCount);
    }

    // Add this method to count approved students
    private int countApprovedStudents() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT COUNT(DISTINCT cr.student_id) as approved_count
                FROM clearance_approvals ca
                JOIN clearance_requests cr ON ca.request_id = cr.id
                WHERE ca.officer_role = 'REGISTRAR'
                AND ca.status = 'APPROVED'
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("approved_count");
            }
        } catch (Exception e) {
            System.err.println("Error counting approved students: " + e.getMessage());
        }
        return 0;
    }

    // Add this method to count academic holds
    private int countAcademicHolds() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT COUNT(*) as hold_count
                FROM student_academic_records
                WHERE academic_hold != 'NONE' 
                AND academic_hold IS NOT NULL
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("hold_count");
            }
        } catch (Exception e) {
            System.err.println("Error counting academic holds: " + e.getMessage());
        }
        return 0;
    }

    // Add this method to update card colors
    private void updateCardStyles(int pendingCount, int approvedCount, int holdsCount) {
        // Update Pending card style
        if (lblPendingCard != null) {
            HBox pendingCard = (HBox) lblPendingCard.getParent();
            if (pendingCard != null) {
                if (pendingCount > 10) {
                    pendingCard.setStyle("-fx-background-color: #c0392b; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (pendingCount > 5) {
                    pendingCard.setStyle("-fx-background-color: #e74c3c; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (pendingCount > 0) {
                    pendingCard.setStyle("-fx-background-color: #f39c12; -fx-padding: 20; -fx-background-radius: 10;");
                } else {
                    pendingCard.setStyle("-fx-background-color: #3498db; -fx-padding: 20; -fx-background-radius: 10;");
                }
            }
        }
        
        // Update Approved card style
        if (lblApprovedCard != null) {
            HBox approvedCard = (HBox) lblApprovedCard.getParent();
            if (approvedCard != null) {
                if (approvedCount > 50) {
                    approvedCard.setStyle("-fx-background-color: #27ae60; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (approvedCount > 20) {
                    approvedCard.setStyle("-fx-background-color: #2ecc71; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (approvedCount > 0) {
                    approvedCard.setStyle("-fx-background-color: #3498db; -fx-padding: 20; -fx-background-radius: 10;");
                } else {
                    approvedCard.setStyle("-fx-background-color: #95a5a6; -fx-padding: 20; -fx-background-radius: 10;");
                }
            }
        }
        
        // Update Holds card style
        if (lblHoldsCard != null) {
            HBox holdsCard = (HBox) lblHoldsCard.getParent();
            if (holdsCard != null) {
                if (holdsCount > 20) {
                    holdsCard.setStyle("-fx-background-color: #c0392b; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (holdsCount > 10) {
                    holdsCard.setStyle("-fx-background-color: #e74c3c; -fx-padding: 20; -fx-background-radius: 10;");
                } else if (holdsCount > 0) {
                    holdsCard.setStyle("-fx-background-color: #f39c12; -fx-padding: 20; -fx-background-radius: 10;");
                } else {
                    holdsCard.setStyle("-fx-background-color: #2ecc71; -fx-padding: 20; -fx-background-radius: 10;");
                }
            }
        }
    }
    
    
    
    

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYearLevel.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colAcademicStatus.setCellValueFactory(new PropertyValueFactory<>("academicStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Verify/Hold buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button(" Verify Clear");
            private final Button btnReject = new Button("  Hold Records");
            private final Button btnViewDetails = new Button("📋 View Records");
            private final HBox buttons = new HBox(5, btnViewDetails, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                
                btnViewDetails.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    viewStudentRecords(request);
                });
                
                btnApprove.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    approveClearance(request);
                });
                
                btnReject.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    rejectClearance(request);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    if (request != null) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        // Color code academic status
        colAcademicStatus.setCellFactory(column -> new TableCell<ClearanceRequest, String>() {
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
                    } else if (item.contains("⚠️")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (item.contains("❌")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupAcademicTableColumns() {
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
        
        // Color code grades
        colGrade.setCellFactory(column -> new TableCell<AcademicRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("F") || item.equals("D") || item.equals("D+") || item.equals("C-") || item.equals("C")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.equals("C+") || item.equals("B-") || item.equals("B")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (item.equals("B+") || item.equals("A-") || item.equals("A")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        System.out.println("=== DEBUG: Refreshing registrar requests ===");
        loadPendingRequests();
        showAlert("Refreshed", "Registrar clearance requests refreshed successfully!");
    }
    
    
    private List<String> getAllDepartmentRoles(Connection conn) throws SQLException {
        List<String> roles = new ArrayList<>();
        String sql = """
            SELECT DISTINCT officer_role 
            FROM clearance_approvals 
            WHERE officer_role != 'REGISTRAR' 
            AND officer_role IS NOT NULL
            ORDER BY officer_role
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        
        System.out.println("=== DEBUG: Found department roles in database ===");
        while (rs.next()) {
            String role = rs.getString("officer_role");
            roles.add(role);
            System.out.println("Department Role: " + role);
        }
        
        // If no records found, show a different approach
        if (roles.isEmpty()) {
            System.out.println("WARNING: No department roles found in clearance_approvals table!");
            System.out.println("Checking users table for department names...");
            
            // Try to get department names from users table
            sql = "SELECT DISTINCT department FROM users WHERE department IS NOT NULL";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                String dept = rs.getString("department");
                System.out.println("User Department: " + dept);
            }
        }
        
        return roles;
    }

    private void loadPendingRequests() {
        System.out.println("\n=== DEBUG: Loading pending registrar requests ===");
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Database connection established");
            
            // DEBUG: First, let's see what department roles actually exist
            List<String> departmentRoles = getAllDepartmentRoles(conn);
            System.out.println("Will check for approvals from these departments: " + departmentRoles);
            
            // If no roles found, use a default list
            if (departmentRoles.isEmpty()) {
                System.out.println("Using default department list");
                departmentRoles = Arrays.asList("LIBRARY", "ACCOUNTING", "LABORATORY", "SPORTS");
            }
            
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    u.year_level,
                    DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
                    AND ca.officer_role = 'REGISTRAR'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND (ca.status IS NULL OR ca.status = 'PENDING')
                ORDER BY cr.request_date ASC
                """;
                
            System.out.println("SQL Query: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            int skippedCount = 0;
            System.out.println("Query executed, processing results...");
            
            while (rs.next()) {
                int requestId = rs.getInt("request_id");
                String studentId = rs.getString("student_id");
                
                // DEBUG: Show what approvals this student has
                System.out.println("\n=== DEBUG: Checking student " + studentId + " (Request ID: " + requestId + ") ===");
                Map<String, String> studentApprovals = getStudentApprovals(conn, requestId);
                System.out.println("Approvals found: " + studentApprovals);
                
                // Check if all required departments have approved
                boolean allApproved = true;
                for (String department : departmentRoles) {
                    String status = studentApprovals.get(department);
                    System.out.println("Checking " + department + ": " + status);
                    if (status == null || !"APPROVED".equals(status)) {
                        System.out.println("❌ Missing or not approved: " + department);
                        allApproved = false;
                    }
                }
                
                if (allApproved) {
                    pendingCount++;
                    System.out.println("✅ Student " + studentId + " is approved by all departments!");
                    System.out.println("Student Name: " + rs.getString("student_name"));
                    System.out.println("Department: " + rs.getString("department"));
                    System.out.println("Year Level: " + rs.getString("year_level"));
                    System.out.println("Request Date: " + rs.getString("request_date"));
                    System.out.println("Approval Status: " + rs.getString("approval_status"));
                    
                    String academicStatus = checkStudentAcademicStatus(conn, studentId);
                    
                    ClearanceRequest request = new ClearanceRequest(
                        rs.getString("student_id"),
                        rs.getString("student_name"),
                        rs.getString("department"),
                        rs.getString("year_level"),
                        rs.getString("request_date"),
                        academicStatus,
                        requestId
                    );
                    
                    requestData.add(request);
                } else {
                    skippedCount++;
                    System.out.println("⏭️ Skipping student " + studentId + " - Not all departments approved yet");
                }
            }
            
            System.out.println("\n=== DEBUG: Query Results Summary ===");
            System.out.println("Total students processed: " + (pendingCount + skippedCount));
            System.out.println("Students ready for registrar: " + pendingCount);
            System.out.println("Students skipped (not fully approved): " + skippedCount);
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Verifications: " + pendingCount);
            
            if (pendingCount == 0) {
                System.out.println("⚠️ WARNING: No registrar clearance requests found!");
                showAlert("No Requests", 
                    "No students are ready for registrar approval.\n\n" +
                    "Students must be approved by all departments first:\n" +
                    departmentRoles.toString() + "\n\n" +
                    "Please check that other departments have approved these students.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR in loadPendingRequests:");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
        }
    }

    // Helper method to get all approvals for a student
    private Map<String, String> getStudentApprovals(Connection conn, int requestId) throws SQLException {
        Map<String, String> approvals = new HashMap<>();
        
        String sql = """
            SELECT officer_role, status 
            FROM clearance_approvals 
            WHERE request_id = ?
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, requestId);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            String role = rs.getString("officer_role");
            String status = rs.getString("status");
            approvals.put(role, status);
        }
        
        return approvals;
    }
    
    
    // Helper method to check if all departments have approved
    private boolean allDepartmentsApproved(Connection conn, int requestId) throws SQLException {
        // Get list of all departments that need to approve
        // You can hardcode these or get them from your database
        String[] requiredDepartments = {"LIBRARY", "ACCOUNTING", "LABORATORY", "SPORTS"};
        
        for (String department : requiredDepartments) {
            String checkSql = """
                SELECT COUNT(*) as approved 
                FROM clearance_approvals 
                WHERE request_id = ? 
                AND officer_role = ? 
                AND status = 'APPROVED'
                """;
            PreparedStatement ps = conn.prepareStatement(checkSql);
            ps.setInt(1, requestId);
            ps.setString(2, department);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next() && rs.getInt("approved") == 0) {
                return false; // This department hasn't approved
            }
        }
        return true; // All departments have approved
    }
    
    private void debugDatabaseStatus(Connection conn) throws SQLException {
        System.out.println("\n=== DEBUG: Database Status Check ===");
        
        // Check clearance_requests
        System.out.println("\n1. Total PENDING/IN_PROGRESS clearance_requests:");
        String checkRequestsSql = "SELECT COUNT(*) as count FROM clearance_requests WHERE status IN ('PENDING', 'IN_PROGRESS')";
        PreparedStatement requestsStmt = conn.prepareStatement(checkRequestsSql);
        ResultSet requestsRs = requestsStmt.executeQuery();
        if (requestsRs.next()) {
            System.out.println("   Count: " + requestsRs.getInt("count"));
        }
        
        // Check clearance_approvals for REGISTRAR
        System.out.println("\n2. clearance_approvals for REGISTRAR:");
        String checkApprovalsSql = """
            SELECT ca.request_id, ca.status, u.username as student_id
            FROM clearance_approvals ca
            LEFT JOIN clearance_requests cr ON ca.request_id = cr.id
            LEFT JOIN users u ON cr.student_id = u.id
            WHERE ca.officer_role = 'REGISTRAR'
            """;
        PreparedStatement approvalsStmt = conn.prepareStatement(checkApprovalsSql);
        ResultSet approvalsRs = approvalsStmt.executeQuery();
        int approvalCount = 0;
        while (approvalsRs.next()) {
            approvalCount++;
            System.out.println("   Approval " + approvalCount + ": Request ID=" + approvalsRs.getInt("request_id") +
                             ", Student=" + approvalsRs.getString("student_id") +
                             ", Status=" + approvalsRs.getString("status"));
        }
        
        // Check requests missing REGISTRAR approvals
        System.out.println("\n3. Requests missing REGISTRAR approvals:");
        String missingSql = """
            SELECT cr.id, u.username, cr.status as request_status
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
            AND NOT EXISTS (
                SELECT 1 FROM clearance_approvals ca 
                WHERE ca.request_id = cr.id 
                AND ca.officer_role = 'REGISTRAR'
            )
            """;
        PreparedStatement missingStmt = conn.prepareStatement(missingSql);
        ResultSet missingRs = missingStmt.executeQuery();
        int missingCount = 0;
        while (missingRs.next()) {
            missingCount++;
            System.out.println("   Missing approval " + missingCount + ": Request ID=" + missingRs.getInt("id") +
                             ", Student=" + missingRs.getString("username") +
                             ", Status=" + missingRs.getString("request_status"));
        }
        
        System.out.println("\n=== DEBUG SUMMARY ===");
        System.out.println("Total REGISTRAR approvals: " + approvalCount);
        System.out.println("Requests missing REGISTRAR approvals: " + missingCount);
    }

    private String checkStudentAcademicStatus(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking academic status for student: " + studentId);
        
        createAcademicRecordsIfNeeded(conn, studentId);
        
        // Check student_academic_records
        String academicSql = """
            SELECT 
                COALESCE(academic_hold, 'NONE') as hold_status,
                COALESCE(outstanding_fees, 0) as fees,
                COALESCE(incomplete_courses, 0) as incompletes,
                COALESCE(gpa, 0.0) as gpa
            FROM student_academic_records sar
            JOIN users u ON sar.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement ps = conn.prepareStatement(academicSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            String holdStatus = rs.getString("hold_status");
            double fees = rs.getDouble("fees");
            int incompletes = rs.getInt("incompletes");
            double gpa = rs.getDouble("gpa");
            
            System.out.println("  Academic status - Hold: " + holdStatus + 
                             ", Fees: $" + fees + 
                             ", Incompletes: " + incompletes +
                             ", GPA: " + gpa);
            
            if (!"NONE".equals(holdStatus)) {
                return "❌ Academic Hold: " + holdStatus;
            } else if (fees > 0) {
                return "❌ Outstanding Fees: $" + String.format("%.2f", fees);
            } else if (incompletes > 0) {
                return "❌ " + incompletes + " Incomplete Course(s)";
            } else if (gpa < 2.0) {
                return "⚠️ Low GPA: " + String.format("%.2f", gpa);
            } else {
                return "✅ Academically Clear (GPA: " + String.format("%.2f", gpa) + ")";
            }
        }
        
        return "Pending Academic Verification";
    }

    private void createAcademicRecordsIfNeeded(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking/creating academic records for: " + studentId);
        
        try {
            String checkTableSql = "SELECT 1 FROM student_academic_records LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
            System.out.println("  student_academic_records table exists");
        } catch (SQLException e) {
            System.out.println("  student_academic_records table doesn't exist, creating...");
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS student_academic_records (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT UNIQUE,
                    academic_hold VARCHAR(100) DEFAULT 'NONE',
                    outstanding_fees DECIMAL(10,2) DEFAULT 0.00,
                    incomplete_courses INT DEFAULT 0,
                    gpa DECIMAL(3,2) DEFAULT 0.00,
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            System.out.println("  Created student_academic_records table");
        }
        
        // Check if student has academic records
        String checkRecordsSql = """
            SELECT COUNT(*) FROM student_academic_records sar 
            JOIN users u ON sar.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        int existingCount = rs.getInt(1);
        
        System.out.println("  Existing academic records: " + existingCount);
        
        if (existingCount == 0) {
            System.out.println("  No academic records found, creating sample records...");
            
            // Get student info
            String studentSql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement studentStmt = conn.prepareStatement(studentSql);
            studentStmt.setString(1, studentId);
            ResultSet studentRs = studentStmt.executeQuery();
            
            if (studentRs.next()) {
                int userId = studentRs.getInt("id");
                
                // Randomly assign academic status (70% clear, 20% warning, 10% hold)
                double random = Math.random();
                
                if (random < 0.7) { // 70% clear
                    double gpa = 2.5 + (Math.random() * 1.5); // GPA between 2.5-4.0
                    String insertSql = """
                        INSERT INTO student_academic_records (student_id, academic_hold, outstanding_fees, incomplete_courses, gpa)
                        VALUES (?, 'NONE', 0.00, 0, ?)
                        """;
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, userId);
                    insertStmt.setDouble(2, gpa);
                    insertStmt.executeUpdate();
                    System.out.println("  Created clear academic record (GPA: " + String.format("%.2f", gpa) + ")");
                } 
                else if (random < 0.9) { // 20% warning (low GPA or minor issues)
                    double gpa = 1.5 + (Math.random() * 1.0); // GPA between 1.5-2.5
                    String insertSql = """
                        INSERT INTO student_academic_records (student_id, academic_hold, outstanding_fees, incomplete_courses, gpa)
                        VALUES (?, 'NONE', 0.00, 0, ?)
                        """;
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, userId);
                    insertStmt.setDouble(2, gpa);
                    insertStmt.executeUpdate();
                    System.out.println("  Created warning academic record (Low GPA: " + String.format("%.2f", gpa) + ")");
                } 
                else { // 10% hold
                    String[] holds = {"LIBRARY_FINES", "OUTSTANDING_FEES", "INCOMPLETE_COURSES", "ACADEMIC_PROBATION"};
                    String hold = holds[(int)(Math.random() * holds.length)];
                    double fees = random < 0.95 ? 0.00 : 500 + (Math.random() * 1000);
                    int incompletes = random < 0.85 ? 0 : (int)(Math.random() * 3) + 1;
                    double gpa = 1.0 + (Math.random() * 2.0);
                    
                    String insertSql = """
                        INSERT INTO student_academic_records (student_id, academic_hold, outstanding_fees, incomplete_courses, gpa)
                        VALUES (?, ?, ?, ?, ?)
                        """;
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, hold);
                    insertStmt.setDouble(3, fees);
                    insertStmt.setInt(4, incompletes);
                    insertStmt.setDouble(5, gpa);
                    insertStmt.executeUpdate();
                    System.out.println("  Created hold academic record (Hold: " + hold + ", Fees: $" + fees + 
                                     ", Incompletes: " + incompletes + ", GPA: " + String.format("%.2f", gpa) + ")");
                }
            }
        }
    }

    private void viewStudentRecords(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Viewing academic records for: " + request.getStudentId());
        loadStudentAcademicDetails(request.getStudentId());
        
        if (lblStudentInfo != null) {
            lblStudentInfo.setText("Academic Records for: " + request.getStudentName() + 
                                 " (" + request.getStudentId() + ") - " + request.getDepartment());
        }
        
        // Switch to Academic Records tab
        for (Tab tab : mainTabPane.getTabs()) {
            if ("Academic Records".equals(tab.getText())) {
                mainTabPane.getSelectionModel().select(tab);
                System.out.println("Switched to Academic Records tab");
                break;
            }
        }
    }

    private void loadStudentAcademicDetails(String studentId) {
        System.out.println("DEBUG: Loading academic details for student: " + studentId);
        academicData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, ensure student_courses table exists and has data
            createStudentCoursesIfNeeded(conn, studentId);
            
            String sql = """
                SELECT 
                    sc.course_code,
                    sc.course_name,
                    sc.grade,
                    sc.credits,
                    sc.semester,
                    sc.academic_year
                FROM student_courses sc
                JOIN users u ON sc.student_id = u.id
                WHERE u.username = ?
                ORDER BY sc.academic_year DESC, sc.semester DESC, sc.course_code ASC
                """;
                
            System.out.println("DEBUG: Load academic details SQL: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                AcademicRecord record = new AcademicRecord(
                    rs.getString("course_code") + " - " + rs.getString("course_name"),
                    rs.getString("grade"),
                    String.valueOf(rs.getInt("credits")),
                    rs.getString("semester") + " " + rs.getString("academic_year")
                );
                academicData.add(record);
            }
            
            tableAcademicRecords.setItems(academicData);
            System.out.println("Loaded " + count + " academic records");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR loading academic details: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load academic records: " + e.getMessage());
        }
    }
    
    private void createStudentCoursesIfNeeded(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking student courses for: " + studentId);
        
        try {
            String checkTableSql = "SELECT 1 FROM student_courses LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
            System.out.println("  student_courses table exists");
        } catch (SQLException e) {
            System.out.println("  student_courses table doesn't exist, creating...");
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS student_courses (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    course_code VARCHAR(20),
                    course_name VARCHAR(100),
                    grade VARCHAR(2),
                    credits INT,
                    semester VARCHAR(20),
                    academic_year VARCHAR(10),
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            System.out.println("  Created student_courses table");
        }
        
        // Check if student has courses
        String checkRecordsSql = """
            SELECT COUNT(*) FROM student_courses sc 
            JOIN users u ON sc.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        int existingCount = rs.getInt(1);
        
        System.out.println("  Existing course records: " + existingCount);
        
        if (existingCount == 0) {
            System.out.println("  No courses found, creating sample course data...");
            
            // Get student info
            String studentSql = "SELECT id, department FROM users WHERE username = ?";
            PreparedStatement studentStmt = conn.prepareStatement(studentSql);
            studentStmt.setString(1, studentId);
            ResultSet studentRs = studentStmt.executeQuery();
            
            if (studentRs.next()) {
                int userId = studentRs.getInt("id");
                String department = studentRs.getString("department");
                
                // Create sample courses based on department
                String[][] courses = getDepartmentCourses(department);
                
                String insertSql = """
                    INSERT INTO student_courses (student_id, course_code, course_name, grade, credits, semester, academic_year)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
                
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                for (String[] course : courses) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, course[0]);
                    insertStmt.setString(3, course[1]);
                    insertStmt.setString(4, course[2]);
                    insertStmt.setInt(5, Integer.parseInt(course[3]));
                    insertStmt.setString(6, course[4]);
                    insertStmt.setString(7, course[5]);
                    insertStmt.addBatch();
                }
                int[] results = insertStmt.executeBatch();
                System.out.println("  Created " + results.length + " course records");
            }
        }
    }
    
    private String[][] getDepartmentCourses(String department) {
        System.out.println("  Getting courses for department: " + department);
        
        // Define sample courses based on department
        switch (department) {
            case "Software Engineering":
            case "Computer Science":
                return new String[][]{
                    {"CS101", "Introduction to Programming", "A", "3", "Fall", "2023"},
                    {"MATH201", "Calculus I", "B+", "4", "Fall", "2023"},
                    {"PHY101", "General Physics", "A-", "3", "Fall", "2023"},
                    {"CS201", "Data Structures", "B", "3", "Spring", "2024"},
                    {"MATH202", "Calculus II", "C+", "4", "Spring", "2024"},
                    {"CS301", "Algorithms", "A", "3", "Fall", "2024"},
                    {"CS302", "Database Systems", "B+", "3", "Fall", "2024"},
                    {"ENG101", "English Composition", "A", "3", "Spring", "2024"}
                };
            case "Electrical Engineering":
                return new String[][]{
                    {"EE101", "Circuit Analysis", "B", "3", "Fall", "2023"},
                    {"MATH201", "Calculus I", "A-", "4", "Fall", "2023"},
                    {"PHY101", "General Physics", "B+", "3", "Fall", "2023"},
                    {"EE201", "Digital Systems", "A", "3", "Spring", "2024"},
                    {"MATH202", "Calculus II", "B", "4", "Spring", "2024"},
                    {"EE301", "Electronics", "B+", "3", "Fall", "2024"},
                    {"EE302", "Power Systems", "A-", "3", "Fall", "2024"}
                };
            default:
                return new String[][]{
                    {"GEN101", "General Education", "B", "3", "Fall", "2023"},
                    {"MATH101", "College Mathematics", "C+", "3", "Fall", "2023"},
                    {"ENG101", "English Composition", "A", "3", "Fall", "2023"},
                    {"HIS101", "History", "B-", "3", "Spring", "2024"},
                    {"SCI101", "General Science", "B", "3", "Spring", "2024"}
                };
        }
    }

    private void approveClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Approving academic clearance for: " + request.getStudentId());
        
        // Check if student has academic issues
        if (request.getAcademicStatus().contains("❌") || request.getAcademicStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Academic Clearance Issue");
            warning.setHeaderText("Student Has Academic Issues");
            warning.setContentText("This student has academic issues:\n\n" + 
                                 request.getAcademicStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                System.out.println("Approval cancelled due to academic issues");
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Academic Clearance");
        confirmation.setHeaderText("Verify Student Academic Clearance");
        confirmation.setContentText("Approve academic clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\nDepartment: " + request.getDepartment() +
                                  "\nYear Level: " + request.getYearLevel() +
                                  "\n\nThis confirms the student has no academic holds or outstanding issues.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User confirmed academic clearance approval");
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Academic clearance approved. No holds or outstanding issues.");
                loadPendingRequests();
                showAlert("Approved", "Academic clearance approved for " + request.getStudentName());
            } else {
                System.out.println("User cancelled approval");
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Rejecting academic clearance for: " + request.getStudentId());
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Academic Clearance");
        dialog.setHeaderText("Place Academic Hold");
        dialog.setContentText("Enter reason for placing academic hold on " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            System.out.println("Rejection reason: " + result.get());
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Academic hold placed: " + result.get().trim());
            
            // Also update academic records with hold
            placeAcademicHold(request.getStudentId(), result.get().trim());
            
            loadPendingRequests();
            showAlert("Rejected", "Academic clearance rejected for " + request.getStudentName());
        } else {
            System.out.println("Rejection cancelled");
        }
    }

    private void placeAcademicHold(String studentId, String reason) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Insert or update academic hold
            String sql = """
                INSERT INTO student_academic_records (student_id, academic_hold, outstanding_fees, incomplete_courses, gpa)
                SELECT id, ?, 0, 0, 0.0 FROM users WHERE username = ?
                ON DUPLICATE KEY UPDATE academic_hold = ?
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, reason);
            ps.setString(2, studentId);
            ps.setString(3, reason);
            ps.executeUpdate();
            
            System.out.println("Academic hold placed for " + studentId + ": " + reason);
            
        } catch (Exception e) {
            System.err.println("Error placing academic hold: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        System.out.println("\n=== DEBUG: Updating academic clearance status ===");
        System.out.println("Request ID: " + requestId);
        System.out.println("Status: " + status);
        System.out.println("Remarks: " + remarks);
        System.out.println("Current User ID: " + currentUser.getId());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = "SELECT COUNT(*) FROM clearance_approvals WHERE request_id = ? AND officer_role = 'REGISTRAR'";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            System.out.println("Existing approval records: " + count);
            
            if (count > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE clearance_approvals 
                    SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                    WHERE request_id = ? AND officer_role = 'REGISTRAR'
                    """;
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setString(1, status);
                ps.setString(2, remarks);
                ps.setInt(3, currentUser.getId());
                ps.setInt(4, requestId);
                int updated = ps.executeUpdate();
                System.out.println("Updated " + updated + " record(s)");
            } else {
                // Insert new record
                String insertSql = """
                    INSERT INTO clearance_approvals (request_id, officer_role, officer_id, status, remarks, approval_date)
                    VALUES (?, 'REGISTRAR', ?, ?, ?, NOW())
                    """;
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setInt(1, requestId);
                ps.setInt(2, currentUser.getId());
                ps.setString(3, status);
                ps.setString(4, remarks);
                int inserted = ps.executeUpdate();
                System.out.println("Inserted " + inserted + " record(s)");
            }
            
            System.out.println("Academic clearance status updated successfully");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR updating clearance status: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
        }
    }

    @FXML
    private void generateAcademicReport() {
        System.out.println("\n=== DEBUG: Generating academic report ===");
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No student selected");
            showAlert("Selection Required", "Please select a student first to generate academic report.");
            return;
        }
        
        System.out.println("Generating report for: " + selected.getStudentId());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    u.department,
                    u.year_level,
                    COUNT(sc.id) as total_courses,
                    AVG(CASE 
                        WHEN sc.grade = 'A' THEN 4.0
                        WHEN sc.grade = 'A-' THEN 3.7
                        WHEN sc.grade = 'B+' THEN 3.3
                        WHEN sc.grade = 'B' THEN 3.0
                        WHEN sc.grade = 'B-' THEN 2.7
                        WHEN sc.grade = 'C+' THEN 2.3
                        WHEN sc.grade = 'C' THEN 2.0
                        WHEN sc.grade = 'C-' THEN 1.7
                        WHEN sc.grade = 'D+' THEN 1.3
                        WHEN sc.grade = 'D' THEN 1.0
                        ELSE 0.0
                    END) as gpa,
                    COALESCE(sar.academic_hold, 'NONE') as hold_status,
                    COALESCE(sar.outstanding_fees, 0) as outstanding_fees,
                    COALESCE(sar.incomplete_courses, 0) as incomplete_courses
                FROM users u
                LEFT JOIN student_courses sc ON u.id = sc.student_id
                LEFT JOIN student_academic_records sar ON u.id = sar.student_id
                WHERE u.username = ?
                GROUP BY u.id
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "🎓 ACADEMIC REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Department: " + rs.getString("department") + "\n" +
                              "Year Level: " + rs.getString("year_level") + "\n" +
                              "Total Courses: " + rs.getInt("total_courses") + "\n" +
                              "GPA: " + String.format("%.2f", rs.getDouble("gpa")) + "\n" +
                              "Academic Hold: " + rs.getString("hold_status") + "\n" +
                              "Outstanding Fees: $" + String.format("%.2f", rs.getDouble("outstanding_fees")) + "\n" +
                              "Incomplete Courses: " + rs.getInt("incomplete_courses") + "\n\n" +
                              "Generated by: " + currentUser.getFullName() + 
                              " (Registrar Office)";
                
                System.out.println("Report generated successfully");
                showAlert("Academic Report", report);
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR generating report: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to generate academic report: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner classes for table data
    public static class ClearanceRequest {
        private final String studentId;
        private final String studentName;
        private final String department;
        private final String yearLevel;
        private final String requestDate;
        private final String academicStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String yearLevel, String requestDate, String academicStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.yearLevel = yearLevel;
            this.requestDate = requestDate;
            this.academicStatus = academicStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getYearLevel() { return yearLevel; }
        public String getRequestDate() { return requestDate; }
        public String getAcademicStatus() { return academicStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class AcademicRecord {
        private final String course;
        private final String grade;
        private final String credits;
        private final String semester;

        public AcademicRecord(String course, String grade, String credits, String semester) {
            this.course = course;
            this.grade = grade;
            this.credits = credits;
            this.semester = semester;
        }

        public String getCourse() { return course; }
        public String getGrade() { return grade; }
        public String getCredits() { return credits; }
        public String getSemester() { return semester; }
    }
}