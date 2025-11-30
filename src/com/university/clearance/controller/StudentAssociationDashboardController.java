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

public class StudentAssociationDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private Label lblAssociationStatus;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colMembership;
    @FXML private TableColumn<ClearanceRequest, String> colAssociationStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<AssociationRecord> tableAssociationRecords;
    @FXML private TableColumn<AssociationRecord, String> colRecordType;
    @FXML private TableColumn<AssociationRecord, String> colDescription;
    @FXML private TableColumn<AssociationRecord, String> colAmount;
    @FXML private TableColumn<AssociationRecord, String> colDueDate;
    @FXML private TableColumn<AssociationRecord, String> colRecordStatus;

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<AssociationRecord> associationData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupAssociationTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Student Association");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colMembership.setCellValueFactory(new PropertyValueFactory<>("membership"));
        colAssociationStatus.setCellValueFactory(new PropertyValueFactory<>("associationStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Approve/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject = new Button("❌ Reject");
            private final Button btnViewDetails = new Button("👥 View Records");
            private final HBox buttons = new HBox(5, btnViewDetails, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                
                btnViewDetails.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    viewStudentAssociationRecords(request);
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
                    if ("Pending Association Check".equals(request.getAssociationStatus()) || 
                        request.getAssociationStatus().contains("Pending")) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupAssociationTableColumns() {
        colRecordType.setCellValueFactory(new PropertyValueFactory<>("recordType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color code status
        colRecordStatus.setCellFactory(column -> new TableCell<AssociationRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Paid") || item.equals("Active") || item.equals("Completed")) {
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
        colAmount.setCellFactory(column -> new TableCell<AssociationRecord, String>() {
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
        showAlert("Refreshed", "Student Association clearance requests refreshed successfully!");
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
                WHERE ca.officer_role = 'ASSOCIATION' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String associationStatus = checkStudentAssociationStatus(conn, studentId);
                String membership = getStudentMembershipStatus(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    membership,
                    rs.getTimestamp("request_date").toString(),
                    associationStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Association Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String checkStudentAssociationStatus(Connection conn, String studentId) throws SQLException {
        // Check if student has any association issues
        createAssociationRecordsIfNeeded(conn, studentId);
        
        String associationSql = """
            SELECT 
                COUNT(*) as total_records,
                SUM(CASE WHEN record_type = 'MEMBERSHIP_FEE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_membership,
                SUM(CASE WHEN record_type = 'CLUB_FEE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_club_fees,
                SUM(CASE WHEN record_type IN ('MEMBERSHIP_FEE', 'CLUB_FEE') AND status != 'Paid' THEN amount ELSE 0 END) as total_fees,
                SUM(CASE WHEN record_type = 'EVENT_FINE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_fines,
                SUM(CASE WHEN record_type = 'CLUB_MEMBERSHIP' AND status = 'Active' THEN 1 ELSE 0 END) as active_clubs
            FROM association_records ar
            JOIN users u ON ar.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement ps = conn.prepareStatement(associationSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int unpaidMembership = rs.getInt("unpaid_membership");
            int unpaidClubFees = rs.getInt("unpaid_club_fees");
            double totalFees = rs.getDouble("total_fees");
            int unpaidFines = rs.getInt("unpaid_fines");
            int activeClubs = rs.getInt("active_clubs");
            
            if (unpaidMembership > 0) {
                return "❌ Unpaid Association Membership Fee";
            } else if (unpaidClubFees > 0 && totalFees > 0) {
                return "❌ Outstanding Club Fees: $" + String.format("%.2f", totalFees);
            } else if (unpaidFines > 0) {
                return "❌ " + unpaidFines + " Unpaid Event Fine(s)";
            } else if (activeClubs > 0) {
                return "✅ Active in " + activeClubs + " Club(s)";
            } else {
                return "✅ Association Clear";
            }
        }
        
        return "Pending Association Check";
    }

    private String getStudentMembershipStatus(Connection conn, String studentId) throws SQLException {
        String membershipSql = """
            SELECT status 
            FROM association_records 
            JOIN users u ON association_records.student_id = u.id 
            WHERE u.username = ? 
            AND record_type = 'MEMBERSHIP_FEE' 
            ORDER BY due_date DESC 
            LIMIT 1
            """;
            
        PreparedStatement ps = conn.prepareStatement(membershipSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            String status = rs.getString("status");
            return "Paid".equals(status) ? "Active Member" : "Inactive (Unpaid)";
        }
        
        return "Not Registered";
    }

    private void createAssociationRecordsIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if association_records table exists
        try {
            String checkTableSql = "SELECT 1 FROM association_records LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS association_records (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    record_type VARCHAR(50),
                    description TEXT,
                    amount DECIMAL(10,2),
                    due_date DATE,
                    status VARCHAR(20),
                    club_name VARCHAR(100),
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
        }
        
        // Check if student has association records, if not insert sample data
        String checkRecordsSql = """
            SELECT COUNT(*) FROM association_records ar 
            JOIN users u ON ar.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Insert sample association records
            String[][] records = generateSampleAssociationRecords();
            
            String insertSql = """
                INSERT INTO association_records (student_id, record_type, description, amount, due_date, status, club_name)
                SELECT id, ?, ?, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            // Insert membership fee (80% paid, 20% pending)
            boolean membershipPaid = Math.random() > 0.2;
            insertStmt.setString(1, "MEMBERSHIP_FEE");
            insertStmt.setString(2, "Student Association Annual Membership");
            insertStmt.setDouble(3, 50.00);
            insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusMonths(3)));
            insertStmt.setString(5, membershipPaid ? "Paid" : "Pending");
            insertStmt.setString(6, null);
            insertStmt.setString(7, studentId);
            insertStmt.executeUpdate();
            
            // Insert club memberships (random 1-3 clubs)
            String[][] clubs = {
                {"Computer Science Club", "Technology"},
                {"Debate Society", "Academic"},
                {"Basketball Team", "Sports"},
                {"Music Club", "Arts"},
                {"Volunteer Corps", "Community Service"}
            };
            
            int numClubs = 1 + (int)(Math.random() * 3);
            for (int i = 0; i < numClubs; i++) {
                String[] club = clubs[(int)(Math.random() * clubs.length)];
                
                // Club membership
                insertStmt.setString(1, "CLUB_MEMBERSHIP");
                insertStmt.setString(2, club[0] + " Membership");
                insertStmt.setDouble(3, 0.00);
                insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusMonths(2)));
                insertStmt.setString(5, "Active");
                insertStmt.setString(6, club[0]);
                insertStmt.setString(7, studentId);
                insertStmt.executeUpdate();
                
                // Club fees (30% chance of unpaid fees)
                if (Math.random() < 0.3) {
                    insertStmt.setString(1, "CLUB_FEE");
                    insertStmt.setString(2, club[0] + " Activity Fee");
                    insertStmt.setDouble(3, 20 + (Math.random() * 30));
                    insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusDays(15)));
                    insertStmt.setString(5, "Pending");
                    insertStmt.setString(6, club[0]);
                    insertStmt.setString(7, studentId);
                    insertStmt.executeUpdate();
                }
            }
            
            // Insert event fines (10% chance)
            if (Math.random() < 0.1) {
                insertStmt.setString(1, "EVENT_FINE");
                insertStmt.setString(2, "Event Equipment Damage Fine");
                insertStmt.setDouble(3, 25.00);
                insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().plusDays(30)));
                insertStmt.setString(5, "Pending");
                insertStmt.setString(6, null);
                insertStmt.setString(7, studentId);
                insertStmt.executeUpdate();
            }
        }
    }

    private String[][] generateSampleAssociationRecords() {
        return new String[][]{
            {"MEMBERSHIP_FEE", "Student Association Annual Membership", "50.00", java.time.LocalDate.now().minusMonths(3).toString(), "Paid"},
            {"CLUB_MEMBERSHIP", "Computer Science Club Membership", "0.00", java.time.LocalDate.now().minusMonths(2).toString(), "Active"},
            {"CLUB_FEE", "CS Club Activity Fee", "25.00", java.time.LocalDate.now().minusDays(15).toString(), "Pending"},
            {"CLUB_MEMBERSHIP", "Basketball Team Membership", "0.00", java.time.LocalDate.now().minusMonths(1).toString(), "Active"},
            {"EVENT_FINE", "Sports Equipment Fine", "25.00", java.time.LocalDate.now().plusDays(30).toString(), "Pending"}
        };
    }

    private void viewStudentAssociationRecords(ClearanceRequest request) {
        loadStudentAssociationRecords(request.getStudentId());
        lblStudentInfo.setText("Association Records for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - Membership: " + request.getMembership());
        
        // Update association status summary
        updateAssociationSummary(request.getStudentId());
    }

    private void loadStudentAssociationRecords(String studentId) {
        associationData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    record_type,
                    description,
                    amount,
                    due_date,
                    status,
                    club_name
                FROM association_records ar
                JOIN users u ON ar.student_id = u.id
                WHERE u.username = ?
                ORDER BY 
                    CASE 
                        WHEN status IN ('Pending', 'Active') THEN 1
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
                
                String description = rs.getString("description");
                if (rs.getString("club_name") != null) {
                    description += " (" + rs.getString("club_name") + ")";
                }
                
                AssociationRecord record = new AssociationRecord(
                    formatRecordType(rs.getString("record_type")),
                    description,
                    amount,
                    dueDate,
                    rs.getString("status")
                );
                associationData.add(record);
            }
            
            tableAssociationRecords.setItems(associationData);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load association records: " + e.getMessage());
        }
    }

    private String formatRecordType(String recordType) {
        return switch (recordType) {
            case "MEMBERSHIP_FEE" -> "🎓 Membership Fee";
            case "CLUB_FEE" -> "💰 Club Fee";
            case "CLUB_MEMBERSHIP" -> "👥 Club Membership";
            case "EVENT_FINE" -> "⚠️ Event Fine";
            case "PAYMENT" -> "✅ Payment";
            default -> recordType;
        };
    }

    private void updateAssociationSummary(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    COUNT(*) as total_records,
                    SUM(CASE WHEN status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as pending_items,
                    SUM(CASE WHEN record_type IN ('MEMBERSHIP_FEE', 'CLUB_FEE', 'EVENT_FINE') AND status != 'Paid' THEN amount ELSE 0 END) as total_fees,
                    SUM(CASE WHEN record_type = 'CLUB_MEMBERSHIP' AND status = 'Active' THEN 1 ELSE 0 END) as active_clubs
                FROM association_records ar
                JOIN users u ON ar.student_id = u.id
                WHERE u.username = ?
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int pendingItems = rs.getInt("pending_items");
                double totalFees = rs.getDouble("total_fees");
                int activeClubs = rs.getInt("active_clubs");
                
                String summary;
                if (pendingItems == 0) {
                    summary = "✅ No pending association issues";
                    lblAssociationStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (totalFees > 0) {
                    summary = String.format("❌ %d pending items | Outstanding Fees: $%.2f", pendingItems, totalFees);
                    lblAssociationStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else if (activeClubs > 0) {
                    summary = String.format("✅ Active in %d club(s) | %d items to review", activeClubs, pendingItems);
                    lblAssociationStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                } else {
                    summary = String.format("ℹ️ %d pending items to review", pendingItems);
                    lblAssociationStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                }
                
                lblAssociationStatus.setText(summary);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        // Check if student has any association issues
        if (request.getAssociationStatus().contains("❌") || request.getAssociationStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Association Clearance Issue");
            warning.setHeaderText("Student Has Association Issues");
            warning.setContentText("This student has outstanding association issues:\n\n" + 
                                 request.getAssociationStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Association Clearance");
        confirmation.setHeaderText("Approve Student Association Clearance");
        confirmation.setContentText("Approve association clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\nMembership: " + request.getMembership());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Student Association clearance approved. All club obligations cleared.");
                loadPendingRequests(); // Refresh table to remove approved request
                showAlert("Approved", "Association clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Association Clearance");
        dialog.setHeaderText("Reject Student Association Clearance");
        dialog.setContentText("Enter reason for rejecting association clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Association clearance rejected: " + result.get().trim());
            loadPendingRequests(); // Refresh table to remove rejected request
            showAlert("Rejected", "Association clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'ASSOCIATION'
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
    private void generateAssociationReport() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to generate association report.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    COUNT(ar.id) as total_records,
                    SUM(CASE WHEN ar.status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as pending_items,
                    SUM(CASE WHEN ar.record_type IN ('MEMBERSHIP_FEE', 'CLUB_FEE', 'EVENT_FINE') AND ar.status != 'Paid' THEN ar.amount ELSE 0 END) as total_fees,
                    SUM(CASE WHEN ar.record_type = 'CLUB_MEMBERSHIP' AND ar.status = 'Active' THEN 1 ELSE 0 END) as active_clubs
                FROM users u
                LEFT JOIN association_records ar ON u.id = ar.student_id
                WHERE u.username = ?
                GROUP BY u.id
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "👥 STUDENT ASSOCIATION CLEARANCE REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Total Records: " + rs.getInt("total_records") + "\n" +
                              "Pending Items: " + rs.getInt("pending_items") + "\n" +
                              "Outstanding Fees: $" + String.format("%.2f", rs.getDouble("total_fees")) + "\n" +
                              "Active Clubs: " + rs.getInt("active_clubs") + "\n\n" +
                              "Generated by: " + currentUser.getFullName() + 
                              " (Student Association)";
                
                showAlert("Association Report", report);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to generate association report: " + e.getMessage());
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
        private final String membership;
        private final String requestDate;
        private final String associationStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String membership, String requestDate, String associationStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.membership = membership;
            this.requestDate = requestDate;
            this.associationStatus = associationStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getMembership() { return membership; }
        public String getRequestDate() { return requestDate; }
        public String getAssociationStatus() { return associationStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class AssociationRecord {
        private final String recordType;
        private final String description;
        private final String amount;
        private final String dueDate;
        private final String status;

        public AssociationRecord(String recordType, String description, String amount, String dueDate, String status) {
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