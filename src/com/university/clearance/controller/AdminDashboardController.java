package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.SelectableUser;
import com.university.clearance.model.ClearanceRequest;
<<<<<<< HEAD
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
=======
import com.university.clearance.util.ValidationService;

>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
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
<<<<<<< HEAD
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

=======
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
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
    
<<<<<<< HEAD
=======
    // Requests Search
>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
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
<<<<<<< HEAD
        // Initialize services
        userManagementService = new UserManagementService();
        dataManagementService = new DataManagementService(this);
        clearanceOperationsService = new ClearanceOperationsService();
        
=======
        System.out.println("[DEBUG] AdminDashboardController.initialize() called");
>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
        setupAllTables();
        setupSearchFunctionality();
        setupTabAnimations();
        setupActiveTabHighlight();
        setupAllowResubmitButtons();
        setupBulkOperationListeners();
        
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
<<<<<<< HEAD
=======
    }
    
    // ==================== VALIDATION HELPER METHODS ====================
    
    private Label createValidationLabel() {
        Label label = new Label();
        label.setPrefWidth(200);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 10px; -fx-padding: 2;");
        return label;
    }
    
    private void updateValidationLabel(Label label, ValidationService.ValidationResult result) {
        if (result.isValid()) {
            if ("Valid".equals(result.getMessage()) || "Optional".equals(result.getMessage())) {
                label.setText("✓ " + result.getMessage());
                label.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 10px; -fx-padding: 2;");
            } else {
                label.setText("✓ " + result.getMessage());
                label.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 10px; -fx-padding: 2;");
            }
        } else {
            label.setText("✗ " + result.getMessage());
            label.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px; -fx-padding: 2;");
        }
    }
    
    private void showValidationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Validation Failed");
        alert.setContentText(message);
        
        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #f8d7da;");
        
        alert.showAndWait();
    }
    
    // ==================== STUDENT REGISTRATION WITH VALIDATION ====================
    
    @FXML
    private void openRegisterStudent() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Student");
        dialog.setHeaderText("Enter Student Information");
        DialogPane pane = new DialogPane();
        dialog.setDialogPane(pane);
        ButtonType registerButton = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        GridPane grid = createStudentFormWithValidation();
        pane.setContent(grid);
        Button btnRegister = (Button) pane.lookupButton(registerButton);
        btnRegister.addEventFilter(ActionEvent.ACTION, event -> {
            boolean valid = registerStudentWithValidation(grid);
            if (!valid) {
                event.consume();
            }
        });
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == registerButton) {
            loadAllUsers();
            loadAllStudents();
            showAlert("Success", "Student registered successfully!");
        }
    }
    
    private GridPane createStudentFormWithValidation() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Validation Labels
        Label lblIdValidation = createValidationLabel();
        Label lblNameValidation = createValidationLabel();
        Label lblPasswordValidation = createValidationLabel();
        Label lblEmailValidation = createValidationLabel();
        Label lblPhoneValidation = createValidationLabel();
        Label lblBlockValidation = createValidationLabel();
        Label lblRoomValidation = createValidationLabel();

        // Student ID Field
        TextField txtStudentId = new TextField();
        txtStudentId.setPromptText("DBU1601374");
        txtStudentId.setText("DBU");
        txtStudentId.setPrefWidth(200);
        
        // Username (auto-generated)
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("dbu1601374");
        txtUsername.setText("dbu");
        txtUsername.setEditable(false);
        txtUsername.setStyle("-fx-background-color: #f0f0f0;");
        
        // Real-time student ID validation
        txtStudentId.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.startsWith("DBU")) {
                newText = "DBU" + newText.replaceAll("(?i)DBU", "");
            }
            String digits = newText.substring(3).replaceAll("[^\\d]", "");
            if (digits.length() > 7) digits = digits.substring(0, 7);
            txtStudentId.setText("DBU" + digits);
            txtStudentId.positionCaret(txtStudentId.getText().length());
            txtUsername.setText("dbu" + digits);
            
            // Validate student ID
            ValidationService.ValidationResult result = 
                ValidationService.validateStudentId(txtStudentId.getText());
            updateValidationLabel(lblIdValidation, result);
        });

        // Full Name
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");
        txtFullName.setPrefWidth(200);
        txtFullName.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validateFullName(newText);
            updateValidationLabel(lblNameValidation, result);
        });

        // Password
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password (min 6 chars)");
        txtPassword.setPrefWidth(200);
        txtPassword.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validatePassword(newText);
            updateValidationLabel(lblPasswordValidation, result);
        });

        // Email
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email (optional)");
        txtEmail.setPrefWidth(200);
        txtEmail.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validateEmail(newText);
            updateValidationLabel(lblEmailValidation, result);
        });

        // Phone Number
        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);

        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(120);

        txtPhoneSuffix.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtPhoneSuffix.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtPhoneSuffix.getText().length() > 8) {
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
            }
            
            // Validate phone
            String prefix = cmbPhonePrefix.getValue();
            String suffix = txtPhoneSuffix.getText();
            if (prefix != null && suffix.length() == 8) {
                ValidationService.ValidationResult result = 
                    ValidationService.validatePhone(prefix + suffix);
                updateValidationLabel(lblPhoneValidation, result);
            } else {
                lblPhoneValidation.setText("");
                lblPhoneValidation.setStyle("");
            }
        });

        cmbPhonePrefix.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && txtPhoneSuffix.getText().length() == 8) {
                ValidationService.ValidationResult result = 
                    ValidationService.validatePhone(newVal + txtPhoneSuffix.getText());
                updateValidationLabel(lblPhoneValidation, result);
            }
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);

        // Department
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll(
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration",
            "Accounting", "Economics", "Mathematics", "Food Engineering", "Chemistry", "Biology"
        );
        cmbDepartment.setPromptText("Select Department");
        cmbDepartment.setPrefWidth(200);

        // Year Level
        ComboBox<String> cmbYear = new ComboBox<>();
        cmbYear.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year");
        cmbYear.setPromptText("Select Year");
        cmbYear.setPrefWidth(200);
        
        // Block Number (UPDATED: numbers only)
        TextField txtBlockNumber = new TextField();
        txtBlockNumber.setPromptText("e.g., 1, 5, 10 (1-99)");
        txtBlockNumber.setPrefWidth(200);
        txtBlockNumber.textProperty().addListener((obs, oldText, newText) -> {
            // Allow only numbers
            if (!newText.matches("\\d*")) {
                txtBlockNumber.setText(newText.replaceAll("[^\\d]", ""));
            }
            // Limit to 2 digits
            if (txtBlockNumber.getText().length() > 2) {
                txtBlockNumber.setText(txtBlockNumber.getText().substring(0, 2));
            }
            
            ValidationService.ValidationResult result = 
                ValidationService.validateBlockNumber(newText);
            updateValidationLabel(lblBlockValidation, result);
        });
        
        // Dorm/Room Number (UPDATED: numbers only)
        TextField txtDormNumber = new TextField();
        txtDormNumber.setPromptText("e.g., 101, 205, 1001 (1-9999)");
        txtDormNumber.setPrefWidth(200);
        txtDormNumber.textProperty().addListener((obs, oldText, newText) -> {
            // Allow only numbers
            if (!newText.matches("\\d*")) {
                txtDormNumber.setText(newText.replaceAll("[^\\d]", ""));
            }
            // Limit to 4 digits
            if (txtDormNumber.getText().length() > 4) {
                txtDormNumber.setText(txtDormNumber.getText().substring(0, 4));
            }
            
            ValidationService.ValidationResult result = 
                ValidationService.validateRoomNumber(newText);
            updateValidationLabel(lblRoomValidation, result);
        });

        // Add to grid - with validation labels
        int row = 0;
        grid.add(new Label("Student ID*:"), 0, row);
        grid.add(txtStudentId, 1, row);
        grid.add(lblIdValidation, 2, row++);
        
        grid.add(new Label("Username*:"), 0, row);
        grid.add(txtUsername, 1, row++);
        
        grid.add(new Label("Full Name*:"), 0, row);
        grid.add(txtFullName, 1, row);
        grid.add(lblNameValidation, 2, row++);
        
        grid.add(new Label("Password*:"), 0, row);
        grid.add(txtPassword, 1, row);
        grid.add(lblPasswordValidation, 2, row++);
        
        grid.add(new Label("Email:"), 0, row);
        grid.add(txtEmail, 1, row);
        grid.add(lblEmailValidation, 2, row++);
        
        grid.add(new Label("Phone*:"), 0, row);
        grid.add(phoneBox, 1, row);
        grid.add(lblPhoneValidation, 2, row++);
        
        grid.add(new Label("Department*:"), 0, row);
        grid.add(cmbDepartment, 1, row++);
        
        grid.add(new Label("Year Level*:"), 0, row);
        grid.add(cmbYear, 1, row++);
        
        grid.add(new Label("Block Number:"), 0, row);
        grid.add(txtBlockNumber, 1, row);
        grid.add(lblBlockValidation, 2, row++);
        
        grid.add(new Label("Room Number:"), 0, row);
        grid.add(txtDormNumber, 1, row);
        grid.add(lblRoomValidation, 2, row++);
        
        // Add instructions
        Label lblInstructions = new Label("* Required fields\n\n" +
                                        "Block Number: Number from 1-99 (e.g., 1, 5, 10)\n" +
                                        "Room Number: Number from 1-9999 (e.g., 101, 205, 1001)\n\n" +
                                        "Password Requirements:\n• Minimum 6 characters\n• At least one letter\n• At least one number");
        lblInstructions.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(lblInstructions, 1, row, 2, 1);

        grid.setUserData(new Object[]{
            txtStudentId, txtUsername, txtFullName, txtPassword,
            txtEmail, cmbPhonePrefix, txtPhoneSuffix, cmbDepartment, cmbYear,
            txtBlockNumber, txtDormNumber,
            lblIdValidation, lblNameValidation, lblPasswordValidation, 
            lblEmailValidation, lblPhoneValidation, lblBlockValidation, lblRoomValidation
        });

        return grid;
    }
    
    private boolean registerStudentWithValidation(GridPane grid) {
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
        TextField txtBlockNumber = (TextField) fields[9];
        TextField txtDormNumber = (TextField) fields[10];

        // Extract values
        String studentId = txtStudentId.getText().trim();
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();
        String blockNumber = txtBlockNumber.getText().trim();
        String dormNumber = txtDormNumber.getText().trim();

        // Validate all fields
        StringBuilder validationErrors = new StringBuilder();
        
        // Student ID validation
        ValidationService.ValidationResult idResult = ValidationService.validateStudentId(studentId);
        if (!idResult.isValid()) {
            validationErrors.append("• Student ID: ").append(idResult.getMessage()).append("\n");
            txtStudentId.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtStudentId.setStyle("");
        }
        
        // Full name validation
        ValidationService.ValidationResult nameResult = ValidationService.validateFullName(fullName);
        if (!nameResult.isValid()) {
            validationErrors.append("• Full Name: ").append(nameResult.getMessage()).append("\n");
            txtFullName.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtFullName.setStyle("");
        }
        
        // Password validation
        ValidationService.ValidationResult passResult = ValidationService.validatePassword(password);
        if (!passResult.isValid()) {
            validationErrors.append("• Password: ").append(passResult.getMessage()).append("\n");
            txtPassword.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtPassword.setStyle("");
        }
        
        // Email validation
        ValidationService.ValidationResult emailResult = ValidationService.validateEmail(email);
        if (!emailResult.isValid()) {
            validationErrors.append("• Email: ").append(emailResult.getMessage()).append("\n");
            txtEmail.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtEmail.setStyle("");
        }
        
        // Phone validation
        String phone = "";
        if (phonePrefix != null && !phoneSuffix.isEmpty()) {
            phone = phonePrefix + phoneSuffix;
            ValidationService.ValidationResult phoneResult = ValidationService.validatePhone(phone);
            if (!phoneResult.isValid()) {
                validationErrors.append("• Phone: ").append(phoneResult.getMessage()).append("\n");
                cmbPhonePrefix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
                txtPhoneSuffix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            } else {
                cmbPhonePrefix.setStyle("");
                txtPhoneSuffix.setStyle("");
            }
        } else {
            validationErrors.append("• Phone: Please enter phone number\n");
            cmbPhonePrefix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            txtPhoneSuffix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
        
        // Department validation
        ValidationService.ValidationResult deptResult = ValidationService.validateDepartment(department);
        if (!deptResult.isValid()) {
            validationErrors.append("• Department: ").append(deptResult.getMessage()).append("\n");
            cmbDepartment.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            cmbDepartment.setStyle("");
        }
        
        // Year validation
        ValidationService.ValidationResult yearResult = ValidationService.validateYearLevel(year);
        if (!yearResult.isValid()) {
            validationErrors.append("• Year Level: ").append(yearResult.getMessage()).append("\n");
            cmbYear.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            cmbYear.setStyle("");
        }
        
        // Block number validation
        ValidationService.ValidationResult blockResult = ValidationService.validateBlockNumber(blockNumber);
        if (!blockResult.isValid()) {
            validationErrors.append("• Block Number: ").append(blockResult.getMessage()).append("\n");
            txtBlockNumber.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtBlockNumber.setStyle("");
        }
        
        // Room number validation
        ValidationService.ValidationResult roomResult = ValidationService.validateRoomNumber(dormNumber);
        if (!roomResult.isValid()) {
            validationErrors.append("• Room Number: ").append(roomResult.getMessage()).append("\n");
            txtDormNumber.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtDormNumber.setStyle("");
        }
        
        // If there are validation errors, show them
        if (validationErrors.length() > 0) {
            showValidationAlert("Registration Validation Failed", 
                "Please correct the following errors:\n\n" + validationErrors.toString());
            return false;
        }

        // Check for duplicates in database
        if (isDuplicateStudent(username, phone)) {
            return false;
        }

        // Proceed with registration
        return performStudentRegistration(studentId, username, fullName, password, 
                                         email, phone, department, year, 
                                         blockNumber, dormNumber);
    }
    
    private boolean isDuplicateStudent(String username, String phone) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkDuplicate = """
                SELECT 
                    CASE WHEN username = ? THEN 'Username' 
                         WHEN phone = ? THEN 'Phone number' 
                    END as duplicate_type
                FROM users 
                WHERE username = ? OR phone = ?
                """;
            
            PreparedStatement checkStmt = conn.prepareStatement(checkDuplicate);
            checkStmt.setString(1, username);
            checkStmt.setString(2, phone);
            checkStmt.setString(3, username);
            checkStmt.setString(4, phone);
            
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                String duplicateType = checkRs.getString("duplicate_type");
                showAlert("Duplicate Found", 
                    duplicateType + " already exists in the system!\n\n" +
                    "Username: " + username + "\n" +
                    "Phone: " + phone + "\n\n" +
                    "Please use a different " + duplicateType.toLowerCase() + ".");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            showAlert("Error", "Failed to check for duplicates: " + e.getMessage());
            return true;
        }
    }
    
    private boolean performStudentRegistration(String studentId, String username, String fullName, 
                                             String password, String email, String phone, 
                                             String department, String year, String blockNumber, 
                                             String dormNumber) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 1. Insert user into users table
                String userSql = """
                    INSERT INTO users (username, password, full_name, role, email, phone, department, year_level, status)
                    VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?, 'ACTIVE')
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
                    showAlert("Error", "Failed to register student in users table.");
                    return false;
                }
                
                // Get the generated student ID
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                int studentDbId = -1;
                if (generatedKeys.next()) {
                    studentDbId = generatedKeys.getInt(1);
                }
                
                // 2. Insert dormitory credentials if provided
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
                
                // 3. Log the registration
                String logSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'STUDENT_REGISTRATION', ?, NOW())
                    """;
                    
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, currentUser.getId());
                logStmt.setString(2, "Registered student: " + username + " - " + fullName);
                logStmt.executeUpdate();
                
                // Commit transaction
                conn.commit();
                
                // Show success message with details
                showRegistrationSuccess(studentId, fullName, department, year, blockNumber, dormNumber);
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            showAlert("Registration Error", 
                "Registration failed!\n\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "Please try again or contact system administrator.");
            e.printStackTrace();
            return false;
        }
    }
    
    private void showRegistrationSuccess(String studentId, String fullName, String department, 
                                       String year, String blockNumber, String dormNumber) {
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Registration Successful");
        success.setHeaderText("✅ Student Registered Successfully!");
        
        StringBuilder content = new StringBuilder();
        content.append("Student Details:\n");
        content.append("────────────────\n");
        content.append("• Name: ").append(fullName).append("\n");
        content.append("• Student ID: ").append(studentId).append("\n");
        content.append("• Department: ").append(department).append("\n");
        content.append("• Year Level: ").append(year).append("\n");
        
        if (!blockNumber.isEmpty() && !dormNumber.isEmpty()) {
            content.append("• Dormitory: Block ").append(blockNumber)
                   .append(", Room ").append(dormNumber).append("\n");
        }
        
        content.append("\nRegistration Status:\n");
        content.append("────────────────\n");
        content.append("✓ Account created successfully\n");
        content.append("✓ Dormitory information saved\n");
        content.append("✓ Student can now login and apply for clearance\n");
        
        success.setContentText(content.toString());
        
        // Style the success alert
        DialogPane dialogPane = success.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #d4edda;");
        
        success.showAndWait();
    }
    
    // ==================== OFFICER REGISTRATION WITH VALIDATION ====================
    
    @FXML
    private void openManageOfficers() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Department Officers");
        dialog.setHeaderText("Add New Officer");
        DialogPane pane = dialog.getDialogPane();
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        GridPane grid = createOfficerFormWithValidation();
        pane.setContent(grid);
        Button btnSave = (Button) pane.lookupButton(saveButton);
        btnSave.addEventFilter(ActionEvent.ACTION, event -> {
            boolean valid = registerOfficerWithValidation(grid);
            if (!valid) {
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButton) {
            loadAllUsers();
            loadOfficers();
            showAlert("Success", "Officer registered successfully!");
        }
    }
    
    private GridPane createOfficerFormWithValidation() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Validation Labels
        Label lblNameValidation = createValidationLabel();
        Label lblUsernameValidation = createValidationLabel();
        Label lblPasswordValidation = createValidationLabel();
        Label lblEmailValidation = createValidationLabel();
        Label lblPhoneValidation = createValidationLabel();

        // Full Name
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");
        txtFullName.setPrefWidth(200);
        txtFullName.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validateFullName(newText);
            updateValidationLabel(lblNameValidation, result);
        });
        
        // Username
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");
        txtUsername.setPrefWidth(200);
        txtUsername.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() < 3) {
                lblUsernameValidation.setText("✗ Username must be at least 3 characters");
                lblUsernameValidation.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
            } else {
                lblUsernameValidation.setText("✓ Valid");
                lblUsernameValidation.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 10px;");
            }
        });
        
        // Password
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");
        txtPassword.setPrefWidth(200);
        txtPassword.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validatePassword(newText);
            updateValidationLabel(lblPasswordValidation, result);
        });
        
        // Email
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");
        txtEmail.setPrefWidth(200);
        txtEmail.textProperty().addListener((obs, oldText, newText) -> {
            ValidationService.ValidationResult result = 
                ValidationService.validateEmail(newText);
            updateValidationLabel(lblEmailValidation, result);
        });
        
        // Phone Number
        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);

        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(120);

        txtPhoneSuffix.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtPhoneSuffix.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (txtPhoneSuffix.getText().length() > 8) {
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
            }
            
            // Validate phone
            String prefix = cmbPhonePrefix.getValue();
            String suffix = txtPhoneSuffix.getText();
            if (prefix != null && suffix.length() == 8) {
                ValidationService.ValidationResult result = 
                    ValidationService.validatePhone(prefix + suffix);
                updateValidationLabel(lblPhoneValidation, result);
            } else {
                lblPhoneValidation.setText("");
                lblPhoneValidation.setStyle("");
            }
        });

        cmbPhonePrefix.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && txtPhoneSuffix.getText().length() == 8) {
                ValidationService.ValidationResult result = 
                    ValidationService.validatePhone(newVal + txtPhoneSuffix.getText());
                updateValidationLabel(lblPhoneValidation, result);
            }
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);

        // Role
        ComboBox<String> cmbRole = new ComboBox<>();
        ComboBox<String> cmbDepartment = new ComboBox<>();

        cmbRole.getItems().addAll("LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD");
        cmbRole.setPromptText("Select Role");
        cmbRole.setPrefWidth(200);
        
        // Department (dynamic based on role)
        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            cmbDepartment.getItems().clear();
            if (newVal != null) {
                switch (newVal) {
                    case "LIBRARIAN":
                        cmbDepartment.getItems().addAll("Library");
                        break;
                    case "CAFETERIA":
                        cmbDepartment.getItems().addAll("Cafeteria");
                        break;
                    case "DORMITORY":
                        cmbDepartment.getItems().addAll("Dormitory");
                        break;
                    case "REGISTRAR":
                        cmbDepartment.getItems().addAll("Registrar Office");
                        break;
                    case "DEPARTMENT_HEAD":
                        cmbDepartment.getItems().addAll(
                            "Software Engineering", "Computer Science", "Electrical Engineering",
                            "Mechanical Engineering", "Civil Engineering", "Business Administration",
                            "Accounting", "Economics", "Mathematics", "Food Engineering", 
                            "Chemistry", "Biology"
                        );
                        break;
                }
                cmbDepartment.setValue(cmbDepartment.getItems().isEmpty() ? null : cmbDepartment.getItems().get(0));
            }
        });

        cmbDepartment.setPromptText("Select Department");
        cmbDepartment.setPrefWidth(200);

        // Add to grid
        int row = 0;
        grid.add(new Label("Full Name*:"), 0, row);
        grid.add(txtFullName, 1, row);
        grid.add(lblNameValidation, 2, row++);
        
        grid.add(new Label("Username*:"), 0, row);
        grid.add(txtUsername, 1, row);
        grid.add(lblUsernameValidation, 2, row++);
        
        grid.add(new Label("Password*:"), 0, row);
        grid.add(txtPassword, 1, row);
        grid.add(lblPasswordValidation, 2, row++);
        
        grid.add(new Label("Email*:"), 0, row);
        grid.add(txtEmail, 1, row);
        grid.add(lblEmailValidation, 2, row++);
        
        grid.add(new Label("Phone*:"), 0, row);
        grid.add(phoneBox, 1, row);
        grid.add(lblPhoneValidation, 2, row++);
        
        grid.add(new Label("Role*:"), 0, row);
        grid.add(cmbRole, 1, row++);
        
        grid.add(new Label("Department*:"), 0, row);
        grid.add(cmbDepartment, 1, row++);

        grid.setUserData(new Object[]{
            txtFullName, txtUsername, txtPassword, txtEmail, 
            cmbPhonePrefix, txtPhoneSuffix, cmbRole, cmbDepartment,
            lblNameValidation, lblUsernameValidation, lblPasswordValidation,
            lblEmailValidation, lblPhoneValidation
        });

        return grid;
    }
    
    private boolean registerOfficerWithValidation(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtFullName = (TextField) fields[0];
        TextField txtUsername = (TextField) fields[1];
        PasswordField txtPassword = (PasswordField) fields[2];
        TextField txtEmail = (TextField) fields[3];
        ComboBox<String> cmbPhonePrefix = (ComboBox<String>) fields[4];
        TextField txtPhoneSuffix = (TextField) fields[5];
        ComboBox<String> cmbRole = (ComboBox<String>) fields[6];
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[7];

        // Extract values
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();
        String role = cmbRole.getValue();
        String department = cmbDepartment.getValue();

        // Validate all fields
        StringBuilder validationErrors = new StringBuilder();
        
        // Full name validation
        ValidationService.ValidationResult nameResult = ValidationService.validateFullName(fullName);
        if (!nameResult.isValid()) {
            validationErrors.append("• Full Name: ").append(nameResult.getMessage()).append("\n");
            txtFullName.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtFullName.setStyle("");
        }
        
        // Username validation
        if (username.isEmpty() || username.length() < 3) {
            validationErrors.append("• Username: Must be at least 3 characters\n");
            txtUsername.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtUsername.setStyle("");
        }
        
        // Password validation
        ValidationService.ValidationResult passResult = ValidationService.validatePassword(password);
        if (!passResult.isValid()) {
            validationErrors.append("• Password: ").append(passResult.getMessage()).append("\n");
            txtPassword.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtPassword.setStyle("");
        }
        
        // Email validation
        ValidationService.ValidationResult emailResult = ValidationService.validateEmail(email);
        if (!emailResult.isValid()) {
            validationErrors.append("• Email: ").append(emailResult.getMessage()).append("\n");
            txtEmail.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            txtEmail.setStyle("");
        }
        
        // Phone validation
        String phone = "";
        if (phonePrefix != null && !phoneSuffix.isEmpty()) {
            phone = phonePrefix + phoneSuffix;
            ValidationService.ValidationResult phoneResult = ValidationService.validatePhone(phone);
            if (!phoneResult.isValid()) {
                validationErrors.append("• Phone: ").append(phoneResult.getMessage()).append("\n");
                cmbPhonePrefix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
                txtPhoneSuffix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            } else {
                cmbPhonePrefix.setStyle("");
                txtPhoneSuffix.setStyle("");
            }
        } else {
            validationErrors.append("• Phone: Please enter phone number\n");
            cmbPhonePrefix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            txtPhoneSuffix.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
        
        // Role validation
        if (role == null || role.trim().isEmpty()) {
            validationErrors.append("• Role: Please select a role\n");
            cmbRole.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            cmbRole.setStyle("");
        }
        
        // Department validation
        if (department == null || department.trim().isEmpty()) {
            validationErrors.append("• Department: Please select a department\n");
            cmbDepartment.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        } else {
            cmbDepartment.setStyle("");
        }
        
        // If there are validation errors, show them
        if (validationErrors.length() > 0) {
            showValidationAlert("Officer Registration Failed", 
                "Please correct the following errors:\n\n" + validationErrors.toString());
            return false;
        }
        
        // Check for duplicates
        if (isDuplicateOfficer(username, phone, email)) {
            return false;
        }

        // Proceed with registration
        return performOfficerRegistration(username, password, fullName, email, 
                                         phone, role, department);
    }
    
    private boolean isDuplicateOfficer(String username, String phone, String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkDuplicate = """
                SELECT 
                    CASE 
                        WHEN username = ? THEN 'Username'
                        WHEN phone = ? THEN 'Phone number'
                        WHEN email = ? THEN 'Email'
                    END as duplicate_type
                FROM users 
                WHERE username = ? OR phone = ? OR email = ?
                """;
            
            PreparedStatement checkStmt = conn.prepareStatement(checkDuplicate);
            checkStmt.setString(1, username);
            checkStmt.setString(2, phone);
            checkStmt.setString(3, email);
            checkStmt.setString(4, username);
            checkStmt.setString(5, phone);
            checkStmt.setString(6, email);
            
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                String duplicateType = checkRs.getString("duplicate_type");
                showAlert("Duplicate Found", 
                    duplicateType + " already exists in the system!\n\n" +
                    "Please use a different " + duplicateType.toLowerCase() + ".");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            showAlert("Error", "Failed to check for duplicates: " + e.getMessage());
            return true;
        }
    }
    
    private boolean performOfficerRegistration(String username, String password, String fullName, 
                                             String email, String phone, String role, 
                                             String department) {
        try (Connection conn = DatabaseConnection.getConnection()) {
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
            stmt.setString(6, phone);
            stmt.setString(7, department);

            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                // Log the registration
                String logSql = """
                    INSERT INTO audit_logs (user_id, action, details, timestamp)
                    VALUES (?, 'OFFICER_REGISTRATION', ?, NOW())
                    """;
                    
                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, currentUser.getId());
                logStmt.setString(2, "Registered officer: " + username + " - " + fullName + " (" + role + ")");
                logStmt.executeUpdate();
                
                return true;
            }
            
            return false;

        } catch (SQLException e) {
            showAlert("Error", "Failed to register officer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ==================== TABLE SETUP METHODS ====================
    
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
        System.out.println("[DEBUG] Starting loadAllStudents()");
        allStudentsData.clear();
        approvedStudentsData.clear();
        rejectedStudentsData.clear();
        pendingStudentsData.clear();
        inProgressStudentsData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("[DEBUG] Database connection successful");
            
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
                
            System.out.println("[DEBUG] SQL: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
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
                
                System.out.println("[DEBUG] Loaded student: " + student.getUsername() + 
                                  " | Status: " + clearanceStatus);
                
                // Set formatted status with reapply info
                student.setClearanceStatus(formatClearanceStatus(clearanceStatus, canReapply));
                student.setCanReapply(canReapply);
                
                allStudentsData.add(student);
                
                // Categorize by status
                if (clearanceStatus.equals("FULLY_CLEARED") || clearanceStatus.equals("APPROVED")) {
                    approvedStudentsData.add(student);
                } else if (clearanceStatus.equals("REJECTED") && !canReapply) {
                    rejectedStudentsData.add(student);
                } else if (clearanceStatus.equals("PENDING")) {
                    pendingStudentsData.add(student);
                } else if (clearanceStatus.equals("IN_PROGRESS")) {
                    inProgressStudentsData.add(student);
                } else if (clearanceStatus.equals("NO_REQUEST")) {
                    System.out.println("[DEBUG] Student " + student.getUsername() + " has no request");
                }
            }
            
            System.out.println("[DEBUG] Total students loaded: " + count);
            System.out.println("[DEBUG] allStudentsData size: " + allStudentsData.size());
            
            // Set data to tables
            tableAllStudents.setItems(allStudentsData);
            tableApprovedStudents.setItems(approvedStudentsData);
            tableRejectedStudents.setItems(rejectedStudentsData);
            tablePendingStudents.setItems(pendingStudentsData);
            tableInProgressStudents.setItems(inProgressStudentsData);
            
            // Force table refresh
            Platform.runLater(() -> {
                tableAllStudents.refresh();
                System.out.println("[DEBUG] Table refreshed on UI thread");
            });
            
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load students: " + e.getMessage());
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

    private String formatClearanceStatus(String status) {
        return formatClearanceStatus(status, false);
    }
    
    private void updateDashboardStats() {
        lblTotalStudents.setText("Students: " + allStudentsData.size());
        lblTotalOfficers.setText("Officers: " + officersData.size());
        
        // Count total requests
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
        
        // Update card labels if they exist
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
    }
    
    // ==================== SEARCH FUNCTIONALITY ====================
    
    private void setupSearchFunctionality() {
        // Initialize search type combobox
        cmbSearchType.setItems(FXCollections.observableArrayList(
            "All Users",
            "Students Only", 
            "Officers Only"
        ));
        cmbSearchType.setValue("All Users");
>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
        
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
            
<<<<<<< HEAD
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
=======
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
                
                // Add additional fields for students
                if ("STUDENT".equals(user.getRole())) {
                    user.setYearLevel(rs.getString("year_level"));
                    user.setPhone(rs.getString("phone"));
                }
                
                allUsersData.add(user);
            }
            
            tableAllUsers.setItems(allUsersData);
            
            // Update search status
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
            
            // Apply styling based on type
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
    
    // ==================== STUDENT MANAGEMENT ====================
>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
    
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
<<<<<<< HEAD
                } else {
                    showAlert("Error", "No rejected or expired request found for this student.");
=======
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
>>>>>>> 9410933850522d8f64ea7c1c0598e60f89a852f4
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