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
        txtStudentId.setPromptText("Student ID");
        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Full Name");
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");
        
        ComboBox<String> cmbPhonePrefix = new ComboBox<>();
        cmbPhonePrefix.getItems().addAll("09", "07");
        cmbPhonePrefix.setValue("09");
        
        TextField txtPhoneSuffix = new TextField();
        txtPhoneSuffix.setPromptText("12345678");
        txtPhoneSuffix.setPrefWidth(120);
        
        Label lblFullPhone = new Label();
        lblFullPhone.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        cmbPhonePrefix.setOnAction(e -> updateFullPhoneDisplay(cmbPhonePrefix, txtPhoneSuffix, lblFullPhone));
        txtPhoneSuffix.textProperty().addListener((observable, oldValue, newValue) -> 
            updateFullPhoneDisplay(cmbPhonePrefix, txtPhoneSuffix, lblFullPhone));
        
        HBox phoneBox = new HBox(5, cmbPhonePrefix, new Label("-"), txtPhoneSuffix);
        phoneBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll("Computer Science", "Electrical Engineering", "Mechanical Engineering", 
                                      "Civil Engineering", "Business Administration", "Mathematics");
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
        grid.add(new Label("Phone Number*:"), 0, 5);
        grid.add(phoneBox, 1, 5);
        grid.add(new Label("Full Phone:"), 0, 6);
        grid.add(lblFullPhone, 1, 6);
        grid.add(new Label("Department*:"), 0, 7);
        grid.add(cmbDepartment, 1, 7);
        grid.add(new Label("Year Level*:"), 0, 8);
        grid.add(cmbYear, 1, 8);

        grid.setUserData(new Object[]{txtStudentId, txtFullName, txtUsername, txtPassword, 
                                    txtEmail, cmbPhonePrefix, txtPhoneSuffix, cmbDepartment, cmbYear});

        return grid;
    }

    private void updateFullPhoneDisplay(ComboBox<String> cmbPhonePrefix, TextField txtPhoneSuffix, Label lblFullPhone) {
        String prefix = cmbPhonePrefix.getValue();
        String suffix = txtPhoneSuffix.getText().trim();
        
        if (prefix != null && !suffix.isEmpty()) {
            String cleanSuffix = suffix.replaceAll("\\D", "");
            if (cleanSuffix.length() == 8) {
                lblFullPhone.setText(prefix + cleanSuffix);
                lblFullPhone.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblFullPhone.setText(prefix + cleanSuffix + " (need " + (8 - cleanSuffix.length()) + " more digits)");
                lblFullPhone.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        } else if (prefix != null) {
            lblFullPhone.setText(prefix + "XXXXXXXX");
            lblFullPhone.setStyle("-fx-text-fill: #7f8c8d;");
        } else {
            lblFullPhone.setText("Select prefix and enter numbers");
            lblFullPhone.setStyle("-fx-text-fill: #7f8c8d;");
        }
    }

    private boolean registerStudentFromForm(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtStudentId = (TextField) fields[0];
        TextField txtFullName = (TextField) fields[1];
        TextField txtUsername = (TextField) fields[2];
        PasswordField txtPassword = (PasswordField) fields[3];
        TextField txtEmail = (TextField) fields[4];
        ComboBox<String> cmbPhonePrefix = (ComboBox<String>) fields[5];
        TextField txtPhoneSuffix = (TextField) fields[6];
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[7];
        ComboBox<String> cmbYear = (ComboBox<String>) fields[8];

        String studentId = txtStudentId.getText().trim();
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phonePrefix = cmbPhonePrefix.getValue();
        String phoneSuffix = txtPhoneSuffix.getText().trim();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();

        // Validation
        if (studentId.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty() ||
            phonePrefix == null || phoneSuffix.isEmpty() || department == null || year == null) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        String cleanPhoneSuffix = phoneSuffix.replaceAll("\\D", "");
        if (cleanPhoneSuffix.length() != 8) {
            showAlert("Error", "Phone number must be exactly 8 digits after the prefix!");
            return false;
        }

        String phone = phonePrefix + cleanPhoneSuffix;

        if (password.length() < 6) {
            showAlert("Error", "Password must be at least 6 characters long!");
            return false;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Error", "Invalid email format!");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT id FROM users WHERE username = ? OR username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, studentId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                showAlert("Error", "Username or Student ID already exists!");
                return false;
            }

            String checkPhoneSql = "SELECT id FROM users WHERE phone = ?";
            PreparedStatement checkPhoneStmt = conn.prepareStatement(checkPhoneSql);
            checkPhoneStmt.setString(1, phone);
            ResultSet checkPhoneRs = checkPhoneStmt.executeQuery();
            
            if (checkPhoneRs.next()) {
                showAlert("Error", "Phone number " + phone + " already exists!");
                return false;
            }

            String sql = "INSERT INTO users (username, password, full_name, role, email, phone, department, year_level) " +
                        "VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.setString(4, email.isEmpty() ? null : email);
            stmt.setString(5, phone);
            stmt.setString(6, department);
            stmt.setString(7, year);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("phone")) {
                showAlert("Error", "Phone number " + phone + " already exists!");
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
        TextField txtUsername = new TextField();
        PasswordField txtPassword = new PasswordField();
        TextField txtEmail = new TextField();
        ComboBox<String> cmbRole = new ComboBox<>();
        ComboBox<String> cmbDepartment = new ComboBox<>();

        cmbRole.getItems().addAll("LIBRARIAN", "CAFETERIA", "DORMITORY", "ASSOCIATION", "REGISTRAR", "DEPARTMENT_HEAD");
        cmbDepartment.getItems().addAll("Library", "Cafeteria", "Dormitory", "Student Association", 
                                      "Registrar Office", "Computer Science", "Electrical Engineering");

        grid.add(new Label("Full Name*:"), 0, 0);
        grid.add(txtFullName, 1, 0);
        grid.add(new Label("Username*:"), 0, 1);
        grid.add(txtUsername, 1, 1);
        grid.add(new Label("Password*:"), 0, 2);
        grid.add(txtPassword, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(txtEmail, 1, 3);
        grid.add(new Label("Role*:"), 0, 4);
        grid.add(cmbRole, 1, 4);
        grid.add(new Label("Department*:"), 0, 5);
        grid.add(cmbDepartment, 1, 5);

        grid.setUserData(new Object[]{txtFullName, txtUsername, txtPassword, txtEmail, cmbRole, cmbDepartment});

        return grid;
    }

    private boolean registerOfficerFromForm(GridPane grid) {
        Object[] fields = (Object[]) grid.getUserData();
        TextField txtFullName = (TextField) fields[0];
        TextField txtUsername = (TextField) fields[1];
        PasswordField txtPassword = (PasswordField) fields[2];
        TextField txtEmail = (TextField) fields[3];
        ComboBox<String> cmbRole = (ComboBox<String>) fields[4];
        ComboBox<String> cmbDepartment = (ComboBox<String>) fields[5];

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String role = cmbRole.getValue();
        String department = cmbDepartment.getValue();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || role == null || department == null) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            if (checkStmt.executeQuery().next()) {
                showAlert("Error", "Username already exists!");
                return false;
            }

            String sql = "INSERT INTO users (username, password, full_name, role, email, department) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullName);
            stmt.setString(4, role);
            stmt.setString(5, email);
            stmt.setString(6, department);

            int rows = stmt.executeUpdate();
            return rows > 0;

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

        TextInputDialog dialog = new TextInputDialog("newpassword123");
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset Password for: " + selectedUser.getFullName());
        dialog.setContentText("Enter new password:");

        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword.length() < 3) {
                showAlert("Error", "Password must be at least 3 characters!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE users SET password = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, newPassword);
                stmt.setInt(2, selectedUser.getId());
                
                if (stmt.executeUpdate() > 0) {
                    showAlert("Success", "Password reset successfully!");
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
        confirm.setContentText("User: " + selectedUser.getFullName());

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

        ButtonType saveButton = new ButtonType("Save Workflow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<ComboBox<String>> steps = new ArrayList<>();
        String[] roles = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "ASSOCIATION", "REGISTRAR", "DEPARTMENT_HEAD"};

        for (int i = 0; i < 6; i++) {
            ComboBox<String> stepComboBox = new ComboBox<>();
            stepComboBox.getItems().addAll(roles);
            stepComboBox.setValue(roles[i]);
            steps.add(stepComboBox);
            grid.add(new Label("Step " + (i + 1) + ":"), 0, i);
            grid.add(stepComboBox, 1, i);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String clearSql = "DELETE FROM workflow_config";
                    PreparedStatement clearStmt = conn.prepareStatement(clearSql);
                    clearStmt.executeUpdate();

                    String insertSql = "INSERT INTO workflow_config (role, sequence_order) VALUES (?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    
                    for (int i = 0; i < steps.size(); i++) {
                        ComboBox<String> step = steps.get(i);
                        if (step.getValue() != null) {
                            insertStmt.setString(1, step.getValue());
                            insertStmt.setInt(2, i + 1);
                            insertStmt.addBatch();
                        }
                    }
                    
                    insertStmt.executeBatch();
                    return "Workflow updated successfully!";
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            showAlert("Workflow Management", result);
        });
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
        txtSessionName.setPromptText("e.g., Fall 2024");
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
                                  "Period: " + startDate + " to " + endDate);
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
        cmbDepartment.getItems().addAll("All Departments", "Computer Science", "Electrical Engineering");

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
                "SELECT u.username, u.full_name, u.department, cr.request_date " +
                "FROM clearance_requests cr " +
                "JOIN users u ON cr.student_id = u.id " +
                "WHERE cr.status = 'FULLY_CLEARED' " +
                "AND DATE(cr.request_date) BETWEEN ? AND ? "
            );

            if (department != null && !"All Departments".equals(department)) {
                sql.append("AND u.department = ? ");
            }

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            
            if (department != null && !"All Departments".equals(department)) {
                ps.setString(3, department);
            }

            ResultSet rs = ps.executeQuery();

            StringBuilder report = new StringBuilder("CERTIFICATES GENERATED:\n\n");
            int count = 0;

            while (rs.next()) {
                count++;
                report.append("Certificate #").append(count).append("\n")
                      .append("Student: ").append(rs.getString("full_name")).append("\n")
                      .append("ID: ").append(rs.getString("username")).append("\n")
                      .append("Department: ").append(rs.getString("department")).append("\n")
                      .append("Date: ").append(rs.getDate("request_date")).append("\n")
                      .append("---\n");
            }

            if (count > 0) {
                showAlert("Certificates Generated", 
                         "Successfully generated " + count + " certificates!\n\n" + report.toString());
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
        dialog.setContentText("Enter Student ID:");

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
                              "Approvals: " + rs.getInt("approved_count") + "/6 departments\n\n" +
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
        confirm.setContentText("This will:\n" +
                             "• Archive completed clearances\n" +
                             "• Reset pending requests\n" +
                             "• Update student year levels\n" +
                             "• Create new academic session\n\n" +
                             "This action cannot be undone!\n\n" +
                             "Proceed with semester rollover?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performSemesterRollover();
        }
    }

    private void performSemesterRollover() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Archive completed requests
                String archiveSql = "INSERT INTO clearance_requests_archive " +
                                  "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                  "FROM clearance_requests WHERE status = 'FULLY_CLEARED'";
                PreparedStatement archiveStmt = conn.prepareStatement(archiveSql);
                int archived = archiveStmt.executeUpdate();

                // 2. Reset pending requests
                String resetSql = "UPDATE clearance_requests SET status = 'EXPIRED' " +
                                "WHERE status IN ('PENDING', 'IN_PROGRESS')";
                PreparedStatement resetStmt = conn.prepareStatement(resetSql);
                int reset = resetStmt.executeUpdate();

                // 3. Update student year levels
                String yearSql = "UPDATE users SET year_level = " +
                               "CASE " +
                               "WHEN year_level = '1st Year' THEN '2nd Year' " +
                               "WHEN year_level = '2nd Year' THEN '3rd Year' " +
                               "WHEN year_level = '3rd Year' THEN '4th Year' " +
                               "WHEN year_level = '4th Year' THEN '5th Year' " +
                               "ELSE year_level " +
                               "END " +
                               "WHERE role = 'STUDENT' AND status = 'ACTIVE'";
                PreparedStatement yearStmt = conn.prepareStatement(yearSql);
                int updated = yearStmt.executeUpdate();

                // 4. Create new session
                String sessionSql = "INSERT INTO academic_sessions (session_name, start_date, end_date, is_active) " +
                                  "VALUES (?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 6 MONTH), true)";
                PreparedStatement sessionStmt = conn.prepareStatement(sessionSql);
                sessionStmt.setString(1, "Semester " + (LocalDate.now().getMonthValue() <= 6 ? "Spring" : "Fall") + " " + LocalDate.now().getYear());
                sessionStmt.executeUpdate();

                // Deactivate old sessions
                String deactivateSql = "UPDATE academic_sessions SET is_active = false WHERE is_active = true";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();

                conn.commit();

                String report = "🎉 SEMESTER ROLLOVER COMPLETED!\n\n" +
                              "✓ Archived " + archived + " cleared requests\n" +
                              "✓ Reset " + reset + " pending requests\n" +
                              "✓ Updated " + updated + " student year levels\n" +
                              "✓ Created new academic session\n\n" +
                              "System is ready for the new semester!";

                showAlert("Rollover Successful", report);
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
        }
    }
    
    // ==================== UTILITY METHODS ====================
    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/com/university/clearance/resources/views/Login.fxml"));
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(login, 600, 400));
            stage.setTitle("Login - University Clearance System");
            stage.centerOnScreen();
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