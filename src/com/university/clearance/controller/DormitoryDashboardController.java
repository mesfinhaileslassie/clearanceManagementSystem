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

    // REMOVED: All non-functional buttons and redundant labels
    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblDormitoryStatus;
    @FXML private TabPane mainTabPane;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
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
        
        // Add listener to handle row selection
        setupDoubleClickHandler();
        
        loadPendingRequests();
    }
    
    
    
    
    private void setupDoubleClickHandler() {
        tableRequests.setRowFactory(tv -> {
            TableRow<ClearanceRequest> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ClearanceRequest selectedRequest = row.getItem();
                    loadStudentDormitoryRecords(selectedRequest.getStudentId());
                    
                    // Switch to Dormitory Records tab on double-click
                    for (Tab tab : mainTabPane.getTabs()) {
                        if ("Dormitory Records".equals(tab.getText())) {
                            mainTabPane.getSelectionModel().select(tab);
                            break;
                        }
                    }
                }
            });
            return row;
        });
    }
    
    
    
    @FXML
    private void handleTableClick() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadStudentDormitoryRecords(selected.getStudentId());
            // Note: No tab switching here - only loads data
        }
    }

    
    

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // SIMPLIFIED: Clean welcome message
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + user.getFullName());
        }
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDormitoryStatus.setCellValueFactory(new PropertyValueFactory<>("dormitoryStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Approve/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject = new Button("❌ Reject");
            private final HBox buttons = new HBox(5, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                
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
                    if (item.equals("Paid") || item.equals("Cleared")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equals("Pending")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Color code amounts (red for outstanding balances)
        colAmount.setCellFactory(column -> new TableCell<DormitoryRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (!item.equals("$0.00") && !item.equals("-") && !item.startsWith("+")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.startsWith("+")) {
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
        loadPendingRequests();
        showAlert("Refreshed", "Dormitory clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, ensure dormitory records table exists
            createDormitoryRecordsTable(conn);
            
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
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
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String dormitoryStatus = checkStudentDormitoryStatus(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getString("request_date"),
                    dormitoryStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createDormitoryRecordsTable(Connection conn) throws SQLException {
        // Create dormitory_records table if it doesn't exist
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS dormitory_records (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT,
                record_type VARCHAR(50),
                description TEXT,
                amount DECIMAL(10,2),
                due_date DATE,
                status VARCHAR(20),
                room_number VARCHAR(20),
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES users(id)
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    private String checkStudentDormitoryStatus(Connection conn, String studentId) throws SQLException {
        // Check dormitory records for issues
        String sql = """
            SELECT 
                COUNT(*) as total_records,
                SUM(CASE WHEN dr.amount > 0 AND dr.status != 'Paid' THEN 1 ELSE 0 END) as balance_issues,
                SUM(CASE WHEN dr.due_date < CURDATE() AND dr.status = 'Pending' THEN 1 ELSE 0 END) as overdue_issues,
                SUM(CASE WHEN dr.amount > 0 AND dr.status != 'Paid' THEN dr.amount ELSE 0 END) as total_balance
            FROM dormitory_records dr
            JOIN users u ON dr.student_id = u.id
            WHERE u.username = ?
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int balanceIssues = rs.getInt("balance_issues");
            int overdueIssues = rs.getInt("overdue_issues");
            double totalBalance = rs.getDouble("total_balance");
            
            if (balanceIssues > 0 && totalBalance > 0) {
                return "❌ Outstanding Balance: $" + String.format("%.2f", totalBalance);
            } else if (overdueIssues > 0) {
                return "❌ Overdue Dormitory Fees";
            } else {
                return "✅ Dormitory Clear";
            }
        }
        
        return "Pending Dormitory Check";
    }

    private void loadStudentDormitoryRecords(String studentId) {
        System.out.println("\n=== DEBUG: Loading dormitory records for student: " + studentId);
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
                    dr.due_date DESC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                String amount;
                double amountValue = rs.getDouble("amount");
                
                if (amountValue < 0) {
                    amount = String.format("+$%.2f", Math.abs(amountValue));
                } else {
                    amount = String.format("$%.2f", amountValue);
                }
                
                String dueDate = rs.getDate("due_date") != null ? 
                    rs.getDate("due_date").toString() : "N/A";
                
                String description = rs.getString("description");
                String roomNumber = rs.getString("room_number");
                if (roomNumber != null && !roomNumber.isEmpty()) {
                    description += " (Room: " + roomNumber + ")";
                }
                
                DormitoryRecord record = new DormitoryRecord(
                    formatRecordType(rs.getString("record_type")),
                    description,
                    amount,
                    dueDate,
                    rs.getString("status")
                );
                dormitoryData.add(record);
            }
            
            tableDormitoryRecords.setItems(dormitoryData);
            lblDormitoryStatus.setText("Found " + count + " dormitory records for " + studentId);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dormitory records: " + e.getMessage());
        }
    }

    private String formatRecordType(String recordType) {
        return switch (recordType) {
            case "DORMITORY_FEE" -> "💰 Dormitory Fee";
            case "DAMAGE_CHARGE" -> "🔨 Damage Charge";
            case "LATE_CHECKOUT" -> "⏰ Late Checkout";
            case "SECURITY_DEPOSIT" -> "🏦 Security Deposit";
            case "PAYMENT" -> "✅ Payment";
            default -> recordType;
        };
    }

    private void approveClearance(ClearanceRequest request) {
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
                                  "\nStudent ID: " + request.getStudentId());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Dormitory clearance approved. " + request.getDormitoryStatus());
                loadPendingRequests();
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
            loadPendingRequests();
            showAlert("Rejected", "Dormitory clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = "SELECT COUNT(*) FROM clearance_approvals WHERE request_id = ? AND officer_role = 'DORMITORY'";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            
            if (rs.getInt(1) > 0) {
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
                ps.executeUpdate();
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
                ps.executeUpdate();
            }
            
            // Update the overall request status
            updateOverallRequestStatus(conn, requestId);
            
            loadPendingRequests();
            showAlert("Success", "Dormitory clearance has been " + status.toLowerCase() + " successfully!");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateOverallRequestStatus(Connection conn, int requestId) throws SQLException {
        String checkSql = """
            SELECT 
                SUM(CASE WHEN status = 'PENDING' OR status IS NULL THEN 1 ELSE 0 END) as pending_count,
                SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count
            FROM clearance_approvals 
            WHERE request_id = ?
            AND officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
            """;
        
        PreparedStatement checkPs = conn.prepareStatement(checkSql);
        checkPs.setInt(1, requestId);
        ResultSet rs = checkPs.executeQuery();
        
        if (rs.next()) {
            int pendingCount = rs.getInt("pending_count");
            int rejectedCount = rs.getInt("rejected_count");
            
            if (rejectedCount > 0) {
                updateRequestStatus(conn, requestId, "REJECTED");
            } else if (pendingCount == 0) {
                updateRequestStatus(conn, requestId, "FULLY_CLEARED");
            }
        }
    }

    private void updateRequestStatus(Connection conn, int requestId, String status) throws SQLException {
        String sql = "UPDATE clearance_requests SET status = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, requestId);
        ps.executeUpdate();
    }

    // REMOVED: createTestData and generateDormitoryReport methods since buttons were removed
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for table data
    public static class ClearanceRequest {
        private final String studentId;
        private final String studentName;
        private final String department;
        private final String requestDate;
        private final String dormitoryStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String requestDate, String dormitoryStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.requestDate = requestDate;
            this.dormitoryStatus = dormitoryStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
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