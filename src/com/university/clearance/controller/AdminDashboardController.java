package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.ClearanceRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.net.URL;
public class AdminDashboardController {

    // Top Section
    @FXML private Label lblWelcome;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblTotalOfficers;
    @FXML private Label lblTotalRequests;
    
    // Main Tab Pane
    @FXML private TabPane mainTabPane;
    
    // Students Tables
    @FXML private TableView<User> tableAllStudents;
    @FXML private TableView<User> tableApprovedStudents;
    @FXML private TableView<User> tableRejectedStudents;
    @FXML private TableView<User> tablePendingStudents;
    @FXML private TableView<User> tableInProgressStudents;
    
    // Students Table Columns
    @FXML private TableColumn<User, String> colStudentId;
    @FXML private TableColumn<User, String> colStudentName;
    @FXML private TableColumn<User, String> colStudentDepartment;
    @FXML private TableColumn<User, String> colStudentYear;
    @FXML private TableColumn<User, String> colClearanceStatus;
    @FXML private TableColumn<User, String> colStudentActions;
    
    // Officers Table
    @FXML private TableView<User> tableOfficers;
    @FXML private TableColumn<User, String> colOfficerId;
    @FXML private TableColumn<User, String> colOfficerName;
    @FXML private TableColumn<User, String> colOfficerRole;
    @FXML private TableColumn<User, String> colOfficerDepartment;
    @FXML private TableColumn<User, String> colOfficerStatus;
    
    // All Users Table
    @FXML private TableView<User> tableAllUsers;
    @FXML private TableColumn<User, String> colAllUserId;
    @FXML private TableColumn<User, String> colAllUserName;
    @FXML private TableColumn<User, String> colAllUserRole;
    @FXML private TableColumn<User, String> colAllUserDepartment;
    @FXML private TableColumn<User, String> colAllUserStatus;
    
    // Clearance Requests Table (from original)
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colRequestStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colRequestName;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colRequestStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, Integer> colRequestApproved;
    
    @FXML private TextField txtSearch;
    
