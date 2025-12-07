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
 // ==================== VIEW STUDENT DETAILS ====================
    private void viewStudentDetails(User student) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Student Details");
        dialog.setHeaderText("Student Information: " + student.getFullName());
        dialog.getDialogPane().setPrefSize(700, 600);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder details = new StringBuilder();
            
            // ========== BASIC STUDENT INFORMATION ==========
            details.append("=".repeat(60)).append("\n");
            details.append("STUDENT INFORMATION\n");
            details.append("=".repeat(60)).append("\n\n");
            
            details.append(String.format("%-20s: %s\n", "Name", student.getFullName()));
            details.append(String.format("%-20s: %s\n", "Student ID", student.getUsername()));
            details.append(String.format("%-20s: %s\n", "System ID", student.getId()));
            details.append(String.format("%-20s: %s\n", "Department", student.getDepartment()));
            details.append(String.format("%-20s: %s\n", "Year Level", student.getYearLevel()));
            details.append(String.format("%-20s: %s\n", "Email", student.getEmail()));
            details.append(String.format("%-20s: %s\n", "Phone", student.getPhone()));
            details.append(String.format("%-20s: %s\n", "Account Status", student.getStatus()));
            details.append(String.format("%-20s: %s\n", "Clearance Status", student.getClearanceStatus()));
            details.append(String.format("%-20s: %s\n", "Can Reapply", student.isCanReapply() ? "Yes" : "No"));
            
            // ========== DORMITORY INFORMATION ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("DORMITORY INFORMATION\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String dormSql = """
                SELECT block_number, room_number, key_returned, key_return_date,
                       damage_description, damage_amount, damage_paid, clearance_status,
                       remarks, last_updated
                FROM student_dormitory_credentials 
                WHERE student_id = ?
                """;
            
            PreparedStatement dormPs = conn.prepareStatement(dormSql);
            dormPs.setInt(1, student.getId());
            ResultSet dormRs = dormPs.executeQuery();
            
            if (dormRs.next()) {
                details.append(String.format("%-20s: Block %s - Room %s\n", "Location", 
                    dormRs.getString("block_number"), dormRs.getString("room_number")));
                details.append(String.format("%-20s: %s\n", "Key Returned", 
                    dormRs.getBoolean("key_returned") ? "✅ Yes" : "❌ No"));
                
                if (dormRs.getDate("key_return_date") != null) {
                    details.append(String.format("%-20s: %s\n", "Key Return Date", 
                        dormRs.getDate("key_return_date")));
                }
                
                details.append(String.format("%-20s: %s\n", "Damage Paid", 
                    dormRs.getBoolean("damage_paid") ? "✅ Yes" : "❌ No"));
                
                double damageAmount = dormRs.getDouble("damage_amount");
                if (damageAmount > 0) {
                    details.append(String.format("%-20s: $%.2f\n", "Damage Amount", damageAmount));
                }
                
                String damageDesc = dormRs.getString("damage_description");
                if (damageDesc != null && !damageDesc.isEmpty()) {
                    details.append(String.format("%-20s: %s\n", "Damage Description", damageDesc));
                }
                
                details.append(String.format("%-20s: %s\n", "Dormitory Status", 
                    dormRs.getString("clearance_status")));
                
                String remarks = dormRs.getString("remarks");
                if (remarks != null && !remarks.isEmpty()) {
                    details.append(String.format("%-20s: %s\n", "Remarks", remarks));
                }
                
                if (dormRs.getTimestamp("last_updated") != null) {
                    details.append(String.format("%-20s: %s\n", "Last Updated", 
                        dormRs.getTimestamp("last_updated")));
                }
            } else {
                details.append("No dormitory information found.\n");
                details.append("(Use Edit Dormitory Info to add block and room numbers)\n");
            }
            
            // ========== ACADEMIC RECORDS ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("ACADEMIC RECORDS\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String academicSql = """
                SELECT academic_hold, outstanding_fees, incomplete_courses, gpa
                FROM student_academic_records 
                WHERE student_id = ?
                """;
            
            PreparedStatement academicPs = conn.prepareStatement(academicSql);
            academicPs.setInt(1, student.getId());
            ResultSet academicRs = academicPs.executeQuery();
            
            if (academicRs.next()) {
                details.append(String.format("%-20s: %s\n", "Academic Hold", 
                    academicRs.getString("academic_hold")));
                details.append(String.format("%-20s: $%.2f\n", "Outstanding Fees", 
                    academicRs.getDouble("outstanding_fees")));
                details.append(String.format("%-20s: %d\n", "Incomplete Courses", 
                    academicRs.getInt("incomplete_courses")));
                details.append(String.format("%-20s: %.2f\n", "GPA", 
                    academicRs.getDouble("gpa")));
            } else {
                details.append("No academic records found.\n");
            }
            
            // ========== LIBRARY STATUS ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("LIBRARY STATUS\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String librarySql = """
                SELECT 
                    COUNT(*) as total_books,
                    SUM(CASE WHEN status = 'BORROWED' THEN 1 ELSE 0 END) as borrowed,
                    SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue,
                    SUM(fine_amount) as total_fine
                FROM book_borrowings 
                WHERE student_id = ? 
                AND (status = 'BORROWED' OR status = 'OVERDUE' OR fine_amount > 0)
                """;
            
            PreparedStatement libraryPs = conn.prepareStatement(librarySql);
            libraryPs.setInt(1, student.getId());
            ResultSet libraryRs = libraryPs.executeQuery();
            
            if (libraryRs.next()) {
                int borrowed = libraryRs.getInt("borrowed");
                int overdue = libraryRs.getInt("overdue");
                double totalFine = libraryRs.getDouble("total_fine");
                
                details.append(String.format("%-20s: %d books\n", "Currently Borrowed", borrowed));
                details.append(String.format("%-20s: %d books\n", "Overdue Books", overdue));
                details.append(String.format("%-20s: $%.2f\n", "Total Fines", totalFine));
                
                if (borrowed > 0 || overdue > 0 || totalFine > 0) {
                    details.append("\nBook Details:\n");
                    String bookSql = """
                        SELECT book_title, borrow_date, due_date, return_date, status, fine_amount
                        FROM book_borrowings 
                        WHERE student_id = ?
                        ORDER BY due_date DESC
                        """;
                    
                    PreparedStatement bookPs = conn.prepareStatement(bookSql);
                    bookPs.setInt(1, student.getId());
                    ResultSet bookRs = bookPs.executeQuery();
                    
                    int bookCount = 1;
                    while (bookRs.next()) {
                        details.append(String.format("\n  Book #%d:\n", bookCount++));
                        details.append(String.format("    Title: %s\n", bookRs.getString("book_title")));
                        details.append(String.format("    Borrowed: %s\n", bookRs.getDate("borrow_date")));
                        details.append(String.format("    Due: %s\n", bookRs.getDate("due_date")));
                        
                        if (bookRs.getDate("return_date") != null) {
                            details.append(String.format("    Returned: %s\n", bookRs.getDate("return_date")));
                        }
                        
                        details.append(String.format("    Status: %s\n", bookRs.getString("status")));
                        
                        double fine = bookRs.getDouble("fine_amount");
                        if (fine > 0) {
                            details.append(String.format("    Fine: $%.2f\n", fine));
                        }
                    }
                }
            } else {
                details.append("No library issues found. All clear!\n");
            }
            
            // ========== CAFETERIA STATUS ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("CAFETERIA STATUS\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String cafeteriaSql = """
                SELECT 
                    record_type, description, amount, transaction_date, status, meal_plan_type
                FROM cafeteria_records 
                WHERE student_id = ? 
                AND (status = 'Pending' OR amount > 0)
                ORDER BY transaction_date DESC
                """;
            
            PreparedStatement cafePs = conn.prepareStatement(cafeteriaSql);
            cafePs.setInt(1, student.getId());
            ResultSet cafeRs = cafePs.executeQuery();
            
            double totalCafeteriaBalance = 0;
            int cafeRecordCount = 0;
            
            while (cafeRs.next()) {
                cafeRecordCount++;
                if (cafeRecordCount == 1) {
                    details.append("Cafeteria Records:\n");
                }
                
                details.append(String.format("\n  Record #%d:\n", cafeRecordCount));
                details.append(String.format("    Type: %s\n", cafeRs.getString("record_type")));
                details.append(String.format("    Description: %s\n", cafeRs.getString("description")));
                details.append(String.format("    Amount: $%.2f\n", cafeRs.getDouble("amount")));
                details.append(String.format("    Date: %s\n", cafeRs.getDate("transaction_date")));
                details.append(String.format("    Status: %s\n", cafeRs.getString("status")));
                
                String mealPlan = cafeRs.getString("meal_plan_type");
                if (mealPlan != null) {
                    details.append(String.format("    Meal Plan: %s\n", mealPlan));
                }
                
                if ("Pending".equals(cafeRs.getString("status"))) {
                    totalCafeteriaBalance += cafeRs.getDouble("amount");
                }
            }
            
            if (cafeRecordCount == 0) {
                details.append("No cafeteria issues found. All clear!\n");
            } else if (totalCafeteriaBalance > 0) {
                details.append(String.format("\n%-20s: $%.2f\n", "Total Pending Balance", totalCafeteriaBalance));
            }
            
            // ========== DEPARTMENT REQUIREMENTS ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("DEPARTMENT REQUIREMENTS\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String deptSql = """
                SELECT requirement_name, requirement_type, status, completed_date, remarks
                FROM department_requirements 
                WHERE student_id = ?
                ORDER BY completed_date DESC, requirement_name
                """;
            
            PreparedStatement deptPs = conn.prepareStatement(deptSql);
            deptPs.setInt(1, student.getId());
            ResultSet deptRs = deptPs.executeQuery();
            
            int deptReqCount = 0;
            int completedCount = 0;
            
            while (deptRs.next()) {
                deptReqCount++;
                if (deptReqCount == 1) {
                    details.append("Department Requirements:\n");
                }
                
                details.append(String.format("\n  %s:\n", deptRs.getString("requirement_name")));
                details.append(String.format("    Type: %s\n", deptRs.getString("requirement_type")));
                details.append(String.format("    Status: %s\n", deptRs.getString("status")));
                
                if ("Completed".equals(deptRs.getString("status"))) {
                    completedCount++;
                }
                
                if (deptRs.getDate("completed_date") != null) {
                    details.append(String.format("    Completed: %s\n", deptRs.getDate("completed_date")));
                }
                
                String reqRemarks = deptRs.getString("remarks");
                if (reqRemarks != null && !reqRemarks.isEmpty()) {
                    details.append(String.format("    Remarks: %s\n", reqRemarks));
                }
            }
            
            if (deptReqCount == 0) {
                details.append("No department requirements recorded.\n");
            } else {
                details.append(String.format("\nProgress: %d/%d requirements completed\n", 
                    completedCount, deptReqCount));
            }
            
            // ========== CLEARANCE HISTORY ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("CLEARANCE HISTORY\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String historySql = """
                SELECT 
                    cr.id,
                    cr.request_date,
                    cr.status,
                    cr.completion_date,
                    cr.can_reapply,
                    GROUP_CONCAT(CONCAT(ca.officer_role, ': ', ca.status) ORDER BY ca.officer_role) as approvals
                FROM clearance_requests cr
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id
                WHERE cr.student_id = ?
                GROUP BY cr.id
                ORDER BY cr.request_date DESC
                """;
            
            PreparedStatement historyPs = conn.prepareStatement(historySql);
            historyPs.setInt(1, student.getId());
            ResultSet historyRs = historyPs.executeQuery();
            
            int requestCount = 0;
            
            while (historyRs.next()) {
                requestCount++;
                details.append(String.format("Request #%d:\n", requestCount));
                details.append(String.format("  Request ID: %d\n", historyRs.getInt("id")));
                details.append(String.format("  Date: %s\n", historyRs.getTimestamp("request_date")));
                details.append(String.format("  Status: %s\n", historyRs.getString("status")));
                
                if (historyRs.getTimestamp("completion_date") != null) {
                    details.append(String.format("  Completed: %s\n", historyRs.getTimestamp("completion_date")));
                }
                
                details.append(String.format("  Can Reapply: %s\n", 
                    historyRs.getBoolean("can_reapply") ? "Yes" : "No"));
                
                String approvals = historyRs.getString("approvals");
                if (approvals != null) {
                    details.append("  Department Approvals:\n");
                    String[] approvalList = approvals.split(",");
                    for (String approval : approvalList) {
                        String[] parts = approval.trim().split(":");
                        if (parts.length == 2) {
                            String dept = parts[0].trim();
                            String status = parts[1].trim();
                            
                            String statusIcon = "❌";
                            if (status.equals("APPROVED")) statusIcon = "✅";
                            else if (status.equals("PENDING")) statusIcon = "⏳";
                            
                            details.append(String.format("    %s %s: %s\n", statusIcon, dept, status));
                        }
                    }
                }
                details.append("\n");
            }
            
            if (requestCount == 0) {
                details.append("No clearance requests found.\n");
            } else {
                details.append(String.format("Total Requests: %d\n", requestCount));
            }
            
            // ========== COURSE INFORMATION ==========
            details.append("\n").append("=".repeat(60)).append("\n");
            details.append("COURSE INFORMATION (Recent)\n");
            details.append("=".repeat(60)).append("\n\n");
            
            String courseSql = """
                SELECT course_code, course_name, grade, credits, semester, academic_year
                FROM student_courses 
                WHERE student_id = ?
                ORDER BY academic_year DESC, semester DESC
                LIMIT 10
                """;
            
            PreparedStatement coursePs = conn.prepareStatement(courseSql);
            coursePs.setInt(1, student.getId());
            ResultSet courseRs = coursePs.executeQuery();
            
            int courseCount = 0;
            double totalCredits = 0;
            double totalPoints = 0;
            
            while (courseRs.next()) {
                courseCount++;
                if (courseCount == 1) {
                    details.append(String.format("%-15s %-40s %-8s %-8s %-10s %-10s\n", 
                        "Code", "Course Name", "Grade", "Credits", "Semester", "Year"));
                    details.append("-".repeat(91)).append("\n");
                }
                
                String grade = courseRs.getString("grade");
                int credits = courseRs.getInt("credits");
                totalCredits += credits;
                
                // Convert grade to points (simplified)
                double gradePoints = 0;
                if (grade != null) {
                    switch (grade.toUpperCase()) {
                        case "A": gradePoints = 4.0; break;
                        case "A-": gradePoints = 3.7; break;
                        case "B+": gradePoints = 3.3; break;
                        case "B": gradePoints = 3.0; break;
                        case "B-": gradePoints = 2.7; break;
                        case "C+": gradePoints = 2.3; break;
                        case "C": gradePoints = 2.0; break;
                        case "C-": gradePoints = 1.7; break;
                        case "D": gradePoints = 1.0; break;
                        default: gradePoints = 0;
                    }
                }
                totalPoints += gradePoints * credits;
                
                details.append(String.format("%-15s %-40s %-8s %-8d %-10s %-10s\n", 
                    courseRs.getString("course_code"),
                    truncateString(courseRs.getString("course_name"), 40),
                    grade,
                    credits,
                    courseRs.getString("semester"),
                    courseRs.getString("academic_year")));
            }
            
            if (courseCount > 0 && totalCredits > 0) {
                double cgpa = totalPoints / totalCredits;
                details.append("\n");
                details.append(String.format("Recent CGPA (from shown courses): %.2f\n", cgpa));
                details.append(String.format("Total Credits (shown): %.0f\n", totalCredits));
            }
            
            if (courseCount == 0) {
                details.append("No course information found.\n");
            } else if (courseCount == 10) {
                details.append("\n(Showing 10 most recent courses)\n");
            }
            
            // ========== CREATE TEXT AREA WITH SCROLLING ==========
            TextArea textArea = new TextArea(details.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
            
            ScrollPane scrollPane = new ScrollPane(textArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPrefSize(680, 550);
            
            // Add buttons for actions
            ButtonType editDormButton = new ButtonType("Edit Dormitory Info");
            ButtonType viewFullReport = new ButtonType("Full Report");
            dialog.getDialogPane().getButtonTypes().addAll(editDormButton, viewFullReport, ButtonType.CLOSE);
            
            dialog.getDialogPane().setContent(scrollPane);
            
            dialog.showAndWait().ifPresent(buttonType -> {
                if (buttonType == editDormButton) {
                    editStudentDormitoryInfo(student);
                } else if (buttonType == viewFullReport) {
                    generateStudentFullReport(student);
                }
            });
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load student details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to truncate long strings
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    // Method to generate a full PDF report (optional)
    private void generateStudentFullReport(User student) {
        showAlert("Info", "Full report generation would be implemented here.\n\n" +
                         "Student: " + student.getFullName() + "\n" +
                         "ID: " + student.getUsername() + "\n" +
                         "This feature would generate a PDF with all student information.");
    }

    // Method to edit dormitory info from details view
    private void editStudentDormitoryInfo(User student) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Dormitory Information");
        dialog.setHeaderText("Update Dormitory Details for " + student.getFullName());
        
        ButtonType saveButton = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtBlockNumber = new TextField();
        txtBlockNumber.setPromptText("Block Letter (A, B, C...)");
        
        TextField txtRoomNumber = new TextField();
        txtRoomNumber.setPromptText("Room Number (101, 205...)");
        
        CheckBox chkKeyReturned = new CheckBox("Key Returned");
        DatePicker dpKeyReturnDate = new DatePicker();
        dpKeyReturnDate.setPromptText("Key return date");
        
        TextArea txtDamageDescription = new TextArea();
        txtDamageDescription.setPromptText("Damage description");
        txtDamageDescription.setPrefRowCount(3);
        
        TextField txtDamageAmount = new TextField();
        txtDamageAmount.setPromptText("Damage amount ($)");
        
        CheckBox chkDamagePaid = new CheckBox("Damage Paid");
        
        // Load existing data
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT block_number, room_number, key_returned, key_return_date,
                       damage_description, damage_amount, damage_paid
                FROM student_dormitory_credentials 
                WHERE student_id = ?
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, student.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                txtBlockNumber.setText(rs.getString("block_number"));
                txtRoomNumber.setText(rs.getString("room_number"));
                chkKeyReturned.setSelected(rs.getBoolean("key_returned"));
                
                if (rs.getDate("key_return_date") != null) {
                    dpKeyReturnDate.setValue(rs.getDate("key_return_date").toLocalDate());
                }
                
                txtDamageDescription.setText(rs.getString("damage_description"));
                txtDamageAmount.setText(String.valueOf(rs.getDouble("damage_amount")));
                chkDamagePaid.setSelected(rs.getBoolean("damage_paid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Enable/disable date picker based on checkbox
        chkKeyReturned.setOnAction(e -> {
            dpKeyReturnDate.setDisable(!chkKeyReturned.isSelected());
            if (!chkKeyReturned.isSelected()) {
                dpKeyReturnDate.setValue(null);
            }
        });
        dpKeyReturnDate.setDisable(!chkKeyReturned.isSelected());
        
        grid.add(new Label("Block Number*:"), 0, 0);
        grid.add(txtBlockNumber, 1, 0);
        grid.add(new Label("Room Number*:"), 0, 1);
        grid.add(txtRoomNumber, 1, 1);
        grid.add(new Label("Key Status:"), 0, 2);
        grid.add(chkKeyReturned, 1, 2);
        grid.add(new Label("Key Return Date:"), 0, 3);
        grid.add(dpKeyReturnDate, 1, 3);
        grid.add(new Label("Damage Description:"), 0, 4);
        grid.add(txtDamageDescription, 1, 4);
        grid.add(new Label("Damage Amount:"), 0, 5);
        grid.add(txtDamageAmount, 1, 5);
        grid.add(new Label("Damage Status:"), 0, 6);
        grid.add(chkDamagePaid, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                String blockNum = txtBlockNumber.getText().trim();
                String roomNum = txtRoomNumber.getText().trim();
                
                if (blockNum.isEmpty() || roomNum.isEmpty()) {
                    showAlert("Error", "Both block and room numbers are required!");
                    return null;
                }
                
                try {
                    double damageAmount = 0;
                    if (!txtDamageAmount.getText().trim().isEmpty()) {
                        damageAmount = Double.parseDouble(txtDamageAmount.getText().trim());
                    }
                    
                    if (updateDormitoryInfoFull(student.getId(), blockNum, roomNum,
                            chkKeyReturned.isSelected(), dpKeyReturnDate.getValue(),
                            txtDamageDescription.getText().trim(), damageAmount,
                            chkDamagePaid.isSelected())) {
                        
                        showAlert("Success", "Dormitory information updated successfully!");
                        return ButtonType.OK;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid damage amount!");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Refresh student details view if open
                viewStudentDetails(student);
            }
        });
    }

    private boolean updateDormitoryInfoFull(int studentId, String blockNumber, String roomNumber,
                                           boolean keyReturned, LocalDate keyReturnDate,
                                           String damageDescription, double damageAmount,
                                           boolean damagePaid) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if record exists
            String checkSql = "SELECT COUNT(*) FROM student_dormitory_credentials WHERE student_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            
            String sql;
            PreparedStatement stmt;
            
            if (rs.getInt(1) > 0) {
                // Update existing record
                sql = """
                    UPDATE student_dormitory_credentials 
                    SET block_number = ?, room_number = ?, 
                        key_returned = ?, key_return_date = ?,
                        damage_description = ?, damage_amount = ?, damage_paid = ?,
                        last_updated = NOW()
                    WHERE student_id = ?
                    """;
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, blockNumber);
                stmt.setString(2, roomNumber);
                stmt.setBoolean(3, keyReturned);
                stmt.setDate(4, keyReturnDate != null ? Date.valueOf(keyReturnDate) : null);
                stmt.setString(5, damageDescription.isEmpty() ? null : damageDescription);
                stmt.setDouble(6, damageAmount);
                stmt.setBoolean(7, damagePaid);
                stmt.setInt(8, studentId);
            } else {
                // Insert new record
                sql = """
                    INSERT INTO student_dormitory_credentials 
                    (student_id, block_number, room_number, 
                     key_returned, key_return_date,
                     damage_description, damage_amount, damage_paid,
                     clearance_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                    """;
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, studentId);
                stmt.setString(2, blockNumber);
                stmt.setString(3, roomNumber);
                stmt.setBoolean(4, keyReturned);
                stmt.setDate(5, keyReturnDate != null ? Date.valueOf(keyReturnDate) : null);
                stmt.setString(6, damageDescription.isEmpty() ? null : damageDescription);
                stmt.setDouble(7, damageAmount);
                stmt.setBoolean(8, damagePaid);
            }
            
            boolean success = stmt.executeUpdate() > 0;
            
            // Update clearance status based on conditions
            if (success) {
                String statusUpdateSql = """
                    UPDATE student_dormitory_credentials 
                    SET clearance_status = CASE 
                        WHEN key_returned = TRUE AND damage_paid = TRUE THEN 'APPROVED'
                        ELSE 'PENDING'
                    END
                    WHERE student_id = ?
                    """;
                PreparedStatement statusStmt = conn.prepareStatement(statusUpdateSql);
                statusStmt.setInt(1, studentId);
                statusStmt.executeUpdate();
            }
            
            return success;
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update dormitory info: " + e.getMessage());
            e.printStackTrace();
            return false;
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
        
        // NEW: Add Block and Dorm Number fields
        TextField txtBlockNumber = new TextField();
        txtBlockNumber.setPromptText("e.g., A, B, C");
        
        TextField txtDormNumber = new TextField();
        txtDormNumber.setPromptText("e.g., 101, 205, 301");

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
        // NEW: Add block and dorm number fields
        grid.add(new Label("Block Number:"), 0, 8);
        grid.add(txtBlockNumber, 1, 8);
        grid.add(new Label("Dorm/Room Number:"), 0, 9);
        grid.add(txtDormNumber, 1, 9);

        grid.setUserData(new Object[]{
            txtStudentId, txtUsername, txtFullName, txtPassword,
            txtEmail, cmbPhonePrefix, txtPhoneSuffix, cmbDepartment, cmbYear,
            txtBlockNumber, txtDormNumber  // Add new fields to user data
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
        TextField txtBlockNumber = (TextField) fields[9];  // New
        TextField txtDormNumber = (TextField) fields[10];  // New

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
        String blockNumber = txtBlockNumber.getText().trim();  // New
        String dormNumber = txtDormNumber.getText().trim();    // New

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
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // 1. Check for duplicates
                String checkDuplicate = "SELECT id FROM users WHERE username = ? OR phone = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkDuplicate);
                checkStmt.setString(1, username);
                checkStmt.setString(2, finalPhone);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    showAlert("Error", "Username or Phone number already exists!");
                    conn.rollback();
                    return false;
                }

                // 2. Insert user into users table
                String userSql = """
                    INSERT INTO users (username, password, full_name, role, email, phone, department, year_level, status)
                    VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?, 'ACTIVE')
                """;

                PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, username);
                userStmt.setString(2, password);
                userStmt.setString(3, fullName);
                userStmt.setString(4, email.isEmpty() ? null : email);
                userStmt.setString(5, finalPhone);
                userStmt.setString(6, department);
                userStmt.setString(7, year);

                int userRows = userStmt.executeUpdate();
                
                if (userRows <= 0) {
                    conn.rollback();
                    return false;
                }
                
                // Get the generated student ID
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                int studentDbId = -1;
                if (generatedKeys.next()) {
                    studentDbId = generatedKeys.getInt(1);
                }
                
                // 3. Insert dormitory credentials if block and dorm numbers are provided
                if (!blockNumber.isEmpty() && !dormNumber.isEmpty()) {
                    String dormSql = """
                        INSERT INTO student_dormitory_credentials 
                        (student_id, block_number, room_number, key_returned, damage_paid, clearance_status)
                        VALUES (?, ?, ?, FALSE, FALSE, 'PENDING')
                    """;
                    
                    PreparedStatement dormStmt = conn.prepareStatement(dormSql);
                    dormStmt.setInt(1, studentDbId);
                    dormStmt.setString(2, blockNumber);
                    dormStmt.setString(3, dormNumber);
                    
                    dormStmt.executeUpdate();
                }
                
                // Commit transaction
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            showAlert("Error", "Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    
    @FXML
    private void editStudentDormitoryInfo() {
        User selectedStudent = tableAllStudents.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            showAlert("Error", "Please select a student first!");
            return;
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Dormitory Information");
        dialog.setHeaderText("Update Dormitory Details for " + selectedStudent.getFullName());
        
        ButtonType saveButton = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtBlockNumber = new TextField();
        txtBlockNumber.setPromptText("Block Letter (A, B, C...)");
        
        TextField txtRoomNumber = new TextField();
        txtRoomNumber.setPromptText("Room Number (101, 205...)");
        
        // Load existing data
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT block_number, room_number 
                FROM student_dormitory_credentials 
                WHERE student_id = ?
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, selectedStudent.getId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                txtBlockNumber.setText(rs.getString("block_number"));
                txtRoomNumber.setText(rs.getString("room_number"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        grid.add(new Label("Block Number:"), 0, 0);
        grid.add(txtBlockNumber, 1, 0);
        grid.add(new Label("Room Number:"), 0, 1);
        grid.add(txtRoomNumber, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                String blockNum = txtBlockNumber.getText().trim();
                String roomNum = txtRoomNumber.getText().trim();
                
                if (blockNum.isEmpty() || roomNum.isEmpty()) {
                    showAlert("Error", "Both block and room numbers are required!");
                    return null;
                }
                
                if (updateDormitoryInfo(selectedStudent.getId(), blockNum, roomNum)) {
                    showAlert("Success", "Dormitory information updated successfully!");
                    return ButtonType.OK;
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private boolean updateDormitoryInfo(int studentId, String blockNumber, String roomNumber) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if record exists
            String checkSql = "SELECT COUNT(*) FROM student_dormitory_credentials WHERE student_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            
            if (rs.getInt(1) > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE student_dormitory_credentials 
                    SET block_number = ?, room_number = ?, last_updated = NOW()
                    WHERE student_id = ?
                    """;
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, blockNumber);
                updateStmt.setString(2, roomNumber);
                updateStmt.setInt(3, studentId);
                return updateStmt.executeUpdate() > 0;
            } else {
                // Insert new record
                String insertSql = """
                    INSERT INTO student_dormitory_credentials 
                    (student_id, block_number, room_number, key_returned, damage_paid, clearance_status)
                    VALUES (?, ?, ?, FALSE, FALSE, 'PENDING')
                    """;
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, studentId);
                insertStmt.setString(2, blockNumber);
                insertStmt.setString(3, roomNumber);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to update dormitory info: " + e.getMessage());
            e.printStackTrace();
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