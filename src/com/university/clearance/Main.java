package com.university.clearance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	
	static {
        // Suppress FXML version warnings
        System.setProperty("javafx.fxml.version", "21.0.8");
    }
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/views/Login.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        stage.setScene(scene);
        stage.setTitle("University Clearance Management System");
        stage.centerOnScreen();
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}