package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.ClearanceRequest;
import com.university.clearance.utils.PhoneInputField;
import com.university.clearance.utils.ValidationHelper;
import com.university.clearance.utils.ValidationHelper.ValidationResult;
import javafx.util.Callback;
import javafx.animation.KeyFrame;

import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        setupSearchFunctionality();
        setupTabAnimations();
        setupActiveTabHighlight();
        setupDeleteButtons();
        setupAllowResubmitButtons(); // Make sure this is called
        
        // Apply CSS with scene listener
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
    
    private void setupDeleteButtons() {
        // Setup delete button for All Users table
        TableColumn<User, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteButton = new Button("🗑️ Delete");
            
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user != null && !"admin".equals(user.getUsername())) {
                        setGraphic(deleteButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        if (tableAllUsers.getColumns().size() >= 6) {
            tableAllUsers.getColumns().add(deleteCol);
        }
        
        // Setup delete button for Officers table
        TableColumn<User, Void> deleteOfficerCol = new TableColumn<>("Actions");
        deleteOfficerCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteButton = new Button("🗑️ Delete");
            
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    User officer = getTableView().getItems().get(getIndex());
                    handleDeleteUser(officer);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User officer = getTableView().getItems().get(getIndex());
                    if (officer != null && !"admin".equals(officer.getUsername())) {
                        setGraphic(deleteButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        if (tableOfficers.getColumns().size() >= 6) {
            tableOfficers.getColumns().add(deleteOfficerCol);
        }
    }
    
    private void setupAllowResubmitButtons() {
        System.out.println("=== DEBUG: Setting up Allow Resubmission buttons ===");
        System.out.println("tableRequests is null: " + (tableRequests == null));
        System.out.println("colResubmission is null: " + (colResubmission == null));
        
        if (tableRequests == null) {
            System.err.println("ERROR: tableRequests is null!");
            return;
        }
        
        if (colResubmission == null) {
            System.err.println("ERROR: colResubmission FXML reference is null!");
            // Try to find it manually
            colResubmission = (TableColumn<ClearanceRequest, Void>) tableRequests.getColumns().stream()
                .filter(col -> "Resubmission".equals(col.getText()))
                .findFirst()
                .orElse(null);
                
            if (colResubmission == null) {
                System.err.println("Creating new column as fallback");
                colResubmission = new TableColumn<>("Resubmission");
                colResubmission.setPrefWidth(150);
                colResubmission.setMinWidth(120);
                tableRequests.getColumns().add(colResubmission);
            }
        }
        
        // Now set the cell factory
        colResubmission.setCellFactory(column -> {
            System.out.println("Creating cell factory for resubmission column");
            return new TableCell<ClearanceRequest, Void>() {
                private final Button resubmitButton = new Button("Allow Resubmission");
                
                {
                    resubmitButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                    resubmitButton.setOnAction(event -> {
                        ClearanceRequest request = getTableView().getItems().get(getIndex());
                        if (request != null) {
                            System.out.println("DEBUG: Allow Resubmission clicked for student: " + request.getStudentId());
                            handleAllowResubmission(request);
                        }
                    });
                    
                    // Add hover effects
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
                
                @Override  // <-- THIS IS THE ONE TO UPDATE!
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        ClearanceRequest request = (ClearanceRequest) getTableRow().getItem();
                        if (request != null) {
                            // Button is ALWAYS VISIBLE
                            setGraphic(resubmitButton);
                            
                            // Check if button should be enabled
                            boolean shouldEnable = isResubmissionAllowed(request);
                            
                            System.out.println("CELL DEBUG: Row " + getIndex() + 
                                             " - Student: " + request.getStudentId() + 
                                             ", Status: " + request.getStatus() + 
                                             ", Can reapply: " + request.isCanReapply() +
                                             ", Is expired: " + request.isExpired() +
                                             ", Should enable: " + shouldEnable);
                            
                            if (shouldEnable) {
                                // Button is enabled and functional
                                resubmitButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                                resubmitButton.setDisable(false);
                                resubmitButton.setTooltip(new Tooltip("Click to allow this student to resubmit their clearance request"));
                            } else {
                                // Button is disabled (grayed out)
                                resubmitButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #ecf0f1; -fx-font-weight: normal; -fx-padding: 5 10;");
                                resubmitButton.setDisable(true);
                                
                                // Set tooltip explaining why button is disabled
                                String tooltipText = getDisableReason(request);
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
    /**
     * Check if resubmission is allowed for this request
     * Conditions: REJECTED, canReapply = false, and expired
     */
    private boolean isResubmissionAllowed(ClearanceRequest request) {
        if (request == null) return false;
        
        debugResubmissionConditions(request);
        
        // Button should be ACTIVE for ANY rejected or expired request
        return "REJECTED".equals(request.getStatus()) || 
               "EXPIRED".equals(request.getStatus());
    }

    /**
     * Get the reason why resubmission is not allowed
     */
    private String getDisableReason(ClearanceRequest request) {
        if (request == null) return "No request data";
        
        // Only disabled for non-rejected/non-expired requests
        if (!"REJECTED".equals(request.getStatus()) && !"EXPIRED".equals(request.getStatus())) {
            return "Only rejected or expired requests can be resubmitted\nCurrent status: " + request.getStatus();
        }
        
        return "Click to allow resubmission";
    }
    
    
    
    
    
    
    private void debugResubmissionConditions(ClearanceRequest request) {
        System.out.println("=== DEBUG RESUBMISSION CONDITIONS ===");
        System.out.println("Student: " + request.getFullName() + " (" + request.getStudentId() + ")");
        System.out.println("Status: " + request.getStatus());
        System.out.println("Is REJECTED: " + "REJECTED".equals(request.getStatus()));
        System.out.println("Can Reapply: " + request.isCanReapply());
        System.out.println("Days Since Request: " + request.getDaysSinceRequest());
        System.out.println("Is Expired: " + request.isExpired());
        System.out.println("Days Until Expiration: " + request.getDaysUntilExpiration());
        System.out.println("Formatted Date: " + request.getFormattedDate());
        System.out.println("Raw Date: " + request.getRequestDate());
        
        boolean condition1 = "REJECTED".equals(request.getStatus());
        boolean condition2 = !request.isCanReapply();
        boolean condition3 = request.isExpired();
        
        System.out.println("Condition 1 (REJECTED): " + condition1);
        System.out.println("Condition 2 (!canReapply): " + condition2);
        System.out.println("Condition 3 (expired): " + condition3);
        System.out.println("ALL CONDITIONS MET: " + (condition1 && condition2 && condition3));
        System.out.println("=====================================");
    }
    
    
    
    
    @FXML
    private void handleDeleteUser() {
        User selectedUser = null;
        
        // Determine which table is currently active
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabText = selectedTab.getText();
            if (tabText.contains("All Users")) {
                selectedUser = tableAllUsers.getSelectionModel().getSelectedItem();
            } else if (tabText.contains("Officers")) {
                selectedUser = tableOfficers.getSelectionModel().getSelectedItem();
            } else if (tabText.contains("Students")) {
                // Check which student tab is active
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
            showAlert("Error", "Please select a user to delete!");
            return;
        }
        
        handleDeleteUser(selectedUser);
    }
    
    private void handleDeleteUser(User user) {
        // Prevent deleting admin user
        if ("admin".equals(user.getUsername())) {
            showAlert("Error", "Cannot delete the admin user!");
            return;
        }
        
        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Are you sure you want to permanently delete this user?");
        confirm.setContentText("User: " + user.getFullName() + 
                             "\nUsername: " + user.getUsername() + 
                             "\nRole: " + user.getRole() +
                             "\n\n⚠️ This action cannot be undone!");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUserFromDatabase(user);
        }
    }
    
    private void deleteUserFromDatabase(User user) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // First, archive the user data for audit purposes
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
                archiveStmt.setInt(1, currentUser.getId());
                archiveStmt.setInt(2, user.getId());
                archiveStmt.executeUpdate();
                
                // Delete dependent records first (due to foreign key constraints)
                String[] deleteQueries = {
                    "DELETE FROM notifications WHERE user_id = ?",
                    "DELETE FROM audit_logs WHERE user_id = ?",
                    "DELETE FROM clearance_approvals WHERE officer_id = ? OR request_id IN (SELECT id FROM clearance_requests WHERE student_id = ?)",
                    "DELETE FROM clearance_requests WHERE student_id = ?",
                    "DELETE FROM student_dormitory_credentials WHERE student_id = ?",
                    "DELETE FROM student_academic_records WHERE student_id = ?",
                    "DELETE FROM book_borrowings WHERE student_id = ?",
                    "DELETE FROM cafeteria_records WHERE student_id = ?",
                    "DELETE FROM dormitory_records WHERE student_id = ?",
                    "DELETE FROM department_requirements WHERE student_id = ?",
                    "DELETE FROM student_courses WHERE student_id = ?"
                };
                
                for (String query : deleteQueries) {
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setInt(1, user.getId());
                    if (query.contains("clearance_approvals")) {
                        stmt.setInt(1, user.getId());
                        stmt.setInt(2, user.getId());
                        stmt.executeUpdate();
                    } else {
                        stmt.executeUpdate();
                    }
                }
                
                // Finally, delete the user
                String deleteUserSql = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteUserSql);
                deleteStmt.setInt(1, user.getId());
                int rowsAffected = deleteStmt.executeUpdate();
                
                // Add audit log
                String auditSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'USER_DELETION', ?, NOW())
                    """;
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                auditStmt.setInt(1, currentUser.getId());
                auditStmt.setString(2, "Deleted user: " + user.getUsername() + " - " + user.getFullName() + 
                                    " (Role: " + user.getRole() + ")");
                auditStmt.executeUpdate();
                
                conn.commit();
                
                if (rowsAffected > 0) {
                    showNotification("Success", "✅ User deleted successfully!", "success");
                    
                    // Refresh all tables
                    loadAllData();
                    
                    // Update report statistics
                    updateReportStatistics();
                } else {
                    conn.rollback();
                    showAlert("Error", "Failed to delete user!");
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to delete user: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAllowResubmission(ClearanceRequest request) {
        System.out.println("=== HANDLE ALLOW RESUBMISSION START ===");
        System.out.println("Processing request for student: " + request.getStudentId());
        System.out.println("Current status: " + request.getStatus());
        
        // Check if request is eligible (either REJECTED or EXPIRED)
        if (!"REJECTED".equals(request.getStatus()) && !"EXPIRED".equals(request.getStatus())) {
            showAlert("Cannot Allow Resubmission", 
                "Only rejected or expired requests can be resubmitted.\n" +
                "Current status: " + request.getStatus());
            return;
        }
        
        // Show confirmation dialog with all details
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Allow Resubmission");
        confirm.setHeaderText("Allow Student to Resubmit Clearance Request");
        
        StringBuilder confirmationText = new StringBuilder();
        confirmationText.append("Student Information:\n");
        confirmationText.append("• Name: ").append(request.getFullName()).append("\n");
        confirmationText.append("• Student ID: ").append(request.getStudentId()).append("\n");
        confirmationText.append("• Department: ").append(request.getDepartment()).append("\n");
        confirmationText.append("• Current Status: ").append(request.getStatus()).append("\n");
        
        if ("REJECTED".equals(request.getStatus())) {
            confirmationText.append("• Request was REJECTED\n");
        } else if ("EXPIRED".equals(request.getStatus())) {
            confirmationText.append("• Request has EXPIRED (over 30 days old or timed out)\n");
        }
        
        confirmationText.append("• Request Date: ").append(request.getFormattedDate()).append("\n");
        confirmationText.append("• Days Since Request: ").append(request.getDaysSinceRequest()).append("\n\n");
        
        confirmationText.append("This action will:\n");
        confirmationText.append("✅ Reset clearance status to PENDING\n");
        confirmationText.append("✅ Clear all existing department approvals\n");
        confirmationText.append("✅ Create new pending approvals for all departments\n");
        confirmationText.append("✅ Update dormitory clearance status to PENDING\n");
        confirmationText.append("✅ Send notification to student\n");
        confirmationText.append("✅ Log this action in audit trail\n\n");
        
        confirmationText.append("⚠️  Important Notes:\n");
        confirmationText.append("• Student will be able to submit a NEW clearance request\n");
        confirmationText.append("• All departments will need to re-approve\n");
        confirmationText.append("• This action cannot be undone\n\n");
        
        confirmationText.append("Proceed with allowing resubmission?");
        
        confirm.setContentText(confirmationText.toString());
        
        // Make the confirmation dialog larger
        confirm.getDialogPane().setPrefSize(700, 500);
        
        // Add custom styling
        confirm.getDialogPane().setStyle("-fx-border-color: #f39c12; -fx-border-width: 2px;");
        
        // Customize buttons
        ButtonType proceedButton = new ButtonType("Proceed", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(proceedButton, cancelButton);
        
        // Get the proceed button and add warning style
        Button proceedBtn = (Button) confirm.getDialogPane().lookupButton(proceedButton);
        proceedBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == proceedButton) {
            System.out.println("User confirmed. Proceeding with resubmission...");
            
            // Show processing alert
            Alert processingAlert = new Alert(Alert.AlertType.INFORMATION);
            processingAlert.setTitle("Processing");
            processingAlert.setHeaderText("Processing Resubmission Request");
            processingAlert.setContentText("Please wait while we process the resubmission...");
            processingAlert.show();
            
            try {
                // Perform the resubmission
                boolean success = performResubmissionProcess(request);
                
                // Close processing alert
                processingAlert.close();
                
                if (success) {
                    System.out.println("Resubmission successful!");
                    
                    // Show success message
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("✅ Resubmission Allowed Successfully!");
                    
                    StringBuilder successMessage = new StringBuilder();
                    successMessage.append("Student: ").append(request.getFullName()).append("\n");
                    successMessage.append("Student ID: ").append(request.getStudentId()).append("\n");
                    successMessage.append("Department: ").append(request.getDepartment()).append("\n\n");
                    
                    successMessage.append("✅ Status has been reset to PENDING\n");
                    successMessage.append("✅ Student can now submit a new clearance request\n");
                    successMessage.append("✅ All department approvals have been reset\n");
                    successMessage.append("✅ Student has been notified\n");
                    successMessage.append("✅ Action has been logged for auditing\n\n");
                    
                    successMessage.append("Next Steps:\n");
                    successMessage.append("1. Student should login and submit a new clearance request\n");
                    successMessage.append("2. All departments will need to re-approve\n");
                    successMessage.append("3. Student will receive email notification\n");
                    
                    successAlert.setContentText(successMessage.toString());
                    successAlert.getDialogPane().setPrefSize(600, 400);
                    successAlert.getDialogPane().setStyle("-fx-border-color: #27ae60; -fx-border-width: 2px;");
                    
                    successAlert.showAndWait();
                    
                    // Refresh all data
                    refreshAllDataAfterResubmission();
                    
                } else {
                    System.out.println("Resubmission failed!");
                    
                    // Show failure message
                    Alert failureAlert = new Alert(Alert.AlertType.ERROR);
                    failureAlert.setTitle("Error");
                    failureAlert.setHeaderText("❌ Failed to Allow Resubmission");
                    failureAlert.setContentText("An error occurred while processing the resubmission.\n" +
                                              "Possible reasons:\n" +
                                              "• No rejected or expired request found in database\n" +
                                              "• Database connection issue\n" +
                                              "• System error\n\n" +
                                              "Please try again or contact system administrator.");
                    failureAlert.showAndWait();
                }
                
            } catch (Exception e) {
                processingAlert.close();
                System.err.println("Error during resubmission: " + e.getMessage());
                e.printStackTrace();
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("System Error");
                errorAlert.setHeaderText("❌ System Error Occurred");
                errorAlert.setContentText("An unexpected error occurred:\n" + e.getMessage() + 
                                        "\n\nPlease contact system administrator.");
                errorAlert.showAndWait();
            }
            
        } else {
            System.out.println("User cancelled the resubmission.");
        }
        
        System.out.println("=== HANDLE ALLOW RESUBMISSION END ===");
    }

    /**
     * Helper method to debug request details
     */
    private void debugRequestDetails(ClearanceRequest request) {
        System.out.println("=== REQUEST DETAILS ===");
        System.out.println("Student ID: " + request.getStudentId());
        System.out.println("Full Name: " + request.getFullName());
        System.out.println("Department: " + request.getDepartment());
        System.out.println("Status: " + request.getStatus());
        System.out.println("Can Reapply: " + request.isCanReapply());
        System.out.println("Request Date: " + request.getRequestDate());
        System.out.println("Formatted Date: " + request.getFormattedDate());
        System.out.println("Days Since Request: " + request.getDaysSinceRequest());
        System.out.println("Is Expired: " + request.isExpired());
        System.out.println("Days Until Expiration: " + request.getDaysUntilExpiration());
        System.out.println("Expiration Status: " + request.getExpirationStatus());
        System.out.println("Resubmission Eligibility: " + request.getResubmissionEligibility());
        System.out.println("=========================");
    }

    /**
     * Perform the actual resubmission process
     */
    private boolean performResubmissionProcess(ClearanceRequest request) {
        System.out.println("Starting resubmission process for: " + request.getStudentId());
        System.out.println("Request status: " + request.getStatus());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 1. Get the student ID from the username
                String studentIdSql = "SELECT id FROM users WHERE username = ?";
                PreparedStatement studentStmt = conn.prepareStatement(studentIdSql);
                studentStmt.setString(1, request.getStudentId());
                ResultSet studentRs = studentStmt.executeQuery();
                
                if (!studentRs.next()) {
                    System.err.println("Student not found: " + request.getStudentId());
                    conn.rollback();
                    return false;
                }
                
                int studentId = studentRs.getInt("id");
                System.out.println("Found student ID: " + studentId);
                
                // 2. Get the latest request for this student (REJECTED or EXPIRED)
                String getRequestSql = """
                    SELECT id FROM clearance_requests 
                    WHERE student_id = ? 
                    AND (status = 'REJECTED' OR status = 'EXPIRED')
                    ORDER BY request_date DESC LIMIT 1
                    """;
                PreparedStatement requestStmt = conn.prepareStatement(getRequestSql);
                requestStmt.setInt(1, studentId);
                ResultSet requestRs = requestStmt.executeQuery();
                
                if (!requestRs.next()) {
                    System.err.println("No rejected or expired request found for student: " + studentId);
                    conn.rollback();
                    return false;
                }
                
                int requestId = requestRs.getInt("id");
                System.out.println("Found request ID: " + requestId + " with status: " + request.getStatus());
                
                // 3. Update the clearance request
                String updateRequestSql = """
                    UPDATE clearance_requests 
                    SET status = 'PENDING', 
                        can_reapply = TRUE,
                        request_date = NOW()
                    WHERE id = ?
                    """;
                PreparedStatement updateStmt = conn.prepareStatement(updateRequestSql);
                updateStmt.setInt(1, requestId);
                int rowsUpdated = updateStmt.executeUpdate();
                
                if (rowsUpdated == 0) {
                    System.err.println("Failed to update clearance request");
                    conn.rollback();
                    return false;
                }
                
                System.out.println("Updated clearance request: " + rowsUpdated + " rows affected");
                
                // 4. Clear existing approvals
                String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                PreparedStatement clearStmt = conn.prepareStatement(clearApprovalsSql);
                clearStmt.setInt(1, requestId);
                int approvalsDeleted = clearStmt.executeUpdate();
                System.out.println("Deleted " + approvalsDeleted + " existing approvals");
                
                // 5. Create new pending approvals based on workflow
                String workflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                PreparedStatement workflowStmt = conn.prepareStatement(workflowSql);
                ResultSet workflowRs = workflowStmt.executeQuery();
                
                // Check if clearance_approvals table has timestamp column
                // Try with timestamp first, if error, try without
                String insertApprovalSql;
                try {
                    // First, let's check the table structure
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet columns = meta.getColumns(null, null, "clearance_approvals", "timestamp");
                    if (columns.next()) {
                        // Table has timestamp column
                        insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, status, timestamp)
                            VALUES (?, ?, 'PENDING', NOW())
                            """;
                    } else {
                        // Table doesn't have timestamp column
                        insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, status)
                            VALUES (?, ?, 'PENDING')
                            """;
                    }
                    columns.close();
                } catch (Exception e) {
                    // Fallback to simple insert
                    insertApprovalSql = """
                        INSERT INTO clearance_approvals (request_id, officer_role, status)
                        VALUES (?, ?, 'PENDING')
                        """;
                }
                
                PreparedStatement insertStmt = conn.prepareStatement(insertApprovalSql);
                
                int approvalsAdded = 0;
                while (workflowRs.next()) {
                    try {
                        insertStmt.setInt(1, requestId);
                        insertStmt.setString(2, workflowRs.getString("role"));
                        insertStmt.addBatch();
                        approvalsAdded++;
                    } catch (Exception e) {
                        System.err.println("Error adding approval for role " + workflowRs.getString("role") + ": " + e.getMessage());
                    }
                }
                
                if (approvalsAdded > 0) {
                    try {
                        insertStmt.executeBatch();
                        System.out.println("Added " + approvalsAdded + " new pending approvals");
                    } catch (Exception e) {
                        System.err.println("Error executing batch: " + e.getMessage());
                        // Try individual inserts as fallback
                        insertStmt.clearBatch();
                        workflowRs.beforeFirst();
                        while (workflowRs.next()) {
                            try {
                                String simpleInsertSql = """
                                    INSERT INTO clearance_approvals (request_id, officer_role, status)
                                    VALUES (?, ?, 'PENDING')
                                    """;
                                PreparedStatement simpleStmt = conn.prepareStatement(simpleInsertSql);
                                simpleStmt.setInt(1, requestId);
                                simpleStmt.setString(2, workflowRs.getString("role"));
                                simpleStmt.executeUpdate();
                                approvalsAdded++;
                            } catch (Exception ex) {
                                System.err.println("Failed individual insert: " + ex.getMessage());
                            }
                        }
                    }
                }
                
                // 6. Update dormitory status if exists
                try {
                    String updateDormSql = """
                        UPDATE student_dormitory_credentials 
                        SET clearance_status = 'PENDING'
                        WHERE student_id = ?
                        """;
                    PreparedStatement dormStmt = conn.prepareStatement(updateDormSql);
                    dormStmt.setInt(1, studentId);
                    int dormUpdated = dormStmt.executeUpdate();
                    System.out.println("Updated dormitory status: " + dormUpdated + " rows affected");
                } catch (Exception e) {
                    System.out.println("Note: Could not update dormitory status (table might not exist or student not in dormitory): " + e.getMessage());
                    // This is not a critical error, continue processing
                }
                
                // 7. Add audit log
                try {
                    String auditSql = """
                        INSERT INTO audit_logs (user_id, action, details, timestamp)
                        VALUES (?, 'ALLOW_RESUBMISSION', ?, NOW())
                        """;
                    PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                    auditStmt.setInt(1, currentUser.getId());
                    auditStmt.setString(2, "Allowed resubmission for student: " + 
                                       request.getStudentId() + " - " + request.getFullName() +
                                       " (Request ID: " + requestId + ", Previous status: " + request.getStatus() + ")");
                    auditStmt.executeUpdate();
                    System.out.println("Added audit log entry");
                } catch (Exception e) {
                    System.err.println("Could not add audit log: " + e.getMessage());
                    // Continue even if audit log fails
                }
                
                // 8. Send notification to student (if notifications table exists)
                try {
                    String notificationSql = """
                        INSERT INTO notifications (user_id, type, subject, message, is_read, created_at)
                        VALUES (?, 'RESUBMISSION_ALLOWED', 'Clearance Resubmission Allowed', 
                                'Your clearance request has been reset to PENDING. You can now submit a new clearance request.', 
                                FALSE, NOW())
                        """;
                    PreparedStatement notifStmt = conn.prepareStatement(notificationSql);
                    notifStmt.setInt(1, studentId);
                    notifStmt.executeUpdate();
                    System.out.println("Added notification for student");
                } catch (Exception e) {
                    System.out.println("Note: Could not add notification (table might not exist): " + e.getMessage());
                    // Not critical, continue
                }
                
                // Commit all changes
                conn.commit();
                System.out.println("Transaction committed successfully");
                
                // Update the request object in memory
                request.setCanReapply(true);
                request.setStatus("PENDING");
                
                return true;
                
            } catch (Exception e) {
                System.err.println("Error during resubmission process: " + e.getMessage());
                e.printStackTrace();
                try {
                    conn.rollback();
                    System.err.println("Transaction rolled back");
                } catch (SQLException ex) {
                    System.err.println("Error rolling back: " + ex.getMessage());
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error setting auto-commit: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Database error in performResubmissionProcess: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Refresh all data after successful resubmission
     */
    private void refreshAllDataAfterResubmission() {
        System.out.println("Refreshing all data after resubmission...");
        
        // Refresh clearance requests table
        loadClearanceRequests();
        
        // Refresh all student tables
        loadAllStudents();
        
        // Refresh dashboard statistics
        updateDashboardStats();
        
        // Refresh report statistics
        updateReportStatistics();
        
        // Refresh the specific table view
        tableRequests.refresh();
        
        System.out.println("All data refreshed successfully");
    }


    
    private void updateReportStatistics() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get today's deleted users count
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
            
            // Get resubmissions allowed count
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
            
            // Get pending resubmissions (expired rejected requests)
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
            
            // Get expired requests
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
        lblWelcome.setText("Welcome, " + user.getFullName() + " (Admin)");
        
        Platform.runLater(() -> {
            loadAllData();
            updateReportStatistics();
            System.out.println("[DEBUG] Data loaded for user: " + user.getUsername());
            
            if (tableAllStudents != null) {
                tableAllStudents.refresh();
                System.out.println("[DEBUG] Table refreshed");
            }
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
        
    private void setupAllTables() {
        setupStudentTable(tableAllStudents, colStudentId, colStudentName, colStudentDepartment, 
                         colStudentYear, colClearanceStatus, colStudentActions);
        
        setupSimpleStudentTable(tableApprovedStudents);
        setupSimpleStudentTable(tableRejectedStudents);
        setupSimpleStudentTable(tablePendingStudents);
        setupSimpleStudentTable(tableInProgressStudents);
        
        colOfficerId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colOfficerName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colOfficerRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colOfficerDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colOfficerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colAllUserId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAllUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAllUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAllUserDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colAllUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
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
    
    private void setupSearchFunctionality() {
        cmbSearchType.setItems(FXCollections.observableArrayList(
            "All Users",
            "Students Only", 
            "Officers Only"
        ));
        cmbSearchType.setValue("All Users");
        
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
    
    @FXML
    private void handleUserSearch() {
        String searchQuery = txtSearchUsers.getText().trim();
        String searchType = cmbSearchType.getValue();
        
        if (searchQuery.isEmpty()) {
            updateSearchStatus("Please enter a search term", "warning");
            return;
        }
        
        performSearch(searchQuery, searchType);
    }
    
    @FXML
    private void handleClearSearch() {
        txtSearchUsers.clear();
        cmbSearchType.setValue("All Users");
        loadAllUsers();
        
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
                searchRequests(query);
            }
        }
    }
    
    @FXML
    private void handleClearRequestsSearch() {
        if (txtSearchRequests != null) {
            txtSearchRequests.clear();
            loadClearanceRequests();
        }
    }
    
    private void performSearch(String searchQuery, String searchType) {
        allUsersData.clear();
        
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
                    for (int i = 1; i <= 5; i++) {
                        ps.setString(i, pattern);
                    }
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
                    for (int i = 1; i <= 4; i++) {
                        ps.setString(i, pattern);
                    }
                    break;
                    
                default:
                    sql = """
                        SELECT * FROM users 
                        WHERE (username LIKE ? OR full_name LIKE ? OR department LIKE ? OR email LIKE ?)
                        ORDER BY role, username
                        """;
                    ps = conn.prepareStatement(sql);
                    pattern = "%" + searchQuery + "%";
                    for (int i = 1; i <= 4; i++) {
                        ps.setString(i, pattern);
                    }
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
                
                allUsersData.add(user);
            }
            
            tableAllUsers.setItems(allUsersData);
            
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
            e.printStackTrace();
        }
    }
    
    private void searchRequests(String searchQuery) {
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
            
            tableRequests.setItems(requestData);
            
            showAlert("Search Results", "Found " + count + " clearance requests matching: '" + searchQuery + "'");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to search requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateSearchStatus(String message, String type) {
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
    
    private void setupStudentTable(TableView<User> tableView, TableColumn<User, String> colId, 
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
    
    private void setupSimpleStudentTable(TableView<User> tableView) {
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
        
        if (tableView.getColumns().size() >= 5) {
            TableColumn<User, String> actionsCol = (TableColumn<User, String>) tableView.getColumns().get(4);
            actionsCol.setCellFactory(column -> new TableCell<User, String>() {
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
    }
    
    @FXML
    private void handleRefresh() {
        loadAllData();
        updateReportStatistics();
        showNotification("Refreshed", "All data has been refreshed successfully!", "info");
    }
    
    private void loadAllData() {
        System.out.println("[DEBUG] loadAllData() called");
        
        loadAllStudents();
        loadOfficers();
        loadAllUsers();
        loadClearanceRequests();
        updateDashboardStats();
        
        System.out.println("[DEBUG] All data loaded");
        System.out.println("[DEBUG] Total students loaded: " + allStudentsData.size());
        
        if (tableAllStudents != null) {
            System.out.println("[DEBUG] tableAllStudents items: " + tableAllStudents.getItems().size());
        } else {
            System.out.println("[ERROR] tableAllStudents is null!");
        }
    }
    
    private void loadAllStudents() {
        System.out.println("[DEBUG] Starting loadAllStudents()");
        
        allStudentsData.clear();
        approvedStudentsData.clear();
        rejectedStudentsData.clear();
        pendingStudentsData.clear();
        inProgressStudentsData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("[DEBUG] Database connection established");
            
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
                
            System.out.println("[DEBUG] SQL query: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("[DEBUG] Found student #" + count + ": " + rs.getString("username"));
                
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
                    System.out.println("[DEBUG] Added to approved: " + student.getUsername());
                } else if (clearanceStatus.equals("REJECTED") && !canReapply) {
                    rejectedStudentsData.add(student);
                    System.out.println("[DEBUG] Added to rejected: " + student.getUsername());
                } else if (clearanceStatus.equals("PENDING")) {
                    pendingStudentsData.add(student);
                    System.out.println("[DEBUG] Added to pending: " + student.getUsername());
                } else if (clearanceStatus.equals("IN_PROGRESS")) {
                    inProgressStudentsData.add(student);
                    System.out.println("[DEBUG] Added to in progress: " + student.getUsername());
                }
            }
            
            System.out.println("[DEBUG] Total students loaded: " + count);
            System.out.println("[DEBUG] allStudentsData size: " + allStudentsData.size());
            System.out.println("[DEBUG] approvedStudentsData size: " + approvedStudentsData.size());
            System.out.println("[DEBUG] rejectedStudentsData size: " + rejectedStudentsData.size());
            System.out.println("[DEBUG] pendingStudentsData size: " + pendingStudentsData.size());
            System.out.println("[DEBUG] inProgressStudentsData size: " + inProgressStudentsData.size());
            
            System.out.println("[DEBUG] tableAllStudents is null: " + (tableAllStudents == null));
            System.out.println("[DEBUG] tableApprovedStudents is null: " + (tableApprovedStudents == null));
            
            tableAllStudents.setItems(allStudentsData);
            tableApprovedStudents.setItems(approvedStudentsData);
            tableRejectedStudents.setItems(rejectedStudentsData);
            tablePendingStudents.setItems(pendingStudentsData);
            tableInProgressStudents.setItems(inProgressStudentsData);
            
            System.out.println("[DEBUG] Data set to tables successfully");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load students: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load students: " + e.getMessage());
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
        System.out.println("=== DEBUG: Loading clearance requests ===");
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

            int count = 0;
            int rejectedCount = 0;
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
                req.setCanReapply(rs.getBoolean("can_reapply"));
                
                // Debug info
                if ("REJECTED".equals(req.getStatus())) {
                    rejectedCount++;
                    System.out.println("DEBUG: Rejected request found - Student: " + req.getStudentId() + 
                                     ", Date: " + req.getRequestDate() +
                                     ", Can reapply: " + req.isCanReapply() +
                                     ", Is expired: " + req.isExpired());
                }
                
                requestData.add(req);
            }

            tableRequests.setItems(requestData);
            System.out.println("DEBUG: Loaded " + count + " clearance requests");
            System.out.println("DEBUG: " + rejectedCount + " rejected requests found");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to load requests: " + e.getMessage());
            e.printStackTrace();
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
    
    private void updateDashboardStats() {
        lblTotalStudents.setText("Students: " + allStudentsData.size());
        lblTotalOfficers.setText("Officers: " + officersData.size());
        
        int totalRequests = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;
        
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
        
        lblTotalRequests.setText("Requests: " + totalRequests);
        
        if (lblTotalStudentsCard != null) {
            lblTotalStudentsCard.setText(String.valueOf(allStudentsData.size()));
        }
        if (lblTotalOfficersCard != null) {
            lblTotalOfficersCard.setText(String.valueOf(officersData.size()));
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
        
        // Update status bar
        lblUpdateTime.setText(LocalDateTime.now().toString().substring(11, 19));
    }
    
    // ==================== STUDENT REGISTRATION ====================
    
    @FXML
    private void openRegisterStudent() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Student");
        dialog.setHeaderText("Enter Student Information");
        dialog.getDialogPane().setPrefSize(850, 800);
        
        ButtonType registerButton = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButton, ButtonType.CANCEL);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        GridPane grid = createStudentFormWithValidation();
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(content);
        
        Button btnRegister = (Button) dialog.getDialogPane().lookupButton(registerButton);
        btnRegister.setDisable(true);
        
        SimpleBooleanProperty allFieldsValid = new SimpleBooleanProperty(false);
        setupValidationListeners(grid, allFieldsValid);
        btnRegister.disableProperty().bind(allFieldsValid.not());
        
        btnRegister.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateAllFields(grid)) {
                event.consume();
            } else {
                if (!registerStudentFromForm(grid)) {
                    event.consume();
                }
            }
        });
        
        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == registerButton) {
            loadAllUsers();
            loadAllStudents();
        }
    }
    
    private GridPane createStudentFormWithValidation() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        int row = 0;
        
        TextField txtStudentId = new TextField();
        txtStudentId.setPromptText("DBU1601111");
        txtStudentId.setPrefWidth(200);
        
        Label lblStudentIdHint = new Label("Format: DBU + 7 digits (e.g., DBU1601111)");
        lblStudentIdHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblStudentIdValidation = new Label("Required");
        lblStudentIdValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("dbu1601111");
        txtUsername.setEditable(false);
        txtUsername.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #000000;");
        
        txtStudentId.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (!newVal.startsWith("DBU")) {
                    Platform.runLater(() -> {
                        txtStudentId.setText("DBU" + newVal.replaceAll("[^\\d]", ""));
                        txtStudentId.positionCaret(txtStudentId.getText().length());
                    });
                }
                
                if (newVal.startsWith("DBU") && newVal.length() > 10) {
                    Platform.runLater(() -> {
                        txtStudentId.setText(newVal.substring(0, 10));
                        txtStudentId.positionCaret(txtStudentId.getText().length());
                    });
                }
                
                if (newVal.startsWith("DBU") && newVal.length() > 3) {
                    String afterDBU = newVal.substring(3);
                    if (!afterDBU.matches("\\d*")) {
                        Platform.runLater(() -> {
                            txtStudentId.setText("DBU" + afterDBU.replaceAll("[^\\d]", ""));
                            txtStudentId.positionCaret(txtStudentId.getText().length());
                        });
                    }
                }
                
                ValidationResult result = ValidationHelper.validateStudentId(newVal);
                updateValidationLabel(lblStudentIdValidation, result);
                
                String username = ValidationHelper.generateUsername(newVal);
                txtUsername.setText(username);
            }
        });
        
        grid.add(new Label("Student ID*:"), 0, row);
        grid.add(txtStudentId, 1, row);
        grid.add(lblStudentIdHint, 2, row);
        row++;
        grid.add(lblStudentIdValidation, 1, row);
        GridPane.setColumnSpan(lblStudentIdValidation, 2);
        row++;
        
        grid.add(new Label("Username*:"), 0, row);
        grid.add(txtUsername, 1, row);
        grid.add(new Label("(Auto-generated from Student ID)"), 2, row);
        row++;
        row++;
        
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("John Smith");
        
        Label lblFullNameHint = new Label("First and last name, letters only");
        lblFullNameHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblFullNameValidation = new Label("Required");
        lblFullNameValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtFullName.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validateFullName(newVal);
            updateValidationLabel(lblFullNameValidation, result);
        });
        
        grid.add(new Label("Full Name*:"), 0, row);
        grid.add(txtFullName, 1, row);
        grid.add(lblFullNameHint, 2, row);
        row++;
        grid.add(lblFullNameValidation, 1, row);
        GridPane.setColumnSpan(lblFullNameValidation, 2);
        row++;
        
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("At least 6 chars, letters & numbers");
        
        Label lblPasswordHint = new Label("Min 6 chars, must include letters & numbers");
        lblPasswordHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblPasswordValidation = new Label("Required");
        lblPasswordValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validatePassword(newVal);
            updateValidationLabel(lblPasswordValidation, result);
        });
        
        grid.add(new Label("Password*:"), 0, row);
        grid.add(txtPassword, 1, row);
        grid.add(lblPasswordHint, 2, row);
        row++;
        grid.add(lblPasswordValidation, 1, row);
        GridPane.setColumnSpan(lblPasswordValidation, 2);
        row++;
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("student@dbu.edu.et");
        
        Label lblEmailHint = new Label("Required - must be valid email");
        lblEmailHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblEmailValidation = new Label("Required");
        lblEmailValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validateEmail(newVal);
            updateValidationLabel(lblEmailValidation, result);
        });
        
        grid.add(new Label("Email*:"), 0, row);
        grid.add(txtEmail, 1, row);
        grid.add(lblEmailHint, 2, row);
        row++;
        grid.add(lblEmailValidation, 1, row);
        GridPane.setColumnSpan(lblEmailValidation, 2);
        row++;
        
        PhoneInputField phoneInputField = new PhoneInputField();
        
        Label lblPhoneHint = new Label("Format: 09xxxxxxx (Provider A) or 07xxxxxxx (Provider B)");
        lblPhoneHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblPhoneValidation = new Label("Required");
        lblPhoneValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        phoneInputField.getPhoneField().textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validatePhoneWithProvider(newVal, phoneInputField.getProvider());
            updateValidationLabel(lblPhoneValidation, result);
        });
        
        phoneInputField.getProviderComboBox().valueProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validatePhoneWithProvider(
                phoneInputField.getPhoneNumber(), 
                newVal
            );
            updateValidationLabel(lblPhoneValidation, result);
        });
        
        grid.add(new Label("Phone*:"), 0, row);
        grid.add(phoneInputField, 1, row);
        grid.add(lblPhoneHint, 2, row);
        row++;
        grid.add(lblPhoneValidation, 1, row);
        GridPane.setColumnSpan(lblPhoneValidation, 2);
        row++;
        
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.setItems(FXCollections.observableArrayList(
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration",
            "Accounting", "Economics", "Mathematics", "Food Engineering", 
            "Chemistry", "Biology"
        ));
        cmbDepartment.setPromptText("Select Department");
        
        Label lblDeptValidation = new Label("Required");
        lblDeptValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        cmbDepartment.valueProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validateDepartment(newVal);
            updateValidationLabel(lblDeptValidation, result);
        });
        
        grid.add(new Label("Department*:"), 0, row);
        grid.add(cmbDepartment, 1, row);
        row++;
        grid.add(lblDeptValidation, 1, row);
        GridPane.setColumnSpan(lblDeptValidation, 2);
        row++;
        
        ComboBox<String> cmbYear = new ComboBox<>();
        cmbYear.setItems(FXCollections.observableArrayList(
            "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"
        ));
        cmbYear.setPromptText("Select Year");
        
        Label lblYearValidation = new Label("Required");
        lblYearValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        cmbYear.valueProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validateYearLevel(newVal);
            updateValidationLabel(lblYearValidation, result);
        });
        
        grid.add(new Label("Year Level*:"), 0, row);
        grid.add(cmbYear, 1, row);
        row++;
        grid.add(lblYearValidation, 1, row);
        GridPane.setColumnSpan(lblYearValidation, 2);
        row++;
        
        TextField txtBlockNumber = new TextField();
        txtBlockNumber.setPromptText("1-45");

        Label lblBlockHint = new Label("Block number 1-45 (optional)");
        lblBlockHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        Label lblBlockValidation = new Label("Optional");
        lblBlockValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        txtBlockNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtBlockNumber.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtBlockNumber.getText().length() > 2) {
                txtBlockNumber.setText(txtBlockNumber.getText().substring(0, 2));
            }
            
            ValidationResult result = ValidationHelper.validateBlockNumber(newVal);
            updateValidationLabel(lblBlockValidation, result);
        });

        grid.add(new Label("Block Number:"), 0, row);
        grid.add(txtBlockNumber, 1, row);
        grid.add(lblBlockHint, 2, row);
        row++;
        grid.add(lblBlockValidation, 1, row);
        GridPane.setColumnSpan(lblBlockValidation, 2);
        row++;
        
        TextField txtDormNumber = new TextField();
        txtDormNumber.setPromptText("101-4999");
        
        Label lblDormHint = new Label("Room number 100-4999 (optional)");
        lblDormHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblDormValidation = new Label("Optional");
        lblDormValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        txtDormNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtDormNumber.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtDormNumber.getText().length() > 4) {
                txtDormNumber.setText(txtDormNumber.getText().substring(0, 4));
            }
            
            ValidationResult result = ValidationHelper.validateDormNumber(newVal);
            updateValidationLabel(lblDormValidation, result);
        });
        
        grid.add(new Label("Dorm/Room Number:"), 0, row);
        grid.add(txtDormNumber, 1, row);
        grid.add(lblDormHint, 2, row);
        row++;
        grid.add(lblDormValidation, 1, row);
        GridPane.setColumnSpan(lblDormValidation, 2);
        
        grid.setUserData(new Object[]{
            txtStudentId, txtUsername, txtFullName, txtPassword,
            txtEmail, phoneInputField, cmbDepartment, cmbYear,
            txtBlockNumber, txtDormNumber,
            lblStudentIdValidation, lblFullNameValidation, lblPasswordValidation,
            lblEmailValidation, lblPhoneValidation, lblDeptValidation,
            lblYearValidation, lblBlockValidation, lblDormValidation
        });
        
        return grid;
    }
    
    private void setupValidationListeners(GridPane grid, SimpleBooleanProperty allFieldsValid) {
        Object[] components = (Object[]) grid.getUserData();
        
        Label lblStudentIdValidation = (Label) components[10];
        Label lblFullNameValidation = (Label) components[11];
        Label lblPasswordValidation = (Label) components[12];
        Label lblEmailValidation = (Label) components[13];
        Label lblPhoneValidation = (Label) components[14];
        Label lblDeptValidation = (Label) components[15];
        Label lblYearValidation = (Label) components[16];
        
        Runnable validationChecker = () -> {
            boolean studentIdValid = lblStudentIdValidation.getText().contains("✓");
            boolean nameValid = lblFullNameValidation.getText().contains("✓");
            boolean passwordValid = lblPasswordValidation.getText().contains("✓");
            boolean emailValid = lblEmailValidation.getText().contains("✓");
            boolean phoneValid = lblPhoneValidation.getText().contains("✓");
            boolean deptValid = lblDeptValidation.getText().contains("✓");
            boolean yearValid = lblYearValidation.getText().contains("✓");
            
            boolean allValid = studentIdValid && nameValid && passwordValid && 
                              emailValid && phoneValid && deptValid && yearValid;
            
            System.out.println("=== VALIDATION STATUS ===");
            System.out.println("Student ID valid: " + studentIdValid + " (" + lblStudentIdValidation.getText() + ")");
            System.out.println("Full Name valid: " + nameValid + " (" + lblFullNameValidation.getText() + ")");
            System.out.println("Password valid: " + passwordValid + " (" + lblPasswordValidation.getText() + ")");
            System.out.println("Email valid: " + emailValid + " (" + lblEmailValidation.getText() + ")");
            System.out.println("Phone valid: " + phoneValid + " (" + lblPhoneValidation.getText() + ")");
            System.out.println("Department valid: " + deptValid + " (" + lblDeptValidation.getText() + ")");
            System.out.println("Year valid: " + yearValid + " (" + lblYearValidation.getText() + ")");
            System.out.println("All fields valid: " + allValid);
            System.out.println("=========================");
            
            allFieldsValid.set(allValid);
        };
        
        lblStudentIdValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblFullNameValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblPasswordValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblEmailValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblPhoneValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblDeptValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblYearValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        
        validationChecker.run();
    }
    
    private void updateValidationLabel(Label label, ValidationResult result) {
        if (result.isValid()) {
            label.setText("✓ " + result.getMessage().replace("✓", "").trim());
            label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            label.setText(result.getMessage());
            label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }
    
    private boolean validateAllFields(GridPane grid) {
        Object[] components = (Object[]) grid.getUserData();
        
        Label lblStudentIdValidation = (Label) components[10];
        Label lblFullNameValidation = (Label) components[11];
        Label lblPasswordValidation = (Label) components[12];
        Label lblEmailValidation = (Label) components[13];
        Label lblPhoneValidation = (Label) components[14];
        Label lblDeptValidation = (Label) components[15];
        Label lblYearValidation = (Label) components[16];
        
        boolean studentIdValid = lblStudentIdValidation.getText().contains("✓");
        boolean nameValid = lblFullNameValidation.getText().contains("✓");
        boolean passwordValid = lblPasswordValidation.getText().contains("✓");
        boolean emailValid = lblEmailValidation.getText().contains("✓");
        boolean phoneValid = lblPhoneValidation.getText().contains("✓");
        boolean deptValid = lblDeptValidation.getText().contains("✓");
        boolean yearValid = lblYearValidation.getText().contains("✓");
        
        boolean allValid = studentIdValid && nameValid && passwordValid && 
                          emailValid && phoneValid && deptValid && yearValid;
        
        if (!allValid) {
            StringBuilder errorMsg = new StringBuilder("Please fix the following fields:\n");
            
            if (!studentIdValid) errorMsg.append("• Student ID: ").append(lblStudentIdValidation.getText()).append("\n");
            if (!nameValid) errorMsg.append("• Full Name: ").append(lblFullNameValidation.getText()).append("\n");
            if (!passwordValid) errorMsg.append("• Password: ").append(lblPasswordValidation.getText()).append("\n");
            if (!emailValid) errorMsg.append("• Email: ").append(lblEmailValidation.getText()).append("\n");
            if (!phoneValid) errorMsg.append("• Phone: ").append(lblPhoneValidation.getText()).append("\n");
            if (!deptValid) errorMsg.append("• Department: ").append(lblDeptValidation.getText()).append("\n");
            if (!yearValid) errorMsg.append("• Year Level: ").append(lblYearValidation.getText()).append("\n");
            
            showAlert("Validation Error", errorMsg.toString());
        }
        
        return allValid;
    }
    
    private boolean registerStudentFromForm(GridPane grid) {
        if (!validateAllFields(grid)) {
            return false;
        }
        
        Object[] components = (Object[]) grid.getUserData();
        
        TextField txtStudentId = (TextField) components[0];
        TextField txtUsername = (TextField) components[1];
        TextField txtFullName = (TextField) components[2];
        PasswordField txtPassword = (PasswordField) components[3];
        TextField txtEmail = (TextField) components[4];
        PhoneInputField phoneInputField = (PhoneInputField) components[5];
        ComboBox<String> cmbDepartment = (ComboBox<String>) components[6];
        ComboBox<String> cmbYear = (ComboBox<String>) components[7];
        TextField txtBlockNumber = (TextField) components[8];
        TextField txtDormNumber = (TextField) components[9];
        
        String studentId = txtStudentId.getText().trim().toUpperCase();
        String username = txtUsername.getText().trim().toLowerCase();
        String fullName = txtFullName.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = phoneInputField.getPhoneNumber();
        String provider = phoneInputField.getProvider();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();
        String blockNumber = txtBlockNumber.getText().trim();
        String dormNumber = txtDormNumber.getText().trim();
        
        System.out.println("=== ATTEMPTING REGISTRATION ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Username: " + username);
        System.out.println("Full Name: " + fullName);
        System.out.println("Department: " + department);
        System.out.println("Year: " + year);
        System.out.println("Phone: " + phone);
        System.out.println("Provider: " + provider);
        System.out.println("Email: " + email);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String checkDuplicate = "SELECT username, full_name FROM users WHERE username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkDuplicate);
                checkStmt.setString(1, username);
                
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next()) {
                    String existingUsername = checkRs.getString("username");
                    String existingName = checkRs.getString("full_name");
                    
                    showAlert("Duplicate Student ID", 
                        "Student ID '" + existingUsername + "' already exists in the system.\n\n" +
                        "Associated with student: " + existingName + "\n\n" +
                        "The Student ID must be unique. Please use a different Student ID.");
                    conn.rollback();
                    return false;
                }
                
                String userSql = """
                    INSERT INTO users (username, password, full_name, role, email, phone, 
                                      department, year_level, status, created_at)
                    VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?, 'ACTIVE', NOW())
                """;

                PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, username);
                userStmt.setString(2, password);
                userStmt.setString(3, fullName);
                userStmt.setString(4, email.isEmpty() ? null : email);
                userStmt.setString(5, phone);
                userStmt.setString(6, department);
                userStmt.setString(7, year);

                int userRows = userStmt.executeUpdate();
                
                if (userRows <= 0) {
                    conn.rollback();
                    showAlert("Error", "Failed to register student!");
                    return false;
                }
                
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                int studentDbId = -1;
                if (generatedKeys.next()) {
                    studentDbId = generatedKeys.getInt(1);
                }
                
                if (!blockNumber.isEmpty() && !dormNumber.isEmpty()) {
                    String dormSql = """
                        INSERT INTO student_dormitory_credentials 
                        (student_id, block_number, room_number, key_returned, 
                         damage_paid, clearance_status, last_updated)
                        VALUES (?, ?, ?, FALSE, FALSE, 'PENDING', NOW())
                    """;
                    
                    PreparedStatement dormStmt = conn.prepareStatement(dormSql);
                    dormStmt.setInt(1, studentDbId);
                    dormStmt.setString(2, blockNumber);
                    dormStmt.setString(3, dormNumber);
                    dormStmt.executeUpdate();
                }
                
                String academicSql = """
                    INSERT INTO student_academic_records 
                    (student_id, academic_hold, outstanding_fees, 
                     incomplete_courses, gpa)
                    VALUES (?, 'NONE', 0.00, 0, 0.00)
                """;
                PreparedStatement academicStmt = conn.prepareStatement(academicSql);
                academicStmt.setInt(1, studentDbId);
                academicStmt.executeUpdate();
                
                String auditSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'STUDENT_REGISTRATION', ?, NOW())
                """;
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                auditStmt.setInt(1, currentUser.getId());
                auditStmt.setString(2, "Registered student: " + username + " - " + fullName);
                auditStmt.executeUpdate();
                
                conn.commit();
                
                StringBuilder successMsg = new StringBuilder();
                successMsg.append("✅ Student Registered Successfully!\n\n");
                successMsg.append("Name: ").append(fullName).append("\n");
                successMsg.append("Student ID: ").append(studentId).append("\n");
                successMsg.append("Username: ").append(username).append("\n");
                successMsg.append("Department: ").append(department).append("\n");
                successMsg.append("Year: ").append(year).append("\n");
                successMsg.append("Phone: ").append(phone).append("\n");
                successMsg.append("Provider: ").append(provider).append("\n");
                if (!email.isEmpty()) {
                    successMsg.append("Email: ").append(email).append("\n");
                }
                if (!blockNumber.isEmpty() && !dormNumber.isEmpty()) {
                    successMsg.append("Dormitory: Block ").append(blockNumber)
                              .append(", Room ").append(dormNumber).append("\n");
                }
                successMsg.append("\nStudent can now login with username: ").append(username);
                
                showAlert("Registration Successful", successMsg.toString());
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                
                if (e.getSQLState().equals("23000")) {
                    showAlert("Duplicate Student ID", 
                        "Student ID '" + username + "' already exists in the system.\n\n" +
                        "The Student ID must be unique. Please use a different Student ID.");
                } else {
                    showAlert("Error", "Registration failed: " + e.getMessage());
                }
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            showAlert("Error", "Database connection failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ==================== OFFICER REGISTRATION ====================
    
    @FXML
    private void openManageOfficers() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Officer");
        dialog.setHeaderText("Enter Officer Information");
        dialog.getDialogPane().setPrefSize(800, 700);
        
        ButtonType saveButton = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        GridPane grid = createOfficerFormWithValidation();
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(content);
        
        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveButton);
        btnSave.setDisable(true);
        
        SimpleBooleanProperty allFieldsValid = new SimpleBooleanProperty(false);
        setupOfficerValidationListeners(grid, allFieldsValid);
        btnSave.disableProperty().bind(allFieldsValid.not());
        
        btnSave.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateOfficerFields(grid)) {
                event.consume();
            } else {
                if (!registerOfficerFromForm(grid)) {
                    event.consume();
                }
            }
        });
        
        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == saveButton) {
            loadAllUsers();
            loadOfficers();
        }
    }
    
    private GridPane createOfficerFormWithValidation() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        int row = 0;
        
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("John Smith");
        
        Label lblFullNameHint = new Label("First and last name");
        lblFullNameHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblFullNameValidation = new Label("Required");
        lblFullNameValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtFullName.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validateFullName(newVal);
            updateValidationLabel(lblFullNameValidation, result);
        });
        
        grid.add(new Label("Full Name*:"), 0, row);
        grid.add(txtFullName, 1, row);
        grid.add(lblFullNameHint, 2, row);
        row++;
        grid.add(lblFullNameValidation, 1, row);
        GridPane.setColumnSpan(lblFullNameValidation, 2);
        row++;
        
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("officer_username");
        
        Label lblUsernameHint = new Label("Lowercase letters, numbers, underscores");
        lblUsernameHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblUsernameValidation = new Label("Required");
        lblUsernameValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String lowerVal = newVal.toLowerCase();
                if (!lowerVal.equals(newVal)) {
                    txtUsername.setText(lowerVal);
                }
            }
            boolean valid = newVal != null && newVal.matches("^[a-z0-9_]{3,50}$") && !newVal.equals("admin");
            if (valid) {
                lblUsernameValidation.setText("Valid ✓");
                lblUsernameValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblUsernameValidation.setText("3-50 chars, lowercase, not 'admin'");
                lblUsernameValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        grid.add(new Label("Username*:"), 0, row);
        grid.add(txtUsername, 1, row);
        grid.add(lblUsernameHint, 2, row);
        row++;
        grid.add(lblUsernameValidation, 1, row);
        GridPane.setColumnSpan(lblUsernameValidation, 2);
        row++;
        
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Min 8 chars, uppercase, lowercase, number");
        
        Label lblPasswordHint = new Label("Min 8 chars, must include uppercase, lowercase, number");
        lblPasswordHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblPasswordValidation = new Label("Required");
        lblPasswordValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal != null && newVal.length() >= 8 &&
                          newVal.matches(".*[A-Z].*") &&
                          newVal.matches(".*[a-z].*") &&
                          newVal.matches(".*\\d.*");
            if (valid) {
                lblPasswordValidation.setText("Strong ✓");
                lblPasswordValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblPasswordValidation.setText("Min 8 chars, A-Z, a-z, 0-9");
                lblPasswordValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        grid.add(new Label("Password*:"), 0, row);
        grid.add(txtPassword, 1, row);
        grid.add(lblPasswordHint, 2, row);
        row++;
        grid.add(lblPasswordValidation, 1, row);
        GridPane.setColumnSpan(lblPasswordValidation, 2);
        row++;
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("officer@dbu.edu.et");
        
        Label lblEmailHint = new Label("Must be @dbu.edu.et domain");
        lblEmailHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label lblEmailValidation = new Label("Required");
        lblEmailValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        txtEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal != null && 
                          newVal.matches("^[A-Za-z0-9+_.-]+@dbu\\.edu\\.et$");
            if (valid) {
                lblEmailValidation.setText("Valid ✓");
                lblEmailValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblEmailValidation.setText("Must end with @dbu.edu.et");
                lblEmailValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        grid.add(new Label("Email*:"), 0, row);
        grid.add(txtEmail, 1, row);
        grid.add(lblEmailHint, 2, row);
        row++;
        grid.add(lblEmailValidation, 1, row);
        GridPane.setColumnSpan(lblEmailValidation, 2);
        row++;
        
        PhoneInputField phoneInputField = new PhoneInputField();
        
        Label lblPhoneHint = new Label("Format: 09xxxxxxx (Ethio Telecom) or 07xxxxxxx (Safaricom)");
        lblPhoneHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        Label lblPhoneValidation = new Label("Required");
        lblPhoneValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        phoneInputField.getPhoneField().textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validatePhoneWithProvider(newVal, phoneInputField.getProvider());
            updateValidationLabel(lblPhoneValidation, result);
        });
        
        phoneInputField.getProviderComboBox().valueProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = ValidationHelper.validatePhoneWithProvider(
                phoneInputField.getPhoneNumber(), 
                newVal
            );
            updateValidationLabel(lblPhoneValidation, result);
        });
        
        grid.add(new Label("Phone*:"), 0, row);
        grid.add(phoneInputField, 1, row);
        grid.add(lblPhoneHint, 2, row);
        row++;
        grid.add(lblPhoneValidation, 1, row);
        GridPane.setColumnSpan(lblPhoneValidation, 2);
        row++;
        
        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.setItems(FXCollections.observableArrayList(
            "LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"
        ));
        cmbRole.setPromptText("Select Role");
        
        Label lblRoleValidation = new Label("Required");
        lblRoleValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                lblRoleValidation.setText("Valid ✓");
                lblRoleValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblRoleValidation.setText("Select a role");
                lblRoleValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        grid.add(new Label("Role*:"), 0, row);
        grid.add(cmbRole, 1, row);
        row++;
        grid.add(lblRoleValidation, 1, row);
        GridPane.setColumnSpan(lblRoleValidation, 2);
        row++;
        
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.setItems(FXCollections.observableArrayList(
            "Library", "Cafeteria", "Dormitory", "Registrar Office",
            "Computer Science", "Software Engineering", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration"
        ));
        cmbDepartment.setPromptText("Select Department");
        
        Label lblDeptValidation = new Label("Required");
        lblDeptValidation.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        
        cmbDepartment.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                lblDeptValidation.setText("Valid ✓");
                lblDeptValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblDeptValidation.setText("Select a department");
                lblDeptValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        
        grid.add(new Label("Department*:"), 0, row);
        grid.add(cmbDepartment, 1, row);
        row++;
        grid.add(lblDeptValidation, 1, row);
        GridPane.setColumnSpan(lblDeptValidation, 2);
        
        grid.setUserData(new Object[]{
            txtFullName, txtUsername, txtPassword, txtEmail,
            phoneInputField, cmbRole, cmbDepartment,
            lblFullNameValidation, lblUsernameValidation, lblPasswordValidation,
            lblEmailValidation, lblPhoneValidation, lblRoleValidation, lblDeptValidation
        });
        
        return grid;
    }
    
    private void setupOfficerValidationListeners(GridPane grid, SimpleBooleanProperty allFieldsValid) {
        Object[] components = (Object[]) grid.getUserData();
        
        Label lblFullNameValidation = (Label) components[7];      // was 8
        Label lblUsernameValidation = (Label) components[8];      // was 9
        Label lblPasswordValidation = (Label) components[9];      // was 10
        Label lblEmailValidation = (Label) components[10];        // was 11
        Label lblPhoneValidation = (Label) components[11];        // was 12
        Label lblRoleValidation = (Label) components[12];         // was 13
        Label lblDeptValidation = (Label) components[13];         // was 14
        
        Runnable validationChecker = () -> {
            boolean allValid = 
                lblFullNameValidation.getText().equals("Valid ✓") &&
                lblUsernameValidation.getText().equals("Valid ✓") &&
                lblPasswordValidation.getText().equals("Strong ✓") &&
                lblEmailValidation.getText().equals("Valid ✓") &&
                lblPhoneValidation.getText().equals("Valid ✓") &&
                lblRoleValidation.getText().equals("Valid ✓") &&
                lblDeptValidation.getText().equals("Valid ✓");
            
            allFieldsValid.set(allValid);
        };
        
        lblFullNameValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblUsernameValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblPasswordValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblEmailValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblPhoneValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblRoleValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
        lblDeptValidation.textProperty().addListener((obs, oldVal, newVal) -> validationChecker.run());
    }
    
    private boolean validateOfficerFields(GridPane grid) {
        Object[] components = (Object[]) grid.getUserData();
        
        // Update indices to match the array structure
        Label[] validationLabels = new Label[]{
            (Label) components[7],   // Full Name (was 8)
            (Label) components[8],   // Username (was 9)
            (Label) components[9],   // Password (was 10)
            (Label) components[10],  // Email (was 11)
            (Label) components[11],  // Phone (was 12)
            (Label) components[12],  // Role (was 13)
            (Label) components[13]   // Department (was 14)
        };
        
        for (Label label : validationLabels) {
            if (!label.getText().equals("Valid ✓") && !label.getText().equals("Strong ✓")) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean registerOfficerFromForm(GridPane grid) {
        Object[] components = (Object[]) grid.getUserData();
        
        TextField txtFullName = (TextField) components[0];
        TextField txtUsername = (TextField) components[1];
        PasswordField txtPassword = (PasswordField) components[2];
        TextField txtEmail = (TextField) components[3];
        PhoneInputField phoneInputField = (PhoneInputField) components[4];
        ComboBox<String> cmbRole = (ComboBox<String>) components[5];
        ComboBox<String> cmbDepartment = (ComboBox<String>) components[6];
        
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim().toLowerCase();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = phoneInputField.getPhoneNumber();
        String role = cmbRole.getValue();
        String department = cmbDepartment.getValue();
        
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() ||
            email.isEmpty() || role == null || department == null) {
            showAlert("Validation Error", "All fields are required!");
            return false;
        }
        
        String finalPhone = phone;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT username FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                showAlert("Duplicate Username", 
                    "Officer username '" + username + "' already exists.\n\n" +
                    "Username must be unique. Please choose a different username.");
                return false;
            }
            
            if ("DEPARTMENT_HEAD".equals(role)) {
                String checkDeptHeadSql = """
                    SELECT COUNT(*) as head_count 
                    FROM users 
                    WHERE role = 'DEPARTMENT_HEAD' AND department = ? AND status = 'ACTIVE'
                """;
                PreparedStatement checkDeptHeadStmt = conn.prepareStatement(checkDeptHeadSql);
                checkDeptHeadStmt.setString(1, department);
                ResultSet deptHeadRs = checkDeptHeadStmt.executeQuery();
                
                if (deptHeadRs.next() && deptHeadRs.getInt("head_count") > 0) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Department Head Already Exists");
                    confirm.setHeaderText("Department Head for " + department);
                    confirm.setContentText("There is already an active department head for " + department + 
                                        ".\nDo you want to replace the existing department head?");
                    
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (!result.isPresent() || result.get() != ButtonType.OK) {
                        return false;
                    }
                    
                    String deactivateSql = """
                        UPDATE users SET status = 'INACTIVE' 
                        WHERE role = 'DEPARTMENT_HEAD' AND department = ? AND status = 'ACTIVE'
                    """;
                    PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                    deactivateStmt.setString(1, department);
                    deactivateStmt.executeUpdate();
                }
            }

            String insertSql = """
                INSERT INTO users (username, password, full_name, role, email, phone, 
                                  department, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())
            """;

            PreparedStatement stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.setString(4, role);
            stmt.setString(5, email);
            stmt.setString(6, finalPhone);
            stmt.setString(7, department);

            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                String auditSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'OFFICER_REGISTRATION', ?, NOW())
                """;
                PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                auditStmt.setInt(1, currentUser.getId());
                auditStmt.setString(2, "Registered officer: " + username + " - " + fullName + " as " + role);
                auditStmt.executeUpdate();
                
                showAlert("Success", 
                    "✅ Officer Registered Successfully!\n\n" +
                    "Name: " + fullName + "\n" +
                    "Username: " + username + "\n" +
                    "Role: " + role + "\n" +
                    "Department: " + department + "\n" +
                    "Email: " + email + "\n" +
                    "Phone: " + finalPhone + "\n\n" +
                    "Default password has been set. Please change it on first login.");
                
                return true;
            } else {
                showAlert("Error", "Failed to register officer!");
                return false;
            }

        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                showAlert("Error", "Officer username '" + username + "' already exists in the system!");
            } else {
                showAlert("Error", "Failed to register officer: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }
    
    // ==================== STUDENT MANAGEMENT METHODS ====================
    
    private void allowStudentReapply(User student) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Allow Student to Reapply");
        confirm.setHeaderText("Allow Clearance Reapplication");
        confirm.setContentText("Allow " + student.getFullName() + " (" + student.getUsername() + 
                             ") to submit a new clearance request?\n\n" +
                             "This student's previous request was rejected.\n" +
                             "Allowing reapplication will:\n" +
                             "• Reset their clearance status to 'IN_PROGRESS'\n" +
                             "• Enable them to submit a new request\n" +
                             "• Update all UI elements in real-time");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                enableStudentReapply(student);
            }
        });
    }
    
    private void enableStudentReapply(User student) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
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
                        String updateRequestSql = """
                            UPDATE clearance_requests 
                            SET can_reapply = TRUE, 
                                status = 'IN_PROGRESS',
                                request_date = NOW()
                            WHERE id = ?
                            """;
                        
                        PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql);
                        updateRequestStmt.setInt(1, latestRequestId);
                        int requestUpdated = updateRequestStmt.executeUpdate();
                        
                        String clearApprovalsSql = """
                            DELETE FROM clearance_approvals 
                            WHERE request_id = ?
                            """;
                        
                        PreparedStatement clearApprovalsStmt = conn.prepareStatement(clearApprovalsSql);
                        clearApprovalsStmt.setInt(1, latestRequestId);
                        clearApprovalsStmt.executeUpdate();
                        
                        String workflowSql = """
                            SELECT role FROM workflow_config ORDER BY sequence_order
                            """;
                        
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
                        
                        conn.commit();
                        
                        if (requestUpdated > 0) {
                            student.setCanReapply(true);
                            student.setClearanceStatus("🔄 In Progress");
                            
                            refreshStudentTableRows();
                            
                            showAlert("Success", 
                                student.getFullName() + " can now reapply for clearance!\n\n" +
                                "Status has been updated to: 🔄 IN PROGRESS\n" +
                                "The student can now proceed with their clearance request.");
                        } else {
                            conn.rollback();
                            showAlert("Error", "Failed to update clearance request.");
                        }
                    } else {
                        showAlert("Error", "No rejected request found for this student.");
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
            showAlert("Error", "Failed to allow reapplication: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void refreshStudentTableRows() {
        tableAllStudents.refresh();
        tableRejectedStudents.refresh();
        loadAllStudents();
    }
    
    // ==================== USER MANAGEMENT ====================
    
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
    
    // ==================== VIEW STUDENT DETAILS ====================
    
    private void viewStudentDetails(User student) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Student Details");
        dialog.setHeaderText("Student Information: " + student.getFullName());
        dialog.getDialogPane().setPrefSize(700, 600);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder details = new StringBuilder();
            
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
            
            TextArea textArea = new TextArea(details.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
            
            ScrollPane scrollPane = new ScrollPane(textArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPrefSize(680, 550);
            
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

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private void generateStudentFullReport(User student) {
        showAlert("Info", "Full report generation would be implemented here.\n\n" +
                         "Student: " + student.getFullName() + "\n" +
                         "ID: " + student.getUsername() + "\n" +
                         "This feature would generate a PDF with all student information.");
    }

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
        txtBlockNumber.setPromptText("Block number (1-10)");
        
        TextField txtRoomNumber = new TextField();
        txtRoomNumber.setPromptText("Room number (100-4999)");
        
        CheckBox chkKeyReturned = new CheckBox("Key Returned");
        DatePicker dpKeyReturnDate = new DatePicker();
        dpKeyReturnDate.setPromptText("Key return date");
        
        TextArea txtDamageDescription = new TextArea();
        txtDamageDescription.setPromptText("Damage description");
        txtDamageDescription.setPrefRowCount(3);
        
        TextField txtDamageAmount = new TextField();
        txtDamageAmount.setPromptText("Damage amount ($)");
        
        CheckBox chkDamagePaid = new CheckBox("Damage Paid");
        
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
                viewStudentDetails(student);
            }
        });
    }

    private boolean updateDormitoryInfoFull(int studentId, String blockNumber, String roomNumber,
                                           boolean keyReturned, LocalDate keyReturnDate,
                                           String damageDescription, double damageAmount,
                                           boolean damagePaid) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM student_dormitory_credentials WHERE student_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            
            String sql;
            PreparedStatement stmt;
            
            if (rs.getInt(1) > 0) {
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
    
    // ==================== WORKFLOW MANAGEMENT ====================
    
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
    
    // ==================== ACADEMIC SESSION MANAGEMENT ====================
    
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
    
    // ==================== CERTIFICATE GENERATION ====================
    
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
    
    // ==================== CERTIFICATE VERIFICATION ====================
    
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
    
    // ==================== SEMESTER ROLLOVER ====================
    
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
                
                String archiveSql = "INSERT INTO clearance_requests_archive " +
                                  "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                  "FROM clearance_requests WHERE status = 'FULLY_CLEARED'";
                PreparedStatement archiveStmt = conn.prepareStatement(archiveSql);
                int archived = archiveStmt.executeUpdate();
                report.append("✓ Archived " + archived + " cleared requests\n");
                
                String archiveRejectedSql = "INSERT INTO clearance_requests_archive " +
                                         "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                         "FROM clearance_requests WHERE status = 'REJECTED'";
                PreparedStatement archiveRejectedStmt = conn.prepareStatement(archiveRejectedSql);
                int archivedRejected = archiveRejectedStmt.executeUpdate();
                report.append("✓ Archived " + archivedRejected + " rejected requests\n");

                String resetSql = "UPDATE clearance_requests SET status = 'EXPIRED' " +
                                "WHERE status IN ('PENDING', 'IN_PROGRESS')";
                PreparedStatement resetStmt = conn.prepareStatement(resetSql);
                int expired = resetStmt.executeUpdate();
                report.append("✓ Expired " + expired + " pending requests\n");

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

                LocalDate today = LocalDate.now();
                String sessionName;
                if (today.getMonthValue() <= 6) {
                    sessionName = "Spring Semester " + today.getYear();
                } else {
                    sessionName = "Fall Semester " + today.getYear();
                }
                
                String deactivateSql = "UPDATE academic_sessions SET is_active = false WHERE is_active = true";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();

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
    
    // ==================== LOGOUT ====================
    
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

    // ==================== UTILITY METHODS ====================
    
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
        
        // Customize based on type
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
}