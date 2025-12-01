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

public class DepartmentHeadDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private Label lblDepartmentRequirements;
    
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

 // Add this to your DepartmentHeadDashboardController class
    @FXML private TabPane mainTabPane;
    
    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<DepartmentRequirement> requirementData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupRequirementsTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Department Head");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYearLevel.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colGPA.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        colDepartmentStatus.setCellValueFactory(new PropertyValueFactory<>("departmentStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Verify/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject = new Button("❌ Reject");
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
                    // Only show buttons if pending
                    if ("Pending Department Review".equals(request.getDepartmentStatus()) || 
                        request.getDepartmentStatus().contains("Pending")) {
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
        
        // Color code status
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
        loadPendingRequests();
        showAlert("Refreshed", "Department clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    u.year_level,
                    cr.request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                JOIN clearance_approvals ca ON cr.id = ca.request_id 
                WHERE ca.officer_role = 'DEPARTMENT_HEAD' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                AND u.department = ?                              -- Only students from same department
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUser.getDepartment());
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String departmentStatus = checkStudentDepartmentStatus(conn, studentId);
                String gpa = getStudentGPA(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getString("year_level"),
                    gpa,
                    rs.getTimestamp("request_date").toString(),
                    departmentStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Department Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String checkStudentDepartmentStatus(Connection conn, String studentId) throws SQLException {
        // Check if student has completed all department requirements
        createDepartmentRequirementsIfNeeded(conn, studentId);
        
        String requirementsSql = """
            SELECT 
                COUNT(*) as total_requirements,
                SUM(CASE WHEN status = 'Completed' THEN 1 ELSE 0 END) as completed_count,
                SUM(CASE WHEN status = 'Not Started' THEN 1 ELSE 0 END) as not_started_count
            FROM department_requirements dr
            JOIN users u ON dr.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement ps = conn.prepareStatement(requirementsSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int totalRequirements = rs.getInt("total_requirements");
            int completedCount = rs.getInt("completed_count");
            int notStartedCount = rs.getInt("not_started_count");
            
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
            
        PreparedStatement ps = conn.prepareStatement(gpaSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            double gpa = rs.getDouble("gpa");
            return String.format("%.2f", gpa);
        }
        
        return "N/A";
    }

    private void createDepartmentRequirementsIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if department_requirements table exists
        try {
            String checkTableSql = "SELECT 1 FROM department_requirements LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
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
        }
        
        // Check if student has requirements, if not insert based on department
        String checkRecordsSql = """
            SELECT COUNT(*) FROM department_requirements dr 
            JOIN users u ON dr.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Get student department to create relevant requirements
            String deptSql = "SELECT department FROM users WHERE username = ?";
            PreparedStatement deptStmt = conn.prepareStatement(deptSql);
            deptStmt.setString(1, studentId);
            ResultSet deptRs = deptStmt.executeQuery();
            
            String department = "General";
            if (deptRs.next()) {
                department = deptRs.getString("department");
            }
            
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
            insertStmt.executeBatch();
        }
    }

    private String[][] getDepartmentRequirements(String department) {
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
        loadStudentRequirements(request.getStudentId());
        lblStudentInfo.setText("Department Requirements for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - " + request.getDepartment());
        
        // Update requirements summary
        updateRequirementsSummary(request.getStudentId());
    }

    private void loadStudentRequirements(String studentId) {
        requirementData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    requirement_name,
                    requirement_type,
                    status,
                    completed_date,
                    remarks
                FROM department_requirements dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                ORDER BY requirement_type, requirement_name
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                DepartmentRequirement requirement = new DepartmentRequirement(
                    rs.getString("requirement_name"),
                    rs.getString("status"),
                    rs.getDate("completed_date") != null ? rs.getDate("completed_date").toString() : "Not Completed",
                    rs.getString("remarks")
                );
                requirementData.add(requirement);
            }
            
            tableRequirements.setItems(requirementData);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load department requirements: " + e.getMessage());
        }
    }

    private void updateRequirementsSummary(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'Completed' THEN 1 ELSE 0 END) as completed,
                    SUM(CASE WHEN status = 'In Progress' THEN 1 ELSE 0 END) as in_progress,
                    SUM(CASE WHEN status = 'Not Started' THEN 1 ELSE 0 END) as not_started
                FROM department_requirements dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                """;
                
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
                
                if (completed == total) {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (completed > 0) {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    lblDepartmentRequirements.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
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
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Department clearance approved. All requirements verified.");
                loadPendingRequests(); // Refresh table to remove approved request
                showAlert("Approved", "Department clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Department Clearance");
        dialog.setHeaderText("Reject Department Clearance");
        dialog.setContentText("Enter reason for rejecting department clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Department clearance rejected: " + result.get().trim());
            loadPendingRequests(); // Refresh table to remove rejected request
            showAlert("Rejected", "Department clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'DEPARTMENT_HEAD'
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, remarks);
            ps.setInt(3, currentUser.getId());
            ps.setInt(4, requestId);
            
            ps.executeUpdate();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void generateDepartmentReport() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to generate department report.");
            return;
        }
        
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
                
                showAlert("Department Report", report);
            }
            
        } catch (Exception e) {
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