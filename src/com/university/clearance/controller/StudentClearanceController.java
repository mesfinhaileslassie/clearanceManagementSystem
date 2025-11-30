package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.*;

public class StudentClearanceController {

    @FXML private VBox vboxSubmit;
    @FXML private VBox vboxInProgress;
    @FXML private VBox vboxCleared;
    @FXML private Label lblMessage;
    @FXML private Button btnPrintCertificate;

    private User currentUser;
    private int requestId = -1;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        checkClearanceStatus();
    }

    private void checkClearanceStatus() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if request exists
            String sql = "SELECT id, status FROM clearance_requests WHERE student_id = ? ORDER BY request_date DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                // No request → Show submit button
                vboxSubmit.setVisible(true);
                vboxSubmit.setManaged(true);
            } else {
                requestId = rs.getInt("id");
                String status = rs.getString("status");

                if ("FULLY_CLEARED".equals(status)) {
                    vboxCleared.setVisible(true);
                    vboxCleared.setManaged(true);
                } else {
                    vboxInProgress.setVisible(true);
                    vboxInProgress.setManaged(true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Error checking status");
        }
    }

    @FXML
    private void submitRequest() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO clearance_requests (student_id) VALUES (?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, currentUser.getId());
            ps.executeUpdate();

            // Insert pending approvals for all officers
            String[] officers = {"LIBRARIAN","CAFETERIA","DORMITORY","ASSOCIATION","REGISTRAR","DEPARTMENT_HEAD"};
            String insertApproval = "INSERT INTO clearance_approvals (request_id, officer_role) VALUES (?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(insertApproval);

            ResultSet generated = ps.getGeneratedKeys();
            if (generated.next()) {
                requestId = generated.getInt(1);
                for (String role : officers) {
                    ps2.setInt(1, requestId);
                    ps2.setString(2, role);
                    ps2.addBatch();
                }
                ps2.executeBatch();
            }

            lblMessage.setText("Clearance request submitted successfully!");
            lblMessage.setStyle("-fx-text-fill: green;");
            checkClearanceStatus();  // Refresh UI

        } catch (Exception e) {
            lblMessage.setText("Failed to submit request!");
            e.printStackTrace();
        }
    }

    @FXML
    private void printCertificate() {
        // This will be implemented in the next message with PDF!
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print Certificate");
        alert.setHeaderText("Ready to Print!");
        alert.setContentText("Your official clearance certificate is ready!\nPDF Generation coming in next message!");
        alert.showAndWait();
    }
}