package com.university.clearance.controller;

import java.io.File;
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
        // Set tab closing policy programmatically
        if (tabPane != null) {
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        }
    }
    
    
    
    
    @FXML
    private void refreshDashboard() {
        System.out.println("Refresh Dashboard clicked");
        // Add your refresh logic here
        // For example: refreshWelcomeTab();
    }

    @FXML
    private void showSystemInfo() {
        System.out.println("Show System Info clicked");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Information");
        alert.setHeaderText("University Clearance System");
        alert.setContentText("Version 2.0\n\nDeveloped for University Clearance Management\n© 2025");
        alert.showAndWait();
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
            case "LIBRARIAN" -> {
                title = "Library Clearance Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/LibrarianDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "CAFETERIA" -> {
                title = "Cafeteria Clearance Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/CafeteriaDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "DORMITORY" -> {
                title = "Dormitory Clearance Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/DormitoryDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "REGISTRAR" -> {
                title = "Registrar Clearance Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/RegistrarDashboard.fxml";
                addTab(title, fxmlPath);
            }
            case "DEPARTMENT_HEAD" -> {
                title = "Department Head Dashboard";
                fxmlPath = "/com/university/clearance/resources/views/DepartmentHeadDashboard.fxml";
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
            System.out.println("=== DEBUG: Loading FXML ===");
            System.out.println("Path: " + fxmlPath);
            
            // Check if file exists
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("❌ ERROR: FXML not found at: " + fxmlPath);
                
                // Check if file exists in src folder
                String projectPath = System.getProperty("user.dir");
                String srcPath = projectPath + "/src" + fxmlPath;
                System.err.println("Looking in: " + srcPath);
                java.io.File file = new java.io.File(srcPath);
                System.err.println("File exists in src: " + file.exists());
                if (file.exists()) {
                    System.err.println("File size: " + file.length() + " bytes");
                }
                
                showAlert("Error", "Dashboard file not found: " + fxmlPath);
                return;
            }
            
            System.out.println("✅ FXML URL found: " + url);
            
            // Read and display first few lines of FXML to check syntax
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    java.nio.file.Paths.get(java.net.URI.create(url.toString().replace(" ", "%20")))
                );
                
                System.out.println("✅ FXML file read successfully (" + lines.size() + " lines)");
                
                // Display lines around the error (line 105)
                System.out.println("\n=== FXML CONTENT AROUND LINE 105 ===");
                int start = Math.max(0, 100);
                int end = Math.min(lines.size() - 1, 110);
                
                for (int i = start; i <= end; i++) {
                    String lineNum = String.format("%4d", i + 1);
                    String marker = (i == 104) ? " >>> ERROR HERE >>> " : " : ";
                    System.out.println(lineNum + marker + lines.get(i));
                }
                
            } catch (Exception readError) {
                System.err.println("⚠️  Cannot read FXML file content: " + readError.getMessage());
            }
            
            System.out.println("\n=== ATTEMPTING TO LOAD FXML ===");
            FXMLLoader loader = new FXMLLoader(url);
            
            // Try to load
            javafx.scene.Parent content = loader.load();
            System.out.println("✅ SUCCESS: FXML loaded!");
            
            // Set the current user for the loaded controller
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("✅ Controller found: " + controller.getClass().getName());
                try {
                    java.lang.reflect.Method method = controller.getClass()
                        .getMethod("setCurrentUser", User.class);
                    method.invoke(controller, currentUser);
                    System.out.println("✅ User set for controller");
                } catch (NoSuchMethodException e) {
                    System.out.println("⚠️  Controller doesn't have setCurrentUser method");
                } catch (Exception e) {
                    System.err.println("❌ Error setting user: " + e.getMessage());
                }
            } else {
                System.err.println("❌ Controller is NULL!");
            }

            Tab tab = new Tab(title, content);
            tab.setClosable(true);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            
            System.out.println("✅ Tab created and added successfully!");

        } catch (Exception e) {
            System.err.println("\n❌❌❌ FXML LOADING FAILED ❌❌❌");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            
            // Print detailed stack trace
            System.err.println("\n=== FULL STACK TRACE ===");
            e.printStackTrace();
            
            // Look for line number in error message
            if (e.getMessage() != null) {
                System.err.println("\n=== PARSING ERROR MESSAGE ===");
                String message = e.getMessage();
                
                // Check for line number pattern
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("line (\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    int lineNumber = Integer.parseInt(matcher.group(1));
                    System.err.println("Error at line: " + lineNumber);
                    
                    // Try to read and display that line
                    try {
                        java.net.URL url = getClass().getResource(fxmlPath);
                        if (url != null) {
                            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                                java.nio.file.Paths.get(java.net.URI.create(url.toString().replace(" ", "%20")))
                            );
                            if (lineNumber <= lines.size()) {
                                System.err.println("Line " + lineNumber + " content: " + lines.get(lineNumber - 1));
                                
                                // Show context around error
                                int start = Math.max(0, lineNumber - 3);
                                int end = Math.min(lines.size() - 1, lineNumber + 2);
                                System.err.println("\nContext (lines " + start + "-" + end + "):");
                                for (int i = start; i <= end; i++) {
                                    String marker = (i == lineNumber - 1) ? " >>> " : "     ";
                                    System.err.println(String.format("%4d", i + 1) + marker + lines.get(i));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not read line " + lineNumber + ": " + ex.getMessage());
                    }
                }
            }
            
            showAlert("FXML Error", "Cannot load dashboard. Check console for details.");
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