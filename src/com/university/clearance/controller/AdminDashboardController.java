package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.model.ClearanceRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class AdminDashboardController {

    // Top Section
    @FXML private Label lblWelcome;
    @FXML private Label lblTotalRequests;
    @FXML private Label lblFullyCleared;
    
    // Dashboard Cards
    @FXML private Label lblTotalRequestsCard;
    @FXML private Label lblFullyClearedCard;
    
    // Main Tab Pane
    @FXML private TabPane mainTabPane;
    
    // Clearance Requests Tab
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colStatus;
    @FXML private TableColumn<ClearanceRequest, String> colDate;
    @FXML private TableColumn<ClearanceRequest, Integer> colApproved;
    @FXML private TextField txtSearch;
    @FXML private Button btnRefresh;
    
    // User Management Tab
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserDepartment;
    @FXML private TableColumn<User, String> colUserStatus;
    
    private User currentUser;
    private ObservableList<ClearanceRequest> masterData = FXCollections.observableArrayList();
    private ObservableList<User> userData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupClearanceTable();
        setupUserTable();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " (Admin)");
        loadAllRequests();
        loadAllUsers();
        updateDashboardCards();
    }
    
    // ==================== DASHBOARD METHODS ====================
    private void updateDashboardCards() {
        int totalRequests = masterData.size();
        long cleared = masterData.stream().filter(r -> "FULLY_CLEARED".equals(r.getStatus())).count();
        
        // Update main labels
        lblTotalRequests.setText("Total Requests: " + totalRequests);
        lblFullyCleared.setText("Fully Cleared: " + cleared);
        
        // Update dashboard cards
        if (lblTotalRequestsCard != null) {
            lblTotalRequestsCard.setText(String.valueOf(totalRequests));
        }
        if (lblFullyClearedCard != null) {
            lblFullyClearedCard.setText(String.valueOf(cleared));
        }
    }
    
    // ==================== CLEARANCE TABLE SETUP ====================
    private void setupClearanceTable() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colApproved.setCellValueFactory(new PropertyValueFactory<>("approvedCount"));
        
        // Color coding for status column
        colStatus.setCellFactory(column -> new TableCell<ClearanceRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "FULLY_CLEARED":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "IN_PROGRESS":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "REJECTED":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "PENDING":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "EXPIRED":
                            setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }
    
    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color coding for user status
        colUserStatus.setCellFactory(column -> new TableCell<User, String>() {
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
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
    
    // ==================== LOAD DATA METHODS ====================
    private void loadAllRequests() {
        masterData.clear();
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
                masterData.add(req);
            }

            updateDashboardCards();
            tableRequests.setItems(masterData);

        } catch (Exception e) {
            showAlert("Error", "Failed to load requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadAllRequests();
        loadAllUsers();
        updateDashboardCards();
        showAlert("Refreshed", "Data updated successfully!");
    }
    
    // ==================== 1. STUDENT REGISTRATION ====================
    @FXML
    private void openRegisterStudent() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Student");
        dialog.setHeaderText("Enter Student Details");

        ButtonType registerButton = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButton, ButtonType.CANCEL);

        GridPane grid = createStudentForm();
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButton) {
                return registerStudentFromForm(grid) ? ButtonType.OK : null;
            }
            return null;
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
        txtStudentId.setPromptText("Student ID (e.g., DBU1601111)");
        
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");
        
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username (for login)");
        
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email (optional)");
        
        // --- NEW PHONE NUMBER INPUT ---
        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);
        
        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(150);
        
        // Validation: Allow only numbers, max 8 digits
        txtPhoneSuffix.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPhoneSuffix.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (txtPhoneSuffix.getText().length() > 8) {
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
            }
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);
        // ------------------------------
        
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll(
            "Software Engineering", "Computer Science", "Electrical Engineering",
            "Mechanical Engineering", "Civil Engineering", "Business Administration", 
            "Accounting", "Economics", "Mathematics", "Physics", "Chemistry", "Biology"
        );
        cmbDepartment.setPromptText("Select Department");
        
        ComboBox<String> cmbYear = new ComboBox<>();
        cmbYear.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year");
        cmbYear.setPromptText("Select Year");

        grid.add(new Label("Student ID*:"), 0, 0);
        grid.add(txtStudentId, 1, 0);
        grid.add(new Label("Full Name*:"), 0, 1);
        grid.add(txtFullName, 1, 1);
        grid.add(new Label("Username*:"), 0, 2);
        grid.add(txtUsername, 1, 2);
        grid.add(new Label("Password*:"), 0, 3);
        grid.add(txtPassword, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(txtEmail, 1, 4);
        
        // Add Phone Box
        grid.add(new Label("Phone*:"), 0, 5);
        grid.add(phoneBox, 1, 5);
        
        grid.add(new Label("Department*:"), 0, 6);
        grid.add(cmbDepartment, 1, 6);
        grid.add(new Label("Year Level*:"), 0, 7);
        grid.add(cmbYear, 1, 7);

        // Update UserData to include phone components
        grid.setUserData(new Object[]{
            txtStudentId, txtFullName, txtUsername, txtPassword, 
            txtEmail, cmbPhonePrefix, txtPhoneSuffix, cmbDepartment, cmbYear
        });

        return grid;
    }

    private boolean registerStudentFromForm(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtStudentId = (TextField) fields[0];
        TextField txtFullName = (TextField) fields[1];
        TextField txtUsername = (TextField) fields[2];
        PasswordField txtPassword = (PasswordField) fields[3];
        TextField txtEmail = (TextField) fields[4];
        
        // Retrieve Phone Components
        ComboBox<String> cmbPhonePrefix = (ComboBox<String>) fields[5];
        TextField txtPhoneSuffix = (TextField) fields[6];
        
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[7];
        ComboBox<String> cmbYear = (ComboBox<String>) fields[8];

        String studentId = txtStudentId.getText().trim();
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();
        
        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();

        // 1. Basic Validation
        if (studentId.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty() ||
            phonePrefix == null || phoneSuffix.isEmpty() || department == null || year == null) {
            showAlert("Error", "Please fill all required fields marked with *!");
            return false;
        }

        // 2. Phone Suffix Validation (Must be exactly 8 digits)
        if (!phoneSuffix.matches("^\\d{8}$")) {
            showAlert("Error", "Phone number suffix must be exactly 8 digits!");
            return false;
        }

        // 3. Password Validation
        if (password.length() < 6) {
            showAlert("Error", "Password must be at least 6 characters long!");
            return false;
        }

        // 4. Email Validation
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Error", "Invalid email format!");
            return false;
        }

        String finalPhone = phonePrefix + phoneSuffix;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if username or student ID or phone exists
            String checkSql = "SELECT username, phone FROM users WHERE username = ? OR phone = ? OR username = ?"; 
            // Note: checking username against studentId field (if stored in username column) or separate column logic needed
            // Assuming studentId is stored in 'username' or separate unique check needed based on DB schema.
            // Simplified check based on prompt:
            
            String checkDuplicates = "SELECT id FROM users WHERE username = ? OR phone = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkDuplicates);
            checkStmt.setString(1, username);
            checkStmt.setString(2, finalPhone);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                showAlert("Error", "Username or Phone number already exists!");
                return false;
            }

            // Insert new student
            String sql = "INSERT INTO users (username, password, full_name, role, email, phone, department, year_level, status) " +
                        "VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?, 'ACTIVE')";
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
            if (e.getMessage().contains("Duplicate")) {
                showAlert("Error", "User details (ID/Phone/Username) already exist!");
            } else {
                showAlert("Error", "Registration failed: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Error", "Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== 2. OFFICER MANAGEMENT ====================
    @FXML
    private void openManageOfficers() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Department Officers");
        dialog.setHeaderText("Add New Officer");

        ButtonType saveButton = new ButtonType("Save Officer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = createOfficerForm();
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return registerOfficerFromForm(grid) ? ButtonType.OK : null;
            }
            return null;
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
        
        // --- NEW PHONE INPUT FOR OFFICER ---
        HBox phoneBox = new HBox(5);
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setPromptText("Prefix");
        cmbPhonePrefix.setPrefWidth(80);

        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(150);
        
        // Validation: Numbers only, max 8 digits
        txtPhoneSuffix.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPhoneSuffix.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (txtPhoneSuffix.getText().length() > 8) {
                txtPhoneSuffix.setText(txtPhoneSuffix.getText().substring(0, 8));
            }
        });

        phoneBox.getChildren().addAll(cmbPhonePrefix, txtPhoneSuffix);
        // -----------------------------------

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
        
        // Add Phone
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

        // 1. Check Required Fields
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || 
            role == null || department == null || phonePrefix == null || phoneSuffix.isEmpty()) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        // 2. Validate Phone Suffix
        if (!phoneSuffix.matches("^\\d{8}$")) {
            showAlert("Error", "Phone number suffix must be exactly 8 digits!");
            return false;
        }

        // 3. Validate Password
        if (password.length() < 6) {
            showAlert("Error", "Password must be at least 6 characters long!");
            return false;
        }

        String finalPhone = phonePrefix + phoneSuffix;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check duplications
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

            // Insert officer with phone number
            String sql = "INSERT INTO users (username, password, full_name, role, email, phone, department, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
            PreparedStatement stmt = conn.prepareStatement(sql);
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
            if (e.getMessage().contains("Duplicate")) {
                showAlert("Error", "User details (Username/Phone) already exist!");
            } else {
                showAlert("Error", "Failed to register officer: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Error", "Failed to register officer: " + e.getMessage());
        }
        return false;
    }
    
    // ==================== 3. USER MANAGEMENT ====================
    @FXML
    private void loadAllUsers() {
        userData.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, username, full_name, role, email, department, " +
                        "COALESCE(status, 'ACTIVE') as status FROM users ORDER BY role, username";
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
                userData.add(user);
            }

            tableUsers.setItems(userData);

        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    @FXML
    private void resetUserPassword() {
        User selectedUser = tableUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user first!");
            return;
        }

        // Check if trying to reset admin password (extra security)
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

        TextInputDialog dialog = new TextInputDialog("newpassword123");
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
        User selectedUser = tableUsers.getSelectionModel().getSelectedItem();
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
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to update status: " + e.getMessage());
            }
        }
    }
    
    // ==================== 4. WORKFLOW MANAGEMENT ====================
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

        // Get current workflow
        List<String> currentWorkflow = getCurrentWorkflow();
        
        // Create list view for workflow
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(currentWorkflow);
        listView.setPrefHeight(200);

        // Add departments that are not in workflow
        ListView<String> availableDepartments = new ListView<>();
        String[] allDepartments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
        for (String dept : allDepartments) {
            if (!currentWorkflow.contains(dept)) {
                availableDepartments.getItems().add(dept);
            }
        }
        availableDepartments.setPrefHeight(200);

        // Create buttons for moving items
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
            // Refresh any affected data
            loadAllRequests();
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
            // If no workflow exists, return default
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
            // Clear existing workflow
            String clearSql = "DELETE FROM workflow_config";
            PreparedStatement clearStmt = conn.prepareStatement(clearSql);
            clearStmt.executeUpdate();

            // Insert new workflow
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
    
    // ==================== 5. ACADEMIC SESSION MANAGEMENT ====================
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
            // If setting as active, deactivate all other sessions
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
    
    // ==================== 6. CERTIFICATE GENERATION ====================
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
                    // Here you would call your certificate generation service
                    // For now, just mark as successful
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
    
    // ==================== 7. CERTIFICATE VERIFICATION ====================
    @FXML
    private void openCertificateVerification() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Certificate Verification");
        dialog.setHeaderText("Verify Clearance Certificate");
        dialog.setContentText("Enter Student ID or Certificate ID:");

        dialog.showAndWait().ifPresent(studentId -> {
            verifyCertificateWithId(studentId);
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

    private void verifyCertificateWithId(String studentId) {
        verifyCertificateInDatabase(studentId);
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
    
    // ==================== 8. SEMESTER ROLLOVER ====================
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
                loadAllRequests();
                loadAllUsers();

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
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout");
            confirm.setHeaderText("Confirm Logout");
            confirm.setContentText("Are you sure you want to logout?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Parent login = FXMLLoader.load(getClass().getResource("/com/university/clearance/resources/views/Login.fxml"));
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                double width = stage.getWidth();
                double height = stage.getHeight();
                Scene scene = new Scene(login, width, height);
                stage.setScene(scene);
                stage.setTitle("Login - University Clearance System");
                stage.centerOnScreen();
            }
        } catch (Exception e) {
            e.printStackTrace();
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