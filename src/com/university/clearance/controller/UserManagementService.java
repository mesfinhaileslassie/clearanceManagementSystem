package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.utils.PhoneInputField;
import com.university.clearance.utils.ValidationHelper;
import com.university.clearance.utils.ValidationHelper.ValidationResult;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class UserManagementService {
    
    private User currentUser;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void openRegisterStudent(User currentUser, Runnable onSuccess) {
        this.currentUser = currentUser;
        
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
        
        if (result.isPresent() && result.get() == registerButton && onSuccess != null) {
            onSuccess.run();
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
                    javafx.application.Platform.runLater(() -> {
                        txtStudentId.setText("DBU" + newVal.replaceAll("[^\\d]", ""));
                        txtStudentId.positionCaret(txtStudentId.getText().length());
                    });
                }
                
                if (newVal.startsWith("DBU") && newVal.length() > 10) {
                    javafx.application.Platform.runLater(() -> {
                        txtStudentId.setText(newVal.substring(0, 10));
                        txtStudentId.positionCaret(txtStudentId.getText().length());
                    });
                }
                
                if (newVal.startsWith("DBU") && newVal.length() > 3) {
                    String afterDBU = newVal.substring(3);
                    if (!afterDBU.matches("\\d*")) {
                        javafx.application.Platform.runLater(() -> {
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
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            showAlert("Error", "Database connection failed: " + e.getMessage());
            return false;
        }
    }
    
    public void openManageOfficers(User currentUser, Runnable onSuccess) {
        this.currentUser = currentUser;
        
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
        
        if (result.isPresent() && result.get() == saveButton && onSuccess != null) {
            onSuccess.run();
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
        
        Label lblFullNameValidation = (Label) components[7];
        Label lblUsernameValidation = (Label) components[8];
        Label lblPasswordValidation = (Label) components[9];
        Label lblEmailValidation = (Label) components[10];
        Label lblPhoneValidation = (Label) components[11];
        Label lblRoleValidation = (Label) components[12];
        Label lblDeptValidation = (Label) components[13];
        
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
        
        Label[] validationLabels = new Label[]{
            (Label) components[7],
            (Label) components[8],
            (Label) components[9],
            (Label) components[10],
            (Label) components[11],
            (Label) components[12],
            (Label) components[13]
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
            return false;
        }
    }
    
    public void resetUserPassword(User selectedUser) {
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
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to reset password: " + e.getMessage());
            }
        });
    }

    public void toggleUserStatus(User selectedUser) {
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
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to update status: " + e.getMessage());
            }
        }
    }
    
    public void handleDeleteUser(User user, User currentUser, Runnable onSuccess) {
        if (user == null) {
            showAlert("Error", "Please select a user to delete!");
            return;
        }
        
        if ("admin".equals(user.getUsername())) {
            showAlert("Error", "Cannot delete the admin user!");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Are you sure you want to permanently delete this user?");
        confirm.setContentText("User: " + user.getFullName() + 
                             "\nUsername: " + user.getUsername() + 
                             "\nRole: " + user.getRole() +
                             "\n\n⚠️ This action cannot be undone!");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUserFromDatabase(user, currentUser);
            if (onSuccess != null) {
                onSuccess.run();
            }
        }
    }
    
    private void deleteUserFromDatabase(User user, User currentUser) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
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
                
                String deleteUserSql = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteUserSql);
                deleteStmt.setInt(1, user.getId());
                int rowsAffected = deleteStmt.executeUpdate();
                
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
                    showAlert("Success", "✅ User deleted successfully!");
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
        }
    }
    
    public void allowStudentReapply(User student) {
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
        }
    }
    
    public void viewStudentDetails(User student) {
        // Student details viewing logic (simplified for brevity)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Details");
        alert.setHeaderText("Student Information: " + student.getFullName());
        alert.setContentText("Name: " + student.getFullName() + "\n" +
                           "Student ID: " + student.getUsername() + "\n" +
                           "Department: " + student.getDepartment() + "\n" +
                           "Status: " + student.getClearanceStatus());
        alert.showAndWait();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}