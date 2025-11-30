package com.university.clearance.controller;
import java.io.IOException;
import com.university.clearance.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class DashboardController {

    @FXML 
    private TabPane tabPane;
    private User currentUser;

    @FXML
    private void initialize() {
        // Wait for user to be initialized
    }
    
    
    
    
    
    

    public void initUser(User user) {
        this.currentUser = user;
        System.out.println("Dashboard initialized for: " + user.getFullName() + " Role: " + user.getRole());
        openDashboardForRole(user);
    }

    private void openDashboardForRole(User user) {
        String role = user.getRole();
        String fxmlPath;
        String title;

        switch (role) {
            case "ADMIN" -> {
                title = "Admin Panel";
                fxmlPath = "/com/university/clearance/resources/views/AdminDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "STUDENT" -> {
                title = "Student Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/StudentDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "LIBRARIAN", "CAFETERIA", "DORMITORY", "ASSOCIATION", "REGISTRAR", "DEPARTMENT_HEAD" -> {
                title = role.replace("_", " ") + " Clearance";
                fxmlPath = "/com/university/clearance/resources/views/OfficerClearance.fxml";
                addTab(title, fxmlPath);
            }
            default -> {
                showAlert("Error", "Unknown user role: " + role);
                return;
            }
        }
    }

    public void addTab(String title, String fxmlPath) {
        try {
            System.out.println("Loading FXML: " + fxmlPath);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Set the current user for the loaded controller
            Object controller = loader.getController();
            if (controller instanceof AdminDashboardController adminCtrl) {
                adminCtrl.setCurrentUser(currentUser);
            } else if (controller instanceof StudentDashboardController studentCtrl) {
                studentCtrl.setCurrentUser(currentUser);
            } else if (controller instanceof OfficerClearanceController officerCtrl) {
                officerCtrl.setCurrentUser(currentUser);
            }

            Tab tab = new Tab(title, content);
            tab.setClosable(true);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
            showAlert("Error", "Cannot load dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/com/university/clearance/resources/views/Login.fxml"));
            Scene scene = tabPane.getScene();
            Stage stage = (Stage) scene.getWindow();
            scene.setRoot(login);
            stage.setTitle("Login - University Clearance System");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}