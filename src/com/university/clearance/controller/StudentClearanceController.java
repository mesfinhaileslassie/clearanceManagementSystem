package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import com.university.clearance.service.PDFCertificateService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
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
            String[] officers = {"LIBRARIAN","CAFETERIA","DORMITORY","REGISTRAR","DEPARTMENT_HEAD"};
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
        if (currentUser == null || requestId == -1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Certificate Not Available");
            alert.setHeaderText("No Completed Clearance Found");
            alert.setContentText("You must have a fully approved clearance request before generating a certificate.");
            alert.showAndWait();
            return;
        }

        // Ensure the current request is fully cleared
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT status FROM clearance_requests WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Certificate Error");
                alert.setHeaderText("Clearance Request Not Found");
                alert.setContentText("We could not find your latest clearance request. Please try again.");
                alert.showAndWait();
                return;
            }

            String status = rs.getString("status");
            if (!"FULLY_CLEARED".equals(status)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Not Fully Approved");
                alert.setHeaderText("Certificate Not Ready");
                alert.setContentText("Your clearance is not fully approved yet. Current status: " + status +
                                     "\nAll departments must approve before a certificate can be generated.");
                alert.showAndWait();
                return;
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Unable to Verify Clearance");
            alert.setContentText("An error occurred while verifying your clearance status: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
            return;
        }

        // Generate professional PDF certificate
        try {
            PDFCertificateService pdfService = new PDFCertificateService();
            String filePath = pdfService.generatePDFCertificate(currentUser.getId(), requestId);

            if (filePath == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Generation Failed");
                alert.setHeaderText("Certificate Generation Failed");
                alert.setContentText("The system could not generate your clearance certificate. Please try again later.");
                alert.showAndWait();
                return;
            }

            File file = new File(filePath);
            if (file.exists()) {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Clearance Certificate Generated");
                success.setHeaderText("Official Certificate Ready");
                success.setContentText("Your clearance certificate has been generated successfully.\n\nSaved to:\n" + filePath);
                success.showAndWait();

                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception openEx) {
                    // Opening the file is a convenience; failures here should not be fatal
                    openEx.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File Not Found");
                alert.setHeaderText("Certificate File Missing");
                alert.setContentText("The certificate was generated but the file could not be found at:\n" + filePath);
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Certificate Error");
            alert.setHeaderText("Unexpected Error");
            alert.setContentText("An unexpected error occurred while generating the certificate: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
