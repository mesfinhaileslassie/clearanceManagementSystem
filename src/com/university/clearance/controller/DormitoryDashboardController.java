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

public class DormitoryDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private Label lblDormitoryStatus;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colRoomNumber;
    @FXML private TableColumn<ClearanceRequest, String> colDormitoryStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<DormitoryRecord> tableDormitoryRecords;
    @FXML private TableColumn<DormitoryRecord, String> colRecordType;
    @FXML private TableColumn<DormitoryRecord, String> colDescription;
    @FXML private TableColumn<DormitoryRecord, String> colAmount;
    @FXML private TableColumn<DormitoryRecord, String> colDueDate;
    @FXML private TableColumn<DormitoryRecord, String> colRecordStatus;

    @FXML private Label lblDashboardPending;
    @FXML private Label lblDashboardCleared;
    @FXML private Label lblFees;
    
    @FXML private TabPane mainTabPane;
   
    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<DormitoryRecord> dormitoryData = FXCollections.observableArrayList();
    private boolean tablesInitialized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== DEBUG: DormitoryDashboardController initialized ===");
        
        if (tableRequests != null) {
            tableRequests.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (tableDormitoryRecords != null) {
            tableDormitoryRecords.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("=== DEBUG: Setting current user for Dormitory ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Full Name: " + user.getFullName());
        System.out.println("Role: " + user.getRole());
        
        // Initialize dormitory records table if needed
        initializeDormitoryTable();
        
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + user.getFullName() + " - Dormitory Office");
        }
        
        initializeTablesIfNeeded();
        loadPendingRequests();
        
        // Check if we need to create test data
        checkAndCreateTestData();
    }
    
    private void initializeDormitoryTable() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("=== DEBUG: Checking/creating dormitory_records table ===");
            
            // Check if table exists
            try {
                String checkSql = "SELECT 1 FROM dormitory_records LIMIT 1";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.executeQuery();
                System.out.println("✅ dormitory_records table exists");
            } catch (SQLException e) {
                System.out.println("⚠️ dormitory_records table doesn't exist, creating...");
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS dormitory_records (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        student_id INT,
                        record_type VARCHAR(50),
                        description TEXT,
                        amount DECIMAL(10,2),
                        due_date DATE,
                        status VARCHAR(20),
                        room_number VARCHAR(10),
                        created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (student_id) REFERENCES users(id)
                    )
                    """;
                PreparedStatement createStmt = conn.prepareStatement(createTableSql);
                createStmt.executeUpdate();
                System.out.println("✅ Created dormitory_records table");
            }
        } catch (Exception e) {
            System.err.println("Error initializing dormitory table: " + e.getMessage());
        }
    }
    
    private void checkAndCreateTestData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("\n=== DEBUG: Checking if we need test data ===");
            
            // Check how many students have DORMITORY approvals
            String checkApprovalsSql = """
                SELECT COUNT(*) as count 
                FROM clearance_approvals 
                WHERE officer_role = 'DORMITORY' 
                AND status = 'PENDING'
                """;
            PreparedStatement checkStmt = conn.prepareStatement(checkApprovalsSql);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int pendingCount = rs.getInt("count");
            
            System.out.println("Pending DORMITORY approvals: " + pendingCount);
            
            if (pendingCount == 0) {
                System.out.println("⚠️ No pending DORMITORY approvals found. Creating test data...");
                createTestClearanceRequests();
            }
        } catch (Exception e) {
            System.err.println("Error checking test data: " + e.getMessage());
        }
    }
    
    private void createTestClearanceRequests() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("\n=== DEBUG: Creating test clearance requests for Dormitory ===");
            
            // Get some random students
            String studentsSql = """
                SELECT u.id, u.username, u.full_name 
                FROM users u 
                WHERE u.role = 'STUDENT' 
                LIMIT 3
                """;
            
            PreparedStatement studentsStmt = conn.prepareStatement(studentsSql);
            ResultSet studentsRs = studentsStmt.executeQuery();
            
            int createdCount = 0;
            while (studentsRs.next()) {
                int studentId = studentsRs.getInt("id");
                String username = studentsRs.getString("username");
                
                // Check if student already has a pending clearance request
                String checkRequestSql = """
                    SELECT cr.id 
                    FROM clearance_requests cr 
                    WHERE cr.student_id = ? 
                    AND cr.status IN ('PENDING', 'IN_PROGRESS')
                    LIMIT 1
                    """;
                
                PreparedStatement checkRequestStmt = conn.prepareStatement(checkRequestSql);
                checkRequestStmt.setInt(1, studentId);
                ResultSet requestRs = checkRequestStmt.executeQuery();
                
                if (!requestRs.next()) {
                    // Create clearance request
                    String insertRequestSql = """
                        INSERT INTO clearance_requests (student_id, request_date, status, remarks)
                        VALUES (?, NOW(), 'PENDING', 'Test clearance request for dormitory')
                        """;
                    
                    PreparedStatement insertRequestStmt = conn.prepareStatement(insertRequestSql, Statement.RETURN_GENERATED_KEYS);
                    insertRequestStmt.setInt(1, studentId);
                    insertRequestStmt.executeUpdate();
                    
                    ResultSet keys = insertRequestStmt.getGeneratedKeys();
                    if (keys.next()) {
                        int requestId = keys.getInt(1);
                        
                        // Create DORMITORY approval record
                        String insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, officer_id, status, remarks)
                            VALUES (?, 'DORMITORY', NULL, 'PENDING', 'Awaiting dormitory clearance')
                            """;
                        
                        PreparedStatement insertApprovalStmt = conn.prepareStatement(insertApprovalSql);
                        insertApprovalStmt.setInt(1, requestId);
                        insertApprovalStmt.executeUpdate();
                        
                        createdCount++;
                        System.out.println("✅ Created test request for: " + username + " (ID: " + requestId + ")");
                        
                        // Create dormitory records for this student
                        createStudentDormitoryRecords(conn, username);
                    }
                }
            }
            
            System.out.println("✅ Created " + createdCount + " test clearance requests");
            
            if (createdCount > 0) {
                // Refresh the table
                loadPendingRequests();
                showAlert("Test Data Created", "Created " + createdCount + " test clearance requests for dormitory review.");
            }
            
        } catch (Exception e) {
            System.err.println("Error creating test clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createStudentDormitoryRecords(Connection conn, String studentUsername) throws SQLException {
        System.out.println("Creating dormitory records for: " + studentUsername);
        
        // Check if student already has dormitory records
        String checkRecordsSql = """
            SELECT COUNT(*) FROM dormitory_records dr
            JOIN users u ON dr.student_id = u.id
            WHERE u.username = ?
            """;
        PreparedStatement checkStmt = conn.prepareStatement(checkRecordsSql);
        checkStmt.setString(1, studentUsername);
        ResultSet rs = checkStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Generate room number
            String roomNumber = "DORM-" + (100 + (int)(Math.random() * 400));
            
            // Insert room assignment
            String insertSql = """
                INSERT INTO dormitory_records (student_id, record_type, description, amount, due_date, status, room_number)
                SELECT id, ?, ?, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            // Room assignment
            insertStmt.setString(1, "ROOM_ASSIGNMENT");
            insertStmt.setString(2, "Dormitory Room Assignment");
            insertStmt.setDouble(3, 0.00);
            insertStmt.setDate(4, null);
            insertStmt.setString(5, "Active");
            insertStmt.setString(6, roomNumber);
            insertStmt.setString(7, studentUsername);
            insertStmt.executeUpdate();
            
            // Sample dormitory issues (with some randomness)
            String[][] records = {
                {"OUTSTANDING_FEE", "Monthly Dormitory Fee", "500.00", getFutureDate(15), "Pending"},
                {"OUTSTANDING_FEE", "Late Payment Penalty", "50.00", getPastDate(5), "Overdue"},
                {"DAMAGE", "Broken Window in Room", "200.00", getFutureDate(30), "Pending"},
                {"DAMAGE", "Damaged Furniture", "150.00", getFutureDate(30), "Pending"},
                {"VIOLATION", "Quiet Hours Violation", "0.00", getFutureDate(7), "Pending"},
                {"VIOLATION", "Unauthorized Guest", "25.00", getFutureDate(7), "Pending"},
                {"OUTSTANDING_FEE", "Utilities Fee", "75.00", getFutureDate(10), "Pending"}
            };
            
            for (String[] record : records) {
                if (Math.random() < 0.5) { // 50% chance to add each record
                    insertStmt.setString(1, record[0]);
                    insertStmt.setString(2, record[1]);
                    insertStmt.setDouble(3, Double.parseDouble(record[2]));
                    insertStmt.setDate(4, Date.valueOf(record[3]));
                    insertStmt.setString(5, record[4]);
                    insertStmt.setString(6, null);
                    insertStmt.setString(7, studentUsername);
                    insertStmt.executeUpdate();
                }
            }
            
            System.out.println("  Created dormitory records for " + studentUsername + " (Room: " + roomNumber + ")");
        }
    }
    
    private String getFutureDate(int days) {
        return java.time.LocalDate.now().plusDays(days).toString();
    }
    
    private String getPastDate(int days) {
        return java.time.LocalDate.now().minusDays(days).toString();
    }
    
    private void initializeTablesIfNeeded() {
        if (!tablesInitialized) {
            System.out.println("=== DEBUG: Initializing table columns ===");
            
            if (colStudentId != null && colStudentName != null && colDepartment != null &&
                colRoomNumber != null && colDormitoryStatus != null && colRequestDate != null &&
                colActions != null) {
                
                setupTableColumns();
                setupDormitoryTableColumns();
                tablesInitialized = true;
                System.out.println("✅ Table columns initialized successfully");
            } else {
                System.err.println("❌ Some table columns are null");
            }
        }
    }

    private void setupTableColumns() {
        try {
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
            colDormitoryStatus.setCellValueFactory(new PropertyValueFactory<>("dormitoryStatus"));
            colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
            
            colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
                private final Button btnApprove = new Button("✅ Approve");
                private final Button btnReject = new Button("❌ Reject");
                private final Button btnViewDetails = new Button("🏠 View Records");
                private final HBox buttons = new HBox(5, btnViewDetails, btnApprove, btnReject);

                {
                    buttons.setPadding(new Insets(5));
                    
                    btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                    btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                    btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                    
                    btnViewDetails.setOnAction(event -> {
                        ClearanceRequest request = getTableView().getItems().get(getIndex());
                        viewStudentDormitoryRecords(request);
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
            
            System.out.println("✅ Table columns setup complete");
        } catch (Exception e) {
            System.err.println("❌ Error in setupTableColumns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDormitoryTableColumns() {
        try {
            if (colRecordType != null) colRecordType.setCellValueFactory(new PropertyValueFactory<>("recordType"));
            if (colDescription != null) colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            if (colAmount != null) colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            if (colDueDate != null) colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
            if (colRecordStatus != null) colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            System.out.println("✅ Dormitory table columns setup complete");
        } catch (Exception e) {
            System.err.println("❌ Error in setupDormitoryTableColumns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshRequests() {
        System.out.println("=== DEBUG: Refresh button clicked ===");
        initializeTablesIfNeeded();
        loadPendingRequests();
        showAlert("Refreshed", "Dormitory clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        System.out.println("\n=== DEBUG: Loading pending dormitory requests ===");
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Database connection established");
            
            // Debug: Check what's in the database
            debugDatabaseStatus(conn);
            
            // FIXED QUERY: Use LEFT JOIN and check for PENDING status
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
                    AND ca.officer_role = 'DORMITORY'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND (ca.status IS NULL OR ca.status = 'PENDING')
                ORDER BY cr.request_date ASC
                """;
                
            System.out.println("SQL Query: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            System.out.println("Query executed, processing results...");
            
            while (rs.next()) {
                pendingCount++;
                String studentId = rs.getString("student_id");
                System.out.println("\nFound student #" + pendingCount + ": " + studentId);
                System.out.println("Student Name: " + rs.getString("student_name"));
                System.out.println("Department: " + rs.getString("department"));
                System.out.println("Request Date: " + rs.getString("request_date"));
                System.out.println("Approval Status: " + rs.getString("approval_status"));
                
                String dormitoryStatus = checkStudentDormitoryStatus(conn, studentId);
                String roomNumber = getStudentRoomNumber(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    roomNumber,
                    rs.getString("request_date"),
                    dormitoryStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
            }
            
            System.out.println("\n=== DEBUG: Query Results Summary ===");
            System.out.println("Total records found: " + pendingCount);
            
            if (tableRequests != null) {
                tableRequests.setItems(requestData);
            }
            
            if (lblPendingCount != null) {
                lblPendingCount.setText("Pending Dormitory Clearances: " + pendingCount);
            }
            
            if (pendingCount == 0) {
                System.out.println("⚠️ WARNING: No dormitory clearance requests found!");
                showAlert("No Requests", "No pending dormitory clearance requests found. Try creating test data.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR in loadPendingRequests:");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
        }
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
        
        // Check clearance_approvals for DORMITORY
        System.out.println("\n2. clearance_approvals for DORMITORY:");
        String checkApprovalsSql = """
            SELECT ca.request_id, ca.status, u.username as student_id
            FROM clearance_approvals ca
            LEFT JOIN clearance_requests cr ON ca.request_id = cr.id
            LEFT JOIN users u ON cr.student_id = u.id
            WHERE ca.officer_role = 'DORMITORY'
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
        
        // Check requests missing DORMITORY approvals
        System.out.println("\n3. Requests missing DORMITORY approvals:");
        String missingSql = """
            SELECT cr.id, u.username, cr.status as request_status
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
            AND NOT EXISTS (
                SELECT 1 FROM clearance_approvals ca 
                WHERE ca.request_id = cr.id 
                AND ca.officer_role = 'DORMITORY'
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
        System.out.println("Total DORMITORY approvals: " + approvalCount);
        System.out.println("Requests missing DORMITORY approvals: " + missingCount);
    }

    private String checkStudentDormitoryStatus(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking dormitory status for student: " + studentId);
        
        createDormitoryRecordsIfNeeded(conn, studentId);
        
        String dormitorySql = """
            SELECT 
                COUNT(*) as total_issues,
                SUM(CASE WHEN dr.record_type = 'OUTSTANDING_FEE' AND dr.status != 'Paid' THEN 1 ELSE 0 END) as unpaid_fees,
                SUM(CASE WHEN dr.record_type = 'DAMAGE' AND dr.status != 'Resolved' THEN 1 ELSE 0 END) as unresolved_damages,
                SUM(CASE WHEN dr.record_type = 'OUTSTANDING_FEE' AND dr.status != 'Paid' THEN dr.amount ELSE 0 END) as total_fees,
                SUM(CASE WHEN dr.record_type = 'VIOLATION' AND dr.status != 'Resolved' THEN 1 ELSE 0 END) as active_violations
            FROM dormitory_records dr
            JOIN users u ON dr.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement ps = conn.prepareStatement(dormitorySql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int unpaidFees = rs.getInt("unpaid_fees");
            int unresolvedDamages = rs.getInt("unresolved_damages");
            double totalFees = rs.getDouble("total_fees");
            int activeViolations = rs.getInt("active_violations");
            
            System.out.println("  Dormitory issues - Unpaid fees: " + unpaidFees + 
                             ", Unresolved damages: " + unresolvedDamages +
                             ", Active violations: " + activeViolations +
                             ", Total fees: $" + totalFees);
            
            if (unpaidFees > 0 && totalFees > 0) {
                return "❌ Outstanding Fees: $" + String.format("%.2f", totalFees) + " (" + unpaidFees + " items)";
            } else if (unresolvedDamages > 0) {
                return "❌ " + unresolvedDamages + " Unresolved Damage(s)";
            } else if (activeViolations > 0) {
                return "❌ " + activeViolations + " Active Violation(s)";
            } else {
                return "✅ Dormitory Clear";
            }
        }
        
        return "Pending Dormitory Check";
    }

    private String getStudentRoomNumber(Connection conn, String studentId) throws SQLException {
        String roomSql = """
            SELECT dr.room_number 
            FROM dormitory_records dr
            JOIN users u ON dr.student_id = u.id 
            WHERE u.username = ? 
            AND dr.record_type = 'ROOM_ASSIGNMENT' 
            ORDER BY dr.created_date DESC 
            LIMIT 1
            """;
            
        PreparedStatement ps = conn.prepareStatement(roomSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            String room = rs.getString("room_number");
            System.out.println("  Room number: " + room);
            return room;
        }
        
        System.out.println("  No room assigned");
        return "Not Assigned";
    }

    private void createDormitoryRecordsIfNeeded(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking/creating dormitory records for: " + studentId);
        
        try {
            String checkTableSql = "SELECT 1 FROM dormitory_records LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
            System.out.println("  dormitory_records table exists");
        } catch (SQLException e) {
            System.out.println("  dormitory_records table doesn't exist, creating...");
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS dormitory_records (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    record_type VARCHAR(50),
                    description TEXT,
                    amount DECIMAL(10,2),
                    due_date DATE,
                    status VARCHAR(20),
                    room_number VARCHAR(10),
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
            System.out.println("  Created dormitory_records table");
        }
        
        // Check if student has dormitory records
        String checkRecordsSql = """
            SELECT COUNT(*) FROM dormitory_records dr 
            JOIN users u ON dr.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        int existingCount = rs.getInt(1);
        
        System.out.println("  Existing dormitory records: " + existingCount);
        
        if (existingCount == 0) {
            System.out.println("  No records found, creating dormitory records...");
            
            // Get student info
            String studentSql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement studentStmt = conn.prepareStatement(studentSql);
            studentStmt.setString(1, studentId);
            ResultSet studentRs = studentStmt.executeQuery();
            
            if (studentRs.next()) {
                int userId = studentRs.getInt("id");
                
                // Insert room assignment
                String roomNumber = "DORM-" + (100 + (int)(Math.random() * 400));
                String insertRoomSql = """
                    INSERT INTO dormitory_records (student_id, record_type, description, amount, due_date, status, room_number)
                    VALUES (?, 'ROOM_ASSIGNMENT', 'Dormitory Room Assignment', 0.00, NULL, 'Active', ?)
                    """;
                
                PreparedStatement insertRoomStmt = conn.prepareStatement(insertRoomSql);
                insertRoomStmt.setInt(1, userId);
                insertRoomStmt.setString(2, roomNumber);
                insertRoomStmt.executeUpdate();
                
                System.out.println("  Assigned room: " + roomNumber);
                
                // Randomly create some issues
                String[][] issues = {
                    {"OUTSTANDING_FEE", "Monthly Dormitory Fee", "500.00", getFutureDate(15), "Pending"},
                    {"OUTSTANDING_FEE", "Late Payment Penalty", "50.00", getPastDate(5), "Overdue"},
                    {"DAMAGE", "Broken Window", "200.00", getFutureDate(30), "Pending"},
                    {"VIOLATION", "Quiet Hours", "0.00", getFutureDate(7), "Pending"}
                };
                
                String insertIssueSql = """
                    INSERT INTO dormitory_records (student_id, record_type, description, amount, due_date, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
                
                PreparedStatement insertIssueStmt = conn.prepareStatement(insertIssueSql);
                for (String[] issue : issues) {
                    if (Math.random() < 0.6) { // 60% chance for each issue
                        insertIssueStmt.setInt(1, userId);
                        insertIssueStmt.setString(2, issue[0]);
                        insertIssueStmt.setString(3, issue[1]);
                        insertIssueStmt.setDouble(4, Double.parseDouble(issue[2]));
                        insertIssueStmt.setDate(5, Date.valueOf(issue[3]));
                        insertIssueStmt.setString(6, issue[4]);
                        insertIssueStmt.executeUpdate();
                    }
                }
                
                System.out.println("  Created dormitory records with issues");
            }
        }
    }

    private void viewStudentDormitoryRecords(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Viewing dormitory records for: " + request.getStudentId());
        loadStudentDormitoryRecords(request.getStudentId());
        
        if (lblStudentInfo != null) {
            lblStudentInfo.setText("Dormitory Records for: " + request.getStudentName() + 
                                  " (" + request.getStudentId() + ") - Room: " + request.getRoomNumber());
        }
        
        updateDormitorySummary(request.getStudentId());
        
        // Switch to Dormitory Records tab if it exists
        if (mainTabPane != null) {
            for (Tab tab : mainTabPane.getTabs()) {
                if (tab.getText() != null && tab.getText().contains("Records")) {
                    mainTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    private void loadStudentDormitoryRecords(String studentId) {
        System.out.println("Loading dormitory records for: " + studentId);
        dormitoryData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    dr.record_type,
                    dr.description,
                    dr.amount,
                    dr.due_date,
                    dr.status,
                    dr.room_number
                FROM dormitory_records dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                ORDER BY 
                    CASE 
                        WHEN dr.status IN ('Pending', 'Overdue') THEN 1
                        ELSE 2
                    END,
                    dr.due_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                String amount = rs.getDouble("amount") > 0 ? 
                    String.format("$%.2f", rs.getDouble("amount")) : "-";
                
                String dueDate = rs.getDate("due_date") != null ? 
                    rs.getDate("due_date").toString() : "N/A";
                
                DormitoryRecord record = new DormitoryRecord(
                    formatRecordType(rs.getString("record_type")),
                    rs.getString("description"),
                    amount,
                    dueDate,
                    rs.getString("status")
                );
                dormitoryData.add(record);
            }
            
            if (tableDormitoryRecords != null) {
                tableDormitoryRecords.setItems(dormitoryData);
            }
            
            System.out.println("Loaded " + count + " dormitory records");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dormitory records: " + e.getMessage());
        }
    }

    private String formatRecordType(String recordType) {
        return switch (recordType) {
            case "OUTSTANDING_FEE" -> "💰 Outstanding Fee";
            case "DAMAGE" -> "⚡ Damage Report";
            case "VIOLATION" -> "⚠️ Violation";
            case "ROOM_ASSIGNMENT" -> "🏠 Room Assignment";
            default -> recordType;
        };
    }

    private void updateDormitorySummary(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    COUNT(*) as total_records,
                    SUM(CASE WHEN dr.status IN ('Pending', 'Overdue') THEN 1 ELSE 0 END) as pending_issues,
                    SUM(CASE WHEN dr.record_type = 'OUTSTANDING_FEE' AND dr.status != 'Paid' THEN dr.amount ELSE 0 END) as total_fees
                FROM dormitory_records dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int pendingIssues = rs.getInt("pending_issues");
                double totalFees = rs.getDouble("total_fees");
                
                String summary;
                if (pendingIssues == 0) {
                    summary = "✅ No pending dormitory issues";
                    if (lblDormitoryStatus != null) {
                        lblDormitoryStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                } else if (totalFees > 0) {
                    summary = String.format("❌ %d pending issues | Outstanding Fees: $%.2f", pendingIssues, totalFees);
                    if (lblDormitoryStatus != null) {
                        lblDormitoryStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                } else {
                    summary = String.format("⚠️ %d pending issues (no fees)", pendingIssues);
                    if (lblDormitoryStatus != null) {
                        lblDormitoryStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
                
                if (lblDormitoryStatus != null) {
                    lblDormitoryStatus.setText(summary);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Approving dormitory clearance for: " + request.getStudentId());
        
        if (request.getDormitoryStatus().contains("❌") || request.getDormitoryStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Dormitory Clearance Issue");
            warning.setHeaderText("Student Has Dormitory Issues");
            warning.setContentText("This student has outstanding dormitory issues:\n\n" + 
                                 request.getDormitoryStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                System.out.println("Approval cancelled due to dormitory issues");
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Dormitory Clearance");
        confirmation.setHeaderText("Approve Dormitory Clearance");
        confirmation.setContentText("Approve dormitory clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\nRoom: " + request.getRoomNumber());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("Approving dormitory clearance for: " + request.getStudentId());
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Dormitory clearance approved. Room check completed.");
                loadPendingRequests();
                showAlert("Approved", "Dormitory clearance approved for " + request.getStudentName());
            } else {
                System.out.println("Approval cancelled by user");
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Rejecting dormitory clearance for: " + request.getStudentId());
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Dormitory Clearance");
        dialog.setHeaderText("Reject Dormitory Clearance");
        dialog.setContentText("Enter reason for rejecting dormitory clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            System.out.println("Rejection reason: " + result.get());
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Dormitory clearance rejected: " + result.get().trim());
            loadPendingRequests();
            showAlert("Rejected", "Dormitory clearance rejected for " + request.getStudentName());
        } else {
            System.out.println("Rejection cancelled");
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        System.out.println("\n=== DEBUG: Updating dormitory clearance status ===");
        System.out.println("Request ID: " + requestId);
        System.out.println("Status: " + status);
        System.out.println("Remarks: " + remarks);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = """
                SELECT COUNT(*) FROM clearance_approvals 
                WHERE request_id = ? AND officer_role = 'DORMITORY'
                """;
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            System.out.println("Existing DORMITORY approval records: " + count);
            
            if (count > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE clearance_approvals 
                    SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                    WHERE request_id = ? AND officer_role = 'DORMITORY'
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
                    VALUES (?, 'DORMITORY', ?, ?, ?, NOW())
                    """;
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setInt(1, requestId);
                ps.setInt(2, currentUser.getId());
                ps.setString(3, status);
                ps.setString(4, remarks);
                int inserted = ps.executeUpdate();
                System.out.println("Inserted " + inserted + " record(s)");
            }
            
            System.out.println("Dormitory clearance status updated successfully");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR updating clearance status: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
        }
    }

    @FXML
    private void generateDormitoryReport() {
        System.out.println("\n=== DEBUG: Generating dormitory report ===");
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No student selected");
            showAlert("Selection Required", "Please select a student first to generate dormitory report.");
            return;
        }
        
        System.out.println("Generating report for: " + selected.getStudentId());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    dr.room_number,
                    COUNT(dr.id) as total_records,
                    SUM(CASE WHEN dr.status IN ('Pending', 'Overdue') THEN 1 ELSE 0 END) as pending_issues,
                    SUM(CASE WHEN dr.record_type = 'OUTSTANDING_FEE' AND dr.status != 'Paid' THEN dr.amount ELSE 0 END) as total_fees
                FROM users u
                LEFT JOIN dormitory_records dr ON u.id = dr.student_id
                WHERE u.username = ?
                GROUP BY u.id, dr.room_number
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "🏠 DORMITORY CLEARANCE REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Room: " + rs.getString("room_number") + "\n" +
                              "Total Records: " + rs.getInt("total_records") + "\n" +
                              "Pending Issues: " + rs.getInt("pending_issues") + "\n" +
                              "Outstanding Fees: $" + String.format("%.2f", rs.getDouble("total_fees")) + "\n\n" +
                              "Generated by: " + currentUser.getFullName() + 
                              " (Dormitory Office)";
                
                System.out.println("Report generated successfully");
                showAlert("Dormitory Report", report);
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR generating report: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to generate dormitory report: " + e.getMessage());
        }
    }

    @FXML
    private void createTestData() {
        System.out.println("\n=== DEBUG: Manual test data creation ===");
        createTestClearanceRequests();
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
        private final String roomNumber;
        private final String requestDate;
        private final String dormitoryStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String roomNumber, String requestDate, String dormitoryStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.roomNumber = roomNumber;
            this.requestDate = requestDate;
            this.dormitoryStatus = dormitoryStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getRoomNumber() { return roomNumber; }
        public String getRequestDate() { return requestDate; }
        public String getDormitoryStatus() { return dormitoryStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class DormitoryRecord {
        private final String recordType;
        private final String description;
        private final String amount;
        private final String dueDate;
        private final String status;

        public DormitoryRecord(String recordType, String description, String amount, String dueDate, String status) {
            this.recordType = recordType;
            this.description = description;
            this.amount = amount;
            this.dueDate = dueDate;
            this.status = status;
        }

        public String getRecordType() { return recordType; }
        public String getDescription() { return description; }
        public String getAmount() { return amount; }
        public String getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}