package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    // NEW FXML COMPONENTS
    @FXML private TextField txtPasswordVisible;
    @FXML private CheckBox chkShowPassword;
    
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label lblMessage;
    @FXML private Button loginbtn;

    private static User currentUser;

    @FXML
    private void initialize() {
        cmbRole.setItems(javafx.collections.FXCollections.observableArrayList(
            "STUDENT", "LIBRARIAN", "CAFETERIA", "DORMITORY",
             "REGISTRAR", "DEPARTMENT_HEAD", "ADMIN"
        ));
        
        // --- NEW LOGIC FOR SHOW/HIDE PASSWORD ---

        // 1. Synchronize text content between the two fields
        // Typing in one field updates the other instantly.
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        // 2. Bind visibility and management based on checkbox state
        // When selected, hide PasswordField and show TextField
        txtPasswordVisible.visibleProperty().bind(chkShowPassword.selectedProperty());
        txtPasswordVisible.managedProperty().bind(chkShowPassword.selectedProperty());
        
        // When selected, show PasswordField and hide TextField
        txtPassword.visibleProperty().bind(chkShowPassword.selectedProperty().not());
        txtPassword.managedProperty().bind(chkShowPassword.selectedProperty().not());

        // 3. Ensure the visible field has the same prompt text
        txtPasswordVisible.setPromptText(txtPassword.getPromptText());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        
        // Get the password from the synchronized text property
        String password = txtPassword.getText();
        
        String role = cmbRole.getValue();

        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + username + ", Role: " + role);

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError("All fields are required!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                showError("Database connection failed!");
                return;
            }

            String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ? AND status = 'ACTIVE'";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setString(3, role);
            
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                System.out.println("✓ LOGIN SUCCESSFUL for: " + rs.getString("full_name"));
                
                currentUser = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("email"),
                    rs.getString("department")
                );

                // Direct navigation based on role
                if ("ADMIN".equals(role)) {
                    loadAdminDashboard();
                } else {
                    loadDashboard();
                }

                clearForm();

            } else {
                System.out.println("✗ LOGIN FAILED - Invalid credentials");
                showError("Invalid username, password, or role!");
            }

        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            showError("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAdminDashboard() {
        try {
            System.out.println("Loading Admin Dashboard...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/clearance/resources/views/AdminDashboard.fxml"));
            Parent root = loader.load();
            
            AdminDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Stage stage = (Stage) loginbtn.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard - University Clearance System");
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("✓ Admin Dashboard loaded successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load Admin Dashboard: " + e.getMessage());
            e.printStackTrace();
            showError("Cannot load admin interface: " + e.getMessage());
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/clearance/resources/views/Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardCtrl = loader.getController();
            dashboardCtrl.initUser(currentUser);

            Stage stage = (Stage) loginbtn.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle("University Clearance System - " + currentUser.getFullName());
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            showError("Cannot load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void clearForm() {
        txtUsername.clear();
        txtPassword.clear();
        // Clearing txtPassword also clears txtPasswordVisible due to bidirectional binding
        cmbRole.setValue(null);
        lblMessage.setText("");
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}