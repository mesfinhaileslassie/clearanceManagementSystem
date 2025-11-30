package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.PendingRequest;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalTime;

public class OfficerClearanceController {

    @FXML private TableView<PendingRequest> tableRequests;
    @FXML private TableColumn<PendingRequest, String> colStudentId;
    @FXML private TableColumn<PendingRequest, String> colFullName;
    @FXML private TableColumn<PendingRequest, String> colDepartment;
    @FXML private TableColumn<PendingRequest, String> colDate;
    @FXML private TableColumn<PendingRequest, String> colMyStatus;

    @FXML private Label lblWelcome;
    @FXML private Label lblMessage;

    private User currentUser;

    @FXML
    private void initialize() {
        colStudentId.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        colFullName.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colMyStatus.setCellValueFactory(cellData -> cellData.getValue().myStatusProperty());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " — " +
                user.getRole().replace("_", " ").toUpperCase());
        refreshData();
    }

    @FXML
    private void refreshData() {
        ObservableList<PendingRequest> list = FXCollections.observableArrayList();

        String sql = """
            SELECT 
                cr.id AS request_id,
                cr.student_id,
                u.full_name,
                u.department,
                DATE(cr.request_date) AS req_date,
                ca.status AS my_status
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            LEFT JOIN clearance_approvals ca 
                ON cr.id = ca.request_id AND ca.officer_role = ?
            WHERE cr.status NOT IN ('FULLY_CLEARED', 'REJECTED', 'EXPIRED')  -- Only active requests
            AND (ca.status IS NULL OR ca.status = 'PENDING')                 -- Only pending approvals for this officer
            ORDER BY cr.request_date ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, currentUser.getRole());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String status = rs.getString("my_status");
                String displayStatus = status == null ? "Pending Your Approval" :
                                     status.equals("APPROVED") ? "Approved" : "Rejected";

                list.add(new PendingRequest(
                    rs.getInt("request_id"),
                    rs.getString("student_id"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("req_date"),
                    displayStatus
                ));
            }

            tableRequests.setItems(list);
            lblMessage.setText("Updated: " + LocalTime.now().toString().substring(0,8) +
                             " | Pending for you: " + list.size());
            lblMessage.setStyle("-fx-text-fill: green;");

        } catch (Exception e) {
            lblMessage.setText("Error loading data: " + e.getMessage());
            lblMessage.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
    @FXML
    private void approveSelected() {
        PendingRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request first!");
            return;
        }
        saveApproval(selected.getRequestId(), "APPROVED");
        showSuccess("APPROVED: " + selected.getFullName());
        refreshData();
    }

    @FXML
    private void rejectSelected() {
        PendingRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request first!");
            return;
        }
        saveApproval(selected.getRequestId(), "REJECTED");
        showError("REJECTED: " + selected.getFullName());
        refreshData();
    }

 // In OfficerClearanceController.java, update the saveApproval method:
    private void saveApproval(int requestId, String action) {
        String sql = """
            INSERT INTO clearance_approvals 
                (request_id, officer_id, officer_role, status, approval_date) 
            VALUES (?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE 
                status = VALUES(status),
                approval_date = NOW(),
                officer_id = VALUES(officer_id)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, currentUser.getId());
            ps.setString(3, currentUser.getRole());
            ps.setString(4, action);

            ps.executeUpdate();

            // Update overall request status
            updateRequestOverallStatus(conn, requestId);

        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateRequestOverallStatus(Connection conn, int requestId) {
        try {
            // Count pending approvals (NULL status) AND rejected approvals
            String countSql = """
                SELECT 
                    SUM(CASE WHEN status IS NULL THEN 1 ELSE 0 END) as pending_count,
                    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count,
                    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved_count,
                    COUNT(*) as total_departments
                FROM clearance_approvals 
                WHERE request_id = ?
                """;
            
            PreparedStatement ps = conn.prepareStatement(countSql);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int pendingCount = rs.getInt("pending_count");
                int rejectedCount = rs.getInt("rejected_count");
                int approvedCount = rs.getInt("approved_count");
                int totalDepartments = rs.getInt("total_departments");
                
                String newStatus;
                
                if (rejectedCount > 0) {
                    newStatus = "REJECTED"; // Any rejection fails the entire clearance
                } else if (pendingCount == 0 && approvedCount == totalDepartments) {
                    newStatus = "FULLY_CLEARED"; // All departments approved
                } else if (approvedCount > 0) {
                    newStatus = "IN_PROGRESS"; // Some approvals, still waiting for others
                } else {
                    newStatus = "PENDING"; // No approvals yet
                }

                String updateSql = "UPDATE clearance_requests SET status = ? WHERE id = ?";
                PreparedStatement ps2 = conn.prepareStatement(updateSql);
                ps2.setString(1, newStatus);
                ps2.setInt(2, requestId);
                ps2.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }
}