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

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<DormitoryRecord> dormitoryData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupDormitoryTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Dormitory Office");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colDormitoryStatus.setCellValueFactory(new PropertyValueFactory<>("dormitoryStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Approve/Reject buttons
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
                    // Only show buttons if pending
                    if ("Pending Dormitory Check".equals(request.getDormitoryStatus()) || 
                        request.getDormitoryStatus().contains("Pending")) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupDormitoryTableColumns() {
        colRecordType.setCellValueFactory(new PropertyValueFactory<>("recordType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color code status
        colRecordStatus.setCellFactory(column -> new TableCell<DormitoryRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Paid") || item.equals("Resolved")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equals("Pending")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Color code amounts (red for outstanding fees)
        colAmount.setCellFactory(column -> new TableCell<DormitoryRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (!item.equals("$0.00") && !item.equals("-")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
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
        showAlert("Refreshed", "Dormitory clearance requests refreshed successfully!");
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
                    cr.request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                JOIN clearance_approvals ca ON cr.id = ca.request_id 
                WHERE ca.officer_role = 'DORMITORY' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String dormitoryStatus = checkStudentDormitoryStatus(conn, studentId);
                String roomNumber = getStudentRoomNumber(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    roomNumber,
                    rs.getTimestamp("request_date").toString(),
                    dormitoryStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Dormitory Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String checkStudentDormitoryStatus(Connection conn, String studentId) throws SQLException {
        // Check if student has any dormitory issues
        createDormitoryRecordsIfNeeded(conn, studentId);
        
        String dormitorySql = """
            SELECT 
                COUNT(*) as total_issues,
                SUM(CASE WHEN record_type = 'OUTSTANDING_FEE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_fees,
                SUM(CASE WHEN record_type = 'DAMAGE' AND status != 'Resolved' THEN 1 ELSE 0 END) as unresolved_damages,
                SUM(CASE WHEN record_type = 'OUTSTANDING_FEE' AND status != 'Paid' THEN amount ELSE 0 END) as total_fees,
                SUM(CASE WHEN record_type = 'VIOLATION' AND status != 'Resolved' THEN 1 ELSE 0 END) as active_violations
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
            
            if (unpaidFees > 0 && totalFees > 0) {
                return "❌ Outstanding Fees: $" + totalFees + " (" + unpaidFees + " items)";
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
            SELECT room_number 
            FROM dormitory_records 
            JOIN users u ON dormitory_records.student_id = u.id 
            WHERE u.username = ? 
            AND record_type = 'ROOM_ASSIGNMENT' 
            ORDER BY created_date DESC 
            LIMIT 1
            """;
            
        PreparedStatement ps = conn.prepareStatement(roomSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return rs.getString("room_number");
        }
        
        return "Not Assigned";
    }

    private void createDormitoryRecordsIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if dormitory_records table exists
        try {
            String checkTableSql = "SELECT 1 FROM dormitory_records LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
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
        }
        
        // Check if student has dormitory records, if not insert sample data
        String checkRecordsSql = """
            SELECT COUNT(*) FROM dormitory_records dr 
            JOIN users u ON dr.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Insert sample dormitory records with random issues
            String[][] records = generateSampleDormitoryRecords();
            
            String insertSql = """
                INSERT INTO dormitory_records (student_id, record_type, description, amount, due_date, status, room_number)
                SELECT id, ?, ?, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            // Always assign a room
            String roomNumber = "DORM-" + (100 + (int)(Math.random() * 400));
            insertStmt.setString(1, "ROOM_ASSIGNMENT");
            insertStmt.setString(2, "Dormitory Room Assignment");
            insertStmt.setDouble(3, 0.00);
            insertStmt.setDate(4, null);
            insertStmt.setString(5, "Active");
            insertStmt.setString(6, roomNumber);
            insertStmt.setString(7, studentId);
            insertStmt.executeUpdate();
            
            // Insert random issues (30% chance of having issues)
            for (String[] record : records) {
                if (Math.random() < 0.3) {
                    insertStmt.setString(1, record[0]);
                    insertStmt.setString(2, record[1]);
                    insertStmt.setDouble(3, Double.parseDouble(record[2]));
                    insertStmt.setDate(4, Date.valueOf(record[3]));
                    insertStmt.setString(5, record[4]);
                    insertStmt.setString(6, null);
                    insertStmt.setString(7, studentId);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    private String[][] generateSampleDormitoryRecords() {
        return new String[][]{
            {"OUTSTANDING_FEE", "Monthly Dormitory Fee", "500.00", java.time.LocalDate.now().plusDays(15).toString(), "Pending"},
            {"OUTSTANDING_FEE", "Late Payment Penalty", "50.00", java.time.LocalDate.now().minusDays(5).toString(), "Overdue"},
            {"DAMAGE", "Broken Window in Room", "200.00", java.time.LocalDate.now().plusDays(30).toString(), "Pending"},
            {"DAMAGE", "Damaged Furniture", "150.00", java.time.LocalDate.now().plusDays(30).toString(), "Pending"},
            {"VIOLATION", "Quiet Hours Violation", "0.00", java.time.LocalDate.now().plusDays(7).toString(), "Pending"},
            {"VIOLATION", "Unauthorized Guest", "25.00", java.time.LocalDate.now().plusDays(7).toString(), "Pending"},
            {"OUTSTANDING_FEE", "Utilities Fee", "75.00", java.time.LocalDate.now().plusDays(10).toString(), "Pending"}
        };
    }

    private void viewStudentDormitoryRecords(ClearanceRequest request) {
        loadStudentDormitoryRecords(request.getStudentId());
        lblStudentInfo.setText("Dormitory Records for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - Room: " + request.getRoomNumber());
        
        // Update dormitory status summary
        updateDormitorySummary(request.getStudentId());
    }

    private void loadStudentDormitoryRecords(String studentId) {
        dormitoryData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    record_type,
                    description,
                    amount,
                    due_date,
                    status,
                    room_number
                FROM dormitory_records dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                ORDER BY 
                    CASE 
                        WHEN status IN ('Pending', 'Overdue') THEN 1
                        ELSE 2
                    END,
                    due_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
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
            
            tableDormitoryRecords.setItems(dormitoryData);
            
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
                    SUM(CASE WHEN status IN ('Pending', 'Overdue') THEN 1 ELSE 0 END) as pending_issues,
                    SUM(CASE WHEN record_type = 'OUTSTANDING_FEE' AND status != 'Paid' THEN amount ELSE 0 END) as total_fees
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
                    lblDormitoryStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (totalFees > 0) {
                    summary = String.format("❌ %d pending issues | Outstanding Fees: $%.2f", pendingIssues, totalFees);
                    lblDormitoryStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else {
                    summary = String.format("⚠️ %d pending issues (no fees)", pendingIssues);
                    lblDormitoryStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                }
                
                lblDormitoryStatus.setText(summary);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        // Check if student has any dormitory issues
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
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Dormitory clearance approved. Room check completed.");
                loadPendingRequests(); // Refresh table to remove approved request
                showAlert("Approved", "Dormitory clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Dormitory Clearance");
        dialog.setHeaderText("Reject Dormitory Clearance");
        dialog.setContentText("Enter reason for rejecting dormitory clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Dormitory clearance rejected: " + result.get().trim());
            loadPendingRequests(); // Refresh table to remove rejected request
            showAlert("Rejected", "Dormitory clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'DORMITORY'
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
    private void generateDormitoryReport() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to generate dormitory report.");
            return;
        }
        
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
                
                showAlert("Dormitory Report", report);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to generate dormitory report: " + e.getMessage());
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