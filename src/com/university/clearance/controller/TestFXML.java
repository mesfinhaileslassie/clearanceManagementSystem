package com.university.clearance.controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestFXML extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("=== TESTING FXML FILE ===");
        
        // Try to load the FXML directly
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/university/clearance/resources/views/DormitoryDashboard.fxml")
            );
            
            System.out.println("Loading FXML...");
            loader.load();
            System.out.println("✅ FXML loaded successfully!");
            
            // Show a simple window if it works
            Scene scene = new Scene(loader.getRoot(), 800, 600);
            stage.setScene(scene);
            stage.setTitle("FXML Test - Success!");
            stage.show();
            
        } catch (Exception e) {
            System.err.println("❌ FXML ERROR:");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            
            // Print the exact line where it failed
            if (e.getMessage() != null && e.getMessage().contains("line")) {
                System.err.println("\nCheck the line number mentioned above in your FXML file.");
            }
            
            e.printStackTrace();
            
            // Show error window
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                "FXML Error:\n" + e.getMessage() + 
                "\n\nCheck console for details."
            );
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            Scene errorScene = new Scene(errorLabel, 600, 200);
            stage.setScene(errorScene);
            stage.setTitle("FXML Test - ERROR!");
            stage.show();
        }
    }
}