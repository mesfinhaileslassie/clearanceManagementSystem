package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterStudentController {

    @FXML private TextField txtStudentId, txtFullName, txtUsername, txtEmail, txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbDepartment, cmbYear;
    @FXML private Label lblMessage;

    @FXML
    private void registerStudent() {
        String studentId = txtStudentId.getText().trim();
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String department = cmbDepartment.getValue();
        String year = cmbYear.getValue();

        if (studentId.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty() ||
            department == null || year == null) {
            showMessage("Please fill all required fields!", "red");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if username or student ID exists
            String check = "SELECT username FROM users WHERE username = ? OR username = ?";
            PreparedStatement psCheck = conn.prepareStatement(check);
            psCheck.setString(1, username);
            psCheck.setString(2, studentId);
            if (psCheck.executeQuery().next()) {
                showMessage("Username or Student ID already exists!", "red");
                return;
            }

            // Insert new student
            String sql = "INSERT INTO users (username, password, full_name, role, email, phone, department, year_level) " +
                         "VALUES (?, ?, ?, 'STUDENT', ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);  // In real app: hash this!
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, department);
            ps.setString(7, year);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                showMessage("Student registered successfully!\nUsername: " + username + "\nPassword: " + password, "green");
                clearFields();
            }

        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) lblMessage.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String text, String color) {
        lblMessage.setText(text);
        lblMessage.setStyle("-fx-text-fill: " + color + ";");
    }

    private void clearFields() {
        txtStudentId.clear(); txtFullName.clear(); txtUsername.clear();
        txtPassword.clear(); txtEmail.clear(); txtPhone.clear();
        cmbDepartment.getSelectionModel().clearSelection();
        cmbYear.getSelectionModel().clearSelection();
    }
}