    private User currentUser;
    private ObservableList<User> allStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> approvedStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> rejectedStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> pendingStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> inProgressStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> officersData = FXCollections.observableArrayList();
    private ObservableList<User> allUsersData = FXCollections.observableArrayList();
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupAllTables();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " (Admin)");
        loadAllData();
    }
    
    private void setupAllTables() {
        // Setup Students Table
        setupStudentTable(tableAllStudents, colStudentId, colStudentName, colStudentDepartment, 
                         colStudentYear, colClearanceStatus, colStudentActions);
        
        // Setup simplified tables for categorized views
        setupSimpleStudentTable(tableApprovedStudents);
        setupSimpleStudentTable(tableRejectedStudents);
        setupSimpleStudentTable(tablePendingStudents);
        setupSimpleStudentTable(tableInProgressStudents);
        
        // Setup Officers Table
        colOfficerId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colOfficerName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colOfficerRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colOfficerDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colOfficerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Setup All Users Table
        colAllUserId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAllUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAllUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAllUserDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colAllUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Setup Clearance Requests Table
        colRequestStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colRequestName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRequestDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRequestStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colRequestApproved.setCellValueFactory(new PropertyValueFactory<>("approvedCount"));
        
        // Color coding for status column in requests
        colRequestStatus.setCellFactory(column -> new TableCell<ClearanceRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("FULLY_CLEARED") || item.equals("APPROVED")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equals("REJECTED")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.equals("IN_PROGRESS")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (item.equals("PENDING")) {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    private void setupStudentTable(TableView<User> tableView, TableColumn<User, String> colId, 
                                  TableColumn<User, String> colName, TableColumn<User, String> colDept,
                                  TableColumn<User, String> colYear, TableColumn<User, String> colStatus,
                                  TableColumn<User, String> colActions) {
        
        colId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("clearanceStatus"));
        
        // Actions column with "Allow Reapply" button for rejected students
        colActions.setCellFactory(param -> new TableCell<User, String>() {
            private final Button btnAllowReapply = new Button("Allow Reapply");
            private final Button btnViewDetails = new Button("View Details");
            private final HBox buttons = new HBox(5, btnViewDetails, btnAllowReapply);

            {
                buttons.setPadding(new Insets(5));
                btnAllowReapply.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                
                btnAllowReapply.setOnAction(event -> {
                    User student = getTableView().getItems().get(getIndex());
                    allowStudentReapply(student);
                });
                
                btnViewDetails.setOnAction(event -> {
                    User student = getTableView().getItems().get(getIndex());
                    viewStudentDetails(student);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User student = getTableView().getItems().get(getIndex());
                    if (student != null) {
                        // Show "Allow Reapply" only for rejected students
                        if (student.getClearanceStatus() != null && 
                            student.getClearanceStatus().contains("❌")) {
                            btnAllowReapply.setVisible(true);
                            btnAllowReapply.setManaged(true);
                        } else {
                            btnAllowReapply.setVisible(false);
                            btnAllowReapply.setManaged(false);
                        }
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        // Color code clearance status
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
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
                    } else if (item.contains("🔄")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (item.contains("⏳")) {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    private void setupSimpleStudentTable(TableView<User> tableView) {
        // Check if we have 5 columns (if so, the last one is Actions)
        boolean hasActionsColumn = tableView.getColumns().size() == 5;
        
        TableColumn<User, String> col1 = (TableColumn<User, String>) tableView.getColumns().get(0);
        TableColumn<User, String> col2 = (TableColumn<User, String>) tableView.getColumns().get(1);
        TableColumn<User, String> col3 = (TableColumn<User, String>) tableView.getColumns().get(2);
        TableColumn<User, String> col4 = (TableColumn<User, String>) tableView.getColumns().get(3);
        
        col1.setCellValueFactory(new PropertyValueFactory<>("username"));
        col2.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        col3.setCellValueFactory(new PropertyValueFactory<>("department"));
        col4.setCellValueFactory(new PropertyValueFactory<>("clearanceStatus"));
        
        // Add Actions column for rejected students table
        if (hasActionsColumn) {
            TableColumn<User, String> col5 = (TableColumn<User, String>) tableView.getColumns().get(4);
            col5.setCellFactory(param -> new TableCell<User, String>() {
                private final Button btnAllowReapply = new Button("Allow Reapply");
                private final Button btnViewDetails = new Button("View Details");
                private final HBox buttons = new HBox(5, btnViewDetails, btnAllowReapply);

                {
                    buttons.setPadding(new Insets(5));
                    btnAllowReapply.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    
                    btnAllowReapply.setOnAction(event -> {
                        User student = getTableView().getItems().get(getIndex());
                        allowStudentReapply(student);
                    });
                    
                    btnViewDetails.setOnAction(event -> {
                        User student = getTableView().getItems().get(getIndex());
                        viewStudentDetails(student);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        User student = getTableView().getItems().get(getIndex());
                        if (student != null) {
                            // Show "Allow Reapply" only for rejected students
                            if (student.getClearanceStatus() != null && 
                                student.getClearanceStatus().contains("❌")) {
                                btnAllowReapply.setVisible(true);
                                btnAllowReapply.setManaged(true);
                            } else {
                                btnAllowReapply.setVisible(false);
                                btnAllowReapply.setManaged(false);
                            }
                            setGraphic(buttons);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            });
        }
        
        // Color code based on status
        col4.setCellFactory(column -> new TableCell<User, String>() {
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
                    } else if (item.contains("🔄")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (item.contains("⏳")) {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    // ==================== LOAD DATA METHODS ====================
    @FXML
    private void handleRefresh() {
        loadAllData();
        showAlert("Refreshed", "All data has been refreshed successfully!");
    }
    
    private void loadAllData() {
        loadAllStudents();
        loadOfficers();
        loadAllUsers();
        loadClearanceRequests();
        updateDashboardStats();
    }
    
    private void loadAllStudents() {
        allStudentsData.clear();
        approvedStudentsData.clear();
        rejectedStudentsData.clear();
        pendingStudentsData.clear();
        inProgressStudentsData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.id,
                    u.username,
                    u.full_name,
                    u.department,
                    u.year_level,
                    u.email,
                    u.phone,
                    u.status,
                    COALESCE(cr.status, 'NO_REQUEST') as clearance_status,
                    COALESCE(cr.can_reapply, FALSE) as can_reapply
                FROM users u
                LEFT JOIN clearance_requests cr ON u.id = cr.student_id 
                    AND cr.id = (SELECT MAX(id) FROM clearance_requests WHERE student_id = u.id)
                WHERE u.role = 'STUDENT'
                ORDER BY u.username
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                User student = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    "STUDENT",
                    rs.getString("email"),
                    rs.getString("department")
                );
                student.setYearLevel(rs.getString("year_level"));
                student.setPhone(rs.getString("phone"));
                student.setStatus(rs.getString("status"));
                
                String clearanceStatus = rs.getString("clearance_status");
                boolean canReapply = rs.getBoolean("can_reapply");
                
                // Set formatted status with reapply info
                student.setClearanceStatus(formatClearanceStatus(clearanceStatus, canReapply));
                student.setCanReapply(canReapply);
                
                allStudentsData.add(student);
                
                // Categorize by status - only show in rejected tab if can't reapply
                if (clearanceStatus.equals("FULLY_CLEARED") || clearanceStatus.equals("APPROVED")) {
                    approvedStudentsData.add(student);
                } else if (clearanceStatus.equals("REJECTED") && !canReapply) {
                    rejectedStudentsData.add(student);
                } else if (clearanceStatus.equals("PENDING")) {
                    pendingStudentsData.add(student);
                } else if (clearanceStatus.equals("IN_PROGRESS")) {
                    inProgressStudentsData.add(student);
                }
                // Note: Students with REJECTED and canReapply=true don't appear in any category tab
                // They only appear in "All Students" tab
            }
            
            // Set data to tables
            tableAllStudents.setItems(allStudentsData);
            tableApprovedStudents.setItems(approvedStudentsData);
            tableRejectedStudents.setItems(rejectedStudentsData);
            tablePendingStudents.setItems(pendingStudentsData);
            tableInProgressStudents.setItems(inProgressStudentsData);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load students: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    private void loadOfficers() {
        officersData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT * FROM users 
                WHERE role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD', 'ADMIN') 
                ORDER BY role, username
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                User officer = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("email"),
                    rs.getString("department")
                );
                officer.setStatus(rs.getString("status"));
                officersData.add(officer);
            }
            
            tableOfficers.setItems(officersData);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load officers: " + e.getMessage());
        }
    }
    
    private void loadAllUsers() {
        allUsersData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM users ORDER BY role, username";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("email"),
                    rs.getString("department")
                );
                user.setStatus(rs.getString("status"));
                allUsersData.add(user);
            }
            
            tableAllUsers.setItems(allUsersData);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    private void loadClearanceRequests() {
        requestData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cr.id, u.username, u.full_name, u.department, 
                       cr.request_date, cr.status, COUNT(ca.id) as approved_count
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                GROUP BY cr.id, u.username, u.full_name, u.department, cr.request_date, cr.status
                ORDER BY cr.request_date DESC
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ClearanceRequest req = new ClearanceRequest(
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("status"),
                    rs.getTimestamp("request_date").toString(),
                    rs.getInt("approved_count")
                );
                req.setRequestId(rs.getInt("id"));
                requestData.add(req);
            }

            tableRequests.setItems(requestData);

        } catch (Exception e) {
            showAlert("Error", "Failed to load requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String formatClearanceStatus(String status, boolean canReapply) {
        if (status == null) status = "NO_REQUEST";
        
        switch (status) {
            case "FULLY_CLEARED":
            case "APPROVED":
                return "✅ Approved";
            case "REJECTED":
                if (canReapply) {
                    return "❌ Rejected - Can Reapply";
                } else {
                    return "❌ Rejected";
                }
            case "IN_PROGRESS":
                return "🔄 In Progress";
            case "PENDING":
                return "⏳ Pending";
            case "NO_REQUEST":
                return "📝 No Request";
            default:
                return status;
        }
    }

    // Add overloaded method for single parameter (if needed elsewhere)
    private String formatClearanceStatus(String status) {
        return formatClearanceStatus(status, false);
    }
    private void updateDashboardStats() {
        lblTotalStudents.setText("Students: " + allStudentsData.size());
        lblTotalOfficers.setText("Officers: " + officersData.size());
        
        // Count total requests
        int totalRequests = 0;
        for (User student : allStudentsData) {
            if (!student.getClearanceStatus().equals("📝 No Request")) {
                totalRequests++;
            }
        }
        lblTotalRequests.setText("Requests: " + totalRequests);
    }
    
    // ==================== ALLOW STUDENT TO REAPPLY ====================
    private void allowStudentReapply(User student) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Allow Student to Reapply");
        confirm.setHeaderText("Allow Clearance Reapplication");
        confirm.setContentText("Allow " + student.getFullName() + " (" + student.getUsername() + 
                             ") to submit a new clearance request?\n\n" +
                             "This student's previous request was rejected.\n" +
                             "Allowing reapplication will enable them to submit a new request.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                enableStudentReapply(student);
            }
        });
    }
    
    private void enableStudentReapply(User student) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Allow Student to Reapply");
        confirm.setHeaderText("Allow Clearance Reapplication");
        confirm.setContentText("Allow " + student.getFullName() + " (" + student.getUsername() + 
                             ") to submit a new clearance request?\n\n" +
                             "This student's previous request was rejected.\n" +
                             "Allowing reapplication will enable them to submit a new request.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // First, get the latest rejected request ID
                    String getLatestIdSql = """
                        SELECT MAX(id) as latest_id 
                        FROM clearance_requests 
                        WHERE student_id = ? AND status = 'REJECTED'
                        """;
                    
                    PreparedStatement getStmt = conn.prepareStatement(getLatestIdSql);
                    getStmt.setInt(1, student.getId());
                    ResultSet rs = getStmt.executeQuery();
                    
                    if (rs.next()) {
                        int latestRequestId = rs.getInt("latest_id");
                        
                        if (latestRequestId > 0) {
                            // Now update using the ID we found
                            String updateSql = """
                                UPDATE clearance_requests 
                                SET can_reapply = TRUE 
                                WHERE id = ?
                                """;
                            
                            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                            updateStmt.setInt(1, latestRequestId);
                            
                            int updated = updateStmt.executeUpdate();
                            if (updated > 0) {
                                // Update the student's status in the UI immediately
                                student.setCanReapply(true);
                                student.setClearanceStatus(formatClearanceStatus("REJECTED", true));
                                
                                // Refresh the table data
                                refreshStudentTableRows();
                                
                                showAlert("Success", student.getFullName() + " can now reapply for clearance!");
                            } else {
                                showAlert("Error", "Failed to update clearance request.");
                            }
                        } else {
                            showAlert("Error", "No rejected request found for this student.");
                        }
                    } else {
                        showAlert("Error", "No rejected request found for this student.");
                    }
                    
                } catch (Exception e) {
                    showAlert("Error", "Failed to allow reapplication: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Add this helper method to refresh table rows
    private void refreshStudentTableRows() {
        // Refresh the tables to reflect changes
        tableAllStudents.refresh();
        tableRejectedStudents.refresh();
        
        // Also reload data to update categorization
        loadAllStudents();
    }
    
    
    
    // ==================== VIEW STUDENT DETAILS ====================
    private void viewStudentDetails(User student) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Student Details");
        dialog.setHeaderText("Student Information: " + student.getFullName());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get student's clearance history
            String sql = """
                SELECT 
                    cr.id,
                    cr.request_date,
                    cr.status,
                    cr.completion_date,
                    cr.can_reapply,
                    GROUP_CONCAT(CONCAT(ca.officer_role, ': ', ca.status)) as approvals
                FROM clearance_requests cr
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id
                WHERE cr.student_id = ?
                GROUP BY cr.id
                ORDER BY cr.request_date DESC
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, student.getId());
            ResultSet rs = ps.executeQuery();
            
            StringBuilder details = new StringBuilder();
            details.append("=== STUDENT INFORMATION ===\n\n");
            details.append("Name: ").append(student.getFullName()).append("\n");
            details.append("ID: ").append(student.getUsername()).append("\n");
            details.append("Department: ").append(student.getDepartment()).append("\n");
            details.append("Year: ").append(student.getYearLevel()).append("\n");
            details.append("Email: ").append(student.getEmail()).append("\n");
            details.append("Phone: ").append(student.getPhone()).append("\n");
            details.append("Status: ").append(student.getStatus()).append("\n");
            details.append("Clearance Status: ").append(student.getClearanceStatus()).append("\n");
            details.append("Can Reapply: ").append(student.isCanReapply() ? "Yes" : "No").append("\n\n");
            
            details.append("=== CLEARANCE HISTORY ===\n\n");
            int requestCount = 0;
            while (rs.next()) {
                requestCount++;
                details.append("Request #").append(requestCount).append(":\n");
                details.append("  ID: ").append(rs.getInt("id")).append("\n");
                details.append("  Date: ").append(rs.getTimestamp("request_date")).append("\n");
                details.append("  Status: ").append(rs.getString("status")).append("\n");
                if (rs.getTimestamp("completion_date") != null) {
                    details.append("  Completed: ").append(rs.getTimestamp("completion_date")).append("\n");
                }
                details.append("  Can Reapply: ").append(rs.getBoolean("can_reapply") ? "Yes" : "No").append("\n");
                
                String approvals = rs.getString("approvals");
                if (approvals != null) {
                    details.append("  Approvals:\n");
                    String[] approvalList = approvals.split(",");
                    for (String approval : approvalList) {
                        details.append("    - ").append(approval.trim()).append("\n");
                    }
                }
                details.append("\n");
            }
            
            if (requestCount == 0) {
                details.append("No clearance requests found.\n");
            }
            
            TextArea textArea = new TextArea(details.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(600, 400);
            
            dialog.getDialogPane().setContent(textArea);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load student details: " + e.getMessage());
        }
    }
    
    // ==================== ORIGINAL METHODS FROM YOUR CODE ====================
    
    // 1. STUDENT REGISTRATION
    @FXML
    private void openRegisterStudent() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Student");
        dialog.setHeaderText("Enter Student Information");
        DialogPane pane = new DialogPane();
        dialog.setDialogPane(pane);
        ButtonType registerButton = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        GridPane grid = createStudentForm();
        pane.setContent(grid);
        Button btnRegister = (Button) pane.lookupButton(registerButton);
        btnRegister.addEventFilter(ActionEvent.ACTION, event -> {
            boolean valid = registerStudentFromForm(grid);
            if (!valid) {
                event.consume();
            }
        });
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == registerButton) {
            loadAllUsers();
            showAlert("Success", "Student registered successfully!");
        }
    }

    private GridPane createStudentForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtStudentId = new TextField();
        txtStudentId.setPromptText("DBU1601374");
        txtStudentId.setText("DBU");

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("dbu1601374");
        txtUsername.setText("dbu");
        txtUsername.setEditable(false);

        txtStudentId.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.startsWith("DBU")) {
                newText = "DBU" + newText.replaceAll("(?i)DBU", "");
            }
            String digits = newText.substring(3).replaceAll("[^\\d]", "");
            if (digits.length() > 7) digits = digits.substring(0, 7);
            txtStudentId.setText("DBU" + digits);
            txtStudentId.positionCaret(txtStudentId.getText().length());
            txtUsername.setText("dbu" + digits);
        });

        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email (optional)");

        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);

        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(150);

        txtPhoneSuffix.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) txtPhoneSuffix.setText(newVal.replaceAll("[^\\d]", ""));
            if (txtPhoneSuffix.getText().length() > 8)
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);

        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll(
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration",
            "Accounting", "Economics", "Mathematics", "Food Engineering", "Chemistry", "Biology"
        );
        cmbDepartment.setPromptText("Select Department");

        ComboBox<String> cmbYear = new ComboBox<>();
        cmbYear.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year");
        cmbYear.setPromptText("Select Year");

        grid.add(new Label("Student ID*:"), 0, 0);
        grid.add(txtStudentId, 1, 0);
        grid.add(new Label("Username*:"), 0, 1);
        grid.add(txtUsername, 1, 1);
        grid.add(new Label("Full Name*:"), 0, 2);
        grid.add(txtFullName, 1, 2);
        grid.add(new Label("Password*:"), 0, 3);
        grid.add(txtPassword, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(txtEmail, 1, 4);
        grid.add(new Label("Phone*:"), 0, 5);
        grid.add(phoneBox, 1, 5);
        grid.add(new Label("Department*:"), 0, 6);
        grid.add(cmbDepartment, 1, 6);
        grid.add(new Label("Year Level*:"), 0, 7);
        grid.add(cmbYear, 1, 7);

        grid.setUserData(new Object[]{
            txtStudentId, txtUsername, txtFullName, txtPassword,
            txtEmail, cmbPhonePrefix, txtPhoneSuffix, cmbDepartment, cmbYear
        });

        return grid;
    }

    private boolean registerStudentFromForm(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtStudentId = (TextField) fields[0];
        TextField txtUsername = (TextField) fields[1];
        TextField txtFullName = (TextField) fields[2];
        PasswordField txtPassword = (PasswordField) fields[3];
        TextField txtEmail = (TextField) fields[4];
        ComboBox<String> cmbPhonePrefix = (ComboBox<String>) fields[5];
        TextField txtPhoneSuffix = (TextField) fields[6];
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[7];
        ComboBox<String> cmbYear = (ComboBox<String>) fields[8];

        String inputId = txtStudentId.getText().trim();
        if (!inputId.startsWith("DBU") || inputId.length() != 10) {
            showAlert("Error", "Student ID must be exactly 7 digits after DBU!");
            return false;
        }

        String digits = inputId.substring(3);
        String studentId = "DBU" + digits;
        String username = "dbu" + digits;

        String fullName = txtFullName.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();

        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();

        if (fullName.isEmpty() || password.isEmpty() ||
            phonePrefix == null || phoneSuffix.isEmpty() || department == null || year == null) {
            showAlert("Error", "Please fill all required fields marked with *!");
            return false;
        }

        if (!phoneSuffix.matches("^\\d{8}$")) {
            showAlert("Error", "Phone number suffix must be exactly 8 digits!");
            return false;
        }

        if (password.length() < 6) {
            showAlert("Error", "Password must be at least 6 characters long!");
            return false;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Error", "Invalid email format!");
            return false;
        }

        String finalPhone = phonePrefix + phoneSuffix;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkDuplicate = "SELECT id FROM users WHERE username = ? OR phone = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkDuplicate);
            checkStmt.setString(1, username);
            checkStmt.setString(2, finalPhone);
            ResultSet checkRs = checkStmt.executeQuery();

            if (checkRs.next()) {
                showAlert("Error", "Username or Phone number already exists!");
                return false;
            }

            String sql = """
                INSERT INTO users (username, password, full_name, role, email, phone, department, year_level, status)
                VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?, 'ACTIVE')
            """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.setString(4, email.isEmpty() ? null : email);
            stmt.setString(5, finalPhone);
            stmt.setString(6, department);
            stmt.setString(7, year);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            showAlert("Error", "Registration failed: " + e.getMessage());
            return false;
        }
    }
    
    // 2. OFFICER MANAGEMENT
    @FXML
    private void openManageOfficers() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Department Officers");
        dialog.setHeaderText("Add New Officer");
        DialogPane pane = dialog.getDialogPane();
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        GridPane grid = createOfficerForm();
        pane.setContent(grid);
        Button btnSave = (Button) pane.lookupButton(saveButton);
        btnSave.addEventFilter(ActionEvent.ACTION, event -> {
            boolean valid = registerOfficerFromForm(grid);
            if (!valid) {
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButton) {
            loadAllUsers();
            showAlert("Success", "Officer registered successfully!");
        }
    }

    private GridPane createOfficerForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");
        
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");
        
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");
        
        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);

        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(150);
        
        txtPhoneSuffix.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPhoneSuffix.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (txtPhoneSuffix.getText().length() > 8) {
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
            }
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);

        ComboBox<String> cmbRole = new ComboBox<>();
        ComboBox<String> cmbDepartment = new ComboBox<>();

        cmbRole.getItems().addAll("LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD");
        cmbRole.setPromptText("Select Role");
        
        cmbDepartment.getItems().addAll(
            "Library", 
            "Cafeteria", 
            "Dormitory", 
            "Registrar Office", 
            "Computer Science",
            "Software Engineering",
            "Electrical Engineering",
            "Mechanical Engineering",
            "Civil Engineering",
            "Business Administration"
        );
        cmbDepartment.setPromptText("Select Department");

        grid.add(new Label("Full Name*:"), 0, 0);
        grid.add(txtFullName, 1, 0);
        grid.add(new Label("Username*:"), 0, 1);
        grid.add(txtUsername, 1, 1);
        grid.add(new Label("Password*:"), 0, 2);
        grid.add(txtPassword, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(txtEmail, 1, 3);
        
        grid.add(new Label("Phone*:"), 0, 4);
        grid.add(phoneBox, 1, 4);
        
        grid.add(new Label("Role*:"), 0, 5);
        grid.add(cmbRole, 1, 5);
        grid.add(new Label("Department*:"), 0, 6);
        grid.add(cmbDepartment, 1, 6);

        grid.setUserData(new Object[]{
            txtFullName, txtUsername, txtPassword, txtEmail, 
            cmbPhonePrefix, txtPhoneSuffix, cmbRole, cmbDepartment
        });

        return grid;
    }

    private boolean registerOfficerFromForm(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtFullName = (TextField) fields[0];
        TextField txtUsername = (TextField) fields[1];
        PasswordField txtPassword = (PasswordField) fields[2];
        TextField txtEmail = (TextField) fields[3];

        ComboBox<String> cmbPhonePrefix = (ComboBox<String>) fields[4];
        TextField txtPhoneSuffix = (TextField) fields[5];

        ComboBox<String> cmbRole = (ComboBox<String>) fields[6];
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[7];

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String role = cmbRole.getValue();
        String department = cmbDepartment.getValue();

        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() ||
            role == null || department == null || phonePrefix == null || phoneSuffix.isEmpty()) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        if (!phoneSuffix.matches("^\\d{8}$")) {
            showAlert("Error", "Phone number suffix must be exactly 8 digits!");
            return false;
        }

        if (password.length() < 6) {
            showAlert("Error", "Password must be at least 6 characters long!");
            return false;
        }

        String finalPhone = phonePrefix + phoneSuffix;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT username, phone FROM users WHERE username = ? OR phone = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, finalPhone);

            ResultSet checkRs = checkStmt.executeQuery();

            if (checkRs.next()) {
                if (username.equals(checkRs.getString("username"))) {
                    showAlert("Error", "Username '" + username + "' already exists!");
                } else {
                    showAlert("Error", "Phone number '" + finalPhone + "' already exists!");
                }
                return false;
            }

            String insertSql = """
                INSERT INTO users (username, password, full_name, role, email, phone, department, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
            """;

            PreparedStatement stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.setString(4, role);
            stmt.setString(5, email.isEmpty() ? null : email);
            stmt.setString(6, finalPhone);
            stmt.setString(7, department);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            showAlert("Error", "Failed to register officer: " + e.getMessage());
            return false;
        }
    }
    
    // 3. USER MANAGEMENT
    @FXML
    private void resetUserPassword() {
        User selectedUser = tableAllUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user first!");
            return;
        }

        if ("admin".equals(selectedUser.getUsername())) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Admin Password Reset");
            confirm.setHeaderText("Reset ADMIN Password");
            confirm.setContentText("You are about to reset the ADMIN password.\nThis requires extra authorization.\n\nProceed?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        TextInputDialog dialog = new TextInputDialog("np123");
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset Password for: " + selectedUser.getFullName());
        dialog.setContentText("Enter new password (min 6 characters):");

        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword.length() < 6) {
                showAlert("Error", "Password must be at least 6 characters!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE users SET password = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, newPassword);
                stmt.setInt(2, selectedUser.getId());
                
                if (stmt.executeUpdate() > 0) {
                    showAlert("Success", "Password reset successfully for " + selectedUser.getFullName() + 
                                      "\nNew password: " + newPassword);
                    loadAllUsers();
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to reset password: " + e.getMessage());
            }
        });
    }

    @FXML
    private void toggleUserStatus() {
        User selectedUser = tableAllUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user first!");
            return;
        }

        String newStatus = "ACTIVE".equals(selectedUser.getStatus()) ? "INACTIVE" : "ACTIVE";
        String action = "ACTIVE".equals(newStatus) ? "activate" : "deactivate";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Status Change");
        confirm.setHeaderText("Are you sure you want to " + action + " this user?");
        confirm.setContentText("User: " + selectedUser.getFullName() + 
                             "\nRole: " + selectedUser.getRole() +
                             "\nUsername: " + selectedUser.getUsername());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE users SET status = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, newStatus);
                stmt.setInt(2, selectedUser.getId());
                
                if (stmt.executeUpdate() > 0) {
                    showAlert("Success", "User status updated to: " + newStatus);
                    loadAllUsers();
                    loadAllStudents();
                    loadOfficers();
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to update status: " + e.getMessage());
            }
        }
    }
    
    // 4. WORKFLOW MANAGEMENT
    @FXML
    private void openWorkflowManagement() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Approval Workflow Management");
        dialog.setHeaderText("Configure Department Approval Sequence");
        dialog.setContentText("Drag and drop or select departments in order of approval:");

        ButtonType saveButton = new ButtonType("Save Workflow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        List<String> currentWorkflow = getCurrentWorkflow();
        
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(currentWorkflow);
        listView.setPrefHeight(200);

        ListView<String> availableDepartments = new ListView<>();
        String[] allDepartments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
        for (String dept : allDepartments) {
            if (!currentWorkflow.contains(dept)) {
                availableDepartments.getItems().add(dept);
            }
        }
        availableDepartments.setPrefHeight(200);

        HBox buttonBox = new HBox(10);
        Button btnUp = new Button("↑");
        Button btnDown = new Button("↓");
        Button btnAdd = new Button("Add →");
        Button btnRemove = new Button("← Remove");
        
        btnUp.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                String item = listView.getItems().get(selectedIndex);
                listView.getItems().remove(selectedIndex);
                listView.getItems().add(selectedIndex - 1, item);
                listView.getSelectionModel().select(selectedIndex - 1);
            }
        });
       
        btnDown.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex < listView.getItems().size() - 1 && selectedIndex >= 0) {
                String item = listView.getItems().get(selectedIndex);
                listView.getItems().remove(selectedIndex);
                listView.getItems().add(selectedIndex + 1, item);
                listView.getSelectionModel().select(selectedIndex + 1);
            }
        });
        
        btnAdd.setOnAction(e -> {
            String selected = availableDepartments.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listView.getItems().add(selected);
                availableDepartments.getItems().remove(selected);
            }
        });
        
        btnRemove.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listView.getItems().remove(selected);
                availableDepartments.getItems().add(selected);
            }
        });

        buttonBox.getChildren().addAll(btnUp, btnDown, btnAdd, btnRemove);

        HBox listsBox = new HBox(20);
        VBox availableBox = new VBox(5, new Label("Available Departments:"), availableDepartments);
        VBox workflowBox = new VBox(5, new Label("Current Workflow:"), listView);
        listsBox.getChildren().addAll(availableBox, workflowBox);

        content.getChildren().addAll(listsBox, buttonBox);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return saveWorkflow(listView.getItems());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            showAlert("Workflow Updated", result);
            loadAllData();
        });
    }

    private List<String> getCurrentWorkflow() {
        List<String> workflow = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT role FROM workflow_config ORDER BY sequence_order";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                workflow.add(rs.getString("role"));
            }
        } catch (Exception e) {
            workflow.add("LIBRARIAN");
            workflow.add("CAFETERIA");
            workflow.add("DORMITORY");
            workflow.add("REGISTRAR");
            workflow.add("DEPARTMENT_HEAD");
        }
        return workflow;
    }

    private String saveWorkflow(List<String> workflow) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String clearSql = "DELETE FROM workflow_config";
            PreparedStatement clearStmt = conn.prepareStatement(clearSql);
            clearStmt.executeUpdate();

            String insertSql = "INSERT INTO workflow_config (role, sequence_order) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            for (int i = 0; i < workflow.size(); i++) {
                insertStmt.setString(1, workflow.get(i));
                insertStmt.setInt(2, i + 1);
                insertStmt.addBatch();
            }
            
            insertStmt.executeBatch();
            return "Workflow updated successfully!\nNew sequence: " + String.join(" → ", workflow);
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 5. ACADEMIC SESSION MANAGEMENT
    @FXML
    private void openSessionManagement() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Academic Session Management");
        dialog.setHeaderText("Create New Academic Session");

        ButtonType saveButton = new ButtonType("Create Session", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtSessionName = new TextField();
        txtSessionName.setPromptText("e.g., Fall 2024, Spring 2025");
        DatePicker dpStartDate = new DatePicker(LocalDate.now());
        DatePicker dpEndDate = new DatePicker(LocalDate.now().plusMonths(6));
        CheckBox chkActive = new CheckBox("Set as Active Session");
        chkActive.setSelected(true);

        grid.add(new Label("Session Name*:"), 0, 0);
        grid.add(txtSessionName, 1, 0);
        grid.add(new Label("Start Date*:"), 0, 1);
        grid.add(dpStartDate, 1, 1);
        grid.add(new Label("End Date*:"), 0, 2);
        grid.add(dpEndDate, 1, 2);
        grid.add(chkActive, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return createAcademicSession(txtSessionName.getText(), dpStartDate.getValue(), 
                                          dpEndDate.getValue(), chkActive.isSelected()) ? ButtonType.OK : null;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private boolean createAcademicSession(String sessionName, LocalDate startDate, LocalDate endDate, boolean isActive) {
        if (sessionName == null || sessionName.trim().isEmpty() || startDate == null || endDate == null) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        if (endDate.isBefore(startDate)) {
            showAlert("Error", "End date cannot be before start date!");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (isActive) {
                String deactivateSql = "UPDATE academic_sessions SET is_active = false";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();
            }

            String sql = "INSERT INTO academic_sessions (session_name, start_date, end_date, is_active) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionName.trim());
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            stmt.setBoolean(4, isActive);

            if (stmt.executeUpdate() > 0) {
                showAlert("Success", "Academic session created successfully!\n" +
                                  "Session: " + sessionName + "\n" +
                                  "Period: " + startDate + " to " + endDate +
                                  (isActive ? "\n\nThis is now the active session." : ""));
                return true;
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to create session: " + e.getMessage());
        }
        return false;
    }
    
    // 6. CERTIFICATE GENERATION
    @FXML
    private void generateClearanceCertificates() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Clearance Certificates");
        dialog.setHeaderText("Generate certificates for cleared students");

        ButtonType generateButton = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker dpFromDate = new DatePicker(LocalDate.now().minusMonths(1));
        DatePicker dpToDate = new DatePicker(LocalDate.now());
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll("All Departments", "Software Engineering", "Computer Science", 
                                      "Electrical Engineering", "Mechanical Engineering", "Civil Engineering");

        grid.add(new Label("From Date:"), 0, 0);
        grid.add(dpFromDate, 1, 0);
        grid.add(new Label("To Date:"), 0, 1);
        grid.add(dpToDate, 1, 1);
        grid.add(new Label("Department:"), 0, 2);
        grid.add(cmbDepartment, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButton) {
                generateCertificates(dpFromDate.getValue(), dpToDate.getValue(), cmbDepartment.getValue());
                return ButtonType.OK;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void generateCertificates(LocalDate fromDate, LocalDate toDate, String department) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT u.username, u.full_name, u.department, cr.request_date, cr.completion_date " +
                "FROM clearance_requests cr " +
                "JOIN users u ON cr.student_id = u.id " +
                "WHERE cr.status = 'FULLY_CLEARED' " +
                "AND DATE(cr.completion_date) BETWEEN ? AND ? "
            );

            if (department != null && !"All Departments".equals(department)) {
                sql.append("AND u.department = ? ");
            }
            
            sql.append("ORDER BY cr.completion_date DESC");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            
            if (department != null && !"All Departments".equals(department)) {
                ps.setString(3, department);
            }

            ResultSet rs = ps.executeQuery();

            StringBuilder report = new StringBuilder();
            report.append("=== CERTIFICATES BATCH GENERATION REPORT ===\n\n");
            report.append("Date Range: ").append(fromDate).append(" to ").append(toDate).append("\n");
            report.append("Department: ").append(department).append("\n");
            report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
            report.append("-".repeat(50)).append("\n\n");
            
            int count = 0;
            int successful = 0;

            while (rs.next()) {
                count++;
                report.append("Certificate #").append(count).append("\n");
                report.append("Student: ").append(rs.getString("full_name")).append("\n");
                report.append("ID: ").append(rs.getString("username")).append("\n");
                report.append("Department: ").append(rs.getString("department")).append("\n");
                report.append("Clearance Date: ").append(rs.getDate("completion_date")).append("\n");
                
                try {
                    successful++;
                    report.append("Status: ✅ Generated\n");
                } catch (Exception e) {
                    report.append("Status: ❌ Failed - ").append(e.getMessage()).append("\n");
                }
                report.append("---\n");
            }

            if (count > 0) {
                report.append("\n=== SUMMARY ===\n");
                report.append("Total students found: ").append(count).append("\n");
                report.append("Successfully generated: ").append(successful).append("\n");
                report.append("Failed: ").append(count - successful).append("\n");
                
                showAlert("Certificates Generated", 
                         "Successfully processed " + count + " students!\n\n" + report.toString());
            } else {
                showAlert("No Certificates", "No cleared students found in selected period.");
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to generate certificates: " + e.getMessage());
        }
    }
    
    // 7. CERTIFICATE VERIFICATION
    @FXML
    private void openCertificateVerification() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Certificate Verification");
        dialog.setHeaderText("Verify Clearance Certificate");
        dialog.setContentText("Enter Student ID or Certificate ID:");

        dialog.showAndWait().ifPresent(studentId -> {
            verifyCertificateInDatabase(studentId);
        });
    }
    
    @FXML
    private void verifyCertificate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verify Certificate");
        dialog.setHeaderText("Verify Clearance Certificate");
        dialog.setContentText("Enter Student ID or Certificate ID:");
        
        dialog.showAndWait().ifPresent(input -> {
            verifyCertificateInDatabase(input);
        });
    }

    private void verifyCertificateInDatabase(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    u.username as student_id,
                    u.department,
                    cr.request_date,
                    cr.completion_date,
                    cr.status,
                    COUNT(ca.id) as approved_count
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                WHERE (u.username = ? OR u.id = ?) AND cr.status = 'FULLY_CLEARED'
                GROUP BY u.id, cr.id
                ORDER BY cr.completion_date DESC 
                LIMIT 1
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, searchTerm);
            try {
                ps.setInt(2, Integer.parseInt(searchTerm));
            } catch (NumberFormatException e) {
                ps.setInt(2, -1);
            }
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String result = "✅ CERTIFICATE VERIFICATION SUCCESSFUL\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Student ID: " + rs.getString("student_id") + "\n" +
                              "Department: " + rs.getString("department") + "\n" +
                              "Clearance Status: " + rs.getString("status") + "\n" +
                              "Request Date: " + rs.getDate("request_date") + "\n" +
                              "Completion Date: " + rs.getDate("completion_date") + "\n" +
                              "Approvals: " + rs.getInt("approved_count") + "/5 departments approved\n\n" +
                              "This certificate is VALID and verified in our system.";
                
                showAlert("Certificate Verified", result);
            } else {
                showAlert("Not Found", "No valid clearance certificate found for: " + searchTerm + 
                                    "\n\nPossible reasons:\n" +
                                    "• Student ID is incorrect\n" +
                                    "• Clearance is not fully approved\n" +
                                    "• No clearance request exists");
            }
            
        } catch (Exception e) {
            showAlert("Error", "Verification failed: " + e.getMessage());
        }
    }
    
    // 8. SEMESTER ROLLOVER
    @FXML
    private void processSemesterRollover() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Semester Rollover");
        confirm.setHeaderText("Process End-of-Semester Rollover");
        confirm.setContentText("⚠️  CRITICAL OPERATION  ⚠️\n\n" +
                             "This will:\n" +
                             "• Archive all completed clearances\n" +
                             "• Reset/expire pending requests\n" +
                             "• Update student year levels\n" +
                             "• Create new academic session\n\n" +
                             "❌ This action cannot be undone!\n\n" +
                             "✅ Proceed with semester rollover?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performSemesterRollover();
        }
    }

    private void performSemesterRollover() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                StringBuilder report = new StringBuilder();
                report.append("🎉 SEMESTER ROLLOVER REPORT\n");
                report.append("==========================\n\n");
                
                // 1. Archive completed requests
                String archiveSql = "INSERT INTO clearance_requests_archive " +
                                  "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                  "FROM clearance_requests WHERE status = 'FULLY_CLEARED'";
                PreparedStatement archiveStmt = conn.prepareStatement(archiveSql);
                int archived = archiveStmt.executeUpdate();
                report.append("✓ Archived " + archived + " cleared requests\n");
                
                // 2. Archive rejected requests
                String archiveRejectedSql = "INSERT INTO clearance_requests_archive " +
                                         "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                         "FROM clearance_requests WHERE status = 'REJECTED'";
                PreparedStatement archiveRejectedStmt = conn.prepareStatement(archiveRejectedSql);
                int archivedRejected = archiveRejectedStmt.executeUpdate();
                report.append("✓ Archived " + archivedRejected + " rejected requests\n");

                // 3. Reset pending/in-progress requests to EXPIRED
                String resetSql = "UPDATE clearance_requests SET status = 'EXPIRED' " +
                                "WHERE status IN ('PENDING', 'IN_PROGRESS')";
                PreparedStatement resetStmt = conn.prepareStatement(resetSql);
                int expired = resetStmt.executeUpdate();
                report.append("✓ Expired " + expired + " pending requests\n");

                // 4. Update student year levels
                String yearSql = "UPDATE users SET year_level = " +
                               "CASE " +
                               "WHEN year_level = '1st Year' THEN '2nd Year' " +
                               "WHEN year_level = '2nd Year' THEN '3rd Year' " +
                               "WHEN year_level = '3rd Year' THEN '4th Year' " +
                               "WHEN year_level = '4th Year' THEN '5th Year' " +
                               "WHEN year_level = '5th Year' THEN 'Graduated' " +
                               "ELSE year_level " +
                               "END " +
                               "WHERE role = 'STUDENT' AND status = 'ACTIVE'";
                PreparedStatement yearStmt = conn.prepareStatement(yearSql);
                int updated = yearStmt.executeUpdate();
                report.append("✓ Updated " + updated + " student year levels\n");

                // 5. Create new session
                LocalDate today = LocalDate.now();
                String sessionName;
                if (today.getMonthValue() <= 6) {
                    sessionName = "Spring Semester " + today.getYear();
                } else {
                    sessionName = "Fall Semester " + today.getYear();
                }
                
                // Deactivate old sessions
                String deactivateSql = "UPDATE academic_sessions SET is_active = false WHERE is_active = true";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();

                // Create new session
                String sessionSql = "INSERT INTO academic_sessions (session_name, start_date, end_date, is_active) " +
                                  "VALUES (?, ?, ?, true)";
                PreparedStatement sessionStmt = conn.prepareStatement(sessionSql);
                sessionStmt.setString(1, sessionName);
                sessionStmt.setDate(2, Date.valueOf(today));
                sessionStmt.setDate(3, Date.valueOf(today.plusMonths(6)));
                sessionStmt.executeUpdate();
                report.append("✓ Created new academic session: " + sessionName + "\n");

                conn.commit();
                report.append("\n✅ Rollover completed successfully!\n");
                report.append("System is ready for the new semester.");

                showAlert("Rollover Successful", report.toString());
                loadAllData();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            showAlert("Rollover Error", "Failed to process rollover: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== UTILITY METHODS ====================
    @FXML
    private void handleLogout() {
        try {
            System.out.println("[DEBUG] Logout button clicked.");

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout");
            confirm.setHeaderText("Confirm Logout");
            confirm.setContentText("Are you sure you want to logout?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                System.out.println("[DEBUG] User confirmed logout.");

                // Correct, consistent FXML path
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

                // Preserve current window size
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                Scene currentScene = lblWelcome.getScene();

                System.out.println("[DEBUG] Current Size -> W: "
                        + currentScene.getWidth() + " H: " + currentScene.getHeight());

                Scene newScene = new Scene(root,
                        currentScene.getWidth(),
                        currentScene.getHeight());

                stage.setScene(newScene);
                stage.setTitle("University Clearance System - Login");
                stage.centerOnScreen();

                System.out.println("[DEBUG] Scene switched successfully.");

            } else {
                System.out.println("[DEBUG] Logout canceled by user.");
            }

        } catch (Exception e) {
            System.out.println("[DEBUG] Exception occurred: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }


    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}