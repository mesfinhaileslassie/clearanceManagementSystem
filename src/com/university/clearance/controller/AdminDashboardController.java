package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.ClearanceRequest;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Optional;

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

    // Students Tables
    @FXML private TableView<User> tableAllStudents;
    @FXML private TableView<User> tableApprovedStudents;
    @FXML private TableView<User> tableRejectedStudents;
    @FXML private TableView<User> tablePendingStudents;
    @FXML private TableView<User> tableInProgressStudents;
    @FXML private TableView<User> tableExpiredStudents;
    // Students Table Columns
    @FXML private TableColumn<User, String> colStudentId;
    @FXML private TableColumn<User, String> colStudentName;
    @FXML private TableColumn<User, String> colStudentDepartment;
    @FXML private TableColumn<User, String> colStudentYear;
    @FXML private TableColumn<User, String> colClearanceStatus;
    
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
        // This method should call the DataManagementService methods
        
        // Setup All Students table
        dataManagementService.setupStudentTable(tableAllStudents, colStudentId, colStudentName, 
                                               colStudentDepartment, colStudentYear, 
                                               colClearanceStatus);
        
        // Setup categorized tables using the simple setup method
        dataManagementService.setupSimpleStudentTable(tableApprovedStudents);
        dataManagementService.setupSimpleStudentTable(tableRejectedStudents);
        dataManagementService.setupSimpleStudentTable(tableExpiredStudents);
        dataManagementService.setupSimpleStudentTable(tablePendingStudents);
        dataManagementService.setupSimpleStudentTable(tableInProgressStudents);
        
        // Setup All Users table columns
        if (colAllUserId != null && colAllUserName != null && colAllUserRole != null && 
            colAllUserDepartment != null && colAllUserStatus != null) {
            
            colAllUserId.setCellValueFactory(new PropertyValueFactory<>("username"));
            colAllUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colAllUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colAllUserDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colAllUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            // Status column styling for All Users table
            colAllUserStatus.setCellFactory(column -> new TableCell<User, String>() {
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
        
        // Setup Officers table columns
        if (colOfficerId != null && colOfficerName != null && colOfficerRole != null && 
            colOfficerDepartment != null && colOfficerStatus != null) {
            
            colOfficerId.setCellValueFactory(new PropertyValueFactory<>("username"));
            colOfficerName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colOfficerRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colOfficerDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colOfficerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            // Status column styling for Officers table
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
        if (colRequestStudentId != null && colRequestName != null && colRequestDepartment != null && 
            colRequestStatus != null && colRequestDate != null && colRequestApproved != null) {
            
            colRequestStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colRequestName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colRequestDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colRequestStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
            colRequestApproved.setCellValueFactory(new PropertyValueFactory<>("approvedCount"));
            
            // Status column styling for Clearance Requests table
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
    
    
    
    private ObservableList<User> expiredStudentsData = FXCollections.observableArrayList();
    
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
    
    
    
    
    @FXML
    private void handleAllowResubmission() {
        // Get selected student based on active tab
        User selectedStudent = getSelectedStudentFromActiveTab();
        
        if (selectedStudent == null) {
            showAlert("Selection Required", "Please select a student from the students tab first!");
            return;
        }
        
        // Check if the selected user is actually a student
        if (!"STUDENT".equals(selectedStudent.getRole())) {
            showAlert("Invalid Selection", "Please select a student, not an officer or admin!");
            return;
        }
        
        // Check if student is eligible for resubmission
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
                
                // Check if status is eligible for resubmission
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
                
                // For expired requests, show additional info
                String additionalInfo = "";
                if ("EXPIRED".equals(status)) {
                    additionalInfo = "\n\nThis request expired " + daysSinceRequest + " days ago.";
                }
                
                // Show confirmation dialog
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
                // Get the latest request for this student (could be rejected or expired)
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
                    
                    // Update the clearance request to allow resubmission (removed updated_at)
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
                        // Clear previous approvals
                        String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                        PreparedStatement clearApprovalsStmt = conn.prepareStatement(clearApprovalsSql);
                        clearApprovalsStmt.setInt(1, latestRequestId);
                        clearApprovalsStmt.executeUpdate();
                        
                        // Get current workflow configuration
                        String workflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                        PreparedStatement workflowStmt = conn.prepareStatement(workflowSql);
                        ResultSet workflowRs = workflowStmt.executeQuery();
                        
                        // Create new pending approvals based on workflow
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
                        
                        // Update dormitory clearance status if applicable
                        String updateDormSql = """
                            UPDATE student_dormitory_credentials 
                            SET clearance_status = 'PENDING'
                            WHERE student_id = ?
                            """;
                        
                        PreparedStatement updateDormStmt = conn.prepareStatement(updateDormSql);
                        updateDormStmt.setInt(1, student.getId());
                        updateDormStmt.executeUpdate();
                        
                        // Log the action with original status
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
                        
                        // Refresh UI
                        refreshAllDataAfterResubmission();
                        
                        // Different success message based on original status
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
                        return tableAllStudents.getSelectionModel().getSelectedItem();
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
        
        // Also check if user is selected from All Users tab
        Tab allUsersTab = mainTabPane.getTabs().stream()
            .filter(tab -> tab.getText().contains("All Users"))
            .findFirst()
            .orElse(null);
        
        if (allUsersTab != null && allUsersTab.isSelected()) {
            User selectedUser = tableAllUsers.getSelectionModel().getSelectedItem();
            if (selectedUser != null && "STUDENT".equals(selectedUser.getRole())) {
                return selectedUser;
            }
        }
        
        return null;
    }

    private void allowStudentResubmit(User student) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get the latest rejected request for this student
                String getLatestIdSql = """
                    SELECT id FROM clearance_requests 
                    WHERE student_id = ? AND status = 'REJECTED'
                    ORDER BY id DESC 
                    LIMIT 1
                    """;
                
                PreparedStatement getStmt = conn.prepareStatement(getLatestIdSql);
                getStmt.setInt(1, student.getId());
                ResultSet rs = getStmt.executeQuery();
                
                if (rs.next()) {
                    int latestRequestId = rs.getInt("id");
                    
                    // Update the clearance request to allow resubmission
                    String updateRequestSql = """
                        UPDATE clearance_requests 
                        SET can_reapply = TRUE, 
                            status = 'IN_PROGRESS',
                            request_date = NOW(),
                            updated_at = NOW()
                        WHERE id = ?
                        """;
                    
                    PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql);
                    updateRequestStmt.setInt(1, latestRequestId);
                    int requestUpdated = updateRequestStmt.executeUpdate();
                    
                    if (requestUpdated > 0) {
                        // Clear previous approvals
                        String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                        PreparedStatement clearApprovalsStmt = conn.prepareStatement(clearApprovalsSql);
                        clearApprovalsStmt.setInt(1, latestRequestId);
                        clearApprovalsStmt.executeUpdate();
                        
                        // Get current workflow configuration
                        String workflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                        PreparedStatement workflowStmt = conn.prepareStatement(workflowSql);
                        ResultSet workflowRs = workflowStmt.executeQuery();
                        
                        // Create new pending approvals based on workflow
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
                        
                        // Update dormitory clearance status if applicable
                        String updateDormSql = """
                            UPDATE student_dormitory_credentials 
                            SET clearance_status = 'PENDING'
                            WHERE student_id = ?
                            """;
                        
                        PreparedStatement updateDormStmt = conn.prepareStatement(updateDormSql);
                        updateDormStmt.setInt(1, student.getId());
                        updateDormStmt.executeUpdate();
                        
                        // Log the action
                        String auditSql = """
                            INSERT INTO audit_logs (user_id, action, details, timestamp)
                            VALUES (?, 'ALLOW_RESUBMISSION', ?, NOW())
                            """;
                        
                        PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                        auditStmt.setInt(1, currentUser.getId());
                        auditStmt.setString(2, "Allowed resubmission for student: " + 
                            student.getUsername() + " - " + student.getFullName());
                        auditStmt.executeUpdate();
                        
                        conn.commit();
                        
                        // Refresh UI
                        refreshAllDataAfterResubmission();
                        
                        showNotification("Resubmission Allowed", 
                            "✅ " + student.getFullName() + " can now resubmit their clearance request!\n\n" +
                            "Status updated to: 🔄 IN PROGRESS\n" +
                            "The student can now proceed with their clearance request.", 
                            "success");
                    } else {
                        conn.rollback();
                        showAlert("Error", "Failed to update clearance request.");
                    }
                } else {
                    showAlert("Error", "No rejected request found for this student.");
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
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        userManagementService.setCurrentUser(user);
        clearanceOperationsService.setCurrentUser(user);
        lblWelcome.setText("Welcome, " + user.getFullName() + " (Admin)");
        
        Platform.runLater(() -> {
            dataManagementService.loadAllData();
            updateCategorizedStudentTables();  // Add this line
            updateReportStatistics();
        });
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
        
        // Add listener to ComboBox selection changes
        cmbSearchType.valueProperty().addListener((obs, oldValue, newValue) -> {
            String searchText = txtSearchUsers.getText().trim();
            if (!searchText.isEmpty()) {
                // Trigger search when ComboBox selection changes and there's search text
                handleUserSearchWithFilter(searchText, newValue);
            } else {
                // If no search text, just reload all users based on the new filter
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
        // If there's no search text, just show all users filtered by type
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
            
            tableAllUsers.setItems(filteredStudents);
            
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
            
            tableAllUsers.setItems(filteredOfficers);
            
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
    private void testLoadUsers() {
        System.out.println("=== TEST LOAD USERS ===");
        dataManagementService.loadAllUsers();
        debugTableStatus();
    }
    
    
    @FXML
    private void handleUserSearch() {
        String searchText = txtSearchUsers.getText().trim();
        String searchType = cmbSearchType.getValue();
        
        handleUserSearchWithFilter(searchText, searchType);
    }
    
    private void handleUserSearchWithFilter(String searchText, String searchType) {
        if (searchText.isEmpty()) {
            // If search is empty, just show filtered users based on ComboBox selection
            if ("Students Only".equals(searchType)) {
                loadFilteredStudents();
            } else if ("Officers Only".equals(searchType)) {
                loadFilteredOfficers();
            } else {
                dataManagementService.loadAllUsers();
                if (lblSearchStatus != null) {
                    lblSearchStatus.setText("Showing all users");
                    lblSearchStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    lblSearchStatus.setVisible(true);
                }
            }
        } else {
            // Perform search with both search text and filter
            dataManagementService.handleUserSearch(searchText, searchType);
        }
    }
    
    @FXML
    private void handleClearSearch() {
        txtSearchUsers.clear();
        cmbSearchType.setValue("All Users");
        dataManagementService.loadAllUsers();
        
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
    private void handleRefresh() {
        System.out.println("DEBUG: Refresh button clicked");
        
        // Load all data
        dataManagementService.loadAllData();
        updateReportStatistics();
        
        // Force refresh specific tables
        if (tableAllUsers != null) {
            System.out.println("DEBUG: Refreshing tableAllUsers");
            tableAllUsers.refresh();
            // If refresh doesn't work, try re-setting items
            tableAllUsers.setItems(dataManagementService.getAllUsersData());
        }
        
        if (tableAllStudents != null) {
            System.out.println("DEBUG: Refreshing tableAllStudents");
            tableAllStudents.refresh();
        }
        
        // Refresh categorized tables
        updateCategorizedStudentTables();
        
        showNotification("Refreshed", "All data has been refreshed successfully!", "info");
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
    
    @FXML
    private void openRegisterStudent() {
        userManagementService.openRegisterStudent(currentUser, () -> {
            dataManagementService.loadAllUsers();
            dataManagementService.loadAllStudents();
        });
    }
    
    
    private void debugTableStatus() {
        System.out.println("=== DEBUG TABLE STATUS ===");
        System.out.println("tableAllUsers is null: " + (tableAllUsers == null));
        System.out.println("tableAllStudents is null: " + (tableAllStudents == null));
        
        if (tableAllUsers != null) {
            System.out.println("tableAllUsers columns: " + tableAllUsers.getColumns().size());
            System.out.println("tableAllUsers items: " + tableAllUsers.getItems().size());
        }
    }
    
    
    
    @FXML
    private void handleDeleteUser() {
        User selectedUser = null;
        
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabText = selectedTab.getText();
            if (tabText.contains("All Users")) {
                selectedUser = tableAllUsers.getSelectionModel().getSelectedItem();
            } else if (tabText.contains("Officers")) {
                selectedUser = tableOfficers.getSelectionModel().getSelectedItem();
            } else if (tabText.contains("Students")) {
                Node content = selectedTab.getContent();
                if (content instanceof TabPane) {
                    TabPane studentTabs = (TabPane) content;
                    Tab selectedStudentTab = studentTabs.getSelectionModel().getSelectedItem();
                    if (selectedStudentTab != null) {
                        String studentTabText = selectedStudentTab.getText();
                        if (studentTabText.contains("All Students")) {
                            selectedUser = tableAllStudents.getSelectionModel().getSelectedItem();
                        } else if (studentTabText.contains("Rejected")) {
                            selectedUser = tableRejectedStudents.getSelectionModel().getSelectedItem();
                        }
                    }
                }
            }
        }
        
        if (selectedUser == null) {
            showAlert("Error", "Please select a user first!");
            return;
        }
        
        userManagementService.handleDeleteUser(selectedUser, currentUser, () -> {
            dataManagementService.loadAllData();
            updateReportStatistics();
        });
    }
    
   
    
    
    
    @FXML
    private void openManageOfficers() {
        userManagementService.openManageOfficers(currentUser, () -> {
            dataManagementService.loadAllUsers();
            dataManagementService.loadOfficers();
        });
    }
    
    @FXML
    private void resetUserPassword() {
        userManagementService.resetUserPassword(tableAllUsers.getSelectionModel().getSelectedItem());
    }
    
    @FXML
    private void toggleUserStatus() {
        userManagementService.toggleUserStatus(tableAllUsers.getSelectionModel().getSelectedItem());
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

    private void refreshAllDataAfterResubmission() {
        dataManagementService.loadAllData();
        updateDashboardStats();
        updateReportStatistics();
        
        // Refresh all tables
        tableAllStudents.refresh();
        tableRejectedStudents.refresh();
        tableExpiredStudents.refresh();
        tableInProgressStudents.refresh();
        tablePendingStudents.refresh();
        tableApprovedStudents.refresh();
        tableAllUsers.refresh();
        tableRequests.refresh();
        
        // Update categorized tables
        updateCategorizedStudentTables();
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
    
    public TableView<User> getTableAllStudents() { return tableAllStudents; }
    public TableView<User> getTableAllUsers() { return tableAllUsers; }
    public TableView<ClearanceRequest> getTableRequests() { return tableRequests; }
    public Label getLblSearchStatus() { return lblSearchStatus; }
    public TextField getTxtSearchUsers() { return txtSearchUsers; }
    public ComboBox<String> getCmbSearchType() { return cmbSearchType; }
}