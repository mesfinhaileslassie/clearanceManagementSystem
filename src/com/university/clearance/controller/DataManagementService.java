package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.ClearanceRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.sql.*;
import java.time.LocalDateTime;

public class DataManagementService {
    
    private final AdminDashboardController controller;
    
    private ObservableList<User> allStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> approvedStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> rejectedStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> pendingStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> inProgressStudentsData = FXCollections.observableArrayList();
    private ObservableList<User> officersData = FXCollections.observableArrayList();
    private ObservableList<User> allUsersData = FXCollections.observableArrayList();
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    
    private int totalStudents = 0;
    private int totalOfficers = 0;
    private int totalRequests = 0;
    private int approvedCount = 0;
    private int rejectedCount = 0;
    private int pendingCount = 0;
    
    public DataManagementService(AdminDashboardController controller) {
        this.controller = controller;
    }
    
    public void loadAllData() {
        loadAllStudents();
        loadOfficers();
        loadAllUsers();
        loadClearanceRequests();
        updateDashboardStats();
    }
    
    public void loadAllStudents() {
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
                
                student.setClearanceStatus(formatClearanceStatus(clearanceStatus, canReapply));
                student.setCanReapply(canReapply);
                
                allStudentsData.add(student);
                
                if (clearanceStatus.equals("FULLY_CLEARED") || clearanceStatus.equals("APPROVED")) {
                    approvedStudentsData.add(student);
                } else if (clearanceStatus.equals("REJECTED") && !canReapply) {
                    rejectedStudentsData.add(student);
                } else if (clearanceStatus.equals("PENDING")) {
                    pendingStudentsData.add(student);
                } else if (clearanceStatus.equals("IN_PROGRESS")) {
                    inProgressStudentsData.add(student);
                }
            }
            
