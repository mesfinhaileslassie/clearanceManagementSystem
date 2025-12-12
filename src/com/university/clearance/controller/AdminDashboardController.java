package com.university.clearance.controller;

import com.university.clearance.model.User;
import com.university.clearance.DatabaseConnection;
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
        setupDeleteButtons();
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
    
    private void setupDeleteButtons() {
        TableColumn<User, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteButton = new Button("🗑️ Delete");
            
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    userManagementService.handleDeleteUser(user, currentUser, () -> {
                        dataManagementService.loadAllData();
                        updateReportStatistics();
                    });
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
        
        TableColumn<User, Void> deleteOfficerCol = new TableColumn<>("Actions");
        deleteOfficerCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteButton = new Button("🗑️ Delete");
            
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    User officer = getTableView().getItems().get(getIndex());
                    userManagementService.handleDeleteUser(officer, currentUser, () -> {
                        dataManagementService.loadAllData();
                        updateReportStatistics();
                    });
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
        if (tableRequests == null) {
            return;
        }
        
        if (colResubmission == null) {
            colResubmission = (TableColumn<ClearanceRequest, Void>) tableRequests.getColumns().stream()
                .filter(col -> "Resubmission".equals(col.getText()))
                .findFirst()
                .orElse(null);
                
            if (colResubmission == null) {
                colResubmission = new TableColumn<>("Resubmission");
                colResubmission.setPrefWidth(150);
                colResubmission.setMinWidth(120);
                tableRequests.getColumns().add(colResubmission);
            }
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
            showAlert("Error", "Please select a user to delete!");
            return;
        }
        
        userManagementService.handleDeleteUser(selectedUser, currentUser, () -> {
            dataManagementService.loadAllData();
            updateReportStatistics();
        });
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
        
    private void setupAllTables() {
        dataManagementService.setupStudentTable(tableAllStudents, colStudentId, colStudentName, 
                                               colStudentDepartment, colStudentYear, 
                                               colClearanceStatus, colStudentActions);
        
        dataManagementService.setupSimpleStudentTable(tableApprovedStudents);
        dataManagementService.setupSimpleStudentTable(tableRejectedStudents);
        dataManagementService.setupSimpleStudentTable(tablePendingStudents);
        dataManagementService.setupSimpleStudentTable(tableInProgressStudents);
        
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
        
        // Add listener to ComboBox selection changes
        cmbSearchType.valueProperty().addListener((obs, oldValue, newValue) -> {
            String searchText = txtSearchUsers.getText().trim();
            if (!searchText.isEmpty()) {
                // Trigger search when ComboBox selection changes and there's search text
                dataManagementService.handleUserSearch(searchText, newValue);
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

    // Add this new method to handle filtering only (without search text)
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
            
            // Use the local tableAllUsers reference
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
            
            // Use the local tableAllUsers reference
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
    private void handleUserSearch() {
        String searchText = txtSearchUsers.getText().trim();
        String searchType = cmbSearchType.getValue();
        
        if (searchText.isEmpty()) {
            // If search is empty, just show filtered users based on ComboBox selection
            if ("Students Only".equals(searchType)) {
                loadFilteredStudents();
            } else if ("Officers Only".equals(searchType)) {
                loadFilteredOfficers();
            } else {
                dataManagementService.loadAllUsers();
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
        dataManagementService.loadAllData();
        updateReportStatistics();
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
    
    private void allowStudentReapply(User student) {
        userManagementService.allowStudentReapply(student);
    }
    
    private void viewStudentDetails(User student) {
        userManagementService.viewStudentDetails(student);
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
        dataManagementService.loadClearanceRequests();
        dataManagementService.loadAllStudents();
        updateDashboardStats();
        updateReportStatistics();
        tableRequests.refresh();
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
    public ObservableList<User> getAllStudentsData() { return dataManagementService.getAllStudentsData(); }
    public ObservableList<User> getApprovedStudentsData() { return dataManagementService.getApprovedStudentsData(); }
    public ObservableList<User> getRejectedStudentsData() { return dataManagementService.getRejectedStudentsData(); }
    public ObservableList<User> getPendingStudentsData() { return dataManagementService.getPendingStudentsData(); }
    public ObservableList<User> getInProgressStudentsData() { return dataManagementService.getInProgressStudentsData(); }
    public ObservableList<User> getOfficersData() { return dataManagementService.getOfficersData(); }
    public ObservableList<User> getAllUsersData() { return dataManagementService.getAllUsersData(); }
    public ObservableList<ClearanceRequest> getRequestData() { return dataManagementService.getRequestData(); }
}