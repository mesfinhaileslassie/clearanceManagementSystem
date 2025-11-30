package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.ApprovalDetail;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class ClearanceStatusController {

    @FXML private TableView<ApprovalDetail> tableApprovals;
    @FXML private TableColumn<ApprovalDetail, String> colDepartment;
    @FXML private TableColumn<ApprovalDetail, String> colOfficer;
    @FXML private TableColumn<ApprovalDetail, String> colStatus;
    @FXML private TableColumn<ApprovalDetail, String> colRemarks;

    @FXML private Label lblStudentInfo;
    @FXML private Label lblOverallStatus;
    @FXML private Label lblMessage;
    @FXML private Button btnPrint;

    private User currentUser;
    private int currentRequestId = -1;

    @FXML
    private void initialize() {
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colOfficer.setCellValueFactory(cellData -> cellData.getValue().officerNameProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colRemarks.setCellValueFactory(cellData -> cellData.getValue().remarksProperty());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblStudentInfo.setText("Student: " + user.getFullName() + " (" + user.getUsername() + ")");
        loadLatestRequest();
    }

    @FXML
    private void refreshStatus() {
        loadLatestRequest();
    }

    private void loadLatestRequest() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            String sql = """
                SELECT id, status FROM clearance_requests 
                WHERE student_id = ? 
                ORDER BY request_date DESC LIMIT 1
                """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                lblOverallStatus.setText("No clearance request found.");
                lblOverallStatus.setStyle("-fx-text-fill: orange;");
                tableApprovals.setItems(FXCollections.observableArrayList());
                btnPrint.setDisable(true);
                return;
            }

            currentRequestId = rs.getInt("id");
            String status = rs.getString("status");
            String display = status != null ? status.replace("_", " ") : "IN PROGRESS";

            lblOverallStatus.setText("Overall Status: " + display);
            lblOverallStatus.setStyle("-fx-text-fill: " + 
                ("FULLY_CLEARED".equals(status) ? "green" : "orange") + "; -fx-font-size: 24px; -fx-font-weight: bold;");

            btnPrint.setDisable(!"FULLY_CLEARED".equals(status));

            loadApprovalDetails(conn);

        } catch (Exception e) {
            lblMessage.setText("Error: " + e.getMessage());
        }
    }

    private void loadApprovalDetails(Connection conn) throws SQLException {
        ObservableList<ApprovalDetail> data = FXCollections.observableArrayList();

        String sql = """
            SELECT 
                ca.officer_role,
                u.full_name AS officer_name,
                ca.status,
                ca.remarks
            FROM clearance_approvals ca
            LEFT JOIN users u ON ca.officer_id = u.id
            WHERE ca.request_id = ?
            ORDER BY FIELD(ca.officer_role, 'LIBRARIAN','CAFETERIA','DORMITORY','ASSOCIATION','REGISTRAR','DEPARTMENT_HEAD')
            """;

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, currentRequestId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String role = rs.getString("officer_role");
            String officer = rs.getString("officer_name");
            String stat = rs.getString("status");
            String remarks = rs.getString("remarks");

            String displayStatus = stat == null ? "Pending" :
                                 stat.equals("APPROVED") ? "Approved" : "Rejected";

            data.add(new ApprovalDetail(
                formatRole(role),
                officer != null ? officer : "Not Assigned",
                displayStatus,
                remarks != null ? remarks : "-"
            ));
        }

        tableApprovals.setItems(data);
        lblMessage.setText("Updated: " + java.time.LocalTime.now().toString().substring(0,8));
        lblMessage.setStyle("-fx-text-fill: green;");
    }

    @FXML
    private void printCertificate() {
        lblMessage.setText("PDF Certificate Ready! Type: SEND PDF NOW!");
    }

    private String formatRole(String role) {
        return switch (role) {
            case "LIBRARIAN" -> "Library";
            case "CAFETERIA" -> "Cafeteria";
            case "DORMITORY" -> "Dormitory";
            case "ASSOCIATION" -> "Student Association";
            case "REGISTRAR" -> "Registrar Office";
            case "DEPARTMENT_HEAD" -> "Department Head";
            default -> role;
        };
    }
}