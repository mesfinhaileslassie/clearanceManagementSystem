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

public class RegistrarDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    
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

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<AcademicRecord> academicData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupAcademicTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Registrar Office");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYearLevel.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colAcademicStatus.setCellValueFactory(new PropertyValueFactory<>("academicStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Verify/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Verify Clear");
            private final Button btnReject = new Button("❌ Hold Records");
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
                    // Only show buttons if pending
                    if ("Pending Verification".equals(request.getAcademicStatus())) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
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
                    if (item.equals("F") || item.equals("D")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.equals("C")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        loadPendingRequests();
        showAlert("Refreshed", "Registrar clearance requests refreshed successfully!");
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
                WHERE ca.officer_role = 'REGISTRAR' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String academicStatus = checkStudentAcademicStatus(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getString("year_level"),
                    rs.getTimestamp("request_date").toString(),
                    academicStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Verifications: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String checkStudentAcademicStatus(Connection conn, String studentId) throws SQLException {
        // Check if student has any academic holds
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
            
            if (!"NONE".equals(holdStatus)) {
                return "❌ Academic Hold: " + holdStatus;
            } else if (fees > 0) {
                return "❌ Outstanding Fees: $" + fees;
            } else if (incompletes > 0) {
                return "❌ " + incompletes + " Incomplete Course(s)";
            } else if (gpa < 2.0) {
                return "⚠️ Low GPA: " + gpa;
            } else {
                return "✅ Academically Clear";
            }
        }
        
        return "Pending Verification";
    }

    private void viewStudentRecords(ClearanceRequest request) {
        loadStudentAcademicDetails(request.getStudentId());
        lblStudentInfo.setText("Academic Records for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - " + request.getDepartment());
    }

    private void loadStudentAcademicDetails(String studentId) {
        academicData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, check if academic records table exists, if not create sample data
            createSampleAcademicDataIfNeeded(conn, studentId);
            
            String sql = """
                SELECT 
                    course_code,
                    course_name,
                    grade,
                    credits,
                    semester,
                    academic_year
                FROM student_courses sc
                JOIN users u ON sc.student_id = u.id
                WHERE u.username = ?
                ORDER BY academic_year DESC, semester DESC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                AcademicRecord record = new AcademicRecord(
                    rs.getString("course_code") + " - " + rs.getString("course_name"),
                    rs.getString("grade"),
                    String.valueOf(rs.getInt("credits")),
                    rs.getString("semester") + " " + rs.getString("academic_year")
                );
                academicData.add(record);
            }
            
            tableAcademicRecords.setItems(academicData);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load academic records: " + e.getMessage());
        }
    }

    private void createSampleAcademicDataIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if student_courses table exists
        try {
            String checkTableSql = "SELECT 1 FROM student_courses LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
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
        }
        
        // Check if student has records, if not insert sample data
        String checkRecordsSql = """
            SELECT COUNT(*) FROM student_courses sc 
            JOIN users u ON sc.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Insert sample academic records
            String insertSql = """
                INSERT INTO student_courses (student_id, course_code, course_name, grade, credits, semester, academic_year)
                SELECT id, ?, ?, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            String[][] sampleCourses = {
                {"CS101", "Introduction to Programming", "A", "3", "Fall", "2023"},
                {"MATH201", "Calculus I", "B+", "4", "Fall", "2023"},
                {"PHY101", "General Physics", "A-", "3", "Fall", "2023"},
                {"CS201", "Data Structures", "B", "3", "Spring", "2024"},
                {"MATH202", "Calculus II", "C+", "4", "Spring", "2024"},
                {"ENG101", "English Composition", "A", "3", "Spring", "2024"}
            };
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            for (String[] course : sampleCourses) {
                insertStmt.setString(1, course[0]);
                insertStmt.setString(2, course[1]);
                insertStmt.setString(3, course[2]);
                insertStmt.setInt(4, Integer.parseInt(course[3]));
                insertStmt.setString(5, course[4]);
                insertStmt.setString(6, course[5]);
                insertStmt.setString(7, studentId);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Academic Clearance");
        confirmation.setHeaderText("Verify Student Academic Clearance");
        confirmation.setContentText("Approve academic clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\n\nThis confirms the student has no academic holds or outstanding issues.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Academic records verified - No holds or outstanding issues.");
                loadPendingRequests();
                showAlert("Approved", "Academic clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Academic Clearance");
        dialog.setHeaderText("Place Academic Hold");
        dialog.setContentText("Enter reason for academic hold for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Academic hold: " + result.get().trim());
            
            // Also update academic records with hold
            placeAcademicHold(request.getStudentId(), result.get().trim());
            
            loadPendingRequests();
            showAlert("Rejected", "Academic clearance rejected for " + request.getStudentName());
        }
    }

    private void placeAcademicHold(String studentId, String reason) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Create academic records table if not exists
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS student_academic_records (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT UNIQUE,
                    academic_hold VARCHAR(100),
                    outstanding_fees DECIMAL(10,2),
                    incomplete_courses INT,
                    gpa DECIMAL(3,2),
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            
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
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'REGISTRAR'
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
    private void generateAcademicReport() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to generate academic report.");
            return;
        }
        
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
                    END) as gpa
                FROM users u
                LEFT JOIN student_courses sc ON u.id = sc.student_id
                WHERE u.username = ?
                GROUP BY u.id
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "📊 ACADEMIC REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Department: " + rs.getString("department") + "\n" +
                              "Year Level: " + rs.getString("year_level") + "\n" +
                              "Total Courses: " + rs.getInt("total_courses") + "\n" +
                              "GPA: " + String.format("%.2f", rs.getDouble("gpa")) + "\n\n" +
                              "Generated by: " + currentUser.getFullName();
                
                showAlert("Academic Report", report);
            }
            
        } catch (Exception e) {
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