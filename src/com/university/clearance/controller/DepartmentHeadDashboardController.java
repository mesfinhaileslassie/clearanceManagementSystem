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
import java.util.Optional;
import java.util.ResourceBundle;



import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DepartmentHeadDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private Label lblDepartmentRequirements;
    @FXML private TabPane mainTabPane;
   
    @FXML private Label lblPendingCard;
    @FXML private Label lblApprovedCard;
    @FXML private Label lblRequirementsCard;

    
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colYearLevel;
    @FXML private TableColumn<ClearanceRequest, String> colGPA;
    @FXML private TableColumn<ClearanceRequest, String> colDepartmentStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<DepartmentRequirement> tableRequirements;
    @FXML private TableColumn<DepartmentRequirement, String> colRequirement;
    @FXML private TableColumn<DepartmentRequirement, String> colStatus;
    @FXML private TableColumn<DepartmentRequirement, String> colCompletedDate;
    @FXML private TableColumn<DepartmentRequirement, String> colRemarks;

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<DepartmentRequirement> requirementData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== DEBUG: DepartmentHeadDashboardController initialized ===");
        setupTableColumns();
        setupRequirementsTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("=== DEBUG: Setting current user ===");
        cleanupDuplicateApprovals();
        
        System.out.println("Username: " + user.getUsername());
        System.out.println("Full Name: " + user.getFullName());
        System.out.println("Role: " + user.getRole());
        System.out.println("Department: " + user.getDepartment());
        
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Department Head");
        loadPendingRequests();
    }
    
    
    
    private void updateDashboardStats() {
        int approvedCount = countApprovedStudents();
        int requirementsPending = countPendingRequirements();
        
        if (lblApprovedCard != null) lblApprovedCard.setText(String.valueOf(approvedCount));
        if (lblRequirementsCard != null) lblRequirementsCard.setText(String.valueOf(requirementsPending));
    }

    private int countApprovedStudents() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT COUNT(DISTINCT cr.student_id) as approved_count
                FROM clearance_approvals ca
                JOIN clearance_requests cr ON ca.request_id = cr.id
                JOIN users u ON cr.student_id = u.id
                WHERE ca.officer_role = 'DEPARTMENT_HEAD'
                AND ca.status = 'APPROVED'
                AND u.department = ?
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getDepartment());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("approved_count");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int countPendingRequirements() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT COUNT(*) as pending_count
                FROM department_requirements dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.department = ?
                AND dr.status IN ('Not Started', 'In Progress')
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getDepartment());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("pending_count");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    
    
    
    @FXML
    private void handleLogout() {
        try {
            System.out.println("[DEBUG] Logout button clicked in Department Head Dashboard.");
            
            // Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout Confirmation");
            confirm.setHeaderText("Confirm Logout");
            confirm.setContentText("Are you sure you want to logout from the Department Head Dashboard?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("[DEBUG] User confirmed logout.");
                
                // Get the logout button or any other UI component to preserve window size
                Button logoutBtn = null; // You can get this from FXML if you add an fx:id
                // For now, use the welcome label to get the scene
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
                stage.setTitle("University Clearance System - Login");
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

    private void setupTableColumns() {
        System.out.println("=== DEBUG: Setting up table columns ===");
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYearLevel.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colGPA.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        colDepartmentStatus.setCellValueFactory(new PropertyValueFactory<>("departmentStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button(" Reject");
            private final Button btnViewRequirements = new Button("📋 Requirements");
            private final HBox buttons = new HBox(5, btnViewRequirements, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                btnViewRequirements.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                
                btnViewRequirements.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    viewStudentRequirements(request);
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
    }

    private void setupRequirementsTableColumns() {
        colRequirement.setCellValueFactory(new PropertyValueFactory<>("requirement"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCompletedDate.setCellValueFactory(new PropertyValueFactory<>("completedDate"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        colStatus.setCellFactory(column -> new TableCell<DepartmentRequirement, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Completed")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equals("In Progress")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        System.out.println("=== DEBUG: Refreshing requests ===");
        loadPendingRequests();
        showAlert("Refreshed", "Department clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Loading pending requests for department: " + currentUser.getDepartment());
            
            // FIXED QUERY: This will find students from the same department who need department head approval
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    u.year_level,
                    DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND u.department = ?
                AND NOT EXISTS (
                    SELECT 1 FROM clearance_approvals ca 
                    WHERE ca.request_id = cr.id 
                    AND ca.officer_role = 'DEPARTMENT_HEAD'
                    AND ca.status = 'APPROVED'
                )
                ORDER BY cr.request_date ASC
                """;
                
            System.out.println("Executing query...");
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getDepartment());
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                pendingCount++;
                String studentId = rs.getString("student_id");
                
                String departmentStatus = checkStudentDepartmentStatus(conn, studentId);
                String gpa = getStudentGPA(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getString("year_level"),
                    gpa,
                    rs.getString("request_date"),
                    departmentStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Department Clearances: " + pendingCount);
            
            // Update dashboard cards
            if (lblPendingCard != null) lblPendingCard.setText(String.valueOf(pendingCount));
            
            System.out.println("Loaded " + pendingCount + " pending requests");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR in loadPendingRequests:");
            e.printStackTrace();
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
        }
    }
    
    
    
    
    private void cleanupDuplicateApprovals() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String cleanupSql = """
                DELETE ca1 
                FROM clearance_approvals ca1
                INNER JOIN clearance_approvals ca2 
                WHERE ca1.id > ca2.id 
                AND ca1.request_id = ca2.request_id 
                AND ca1.officer_role = ca2.officer_role
                AND ca1.officer_role = 'DEPARTMENT_HEAD'
                """;
            
            PreparedStatement ps = conn.prepareStatement(cleanupSql);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("Cleaned up " + deleted + " duplicate DEPARTMENT_HEAD approvals");
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up duplicates: " + e.getMessage());
        }
    }
    
    
    
    
    private void debugMissingStudent(Connection conn, String studentId) throws SQLException {
        System.out.println("\n=== DEBUG: Checking why student " + studentId + " is not appearing ===");
        
        String debugSql = """
            SELECT 
                cr.id as request_id,
                u.username,
                u.full_name,
                u.department,
                cr.status as request_status,
                ca.status as approval_status,
                ca.officer_role,
                CASE 
                    WHEN ca.status IS NULL THEN 'No approval record'
                    WHEN ca.status = 'APPROVED' THEN 'Already approved'
                    ELSE 'Other status: ' || ca.status
                END as reason
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
                AND ca.officer_role = 'DEPARTMENT_HEAD'
            WHERE u.username = ?
            AND cr.status IN ('PENDING', 'IN_PROGRESS')
            """;
        
        PreparedStatement debugStmt = conn.prepareStatement(debugSql);
        debugStmt.setString(1, studentId);
        ResultSet debugRs = debugStmt.executeQuery();
        
        if (debugRs.next()) {
            System.out.println("Request ID: " + debugRs.getInt("request_id"));
            System.out.println("Student: " + debugRs.getString("username"));
            System.out.println("Request Status: " + debugRs.getString("request_status"));
            System.out.println("Approval Status: " + debugRs.getString("approval_status"));
            System.out.println("Officer Role: " + debugRs.getString("officer_role"));
            System.out.println("Reason: " + debugRs.getString("reason"));
        } else {
            System.out.println("No pending/in-progress requests found for student " + studentId);
        }
    }
    
    
    private void debugDatabaseStructure(Connection conn) throws SQLException {
        System.out.println("\n=== DEBUG: Database Structure ===");
        
        // Check clearance_requests table
        System.out.println("\n1. clearance_requests table:");
        String checkRequestsSql = "SELECT COUNT(*) as count FROM clearance_requests WHERE status IN ('PENDING', 'IN_PROGRESS')";
        PreparedStatement checkRequestsStmt = conn.prepareStatement(checkRequestsSql);
        ResultSet requestsRs = checkRequestsStmt.executeQuery();
        if (requestsRs.next()) {
            System.out.println("   Total pending/in-progress requests: " + requestsRs.getInt("count"));
        }
        
        // Check users in same department
        System.out.println("\n2. Users in same department (" + currentUser.getDepartment() + "):");
        String checkUsersSql = "SELECT username, full_name, role FROM users WHERE department = ?";
        PreparedStatement checkUsersStmt = conn.prepareStatement(checkUsersSql);
        checkUsersStmt.setString(1, currentUser.getDepartment());
        ResultSet usersRs = checkUsersStmt.executeQuery();
        int userCount = 0;
        while (usersRs.next()) {
            userCount++;
            System.out.println("   User " + userCount + ": " + usersRs.getString("username") + 
                             " - " + usersRs.getString("full_name") + 
                             " (" + usersRs.getString("role") + ")");
        }
        
        // Check clearance_approvals for DEPARTMENT_HEAD
        System.out.println("\n3. clearance_approvals for DEPARTMENT_HEAD:");
        String checkApprovalsSql = """
            SELECT ca.request_id, ca.status, u.username as student_id, u.full_name
            FROM clearance_approvals ca
            JOIN clearance_requests cr ON ca.request_id = cr.id
            JOIN users u ON cr.student_id = u.id
            WHERE ca.officer_role = 'DEPARTMENT_HEAD'
            """;
        PreparedStatement checkApprovalsStmt = conn.prepareStatement(checkApprovalsSql);
        ResultSet approvalsRs = checkApprovalsStmt.executeQuery();
        int approvalCount = 0;
        while (approvalsRs.next()) {
            approvalCount++;
            System.out.println("   Approval " + approvalCount + ": Request ID=" + approvalsRs.getInt("request_id") +
                             ", Student=" + approvalsRs.getString("student_id") +
                             ", Status=" + approvalsRs.getString("status"));
        }
        
     // In the debugDatabaseStructure method, update section 4:
        System.out.println("\n4. Requests missing DEPARTMENT_HEAD approvals:");
        String missingApprovalsSql = """
            SELECT cr.id, u.username, u.full_name, u.department, cr.status as request_status
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
            AND NOT EXISTS (
                SELECT 1 FROM clearance_approvals ca 
                WHERE ca.request_id = cr.id 
                AND ca.officer_role = 'DEPARTMENT_HEAD'
            )
            """;
        PreparedStatement missingApprovalsStmt = conn.prepareStatement(missingApprovalsSql);
        ResultSet missingRs = missingApprovalsStmt.executeQuery();
        int missingCount = 0;
        while (missingRs.next()) {
            missingCount++;
            System.out.println("   Missing approval " + missingCount + ": Request ID=" + missingRs.getInt("id") +
                             ", Student=" + missingRs.getString("username") +
                             ", Department=" + missingRs.getString("department") +
                             ", Request Status=" + missingRs.getString("request_status"));
        }
        
        System.out.println("\n=== DEBUG SUMMARY ===");
        System.out.println("Users in department: " + userCount);
        System.out.println("Existing DEPARTMENT_HEAD approvals: " + approvalCount);
        System.out.println("Missing DEPARTMENT_HEAD approvals: " + missingCount);
    }

    private String checkStudentDepartmentStatus(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking department status for student: " + studentId);
        
        createDepartmentRequirementsIfNeeded(conn, studentId);
        
        String requirementsSql = """
            SELECT 
                COUNT(*) as total_requirements,
                SUM(CASE WHEN dr.status = 'Completed' THEN 1 ELSE 0 END) as completed_count,
                SUM(CASE WHEN dr.status = 'Not Started' THEN 1 ELSE 0 END) as not_started_count
            FROM department_requirements dr
            JOIN users u ON dr.student_id = u.id
            WHERE u.username = ?
            """;
            
        System.out.println("DEBUG: Requirements SQL: " + requirementsSql);
        System.out.println("DEBUG: Student ID parameter: " + studentId);
        
        PreparedStatement ps = conn.prepareStatement(requirementsSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int totalRequirements = rs.getInt("total_requirements");
            int completedCount = rs.getInt("completed_count");
            int notStartedCount = rs.getInt("not_started_count");
            
            System.out.println("  Requirements - Total: " + totalRequirements + 
                             ", Completed: " + completedCount + 
                             ", Not Started: " + notStartedCount);
            
            if (completedCount == totalRequirements) {
                return "✅ All Department Requirements Completed";
            } else if (completedCount > 0) {
                return "⚠️ " + completedCount + "/" + totalRequirements + " Requirements Completed";
            } else {
                return "❌ No Requirements Completed (" + notStartedCount + " pending)";
            }
        }
        
        return "Pending Department Review";
    }

    private String getStudentGPA(Connection conn, String studentId) throws SQLException {
        String gpaSql = """
            SELECT 
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
                END) as gpa
            FROM student_courses sc
            JOIN users u ON sc.student_id = u.id
            WHERE u.username = ?
            GROUP BY u.id
            """;
            
        System.out.println("DEBUG: GPA SQL: " + gpaSql);
        System.out.println("DEBUG: Student ID parameter: " + studentId);
        
        PreparedStatement ps = conn.prepareStatement(gpaSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            double gpa = rs.getDouble("gpa");
            System.out.println("  GPA calculated: " + gpa);
            return String.format("%.2f", gpa);
        }
        
        System.out.println("  GPA not found, returning N/A");
        return "N/A";
    }

    private void createDepartmentRequirementsIfNeeded(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking/creating department requirements for: " + studentId);
        
        // Check if department_requirements table exists
        try {
            String checkTableSql = "SELECT 1 FROM department_requirements LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
            System.out.println("  department_requirements table exists");
        } catch (SQLException e) {
            System.out.println("  department_requirements table doesn't exist, creating...");
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS department_requirements (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    requirement_name VARCHAR(100),
                    requirement_type VARCHAR(50),
                    status VARCHAR(20),
                    completed_date DATE,
                    remarks TEXT,
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            System.out.println("  Created department_requirements table");
        }
        
        // Check if student has requirements
        String checkRecordsSql = """
            SELECT COUNT(*) FROM department_requirements dr 
            JOIN users u ON dr.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        int existingCount = rs.getInt(1);
        
        System.out.println("  Existing requirements count: " + existingCount);
        
        if (existingCount == 0) {
            System.out.println("  No requirements found, creating default requirements...");
            
            // Get student department
            String deptSql = "SELECT department FROM users WHERE username = ?";
            PreparedStatement deptStmt = conn.prepareStatement(deptSql);
            deptStmt.setString(1, studentId);
            ResultSet deptRs = deptStmt.executeQuery();
            
            String department = "General";
            if (deptRs.next()) {
                department = deptRs.getString("department");
            }
            System.out.println("  Student department: " + department);
            
            // Insert department-specific requirements
            String[][] requirements = getDepartmentRequirements(department);
            
            String insertSql = """
                INSERT INTO department_requirements (student_id, requirement_name, requirement_type, status, remarks)
                SELECT id, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            for (String[] requirement : requirements) {
                insertStmt.setString(1, requirement[0]);
                insertStmt.setString(2, requirement[1]);
                insertStmt.setString(3, requirement[2]);
                insertStmt.setString(4, requirement[3]);
                insertStmt.setString(5, studentId);
                insertStmt.addBatch();
            }
            int[] results = insertStmt.executeBatch();
            System.out.println("  Created " + results.length + " requirement records");
        }
    }

    private String[][] getDepartmentRequirements(String department) {
        System.out.println("  Getting requirements for department: " + department);
        // Define requirements based on department
        switch (department) {
            case "Computer Science":
                return new String[][]{
                    {"Final Year Project", "Project", "Completed", "Successfully defended"},
                    {"Internship Completion", "Internship", "Completed", "6-month industry internship"},
                    {"Programming Portfolio", "Portfolio", "Completed", "GitHub portfolio reviewed"},
                    {"Ethics Certification", "Certification", "Completed", "CS ethics course completed"},
                    {"Department Seminar", "Seminar", "In Progress", "Scheduled for next month"}
                };
            case "Electrical Engineering":
                return new String[][]{
                    {"Senior Design Project", "Project", "Completed", "Hardware prototype built"},
                    {"Lab Safety Certification", "Certification", "Completed", "Safety training completed"},
                    {"Circuit Design Portfolio", "Portfolio", "Completed", "Design portfolio submitted"},
                    {"Internship Program", "Internship", "Completed", "Industry placement done"},
                    {"Technical Report", "Documentation", "Not Started", "Due end of semester"}
                };
            case "Mechanical Engineering":
                return new String[][]{
                    {"Capstone Design Project", "Project", "Completed", "Design validated"},
                    {"CAD Certification", "Certification", "Completed", "AutoCAD certified"},
                    {"Laboratory Hours", "Practical", "Completed", "All lab hours completed"},
                    {"Technical Drawing Portfolio", "Portfolio", "In Progress", "80% completed"},
                    {"Senior Thesis", "Thesis", "Not Started", "Topic approved"}
                };
            default:
                return new String[][]{
                    {"Final Project", "Project", "Completed", "Project submitted"},
                    {"Internship", "Internship", "In Progress", "Ongoing"},
                    {"Portfolio Review", "Portfolio", "Not Started", "Pending submission"},
                    {"Department Exam", "Examination", "Completed", "Passed with 85%"}
                };
        }
    }

    private void viewStudentRequirements(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Viewing requirements for student: " + request.getStudentId());
        loadStudentRequirements(request.getStudentId());
        lblStudentInfo.setText("Department Requirements for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - " + request.getDepartment());
        
        updateRequirementsSummary(request.getStudentId());
        
        // Auto-switch to Department Requirements tab
        for (Tab tab : mainTabPane.getTabs()) {
            if ("Department Requirements".equals(tab.getText())) {
                mainTabPane.getSelectionModel().select(tab);
                System.out.println("Switched to Department Requirements tab");
                break;
            }
        }
    }

    private void loadStudentRequirements(String studentId) {
        System.out.println("DEBUG: Loading requirements for student: " + studentId);
        requirementData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    dr.requirement_name,
                    dr.requirement_type,
                    dr.status as requirement_status,
                    dr.completed_date,
                    dr.remarks
                FROM department_requirements dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                ORDER BY dr.requirement_type, dr.requirement_name
                """;
                
            System.out.println("DEBUG: Load requirements SQL: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                DepartmentRequirement requirement = new DepartmentRequirement(
                    rs.getString("requirement_name"),
                    rs.getString("requirement_status"),  // Changed from "status" to "requirement_status"
                    rs.getDate("completed_date") != null ? rs.getDate("completed_date").toString() : "Not Completed",
                    rs.getString("remarks")
                );
                requirementData.add(requirement);
            }
            
            tableRequirements.setItems(requirementData);
            System.out.println("Loaded " + count + " requirements");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR loading requirements: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load department requirements: " + e.getMessage());
        }
    }

    
    private void debugStudentDepartmentInfo(Connection conn) throws SQLException {
        System.out.println("\n=== DEBUG: Checking all students with PENDING/IN_PROGRESS requests ===");
        
        String sql = """
            SELECT DISTINCT
                u.username,
                u.full_name,
                u.department,
                cr.status as request_status,
                COUNT(cr.id) as request_count
            FROM users u
            JOIN clearance_requests cr ON u.id = cr.student_id
            WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
            GROUP BY u.username, u.full_name, u.department, cr.status
            ORDER BY u.department, u.username
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        
        System.out.println("All students with pending/in-progress requests:");
        System.out.println("Username\tName\t\t\tDepartment\tRequest Status\tCount");
        System.out.println("-------------------------------------------------------------------");
        
        while (rs.next()) {
            System.out.println(String.format("%-10s\t%-20s\t%-15s\t%-15s\t%d",
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("department"),
                rs.getString("request_status"),
                rs.getInt("request_count")
            ));
        }
    }
    
    private void updateRequirementsSummary(String studentId) {
        System.out.println("DEBUG: Updating requirements summary for: " + studentId);
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String sql = """
        		    SELECT 
        		        cr.id as request_id,
        		        u.username as student_id,
        		        u.full_name as student_name,
        		        u.department,
        		        u.year_level,
        		        DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date,
        		        COALESCE(ca.status, 'PENDING') as approval_status
        		    FROM clearance_requests cr
        		    JOIN users u ON cr.student_id = u.id
        		    LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
        		        AND ca.officer_role = 'DEPARTMENT_HEAD'
        		    WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
        		    AND u.department = ?
        		    AND (ca.status IS NULL OR ca.status = 'PENDING')
        		    ORDER BY cr.request_date ASC
        		    """;
                
            System.out.println("DEBUG: Requirements summary SQL: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                int completed = rs.getInt("completed");
                int inProgress = rs.getInt("in_progress");
                int notStarted = rs.getInt("not_started");
                
                String summary = String.format("Requirements: %d/%d Completed | %d In Progress | %d Not Started", 
                    completed, total, inProgress, notStarted);
                
                lblDepartmentRequirements.setText(summary);
                System.out.println("Requirements summary: " + summary);
                
                if (completed == total) {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (completed > 0) {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR updating requirements summary: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void approveClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Approving clearance for student: " + request.getStudentId());
        
        // Check if student has completed all requirements
        if (request.getDepartmentStatus().contains("❌") || request.getDepartmentStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Department Requirements Issue");
            warning.setHeaderText("Student Has Incomplete Requirements");
            warning.setContentText("This student has incomplete department requirements:\n\n" + 
                                 request.getDepartmentStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                System.out.println("Approval cancelled by user");
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Department Clearance");
        confirmation.setHeaderText("Approve Department Clearance");
        confirmation.setContentText("Approve department clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\nDepartment: " + request.getDepartment() +
                                  "\nGPA: " + request.getGpa());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User confirmed approval");
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Department clearance approved. All requirements verified.");
                loadPendingRequests();
                showAlert("Approved", "Department clearance approved for " + request.getStudentName());
            } else {
                System.out.println("User cancelled approval");
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Rejecting clearance for student: " + request.getStudentId());
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Department Clearance");
        dialog.setHeaderText("Reject Department Clearance");
        dialog.setContentText("Enter reason for rejecting department clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            System.out.println("Rejection reason: " + result.get());
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Department clearance rejected: " + result.get().trim());
            loadPendingRequests();
            showAlert("Rejected", "Department clearance rejected for " + request.getStudentName());
        } else {
            System.out.println("Rejection cancelled");
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        System.out.println("\n=== DEBUG: Updating clearance status ===");
        System.out.println("Request ID: " + requestId);
        System.out.println("Status: " + status);
        System.out.println("Remarks: " + remarks);
        System.out.println("Current User ID: " + currentUser.getId());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = "SELECT COUNT(*) FROM clearance_approvals WHERE request_id = ? AND officer_role = 'DEPARTMENT_HEAD'";
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
                    WHERE request_id = ? AND officer_role = 'DEPARTMENT_HEAD'
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
                    VALUES (?, 'DEPARTMENT_HEAD', ?, ?, ?, NOW())
                    """;
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setInt(1, requestId);
                ps.setInt(2, currentUser.getId());
                ps.setString(3, status);
                ps.setString(4, remarks);
                int inserted = ps.executeUpdate();
                System.out.println("Inserted " + inserted + " record(s)");
            }
            
            System.out.println("Clearance status updated successfully");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR updating clearance status: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
        }
    }

    @FXML
    private void generateDepartmentReport() {
        System.out.println("\n=== DEBUG: Generating department report ===");
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No student selected");
            showAlert("Selection Required", "Please select a student first to generate department report.");
            return;
        }
        
        System.out.println("Generating report for: " + selected.getStudentId());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    u.department,
                    u.year_level,
                    COUNT(dr.id) as total_requirements,
                    SUM(CASE WHEN dr.status = 'Completed' THEN 1 ELSE 0 END) as completed_requirements
                FROM users u
                LEFT JOIN department_requirements dr ON u.id = dr.student_id
                WHERE u.username = ?
                GROUP BY u.id
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "🎓 DEPARTMENT CLEARANCE REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Department: " + rs.getString("department") + "\n" +
                              "Year Level: " + rs.getString("year_level") + "\n" +
                              "Requirements Completed: " + rs.getInt("completed_requirements") + 
                              "/" + rs.getInt("total_requirements") + "\n" +
                              "GPA: " + selected.getGpa() + "\n\n" +
                              "Generated by: " + currentUser.getFullName() + 
                              " (Department Head)";
                
                System.out.println("Report generated successfully");
                showAlert("Department Report", report);
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR generating report: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to generate department report: " + e.getMessage());
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
        private final String gpa;
        private final String requestDate;
        private final String departmentStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String yearLevel, String gpa, String requestDate, 
                               String departmentStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.yearLevel = yearLevel;
            this.gpa = gpa;
            this.requestDate = requestDate;
            this.departmentStatus = departmentStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getYearLevel() { return yearLevel; }
        public String getGpa() { return gpa; }
        public String getRequestDate() { return requestDate; }
        public String getDepartmentStatus() { return departmentStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class DepartmentRequirement {
        private final String requirement;
        private final String status;
        private final String completedDate;
        private final String remarks;

        public DepartmentRequirement(String requirement, String status, String completedDate, String remarks) {
            this.requirement = requirement;
            this.status = status;
            this.completedDate = completedDate;
            this.remarks = remarks;
        }

        public String getRequirement() { return requirement; }
        public String getStatus() { return status; }
        public String getCompletedDate() { return completedDate; }
        public String getRemarks() { return remarks; }
    }
}