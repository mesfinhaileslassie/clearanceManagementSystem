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
        
        cmbRole.setStyle("-fx-prompt-text-fill: white; -fx-font-size: 14px;");
        
        // Show/Hide Password logic
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        txtPasswordVisible.visibleProperty().bind(chkShowPassword.selectedProperty());
        txtPasswordVisible.managedProperty().bind(chkShowPassword.selectedProperty());
        txtPassword.visibleProperty().bind(chkShowPassword.selectedProperty().not());
        txtPassword.managedProperty().bind(chkShowPassword.selectedProperty().not());
        txtPasswordVisible.setPromptText(txtPassword.getPromptText());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
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

                // Load the appropriate dashboard directly
                loadRoleDashboard(currentUser);
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

    private void loadRoleDashboard(User user) {
        try {
            String role = user.getRole();
            String fxmlPath;
            String title;

            // Determine which dashboard to load
            switch (role) {
                case "ADMIN":
                    fxmlPath = "/com/university/clearance/resources/views/AdminDashboard.fxml";
                    title = "Admin Dashboard";
                    break;
                case "STUDENT":
                    fxmlPath = "/com/university/clearance/resources/views/StudentDashboard.fxml";
                    title = "Student Dashboard";
                    break;
                case "LIBRARIAN":
                    fxmlPath = "/com/university/clearance/resources/views/LibrarianDashboard.fxml";
                    title = "Library Clearance Dashboard";
                    break;
                case "CAFETERIA":
                    fxmlPath = "/com/university/clearance/resources/views/CafeteriaDashboard.fxml";
                    title = "Cafeteria Clearance Dashboard";
                    break;
                case "DORMITORY":
                    fxmlPath = "/com/university/clearance/resources/views/DormitoryDashboard.fxml";
                    title = "Dormitory Clearance Dashboard";
                    break;
                case "REGISTRAR":
                    fxmlPath = "/com/university/clearance/resources/views/RegistrarDashboard.fxml";
                    title = "Registrar Clearance Dashboard";
                    break;
                case "DEPARTMENT_HEAD":
                    fxmlPath = "/com/university/clearance/resources/views/DepartmentHeadDashboard.fxml";
                    title = "Department Head Dashboard";
                    break;
                default:
                    showError("Unsupported user role: " + role);
                    return;
            }

            System.out.println("Loading dashboard: " + title);
            System.out.println("FXML Path: " + fxmlPath);

            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("❌ FXML not found: " + fxmlPath);
                showError("Dashboard file not found for role: " + role);
                return;
            }

            Parent root = loader.load();

            // Set current user on the controller
            Object controller = loader.getController();
            if (controller != null) {
                // Try to call setCurrentUser method (common for most dashboards)
                try {
                    java.lang.reflect.Method method = controller.getClass()
                        .getMethod("setCurrentUser", User.class);
                    method.invoke(controller, user);
                    System.out.println("✓ User set on controller: " + controller.getClass().getSimpleName());
                } catch (NoSuchMethodException e) {
                    System.out.println("⚠️ Controller doesn't have setCurrentUser method");
                    // Try initUser method (for DashboardController compatibility)
                    try {
                        java.lang.reflect.Method method = controller.getClass()
                            .getMethod("initUser", User.class);
                        method.invoke(controller, user);
                        System.out.println("✓ User set via initUser method");
                    } catch (NoSuchMethodException e2) {
                        System.out.println("⚠️ Controller doesn't have initUser method either");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error setting user: " + e.getMessage());
                }
            } else {
                System.err.println("❌ Controller is null!");
            }

            // Switch to the dashboard scene
            Stage stage = (Stage) loginbtn.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle(title + " - University Clearance System");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✓ Dashboard loaded successfully!");

        } catch (Exception e) {
            System.err.println("❌ Failed to load dashboard: " + e.getMessage());
            e.printStackTrace();
            showError("Cannot load dashboard: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void clearForm() {
        txtUsername.clear();
        txtPassword.clear();
        cmbRole.setValue(null);
        lblMessage.setText("");
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}