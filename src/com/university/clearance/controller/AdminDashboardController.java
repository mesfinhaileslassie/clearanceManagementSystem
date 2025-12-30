package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.SelectableUser;
import com.university.clearance.model.ClearanceRequest;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class AdminDashboardController {

    // Top Section
    @FXML private Label lblWelcome;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblTotalOfficers;
    @FXML private Label lblTotalRequests;
    
    // Card Labels
    @FXML private Label lblTotalStudentsCard;
    @FXML private Label lblTotalOfficersCard;
    @FXML private Label lblTotalRequestsCard;
    @FXML private Label lblApprovedCount;
    @FXML private Label lblRejectedCount;
    @FXML private Label lblPendingCount;
    
    // Main Tab Pane
    @FXML private TabPane mainTabPane;
    @FXML private BorderPane mainBorderPane;

    // Students Tables - Use SelectableUser for tables with checkboxes
    @FXML private TableView<SelectableUser> tableAllStudents;
    @FXML private TableView<User> tableApprovedStudents;
    @FXML private TableView<User> tableRejectedStudents;
    @FXML private TableView<User> tablePendingStudents;
    @FXML private TableView<User> tableInProgressStudents;
    @FXML private TableView<User> tableExpiredStudents;
    
    // Students Table Columns
    @FXML private TableColumn<SelectableUser, String> colStudentId;
    @FXML private TableColumn<SelectableUser, String> colStudentName;
    @FXML private TableColumn<SelectableUser, String> colStudentDepartment;
    @FXML private TableColumn<SelectableUser, String> colStudentYear;
    @FXML private TableColumn<SelectableUser, String> colClearanceStatus;
    @FXML private TableColumn<SelectableUser, Boolean> colSelectStudent;
    
    // Officers Table
    @FXML private TableView<User> tableOfficers;
    @FXML private TableColumn<User, String> colOfficerId;
    @FXML private TableColumn<User, String> colOfficerName;
    @FXML private TableColumn<User, String> colOfficerRole;
    @FXML private TableColumn<User, String> colOfficerDepartment;
    @FXML private TableColumn<User, String> colOfficerStatus;
    
    // All Users Table - Use SelectableUser for tables with checkboxes
    @FXML private TableView<SelectableUser> tableAllUsers;
    @FXML private TableColumn<SelectableUser, Boolean> colSelectUser;
    @FXML private TableColumn<SelectableUser, String> colAllUserId;
    @FXML private TableColumn<SelectableUser, String> colAllUserName;
    @FXML private TableColumn<SelectableUser, String> colAllUserRole;
    @FXML private TableColumn<SelectableUser, String> colAllUserDepartment;
    @FXML private TableColumn<SelectableUser, String> colAllUserStatus;
    
    // Clearance Requests Table
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colRequestStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colRequestName;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colRequestStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, Integer> colRequestApproved;
    @FXML private TableColumn<ClearanceRequest, Void> colResubmission;
    
    // Search Components
    @FXML private TextField txtSearchUsers;
    @FXML private ComboBox<String> cmbSearchType;
    @FXML private Button btnSearchUsers;
    @FXML private Button btnClearSearch;
    @FXML private Label lblSearchStatus;
    
    @FXML private TextField txtSearchRequests;
    @FXML private Button btnSearchRequests;
    @FXML private Button btnClearRequestsSearch;
    
    // Bulk Operation Labels
    @FXML private Label lblSelectedCountUsers;
    @FXML private Label lblSelectedCountStudents;
    
    @FXML private VBox cardsContainer;
    @FXML private Label lblActiveSessions;
    @FXML private Label lblSessionStatus;
    @FXML private Label lblPendingActions;
    @FXML private Label lblActionStatus;
    @FXML private Label lblSystemLoad;
    @FXML private Label lblLoadStatus;
    @FXML private Label lblUptime;
    @FXML private Label lblUptimeStatus;
    @FXML private Label lblDatabaseSize;
    @FXML private Label lblDbStatus;
    @FXML private Label lblTodayLogins;
    @FXML private Label lblLoginTrend;
    @FXML private Label lblActiveOfficers;
    @FXML private Label lblOfficerStatus;
    @FXML private Label lblClearanceRate;
    @FXML private Label lblRateStatus;
    
    // Report Labels
    @FXML private Label lblDeletedToday;
    @FXML private Label lblResubmissionsAllowed;
    @FXML private Label lblPendingResubmissions;
    @FXML private Label lblExpiredRequests;
    
    // Status Bar
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblUpdateTime;
    @FXML private Label lblResubmissionStatus;
    
    private User currentUser;
    
    // Services
    private UserManagementService userManagementService;
    private DataManagementService dataManagementService;
    private ClearanceOperationsService clearanceOperationsService;
    
    // Bulk operation data
    private ObservableList<SelectableUser> selectableUsersData = FXCollections.observableArrayList();
    private ObservableList<SelectableUser> selectableStudentsData = FXCollections.observableArrayList();
    
    @FXML
    private void initialize() {
        // Initialize services
        userManagementService = new UserManagementService();
        dataManagementService = new DataManagementService(this);
        clearanceOperationsService = new ClearanceOperationsService();
        
        setupAllTables();
        setupSearchFunctionality();
        setupTabAnimations();
        setupActiveTabHighlight();
        setupAllowResubmitButtons();
        setupBulkOperationListeners();
        
        // Load initial data immediately
        refreshAllData();
        
        Platform.runLater(() -> {
            try {
                Node node = lblWelcome;
                while (node != null && !(node instanceof BorderPane)) {
                    node = node.getParent();
                }
                
                if (node != null) {
                    BorderPane root = (BorderPane) node;
                    Scene scene = root.getScene();
                    
                    if (scene != null) {
                        String cssPath = "/com/university/clearance/resources/css/admin-dashboard.css";
                        URL cssUrl = getClass().getResource(cssPath);
                        
                        if (cssUrl != null) {
                            scene.getStylesheets().add(cssUrl.toExternalForm());
                            System.out.println("CSS applied successfully");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load CSS: " + e.getMessage());
            }
        });
    }
    
    private void setupAllTables() {
        // Setup All Users table with checkbox column
        setupTableWithCheckboxes(tableAllUsers, selectableUsersData, colSelectUser);
        
        // Setup other columns for All Users table
        colAllUserId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAllUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAllUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAllUserDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colAllUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Status column styling for All Users table
        colAllUserStatus.setCellFactory(column -> new TableCell<SelectableUser, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if ("INACTIVE".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Setup All Students table with checkbox column
        setupTableWithCheckboxes(tableAllStudents, selectableStudentsData, colSelectStudent);
        
        // Setup other columns for All Students table
        setupStudentTableWithSelectable();
        
        // Setup categorized tables (these don't need checkboxes, so keep as User)
        if (tableApprovedStudents != null) {
            dataManagementService.setupSimpleStudentTable(tableApprovedStudents);
        }
        if (tableRejectedStudents != null) {
            dataManagementService.setupSimpleStudentTable(tableRejectedStudents);
        }
        if (tableExpiredStudents != null) {
            dataManagementService.setupSimpleStudentTable(tableExpiredStudents);
        }
        if (tablePendingStudents != null) {
            dataManagementService.setupSimpleStudentTable(tablePendingStudents);
        }
        if (tableInProgressStudents != null) {
            dataManagementService.setupSimpleStudentTable(tableInProgressStudents);
        }
        
        // Setup Officers table columns
        if (tableOfficers != null && colOfficerId != null && colOfficerName != null && 
            colOfficerRole != null && colOfficerDepartment != null && colOfficerStatus != null) {
            
            colOfficerId.setCellValueFactory(new PropertyValueFactory<>("username"));
            colOfficerName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colOfficerRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colOfficerDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colOfficerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            colOfficerStatus.setCellFactory(column -> new TableCell<User, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("ACTIVE".equals(item)) {
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        } else if ("INACTIVE".equals(item)) {
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
        }
        
        // Setup Clearance Requests table columns
        if (tableRequests != null && colRequestStudentId != null && colRequestName != null && 
            colRequestDepartment != null && colRequestStatus != null && colRequestDate != null && 
            colRequestApproved != null && colResubmission != null) {
            
            colRequestStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colRequestName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colRequestDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colRequestStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
            colRequestApproved.setCellValueFactory(new PropertyValueFactory<>("approvedCount"));
            
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
                        } else if (item.equals("EXPIRED")) {
                            setStyle("-fx-text-fill: #95a5a6; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
        }
        
        setupAllowResubmitButtons();
    }
    
    private void setupStudentTableWithSelectable() {
        if (colStudentId != null && colStudentName != null && colStudentDepartment != null && 
            colStudentYear != null && colClearanceStatus != null) {
            
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("username"));
            colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colStudentDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colStudentYear.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
            colClearanceStatus.setCellValueFactory(new PropertyValueFactory<>("clearanceStatus"));
            
            colClearanceStatus.setCellFactory(column -> new TableCell<SelectableUser, String>() {
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
    }
    
    private void setupTableWithCheckboxes(TableView<SelectableUser> tableView, 
                                         ObservableList<SelectableUser> selectableList,
                                         TableColumn<SelectableUser, Boolean> selectColumn) {
        
        // Set the items to the table
        tableView.setItems(selectableList);
        
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        
        selectColumn.setCellFactory(col -> new TableCell<SelectableUser, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            
            {
                checkBox.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        SelectableUser user = getTableView().getItems().get(index);
                        if (user != null) {
                            user.setSelected(checkBox.isSelected());
                            updateSelectionCounts();
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean selected, boolean empty) {
                super.updateItem(selected, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    SelectableUser user = getTableView().getItems().get(getIndex());
                    if (user != null) {
                        checkBox.setSelected(user.isSelected());
                        setGraphic(checkBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }
    
    private void setupBulkOperationListeners() {
        // Listen to selection changes
        selectableUsersData.addListener((javafx.collections.ListChangeListener.Change<? extends SelectableUser> change) -> {
            updateSelectionCounts();
        });
        
        selectableStudentsData.addListener((javafx.collections.ListChangeListener.Change<? extends SelectableUser> change) -> {
            updateSelectionCounts();
        });
    }
    
    private void updateSelectionCounts() {
        long selectedUsersCount = selectableUsersData.stream()
            .filter(SelectableUser::isSelected)
            .count();
        
        long selectedStudentsCount = selectableStudentsData.stream()
            .filter(SelectableUser::isSelected)
            .count();
        
        Platform.runLater(() -> {
            if (lblSelectedCountUsers != null) {
                lblSelectedCountUsers.setText("Selected: " + selectedUsersCount);
            }
            if (lblSelectedCountStudents != null) {
                lblSelectedCountStudents.setText("Selected: " + selectedStudentsCount);
            }
        });
    }
    
    // Bulk Operation Handlers
    @FXML
    private void handleSelectAllUsers() {
        for (SelectableUser user : selectableUsersData) {
            user.setSelected(true);
        }
        tableAllUsers.refresh();
        updateSelectionCounts();
    }
    
    @FXML
    private void handleDeselectAllUsers() {
        for (SelectableUser user : selectableUsersData) {
            user.setSelected(false);
        }
        tableAllUsers.refresh();
        updateSelectionCounts();
    }
    
    @FXML
    private void handleSelectAllStudents() {
        for (SelectableUser student : selectableStudentsData) {
            student.setSelected(true);
        }
        tableAllStudents.refresh();
        updateSelectionCounts();
    }
    
    @FXML
    private void handleDeselectAllStudents() {
        for (SelectableUser student : selectableStudentsData) {
            student.setSelected(false);
        }
        tableAllStudents.refresh();
        updateSelectionCounts();
    }
    
    private List<User> getSelectedUsers() {
        List<User> selectedUsers = new ArrayList<>();
        for (SelectableUser selectableUser : selectableUsersData) {
            if (selectableUser.isSelected()) {
                // Convert SelectableUser back to User for operations
                User user = new User(
                    selectableUser.getId(),
                    selectableUser.getUsername(),
                    selectableUser.getFullName(),
                    selectableUser.getRole(),
                    selectableUser.getEmail(),
                    selectableUser.getDepartment()
                );
                user.setStatus(selectableUser.getStatus());
                user.setYearLevel(selectableUser.getYearLevel());
                user.setPhone(selectableUser.getPhone());
                user.setClearanceStatus(selectableUser.getClearanceStatus());
                user.setCanReapply(selectableUser.isCanReapply());
                selectedUsers.add(user);
            }
        }
        return selectedUsers;
    }
    
    private List<User> getSelectedStudents() {
        List<User> selectedStudents = new ArrayList<>();
        for (SelectableUser selectableStudent : selectableStudentsData) {
            if (selectableStudent.isSelected()) {
                // Convert SelectableUser back to User for operations
                User student = new User(
                    selectableStudent.getId(),
                    selectableStudent.getUsername(),
                    selectableStudent.getFullName(),
                    selectableStudent.getRole(),
                    selectableStudent.getEmail(),
                    selectableStudent.getDepartment()
                );
                student.setStatus(selectableStudent.getStatus());
                student.setYearLevel(selectableStudent.getYearLevel());
                student.setPhone(selectableStudent.getPhone());
                student.setClearanceStatus(selectableStudent.getClearanceStatus());
                student.setCanReapply(selectableStudent.isCanReapply());
                selectedStudents.add(student);
            }
        }
        return selectedStudents;
    }
    
    @FXML
    private void handleBulkDeleteUsers() {
        List<User> selectedUsers = getSelectedUsers();
        
        if (selectedUsers.isEmpty()) {
            showAlert("No Selection", "Please select users to delete!");
            return;
        }
        
        // Check if trying to delete admin
        boolean containsAdmin = selectedUsers.stream()
            .anyMatch(user -> "admin".equals(user.getUsername()));
        
        if (containsAdmin) {
            showAlert("Cannot Delete Admin", "Cannot delete the admin user!");
            return;
        }
        
        StringBuilder userList = new StringBuilder();
        for (User user : selectedUsers) {
            userList.append("• ").append(user.getFullName())
                    .append(" (").append(user.getUsername()).append(") - ")
                    .append(user.getRole()).append("\n");
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bulk Delete Users");
        confirm.setHeaderText("Delete " + selectedUsers.size() + " selected users?");
        confirm.setContentText("The following users will be deleted:\n\n" + userList.toString() + 
                              "\n⚠️ This action cannot be undone!");
        
        confirm.getDialogPane().setPrefSize(500, 400);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            bulkDeleteUsers(selectedUsers);
        }
    }
    
    private void bulkDeleteUsers(List<User> users) {
        int successCount = 0;
        int failCount = 0;
        StringBuilder failures = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Prepare statements
                String archiveSql = """
                    INSERT INTO deleted_users_archive 
                    (original_id, username, full_name, role, email, phone, 
                     department, year_level, status, deleted_by, deleted_at)
                    SELECT id, username, full_name, role, email, phone, 
                           department, year_level, status, ?, NOW()
                    FROM users 
                    WHERE id = ?
                    """;
                PreparedStatement archiveStmt = conn.prepareStatement(archiveSql);
                
                String deleteUserSql = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteUserSql);
                
                String auditSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'BULK_USER_DELETION', ?, NOW())
                    """;
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                
                for (User user : users) {
                    try {
                        // Archive user
                        archiveStmt.setInt(1, currentUser.getId());
                        archiveStmt.setInt(2, user.getId());
                        archiveStmt.addBatch();
                        
                        // Delete user
                        deleteStmt.setInt(1, user.getId());
                        deleteStmt.addBatch();
                        
                        successCount++;
                        
                    } catch (Exception e) {
                        failCount++;
                        failures.append("• ").append(user.getFullName())
                               .append(": ").append(e.getMessage()).append("\n");
                    }
                }
                
                // Execute batches
                if (successCount > 0) {
                    archiveStmt.executeBatch();
                    deleteStmt.executeBatch();
                    
                    // Log bulk action
                    auditStmt.setInt(1, currentUser.getId());
                    auditStmt.setString(2, "Bulk deleted " + successCount + " users");
                    auditStmt.executeUpdate();
                    
                    conn.commit();
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to bulk delete users: " + e.getMessage());
            return;
        }
        
        // Refresh data
        refreshAllData();
        handleDeselectAllUsers();
        
        // Show results
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("Bulk delete completed!\n\n");
        resultMessage.append("✅ Successfully deleted: ").append(successCount).append(" users\n");
        
        if (failCount > 0) {
            resultMessage.append("❌ Failed to delete: ").append(failCount).append(" users\n");
            if (!failures.toString().isEmpty()) {
                resultMessage.append("\nFailures:\n").append(failures.toString());
            }
        }
        
        showAlert("Bulk Delete Results", resultMessage.toString());
    }
    
    @FXML
    private void handleBulkToggleStatus() {
        List<User> selectedUsers = getSelectedUsers();
        
        if (selectedUsers.isEmpty()) {
            showAlert("No Selection", "Please select users to toggle status!");
            return;
        }
        
        StringBuilder userList = new StringBuilder();
        for (User user : selectedUsers) {
            String currentStatus = "ACTIVE".equals(user.getStatus()) ? "✅ ACTIVE" : "❌ INACTIVE";
            String newStatus = "ACTIVE".equals(user.getStatus()) ? "❌ INACTIVE" : "✅ ACTIVE";
            userList.append("• ").append(user.getFullName())
                    .append(" (").append(user.getUsername()).append("): ")
                    .append(currentStatus).append(" → ").append(newStatus).append("\n");
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bulk Toggle Status");
        confirm.setHeaderText("Toggle status for " + selectedUsers.size() + " selected users?");
        confirm.setContentText("Status changes:\n\n" + userList.toString());
        
        confirm.getDialogPane().setPrefSize(500, 400);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            bulkToggleUserStatus(selectedUsers);
        }
    }
    
    private void bulkToggleUserStatus(List<User> users) {
        int successCount = 0;
        int failCount = 0;
        StringBuilder failures = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String sql = "UPDATE users SET status = CASE WHEN status = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                
                String auditSql = "INSERT INTO audit_logs (user_id, action, details, timestamp) VALUES (?, 'BULK_TOGGLE_STATUS', ?, NOW())";
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                
                for (User user : users) {
                    try {
                        stmt.setInt(1, user.getId());
                        stmt.addBatch();
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        failures.append("• ").append(user.getFullName())
                               .append(": ").append(e.getMessage()).append("\n");
                    }
                }
                
                if (successCount > 0) {
                    int[] results = stmt.executeBatch();
                    
                    // Count successful updates
                    successCount = 0;
                    for (int i = 0; i < results.length; i++) {
                        if (results[i] > 0) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                    
                    // Audit log
                    auditStmt.setInt(1, currentUser.getId());
                    auditStmt.setString(2, "Toggled status for " + successCount + " users");
                    auditStmt.executeUpdate();
                    
                    conn.commit();
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to bulk toggle status: " + e.getMessage());
            return;
        }
        
        // Refresh data
        refreshAllData();
        handleDeselectAllUsers();
        
        // Show results
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("Bulk status toggle completed!\n\n");
        resultMessage.append("✅ Successfully updated: ").append(successCount).append(" users\n");
        
        if (failCount > 0) {
            resultMessage.append("❌ Failed to update: ").append(failCount).append(" users\n");
            if (!failures.toString().isEmpty()) {
                resultMessage.append("\nFailures:\n").append(failures.toString());
            }
        }
        
        showAlert("Bulk Status Toggle Results", resultMessage.toString());
    }
    
    @FXML
    private void handleBulkAllowResubmission() {
        List<User> selectedStudents = getSelectedStudents();
        
        if (selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select students to allow resubmission!");
            return;
        }
        
        // Check eligibility for each student
        List<User> eligibleStudents = new ArrayList<>();
        List<User> ineligibleStudents = new ArrayList<>();
        StringBuilder eligibilityDetails = new StringBuilder();
        
        for (User student : selectedStudents) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String checkSql = """
                    SELECT cr.status, cr.can_reapply 
                    FROM clearance_requests cr 
                    WHERE cr.student_id = ? 
                    ORDER BY cr.id DESC 
                    LIMIT 1
                    """;
                
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, student.getId());
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    String status = rs.getString("status");
                    boolean canReapply = rs.getBoolean("can_reapply");
                    
                    boolean isEligible = ("REJECTED".equals(status) || "EXPIRED".equals(status)) && !canReapply;
                    
                    if (isEligible) {
                        eligibleStudents.add(student);
                        eligibilityDetails.append("✅ ").append(student.getFullName())
                                        .append(" - ").append(status).append("\n");
                    } else {
                        ineligibleStudents.add(student);
                        eligibilityDetails.append("❌ ").append(student.getFullName())
                                        .append(" - ").append(status);
                        if (canReapply) eligibilityDetails.append(" (Already allowed)");
                        eligibilityDetails.append("\n");
                    }
                } else {
                    ineligibleStudents.add(student);
                    eligibilityDetails.append("❌ ").append(student.getFullName())
                                    .append(" - No clearance request\n");
                }
            } catch (Exception e) {
                ineligibleStudents.add(student);
                eligibilityDetails.append("❌ ").append(student.getFullName())
                                .append(" - Error checking eligibility\n");
            }
        }
        
        if (eligibleStudents.isEmpty()) {
            showAlert("No Eligible Students", 
                "No selected students are eligible for resubmission!\n\n" +
                "Only students with REJECTED or EXPIRED clearance requests\n" +
                "that haven't been allowed to resubmit can be processed.\n\n" +
                "Eligibility check results:\n" + eligibilityDetails.toString());
            return;
        }
        
        // Show confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bulk Allow Resubmission");
        confirm.setHeaderText("Allow " + eligibleStudents.size() + " students to resubmit?");
        
        StringBuilder content = new StringBuilder();
        content.append("Eligible students (").append(eligibleStudents.size()).append("):\n");
        for (User student : eligibleStudents) {
            content.append("• ").append(student.getFullName())
                  .append(" (").append(student.getUsername()).append(")\n");
        }
        
        if (!ineligibleStudents.isEmpty()) {
            content.append("\nNot eligible (").append(ineligibleStudents.size()).append("):\n");
            for (User student : ineligibleStudents) {
                content.append("• ").append(student.getFullName())
                      .append(" (").append(student.getUsername()).append(")\n");
            }
        }
        
        content.append("\nThis will reset their clearance status to PENDING\n");
        content.append("and allow them to submit new clearance requests.");
        
        confirm.setContentText(content.toString());
        confirm.getDialogPane().setPrefSize(600, 500);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            bulkAllowResubmission(eligibleStudents);
        }
    }
    
    private void bulkAllowResubmission(List<User> students) {
        int successCount = 0;
        int failCount = 0;
        StringBuilder successes = new StringBuilder();
        StringBuilder failures = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Prepare statements
                String updateRequestSql = """
                    UPDATE clearance_requests 
                    SET can_reapply = TRUE, 
                        status = 'PENDING',
                        request_date = NOW(),
                        previous_status = ?
                    WHERE id = ?
                    """;
                PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql);
                
                String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                PreparedStatement clearApprovalsStmt = conn.prepareStatement(clearApprovalsSql);
                
                String getWorkflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                PreparedStatement workflowStmt = conn.prepareStatement(getWorkflowSql);
                ResultSet workflowRs = workflowStmt.executeQuery();
                
                List<String> workflowRoles = new ArrayList<>();
                while (workflowRs.next()) {
                    workflowRoles.add(workflowRs.getString("role"));
                }
                
                String insertApprovalSql = """
                    INSERT INTO clearance_approvals (request_id, officer_role, status)
                    VALUES (?, ?, 'PENDING')
                    """;
                PreparedStatement insertApprovalStmt = conn.prepareStatement(insertApprovalSql);
                
                String updateDormSql = """
                    UPDATE student_dormitory_credentials 
                    SET clearance_status = 'PENDING'
                    WHERE student_id = ?
                    """;
                PreparedStatement updateDormStmt = conn.prepareStatement(updateDormSql);
                
                String auditSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'BULK_ALLOW_RESUBMISSION', ?, NOW())
                    """;
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                
                for (User student : students) {
                    try {
                        // Get latest request ID
                        String getRequestIdSql = """
                            SELECT id, status FROM clearance_requests 
                            WHERE student_id = ? 
                            ORDER BY id DESC 
                            LIMIT 1
                            """;
                        PreparedStatement getRequestIdStmt = conn.prepareStatement(getRequestIdSql);
                        getRequestIdStmt.setInt(1, student.getId());
                        ResultSet rs = getRequestIdStmt.executeQuery();
                        
                        if (rs.next()) {
                            int requestId = rs.getInt("id");
                            String originalStatus = rs.getString("status");
                            
                            // Update request
                            updateRequestStmt.setString(1, originalStatus);
                            updateRequestStmt.setInt(2, requestId);
                            updateRequestStmt.executeUpdate();
                            
                            // Clear previous approvals
                            clearApprovalsStmt.setInt(1, requestId);
                            clearApprovalsStmt.executeUpdate();
                            
                            // Create new approvals
                            for (String role : workflowRoles) {
                                insertApprovalStmt.setInt(1, requestId);
                                insertApprovalStmt.setString(2, role);
                                insertApprovalStmt.addBatch();
                            }
                            insertApprovalStmt.executeBatch();
                            insertApprovalStmt.clearBatch();
                            
                            // Update dormitory status
                            updateDormStmt.setInt(1, student.getId());
                            updateDormStmt.executeUpdate();
                            
                            successCount++;
                            successes.append("✅ ").append(student.getFullName()).append("\n");
                            
                        } else {
                            failCount++;
                            failures.append("❌ ").append(student.getFullName())
                                   .append(": No clearance request found\n");
                        }
                        
                    } catch (Exception e) {
                        failCount++;
                        failures.append("❌ ").append(student.getFullName())
                               .append(": ").append(e.getMessage()).append("\n");
                    }
                }
                
                if (successCount > 0) {
                    // Log bulk action
                    auditStmt.setInt(1, currentUser.getId());
                    auditStmt.setString(2, "Allowed resubmission for " + successCount + " students");
                    auditStmt.executeUpdate();
                    
                    conn.commit();
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to bulk allow resubmission: " + e.getMessage());
            return;
        }
        
        // Refresh data
        refreshAllData();
        handleDeselectAllStudents();
        
        // Show results
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("Bulk resubmission completed!\n\n");
        resultMessage.append("✅ Successfully allowed: ").append(successCount).append(" students\n");
        resultMessage.append("❌ Failed: ").append(failCount).append(" students\n");
        
        if (successCount > 0) {
            resultMessage.append("\nSuccesses:\n").append(successes.toString());
        }
        
        if (failCount > 0) {
            resultMessage.append("\nFailures:\n").append(failures.toString());
        }
        
        showAlert("Bulk Resubmission Results", resultMessage.toString());
    }
    
    private void refreshSelectableData() {
        // Clear existing data
        selectableUsersData.clear();
        selectableStudentsData.clear();
        
        // Convert User objects to SelectableUser objects for Users table
        ObservableList<User> allUsers = dataManagementService.getAllUsersData();
        for (User user : allUsers) {
            SelectableUser selectableUser = new SelectableUser(user);
            selectableUser.setStatus(user.getStatus());
            selectableUsersData.add(selectableUser);
        }
        
        // Convert User objects to SelectableUser objects for Students table
        ObservableList<User> allStudents = dataManagementService.getAllStudentsData();
        for (User student : allStudents) {
            SelectableUser selectableStudent = new SelectableUser(student);
            selectableStudent.setStatus(student.getStatus());
            selectableStudent.setClearanceStatus(student.getClearanceStatus());
            selectableStudent.setYearLevel(student.getYearLevel());
            selectableStudent.setPhone(student.getPhone());
            selectableStudent.setCanReapply(student.isCanReapply());
            selectableStudentsData.add(selectableStudent);
        }
        
        // Set items to tables
        tableAllUsers.setItems(selectableUsersData);
        tableAllStudents.setItems(selectableStudentsData);
        
        // Refresh the tables
        tableAllUsers.refresh();
        tableAllStudents.refresh();
        
        updateSelectionCounts();
    }
    
    private void refreshAllData() {
        dataManagementService.loadAllData();
        refreshSelectableData();
        updateDashboardStats();
        updateReportStatistics();
        
        // Refresh tables
        tableRequests.refresh();
        updateCategorizedStudentTables();
        
        // Update officer table
        if (tableOfficers != null) {
            tableOfficers.setItems(dataManagementService.getOfficersData());
            tableOfficers.refresh();
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        userManagementService.setCurrentUser(user);
        clearanceOperationsService.setCurrentUser(user);
        lblWelcome.setText("Welcome, " + user.getFullName() + " (Admin)");
        
        Platform.runLater(() -> {
            dataManagementService.loadAllData();
            refreshSelectableData();
            updateCategorizedStudentTables();
            updateReportStatistics();
        });
    }
    
    @FXML
    private void handleRefresh() {
        refreshAllData();
        showNotification("Refreshed", "All data has been refreshed successfully!", "info");
    }
    
    private void refreshAllDataAfterResubmission() {
        refreshAllData();
    }
    
    private void updateDashboardStats() {
        int totalStudents = dataManagementService.getTotalStudents();
        int totalOfficers = dataManagementService.getTotalOfficers();
        int totalRequests = dataManagementService.getTotalRequests();
        int approvedCount = dataManagementService.getApprovedCount();
        int rejectedCount = dataManagementService.getRejectedCount();
        int pendingCount = dataManagementService.getPendingCount();
        
        lblTotalStudents.setText("Students: " + totalStudents);
        lblTotalOfficers.setText("Officers: " + totalOfficers);
        lblTotalRequests.setText("Requests: " + totalRequests);
        
        if (lblTotalStudentsCard != null) {
            lblTotalStudentsCard.setText(String.valueOf(totalStudents));
        }
        if (lblTotalOfficersCard != null) {
            lblTotalOfficersCard.setText(String.valueOf(totalOfficers));
        }
        if (lblTotalRequestsCard != null) {
            lblTotalRequestsCard.setText(String.valueOf(totalRequests));
        }
        if (lblApprovedCount != null) {
            lblApprovedCount.setText(String.valueOf(approvedCount));
        }
        if (lblRejectedCount != null) {
            lblRejectedCount.setText(String.valueOf(rejectedCount));
        }
        if (lblPendingCount != null) {
            lblPendingCount.setText(String.valueOf(pendingCount));
        }
        
        lblUpdateTime.setText(LocalDateTime.now().toString().substring(11, 19));
    }
    
    private void updateReportStatistics() {
        dataManagementService.updateReportStatistics(lblDeletedToday, lblResubmissionsAllowed, 
                                                    lblPendingResubmissions, lblExpiredRequests);
    }
    
    private void setupTabAnimations() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                Node content = newTab.getContent();
                content.setOpacity(0);
                content.setTranslateY(10);
                
                Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(content.opacityProperty(), 0),
                        new KeyValue(content.translateYProperty(), 10)
                    ),
                    new KeyFrame(Duration.millis(300),
                        new KeyValue(content.opacityProperty(), 1),
                        new KeyValue(content.translateYProperty(), 0)
                    )
                );
                fadeIn.play();
            }
        });
    }
    
    private void setupActiveTabHighlight() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab != null) {
                oldTab.setStyle("-fx-background-color: #bdc3c7; -fx-border-color: transparent;");
            }
            if (newTab != null) {
                newTab.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 0 0 3px 0;");
                
                PauseTransition pause = new PauseTransition(Duration.millis(100));
                pause.setOnFinished(e -> {
                    newTab.getStyleClass().add("glow");
                    PauseTransition removeGlow = new PauseTransition(Duration.millis(300));
                    removeGlow.setOnFinished(e2 -> newTab.getStyleClass().remove("glow"));
                    removeGlow.play();
                });
                pause.play();
            }
        });
        
        if (mainTabPane.getTabs().size() > 0) {
            mainTabPane.getSelectionModel().getSelectedItem().setStyle(
                "-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 0 0 3px 0;"
            );
        }
    }
    
    @FXML
    private void showCurrentInfo() {
        boolean isVisible = cardsContainer.isVisible();
        cardsContainer.setVisible(!isVisible);
        cardsContainer.setManaged(!isVisible);
        
        if (!isVisible) {
            loadCurrentInfoData();
        }
    }

    private void loadCurrentInfoData() {
        lblActiveSessions.setText("3");
        lblSessionStatus.setText("Current: 2024 Spring");
        lblPendingActions.setText("12");
        lblActionStatus.setText("Requires attention");
        lblSystemLoad.setText("42%");
        lblLoadStatus.setText("Optimal");
        lblUptime.setText("15d 6h 30m");
        lblUptimeStatus.setText("Running");
        lblDatabaseSize.setText("256 MB");
        lblDbStatus.setText("Connected");
        lblTodayLogins.setText("47");
        lblLoginTrend.setText("+12%");
        lblActiveOfficers.setText("8");
        lblOfficerStatus.setText("Online: 5");
        lblClearanceRate.setText("78%");
        lblRateStatus.setText("This week");
    }
        
    public void updateCategorizedStudentTables() {
        if (tableApprovedStudents != null) {
            tableApprovedStudents.setItems(dataManagementService.getApprovedStudentsData());
        }
        if (tableRejectedStudents != null) {
            tableRejectedStudents.setItems(dataManagementService.getRejectedStudentsData());
        }
        if (tableExpiredStudents != null) {
            tableExpiredStudents.setItems(dataManagementService.getExpiredStudentsData());
        }
        if (tablePendingStudents != null) {
            tablePendingStudents.setItems(dataManagementService.getPendingStudentsData());
        }
        if (tableInProgressStudents != null) {
            tableInProgressStudents.setItems(dataManagementService.getInProgressStudentsData());
        }
    }
    
    private void setupSearchFunctionality() {
        cmbSearchType.setItems(FXCollections.observableArrayList(
            "All Users",
            "Students Only", 
            "Officers Only"
        ));
        cmbSearchType.setValue("All Users");
        
        cmbSearchType.valueProperty().addListener((obs, oldValue, newValue) -> {
            String searchText = txtSearchUsers.getText().trim();
            if (!searchText.isEmpty()) {
                handleUserSearchWithFilter(searchText, newValue);
            } else {
                handleFilterOnly(newValue);
            }
        });
        
        if (lblSearchStatus != null) {
            lblSearchStatus.setText("");
            lblSearchStatus.setVisible(false);
        }
        
        btnSearchUsers.setOnAction(e -> handleUserSearch());
        btnClearSearch.setOnAction(e -> handleClearSearch());
        
        btnSearchUsers.disableProperty().bind(
            txtSearchUsers.textProperty().isEmpty()
        );
        
        txtSearchUsers.setOnAction(e -> handleUserSearch());
        
        cmbSearchType.setTooltip(new Tooltip("Select user type to filter"));
        
        if (txtSearchRequests != null && btnSearchRequests != null) {
            btnSearchRequests.setOnAction(e -> handleRequestsSearch());
            btnClearRequestsSearch.setOnAction(e -> handleClearRequestsSearch());
            txtSearchRequests.setOnAction(e -> handleRequestsSearch());
        }
    }
    
    private void handleFilterOnly(String filterType) {
        if (txtSearchUsers.getText().trim().isEmpty()) {
            switch (filterType) {
                case "Students Only":
                    loadFilteredStudents();
                    break;
                case "Officers Only":
                    loadFilteredOfficers();
                    break;
                default: // "All Users"
                    dataManagementService.loadAllUsers();
                    refreshSelectableData();
                    if (lblSearchStatus != null) {
                        lblSearchStatus.setText("Showing all users");
                        lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        lblSearchStatus.setVisible(true);
                    }
                    break;
            }
        }
    }
    
    private void loadFilteredStudents() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT * FROM users 
                WHERE role = 'STUDENT'
                ORDER BY username
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            ObservableList<User> filteredStudents = FXCollections.observableArrayList();
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
                if ("STUDENT".equals(user.getRole())) {
                    user.setYearLevel(rs.getString("year_level"));
                    user.setPhone(rs.getString("phone"));
                }
                filteredStudents.add(user);
            }
            
            // Convert to SelectableUser
            selectableUsersData.clear();
            for (User user : filteredStudents) {
                SelectableUser selectableUser = new SelectableUser(user);
                selectableUser.setStatus(user.getStatus());
                selectableUser.setYearLevel(user.getYearLevel());
                selectableUser.setPhone(user.getPhone());
                selectableUsersData.add(selectableUser);
            }
            
            tableAllUsers.setItems(selectableUsersData);
            tableAllUsers.refresh();
            
            if (lblSearchStatus != null) {
                lblSearchStatus.setText("Showing only students (" + filteredStudents.size() + " users)");
                lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                lblSearchStatus.setVisible(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load students: " + e.getMessage());
        }
    }
    
    private void loadFilteredOfficers() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT * FROM users 
                WHERE role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD', 'ADMIN') 
                ORDER BY role, username
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            ObservableList<User> filteredOfficers = FXCollections.observableArrayList();
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
                filteredOfficers.add(user);
            }
            
            // Convert to SelectableUser
            selectableUsersData.clear();
            for (User user : filteredOfficers) {
                SelectableUser selectableUser = new SelectableUser(user);
                selectableUser.setStatus(user.getStatus());
                selectableUsersData.add(selectableUser);
            }
            
            tableAllUsers.setItems(selectableUsersData);
            tableAllUsers.refresh();
            
            if (lblSearchStatus != null) {
                lblSearchStatus.setText("Showing only officers (" + filteredOfficers.size() + " users)");
                lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                lblSearchStatus.setVisible(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load officers: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUserSearch() {
        String searchText = txtSearchUsers.getText().trim();
        String searchType = cmbSearchType.getValue();
        
        handleUserSearchWithFilter(searchText, searchType);
    }
    
    private void handleUserSearchWithFilter(String searchText, String searchType) {
        if (searchText.isEmpty()) {
            if ("Students Only".equals(searchType)) {
                loadFilteredStudents();
            } else if ("Officers Only".equals(searchType)) {
                loadFilteredOfficers();
            } else {
                dataManagementService.loadAllUsers();
                refreshSelectableData();
                if (lblSearchStatus != null) {
                    lblSearchStatus.setText("Showing all users");
                    lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    lblSearchStatus.setVisible(true);
                }
            }
        } else {
            dataManagementService.handleUserSearch(searchText, searchType);
            refreshSelectableData();
        }
    }
    
    @FXML
    private void handleClearSearch() {
        txtSearchUsers.clear();
        cmbSearchType.setValue("All Users");
        dataManagementService.loadAllUsers();
        refreshSelectableData();
        
        if (lblSearchStatus != null) {
            lblSearchStatus.setText("Showing all users");
            lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            lblSearchStatus.setVisible(true);
        }
    }
    
    @FXML
    private void handleRequestsSearch() {
        if (txtSearchRequests != null) {
            String query = txtSearchRequests.getText().trim();
            if (!query.isEmpty()) {
                dataManagementService.searchRequests(query);
            }
        }
    }
    
    @FXML
    private void handleClearRequestsSearch() {
        if (txtSearchRequests != null) {
            txtSearchRequests.clear();
            dataManagementService.loadClearanceRequests();
        }
    }
    
    @FXML
    private void openRegisterStudent() {
        userManagementService.openRegisterStudent(currentUser, () -> {
            refreshAllData();
        });
    }
    
    @FXML
    private void handleDeleteUser() {
        SelectableUser selectedSelectableUser = tableAllUsers.getSelectionModel().getSelectedItem();
        User selectedUser = null;
        
        if (selectedSelectableUser != null) {
            // Convert SelectableUser to User
            selectedUser = new User(
                selectedSelectableUser.getId(),
                selectedSelectableUser.getUsername(),
                selectedSelectableUser.getFullName(),
                selectedSelectableUser.getRole(),
                selectedSelectableUser.getEmail(),
                selectedSelectableUser.getDepartment()
            );
            selectedUser.setStatus(selectedSelectableUser.getStatus());
        }
        
        if (selectedUser == null) {
            showAlert("Error", "Please select a user first!");
            return;
        }
        
        userManagementService.handleDeleteUser(selectedUser, currentUser, () -> {
            refreshAllData();
        });
    }
    
    @FXML
    private void openManageOfficers() {
        userManagementService.openManageOfficers(currentUser, () -> {
            refreshAllData();
        });
    }
    
    @FXML
    private void resetUserPassword() {
        SelectableUser selectedSelectableUser = tableAllUsers.getSelectionModel().getSelectedItem();
        if (selectedSelectableUser != null) {
            // Convert SelectableUser to User
            User selectedUser = new User(
                selectedSelectableUser.getId(),
                selectedSelectableUser.getUsername(),
                selectedSelectableUser.getFullName(),
                selectedSelectableUser.getRole(),
                selectedSelectableUser.getEmail(),
                selectedSelectableUser.getDepartment()
            );
            selectedUser.setStatus(selectedSelectableUser.getStatus());
            userManagementService.resetUserPassword(selectedUser);
        } else {
            showAlert("Error", "Please select a user first!");
        }
    }
    
    @FXML
    private void toggleUserStatus() {
        SelectableUser selectedSelectableUser = tableAllUsers.getSelectionModel().getSelectedItem();
        if (selectedSelectableUser != null) {
            // Convert SelectableUser to User
            User selectedUser = new User(
                selectedSelectableUser.getId(),
                selectedSelectableUser.getUsername(),
                selectedSelectableUser.getFullName(),
                selectedSelectableUser.getRole(),
                selectedSelectableUser.getEmail(),
                selectedSelectableUser.getDepartment()
            );
            selectedUser.setStatus(selectedSelectableUser.getStatus());
            userManagementService.toggleUserStatus(selectedUser);
        } else {
            showAlert("Error", "Please select a user first!");
        }
    }
    
    @FXML
    private void openWorkflowManagement() {
        clearanceOperationsService.openWorkflowManagement();
    }
    
    @FXML
    private void openSessionManagement() {
        clearanceOperationsService.openSessionManagement();
    }
    
    @FXML
    private void generateClearanceCertificates() {
        clearanceOperationsService.generateClearanceCertificates();
    }
    
    @FXML
    private void verifyCertificate() {
        clearanceOperationsService.verifyCertificate();
    }
    
    @FXML
    private void processSemesterRollover() {
        clearanceOperationsService.processSemesterRollover();
    }
    
    @FXML
    private void handleLogout() {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout");
            confirm.setHeaderText("Confirm Logout");
            confirm.setContentText("Are you sure you want to logout?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String fxmlPath = "/com/university/clearance/resources/views/Login.fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

                if (loader.getLocation() == null) {
                    showAlert("Error", "Login screen not found. Check FXML path.");
                    return;
                }
                
                Parent root = loader.load();
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                Scene currentScene = lblWelcome.getScene();

                Scene newScene = new Scene(root,
                        currentScene.getWidth(),
                        currentScene.getHeight());

                stage.setScene(newScene);
                stage.setTitle("University Clearance System - Login");
                stage.centerOnScreen();
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAllowResubmission() {
        User selectedStudent = getSelectedStudentFromActiveTab();
        
        if (selectedStudent == null) {
            showAlert("Selection Required", "Please select a student from the students tab first!");
            return;
        }
        
        if (!"STUDENT".equals(selectedStudent.getRole())) {
            showAlert("Invalid Selection", "Please select a student, not an officer or admin!");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = """
                SELECT cr.status, cr.can_reapply, cr.request_date,
                       DATEDIFF(NOW(), cr.request_date) as days_since_request
                FROM clearance_requests cr 
                WHERE cr.student_id = ? 
                ORDER BY cr.id DESC 
                LIMIT 1
                """;
            
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, selectedStudent.getId());
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                boolean canReapply = rs.getBoolean("can_reapply");
                Date requestDate = rs.getDate("request_date");
                int daysSinceRequest = rs.getInt("days_since_request");
                
                boolean isEligibleStatus = "REJECTED".equals(status) || "EXPIRED".equals(status);
                
                if (!isEligibleStatus) {
                    showAlert("Not Eligible", 
                        "This student's request is not rejected or expired.\n" +
                        "Current status: " + status + "\n\n" +
                        "Only rejected or expired students can be allowed to resubmit.");
                    return;
                }
                
                if (canReapply) {
                    showAlert("Already Allowed", 
                        "This student has already been allowed to resubmit.\n" +
                        "They should be able to submit a new clearance request.");
                    return;
                }
                
                String additionalInfo = "";
                if ("EXPIRED".equals(status)) {
                    additionalInfo = "\n\nThis request expired " + daysSinceRequest + " days ago.";
                }
                
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Allow Clearance Resubmission");
                confirm.setHeaderText("Allow Student to Resubmit");
                confirm.setContentText("Allow " + selectedStudent.getFullName() + 
                                     " (" + selectedStudent.getUsername() + ") to submit a new clearance request?\n\n" +
                                     "Current Status: " + status + 
                                     additionalInfo + "\n\n" +
                                     "This will:\n" +
                                     "• Reset their clearance status to 'IN_PROGRESS'\n" +
                                     "• Clear previous approval records\n" +
                                     "• Enable them to submit a new request\n" +
                                     "• Update all relevant records");
                
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    allowStudentResubmit(selectedStudent, status);
                }
                
            } else {
                showAlert("No Request Found", 
                    "This student has no clearance request to resubmit.\n" +
                    "They need to submit their first clearance request.");
                return;
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to check student eligibility: " + e.getMessage());
            return;
        }
    }

    private void allowStudentResubmit(User student, String originalStatus) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String getLatestIdSql = """
                    SELECT id FROM clearance_requests 
                    WHERE student_id = ? AND (status = 'REJECTED' OR status = 'EXPIRED')
                    ORDER BY id DESC 
                    LIMIT 1
                    """;
                
                PreparedStatement getStmt = conn.prepareStatement(getLatestIdSql);
                getStmt.setInt(1, student.getId());
                ResultSet rs = getStmt.executeQuery();
                
                if (rs.next()) {
                    int latestRequestId = rs.getInt("id");
                    
                    String updateRequestSql = """
                        UPDATE clearance_requests 
                        SET can_reapply = TRUE, 
                            status = 'IN_PROGRESS',
                            request_date = NOW(),
                            previous_status = ?
                        WHERE id = ?
                        """;
                    
                    PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql);
                    updateRequestStmt.setString(1, originalStatus);
                    updateRequestStmt.setInt(2, latestRequestId);
                    int requestUpdated = updateRequestStmt.executeUpdate();
                    
                    if (requestUpdated > 0) {
                        String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                        PreparedStatement clearApprovalsStmt = conn.prepareStatement(clearApprovalsSql);
                        clearApprovalsStmt.setInt(1, latestRequestId);
                        clearApprovalsStmt.executeUpdate();
                        
                        String workflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                        PreparedStatement workflowStmt = conn.prepareStatement(workflowSql);
                        ResultSet workflowRs = workflowStmt.executeQuery();
                        
                        String insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, status)
                            VALUES (?, ?, 'PENDING')
                            """;
                        
                        PreparedStatement insertApprovalStmt = conn.prepareStatement(insertApprovalSql);
                        
                        while (workflowRs.next()) {
                            insertApprovalStmt.setInt(1, latestRequestId);
                            insertApprovalStmt.setString(2, workflowRs.getString("role"));
                            insertApprovalStmt.addBatch();
                        }
                        
                        insertApprovalStmt.executeBatch();
                        
                        String updateDormSql = """
                            UPDATE student_dormitory_credentials 
                            SET clearance_status = 'PENDING'
                            WHERE student_id = ?
                            """;
                        
                        PreparedStatement updateDormStmt = conn.prepareStatement(updateDormSql);
                        updateDormStmt.setInt(1, student.getId());
                        updateDormStmt.executeUpdate();
                        
                        String auditSql = """
                            INSERT INTO audit_logs (user_id, action, details, timestamp)
                            VALUES (?, 'ALLOW_RESUBMISSION', ?, NOW())
                            """;
                        
                        PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                        auditStmt.setInt(1, currentUser.getId());
                        auditStmt.setString(2, "Allowed resubmission for student: " + 
                            student.getUsername() + " - " + student.getFullName() + 
                            " (Previous status: " + originalStatus + ")");
                        auditStmt.executeUpdate();
                        
                        conn.commit();
                        
                        refreshAllDataAfterResubmission();
                        
                        String statusMessage = "EXPIRED".equals(originalStatus) ? 
                            "expired request" : "rejected request";
                        
                        showNotification("Resubmission Allowed", 
                            "✅ " + student.getFullName() + " can now resubmit their clearance request!\n\n" +
                            "Previous Status: " + originalStatus + "\n" +
                            "New Status: 🔄 IN PROGRESS\n\n" +
                            "The student can now proceed with their clearance request.", 
                            "success");
                    } else {
                        conn.rollback();
                        showAlert("Error", "Failed to update clearance request.");
                    }
                } else {
                    showAlert("Error", "No rejected or expired request found for this student.");
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to allow resubmission: " + e.getMessage());
        }
    }
    
    private User getSelectedStudentFromActiveTab() {
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        
        if (selectedTab != null && selectedTab.getText().contains("Students")) {
            Node content = selectedTab.getContent();
            if (content instanceof TabPane) {
                TabPane studentTabs = (TabPane) content;
                Tab selectedStudentTab = studentTabs.getSelectionModel().getSelectedItem();
                
                if (selectedStudentTab != null) {
                    String tabText = selectedStudentTab.getText();
                    
                    if (tabText.contains("All Students")) {
                        SelectableUser selectedSelectable = tableAllStudents.getSelectionModel().getSelectedItem();
                        if (selectedSelectable != null) {
                            // Convert SelectableUser to User
                            User student = new User(
                                selectedSelectable.getId(),
                                selectedSelectable.getUsername(),
                                selectedSelectable.getFullName(),
                                selectedSelectable.getRole(),
                                selectedSelectable.getEmail(),
                                selectedSelectable.getDepartment()
                            );
                            student.setStatus(selectedSelectable.getStatus());
                            student.setYearLevel(selectedSelectable.getYearLevel());
                            student.setPhone(selectedSelectable.getPhone());
                            student.setClearanceStatus(selectedSelectable.getClearanceStatus());
                            student.setCanReapply(selectedSelectable.isCanReapply());
                            return student;
                        }
                    } else if (tabText.contains("Approved")) {
                        return tableApprovedStudents.getSelectionModel().getSelectedItem();
                    } else if (tabText.contains("Rejected")) {
                        return tableRejectedStudents.getSelectionModel().getSelectedItem();
                    } else if (tabText.contains("Expired")) {
                        return tableExpiredStudents.getSelectionModel().getSelectedItem();
                    } else if (tabText.contains("Pending")) {
                        return tablePendingStudents.getSelectionModel().getSelectedItem();
                    } else if (tabText.contains("In Progress")) {
                        return tableInProgressStudents.getSelectionModel().getSelectedItem();
                    }
                }
            }
        }
        
        // Check All Users tab
        Tab allUsersTab = mainTabPane.getTabs().stream()
            .filter(tab -> tab.getText().contains("All Users"))
            .findFirst()
            .orElse(null);
        
        if (allUsersTab != null && allUsersTab.isSelected()) {
            SelectableUser selectedSelectable = tableAllUsers.getSelectionModel().getSelectedItem();
            if (selectedSelectable != null && "STUDENT".equals(selectedSelectable.getRole())) {
                // Convert SelectableUser to User
                User student = new User(
                    selectedSelectable.getId(),
                    selectedSelectable.getUsername(),
                    selectedSelectable.getFullName(),
                    selectedSelectable.getRole(),
                    selectedSelectable.getEmail(),
                    selectedSelectable.getDepartment()
                );
                student.setStatus(selectedSelectable.getStatus());
                student.setYearLevel(selectedSelectable.getYearLevel());
                student.setPhone(selectedSelectable.getPhone());
                student.setClearanceStatus(selectedSelectable.getClearanceStatus());
                student.setCanReapply(selectedSelectable.isCanReapply());
                return student;
            }
        }
        
        return null;
    }
    
    private void setupAllowResubmitButtons() {
        if (tableRequests == null || colResubmission == null) {
            return;
        }
        
        colResubmission.setCellFactory(column -> {
            return new TableCell<ClearanceRequest, Void>() {
                private final Button resubmitButton = new Button("Allow Resubmission");
                
                {
                    resubmitButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                    resubmitButton.setOnAction(event -> {
                        ClearanceRequest request = getTableView().getItems().get(getIndex());
                        if (request != null) {
                            clearanceOperationsService.handleAllowResubmission(request, currentUser, () -> {
                                refreshAllDataAfterResubmission();
                            });
                        }
                    });
                    
                    resubmitButton.setOnMouseEntered(e -> {
                        if (!resubmitButton.isDisabled()) {
                            resubmitButton.setStyle("-fx-background-color: #229954; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                        }
                    });
                    
                    resubmitButton.setOnMouseExited(e -> {
                        if (!resubmitButton.isDisabled()) {
                            resubmitButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                        }
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        ClearanceRequest request = (ClearanceRequest) getTableRow().getItem();
                        if (request != null) {
                            setGraphic(resubmitButton);
                            boolean shouldEnable = clearanceOperationsService.isResubmissionAllowed(request);
                            
                            if (shouldEnable) {
                                resubmitButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                                resubmitButton.setDisable(false);
                                resubmitButton.setTooltip(new Tooltip("Click to allow this student to resubmit their clearance request"));
                            } else {
                                resubmitButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #ecf0f1; -fx-font-weight: normal; -fx-padding: 5 10;");
                                resubmitButton.setDisable(true);
                                String tooltipText = clearanceOperationsService.getDisableReason(request);
                                resubmitButton.setTooltip(new Tooltip(tooltipText));
                            }
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
        });
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showNotification(String title, String message, String type) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        switch (type) {
            case "success":
                alert.getDialogPane().setStyle("-fx-border-color: #27ae60; -fx-border-width: 2px;");
                break;
            case "error":
                alert.getDialogPane().setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                break;
            case "warning":
                alert.getDialogPane().setStyle("-fx-border-color: #f39c12; -fx-border-width: 2px;");
                break;
            case "info":
                alert.getDialogPane().setStyle("-fx-border-color: #3498db; -fx-border-width: 2px;");
                break;
        }
        
        alert.showAndWait();
    }
    
    // Getters for DataManagementService
    public TableView<SelectableUser> getTableAllStudents() { return tableAllStudents; }
    public TableView<SelectableUser> getTableAllUsers() { return tableAllUsers; }
    public TableView<ClearanceRequest> getTableRequests() { return tableRequests; }
    public Label getLblSearchStatus() { return lblSearchStatus; }
    public TextField getTxtSearchUsers() { return txtSearchUsers; }
    public ComboBox<String> getCmbSearchType() { return cmbSearchType; }
    
    // Getter for selectable data (for DataManagementService if needed)
    public ObservableList<SelectableUser> getSelectableUsersData() { return selectableUsersData; }
    public ObservableList<SelectableUser> getSelectableStudentsData() { return selectableStudentsData; }
}