            controller.getTableAllStudents().setItems(allStudentsData);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load students: " + e.getMessage());
        }
    }
    
    public void loadOfficers() {
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
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load officers: " + e.getMessage());
        }
    }
    
    public void loadAllUsers() {
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
            
            controller.getTableAllUsers().setItems(allUsersData);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    public void loadClearanceRequests() {
        requestData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cr.id, u.username, u.full_name, u.department, 
                       cr.request_date, cr.status, COUNT(ca.id) as approved_count,
                       cr.can_reapply
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                GROUP BY cr.id, u.username, u.full_name, u.department, cr.request_date, cr.status, cr.can_reapply
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
                req.setCanReapply(rs.getBoolean("can_reapply"));
                
                requestData.add(req);
            }

            controller.getTableRequests().setItems(requestData);

        } catch (Exception e) {
            showAlert("Error", "Failed to load requests: " + e.getMessage());
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
            case "EXPIRED":
                return "⌛ Expired";
            case "NO_REQUEST":
                return "📝 No Request";
            default:
                return status;
        }
    }
    
    public void updateDashboardStats() {
        totalStudents = allStudentsData.size();
        totalOfficers = officersData.size();
        
        totalRequests = 0;
        approvedCount = 0;
        rejectedCount = 0;
        pendingCount = 0;
        
        for (User student : allStudentsData) {
            String status = student.getClearanceStatus();
            if (!status.equals("📝 No Request")) {
                totalRequests++;
            }
            
            if (status.contains("✅")) {
                approvedCount++;
            } else if (status.contains("❌")) {
                rejectedCount++;
            } else if (status.contains("⏳") || status.contains("🔄")) {
                pendingCount++;
            }
        }
    }
    
    public void updateReportStatistics(Label lblDeletedToday, Label lblResubmissionsAllowed, 
                                      Label lblPendingResubmissions, Label lblExpiredRequests) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String deletedSql = """
                SELECT COUNT(*) as count FROM audit_logs 
                WHERE action = 'USER_DELETION' 
                AND DATE(timestamp) = CURDATE()
                """;
            PreparedStatement deletedStmt = conn.prepareStatement(deletedSql);
            ResultSet deletedRs = deletedStmt.executeQuery();
            if (deletedRs.next()) {
                lblDeletedToday.setText(String.valueOf(deletedRs.getInt("count")));
            }
            
            String resubmitSql = """
                SELECT COUNT(*) as count FROM audit_logs 
                WHERE action = 'ALLOW_RESUBMISSION'
                AND DATE(timestamp) = CURDATE()
                """;
            PreparedStatement resubmitStmt = conn.prepareStatement(resubmitSql);
            ResultSet resubmitRs = resubmitStmt.executeQuery();
            if (resubmitRs.next()) {
                lblResubmissionsAllowed.setText(String.valueOf(resubmitRs.getInt("count")));
            }
            
            String pendingSql = """
                SELECT COUNT(*) as count FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                WHERE cr.status = 'REJECTED'
                AND cr.request_date < DATE_SUB(NOW(), INTERVAL 30 DAY)
                AND cr.can_reapply = FALSE
                """;
            PreparedStatement pendingStmt = conn.prepareStatement(pendingSql);
            ResultSet pendingRs = pendingStmt.executeQuery();
            if (pendingRs.next()) {
                lblPendingResubmissions.setText(String.valueOf(pendingRs.getInt("count")));
            }
            
            String expiredSql = """
                SELECT COUNT(*) as count FROM clearance_requests 
                WHERE status = 'EXPIRED'
                """;
            PreparedStatement expiredStmt = conn.prepareStatement(expiredSql);
            ResultSet expiredRs = expiredStmt.executeQuery();
            if (expiredRs.next()) {
                lblExpiredRequests.setText(String.valueOf(expiredRs.getInt("count")));
            }
            
        } catch (Exception e) {
            System.err.println("Error updating report statistics: " + e.getMessage());
        }
    }
    
    public void handleUserSearch(String searchQuery, String searchType) {
        if (searchQuery.isEmpty()) {
            updateSearchStatus("Please enter a search term", "warning");
            return;
        }
        
        performSearch(searchQuery, searchType);
    }
    
    private void performSearch(String searchQuery, String searchType) {
        // Clear the list first
        ObservableList<User> searchResults = FXCollections.observableArrayList();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql;
            PreparedStatement ps;
            
            switch (searchType) {
                case "Students Only":
                    sql = """
                        SELECT * FROM users 
                        WHERE role = 'STUDENT'
                        AND (username LIKE ? OR full_name LIKE ? OR department LIKE ? 
                             OR email LIKE ? OR phone LIKE ?)
                        ORDER BY username
                        """;
                    ps = conn.prepareStatement(sql);
                    String pattern = "%" + searchQuery + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                    ps.setString(3, pattern);
                    ps.setString(4, pattern);
                    ps.setString(5, pattern);
                    break;
                    
                case "Officers Only":
                    sql = """
                        SELECT * FROM users 
                        WHERE role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD', 'ADMIN')
                        AND (username LIKE ? OR full_name LIKE ? OR department LIKE ? OR email LIKE ?)
                        ORDER BY role, username
                        """;
                    ps = conn.prepareStatement(sql);
                    pattern = "%" + searchQuery + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                    ps.setString(3, pattern);
                    ps.setString(4, pattern);
                    break;
                    
                default: // "All Users"
                    sql = """
                        SELECT * FROM users 
                        WHERE (username LIKE ? OR full_name LIKE ? OR department LIKE ? OR email LIKE ?)
                        ORDER BY role, username
                        """;
                    ps = conn.prepareStatement(sql);
                    pattern = "%" + searchQuery + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                    ps.setString(3, pattern);
                    ps.setString(4, pattern);
                    break;
            }
            
            ResultSet rs = ps.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("email"),
                    rs.getString("department")
                );
                user.setStatus(rs.getString("status"));
                
                if ("STUDENT".equals(user.getRole())) {
                    user.setYearLevel(rs.getString("year_level"));
                    user.setPhone(rs.getString("phone"));
                }
                
                searchResults.add(user);
            }
            
            // Instead of trying to access controller's table directly, 
            // we'll return the results and let the controller handle it
            allUsersData = searchResults;
            
            if (count > 0) {
                updateSearchStatus("Found " + count + " " + 
                    (searchType.equals("All Users") ? "users" : 
                     searchType.equals("Students Only") ? "students" : "officers") + 
                    " matching: '" + searchQuery + "'", "success");
            } else {
                updateSearchStatus("No " + 
                    (searchType.equals("All Users") ? "users" : 
                     searchType.equals("Students Only") ? "students" : "officers") + 
                    " found matching: '" + searchQuery + "'", "warning");
            }
            
        } catch (Exception e) {
            updateSearchStatus("Error searching: " + e.getMessage(), "error");
        }
    }
    
    public void searchRequests(String searchQuery) {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cr.id, u.username, u.full_name, u.department, 
                       cr.request_date, cr.status, COUNT(ca.id) as approved_count
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                WHERE (u.username LIKE ? OR u.full_name LIKE ? OR u.department LIKE ?)
                GROUP BY cr.id, u.username, u.full_name, u.department, cr.request_date, cr.status
                ORDER BY cr.request_date DESC
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            String pattern = "%" + searchQuery + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            
            ResultSet rs = ps.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
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
            
            controller.getTableRequests().setItems(requestData);
            
            showAlert("Search Results", "Found " + count + " clearance requests matching: '" + searchQuery + "'");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to search requests: " + e.getMessage());
        }
    }
    
    private void updateSearchStatus(String message, String type) {
        Label lblSearchStatus = controller.getLblSearchStatus();
        if (lblSearchStatus != null) {
            lblSearchStatus.setText(message);
            lblSearchStatus.setVisible(true);
            
            switch (type) {
                case "success":
                    lblSearchStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    break;
                case "warning":
                    lblSearchStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    break;
                case "error":
                    lblSearchStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    break;
                default:
                    lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            }
        }
    }
    
    public void setupStudentTable(TableView<User> tableView, TableColumn<User, String> colId, 
            TableColumn<User, String> colName, TableColumn<User, String> colDept,
            TableColumn<User, String> colYear, TableColumn<User, String> colStatus,
            TableColumn<User, String> colActions) {
        
        colId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("clearanceStatus"));
        
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
                    // This would call back to controller
                });
                
                btnViewDetails.setOnAction(event -> {
                    User student = getTableView().getItems().get(getIndex());
                    // This would call back to controller
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
    
    public void setupSimpleStudentTable(TableView<User> tableView) {
        if (tableView.getColumns().size() < 4) return;
        
        tableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("username"));
        tableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("fullName"));
        tableView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("department"));
        tableView.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("clearanceStatus"));
        
        TableColumn<User, String> statusCol = (TableColumn<User, String>) tableView.getColumns().get(3);
        statusCol.setCellFactory(column -> new TableCell<User, String>() {
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
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public int getTotalStudents() { return totalStudents; }
    public int getTotalOfficers() { return totalOfficers; }
    public int getTotalRequests() { return totalRequests; }
    public int getApprovedCount() { return approvedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public int getPendingCount() { return pendingCount; }
    
    public ObservableList<User> getAllStudentsData() { return allStudentsData; }
    public ObservableList<User> getApprovedStudentsData() { return approvedStudentsData; }
    public ObservableList<User> getRejectedStudentsData() { return rejectedStudentsData; }
    public ObservableList<User> getPendingStudentsData() { return pendingStudentsData; }
    public ObservableList<User> getInProgressStudentsData() { return inProgressStudentsData; }
    public ObservableList<User> getOfficersData() { return officersData; }
    public ObservableList<User> getAllUsersData() { return allUsersData; }
    public ObservableList<ClearanceRequest> getRequestData() { return requestData; }
}