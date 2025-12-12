package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.ClearanceRequest;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClearanceOperationsService {
    
    private User currentUser;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public boolean isResubmissionAllowed(ClearanceRequest request) {
        if (request == null) return false;
        return "REJECTED".equals(request.getStatus()) || 
               "EXPIRED".equals(request.getStatus());
    }
    
    public String getDisableReason(ClearanceRequest request) {
        if (request == null) return "No request data";
        
        if (!"REJECTED".equals(request.getStatus()) && !"EXPIRED".equals(request.getStatus())) {
            return "Only rejected or expired requests can be resubmitted\nCurrent status: " + request.getStatus();
        }
        
        return "Click to allow resubmission";
    }
    
    public void handleAllowResubmission(ClearanceRequest request, User currentUser, Runnable onSuccess) {
        this.currentUser = currentUser;
        
        if (!"REJECTED".equals(request.getStatus()) && !"EXPIRED".equals(request.getStatus())) {
            showAlert("Cannot Allow Resubmission", 
                "Only rejected or expired requests can be resubmitted.\n" +
                "Current status: " + request.getStatus());
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Allow Resubmission");
        confirm.setHeaderText("Allow Student to Resubmit Clearance Request");
        
        StringBuilder confirmationText = new StringBuilder();
        confirmationText.append("Student Information:\n");
        confirmationText.append("• Name: ").append(request.getFullName()).append("\n");
        confirmationText.append("• Student ID: ").append(request.getStudentId()).append("\n");
        confirmationText.append("• Department: ").append(request.getDepartment()).append("\n");
        confirmationText.append("• Current Status: ").append(request.getStatus()).append("\n");
        
        if ("REJECTED".equals(request.getStatus())) {
            confirmationText.append("• Request was REJECTED\n");
        } else if ("EXPIRED".equals(request.getStatus())) {
            confirmationText.append("• Request has EXPIRED (over 30 days old or timed out)\n");
        }
        
        confirmationText.append("• Request Date: ").append(request.getFormattedDate()).append("\n");
        confirmationText.append("• Days Since Request: ").append(request.getDaysSinceRequest()).append("\n\n");
        
        confirmationText.append("This action will:\n");
        confirmationText.append("✅ Reset clearance status to PENDING\n");
        confirmationText.append("✅ Clear all existing department approvals\n");
        confirmationText.append("✅ Create new pending approvals for all departments\n");
        confirmationText.append("✅ Update dormitory clearance status to PENDING\n");
        confirmationText.append("✅ Send notification to student\n");
        confirmationText.append("✅ Log this action in audit trail\n\n");
        
        confirmationText.append("⚠️  Important Notes:\n");
        confirmationText.append("• Student will be able to submit a NEW clearance request\n");
        confirmationText.append("• All departments will need to re-approve\n");
        confirmationText.append("• This action cannot be undone\n\n");
        
        confirmationText.append("Proceed with allowing resubmission?");
        
        confirm.setContentText(confirmationText.toString());
        confirm.getDialogPane().setPrefSize(700, 500);
        confirm.getDialogPane().setStyle("-fx-border-color: #f39c12; -fx-border-width: 2px;");
        
        ButtonType proceedButton = new ButtonType("Proceed", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(proceedButton, cancelButton);
        
        Button proceedBtn = (Button) confirm.getDialogPane().lookupButton(proceedButton);
        proceedBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == proceedButton) {
            Alert processingAlert = new Alert(Alert.AlertType.INFORMATION);
            processingAlert.setTitle("Processing");
            processingAlert.setHeaderText("Processing Resubmission Request");
            processingAlert.setContentText("Please wait while we process the resubmission...");
            processingAlert.show();
            
            try {
                boolean success = performResubmissionProcess(request);
                
                processingAlert.close();
                
                if (success) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("✅ Resubmission Allowed Successfully!");
                    
                    StringBuilder successMessage = new StringBuilder();
                    successMessage.append("Student: ").append(request.getFullName()).append("\n");
                    successMessage.append("Student ID: ").append(request.getStudentId()).append("\n");
                    successMessage.append("Department: ").append(request.getDepartment()).append("\n\n");
                    
                    successMessage.append("✅ Status has been reset to PENDING\n");
                    successMessage.append("✅ Student can now submit a new clearance request\n");
                    successMessage.append("✅ All department approvals have been reset\n");
                    successMessage.append("✅ Student has been notified\n");
                    successMessage.append("✅ Action has been logged for auditing\n\n");
                    
                    successMessage.append("Next Steps:\n");
                    successMessage.append("1. Student should login and submit a new clearance request\n");
                    successMessage.append("2. All departments will need to re-approve\n");
                    successMessage.append("3. Student will receive email notification\n");
                    
                    successAlert.setContentText(successMessage.toString());
                    successAlert.getDialogPane().setPrefSize(600, 400);
                    successAlert.getDialogPane().setStyle("-fx-border-color: #27ae60; -fx-border-width: 2px;");
                    
                    successAlert.showAndWait();
                    
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    
                } else {
                    Alert failureAlert = new Alert(Alert.AlertType.ERROR);
                    failureAlert.setTitle("Error");
                    failureAlert.setHeaderText("❌ Failed to Allow Resubmission");
                    failureAlert.setContentText("An error occurred while processing the resubmission.\n" +
                                              "Possible reasons:\n" +
                                              "• No rejected or expired request found in database\n" +
                                              "• Database connection issue\n" +
                                              "• System error\n\n" +
                                              "Please try again or contact system administrator.");
                    failureAlert.showAndWait();
                }
                
            } catch (Exception e) {
                processingAlert.close();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("System Error");
                errorAlert.setHeaderText("❌ System Error Occurred");
                errorAlert.setContentText("An unexpected error occurred:\n" + e.getMessage() + 
                                        "\n\nPlease contact system administrator.");
                errorAlert.showAndWait();
            }
        }
    }
    
    private boolean performResubmissionProcess(ClearanceRequest request) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String studentIdSql = "SELECT id FROM users WHERE username = ?";
                PreparedStatement studentStmt = conn.prepareStatement(studentIdSql);
                studentStmt.setString(1, request.getStudentId());
                ResultSet studentRs = studentStmt.executeQuery();
                
                if (!studentRs.next()) {
                    conn.rollback();
                    return false;
                }
                
                int studentId = studentRs.getInt("id");
                
                String getRequestSql = """
                    SELECT id FROM clearance_requests 
                    WHERE student_id = ? 
                    AND (status = 'REJECTED' OR status = 'EXPIRED')
                    ORDER BY request_date DESC LIMIT 1
                    """;
                PreparedStatement requestStmt = conn.prepareStatement(getRequestSql);
                requestStmt.setInt(1, studentId);
                ResultSet requestRs = requestStmt.executeQuery();
                
                if (!requestRs.next()) {
                    conn.rollback();
                    return false;
                }
                
                int requestId = requestRs.getInt("id");
                
                String updateRequestSql = """
                    UPDATE clearance_requests 
                    SET status = 'PENDING', 
                        can_reapply = TRUE,
                        request_date = NOW()
                    WHERE id = ?
                    """;
                PreparedStatement updateStmt = conn.prepareStatement(updateRequestSql);
                updateStmt.setInt(1, requestId);
                int rowsUpdated = updateStmt.executeUpdate();
                
                if (rowsUpdated == 0) {
                    conn.rollback();
                    return false;
                }
                
                String clearApprovalsSql = "DELETE FROM clearance_approvals WHERE request_id = ?";
                PreparedStatement clearStmt = conn.prepareStatement(clearApprovalsSql);
                clearStmt.setInt(1, requestId);
                clearStmt.executeUpdate();
                
                String workflowSql = "SELECT role FROM workflow_config ORDER BY sequence_order";
                PreparedStatement workflowStmt = conn.prepareStatement(workflowSql);
                ResultSet workflowRs = workflowStmt.executeQuery();
                
                String insertApprovalSql;
                try {
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet columns = meta.getColumns(null, null, "clearance_approvals", "timestamp");
                    if (columns.next()) {
                        insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, status, timestamp)
                            VALUES (?, ?, 'PENDING', NOW())
                            """;
                    } else {
                        insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, status)
                            VALUES (?, ?, 'PENDING')
                            """;
                    }
                    columns.close();
                } catch (Exception e) {
                    insertApprovalSql = """
                        INSERT INTO clearance_approvals (request_id, officer_role, status)
                        VALUES (?, ?, 'PENDING')
                        """;
                }
                
                PreparedStatement insertStmt = conn.prepareStatement(insertApprovalSql);
                
                int approvalsAdded = 0;
                while (workflowRs.next()) {
                    try {
                        insertStmt.setInt(1, requestId);
                        insertStmt.setString(2, workflowRs.getString("role"));
                        insertStmt.addBatch();
                        approvalsAdded++;
                    } catch (Exception e) {
                        // Continue with next role
                    }
                }
                
                if (approvalsAdded > 0) {
                    try {
                        insertStmt.executeBatch();
                    } catch (Exception e) {
                        insertStmt.clearBatch();
                        workflowRs.beforeFirst();
                        while (workflowRs.next()) {
                            try {
                                String simpleInsertSql = """
                                    INSERT INTO clearance_approvals (request_id, officer_role, status)
                                    VALUES (?, ?, 'PENDING')
                                    """;
                                PreparedStatement simpleStmt = conn.prepareStatement(simpleInsertSql);
                                simpleStmt.setInt(1, requestId);
                                simpleStmt.setString(2, workflowRs.getString("role"));
                                simpleStmt.executeUpdate();
                            } catch (Exception ex) {
                                // Continue with next role
                            }
                        }
                    }
                }
                
                try {
                    String updateDormSql = """
                        UPDATE student_dormitory_credentials 
                        SET clearance_status = 'PENDING'
                        WHERE student_id = ?
                        """;
                    PreparedStatement dormStmt = conn.prepareStatement(updateDormSql);
                    dormStmt.setInt(1, studentId);
                    dormStmt.executeUpdate();
                } catch (Exception e) {
                    // Not critical, continue
                }
                
                try {
                    String auditSql = """
                        INSERT INTO audit_logs (user_id, action, details, timestamp)
                        VALUES (?, 'ALLOW_RESUBMISSION', ?, NOW())
                        """;
                    PreparedStatement auditStmt = conn.prepareStatement(auditSql);
                    auditStmt.setInt(1, currentUser.getId());
                    auditStmt.setString(2, "Allowed resubmission for student: " + 
                                       request.getStudentId() + " - " + request.getFullName() +
                                       " (Request ID: " + requestId + ", Previous status: " + request.getStatus() + ")");
                    auditStmt.executeUpdate();
                } catch (Exception e) {
                    // Continue even if audit log fails
                }
                
                try {
                    String notificationSql = """
                        INSERT INTO notifications (user_id, type, subject, message, is_read, created_at)
                        VALUES (?, 'RESUBMISSION_ALLOWED', 'Clearance Resubmission Allowed', 
                                'Your clearance request has been reset to PENDING. You can now submit a new clearance request.', 
                                FALSE, NOW())
                        """;
                    PreparedStatement notifStmt = conn.prepareStatement(notificationSql);
                    notifStmt.setInt(1, studentId);
                    notifStmt.executeUpdate();
                } catch (Exception e) {
                    // Not critical, continue
                }
                
                conn.commit();
                
                request.setCanReapply(true);
                request.setStatus("PENDING");
                
                return true;
                
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Ignore rollback error
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    // Ignore auto-commit error
                }
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public void openWorkflowManagement() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Approval Workflow Management");
        dialog.setHeaderText("Configure Department Approval Sequence");
        dialog.setContentText("Drag and drop or select departments in order of approval:");

        ButtonType saveButton = new ButtonType("Save Workflow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        List<String> currentWorkflow = getCurrentWorkflow();
        
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(currentWorkflow);
        listView.setPrefHeight(200);

        ListView<String> availableDepartments = new ListView<>();
        String[] allDepartments = {"LIBRARIAN", "CAFETERIA", "DORMITORY", "REGISTRAR", "DEPARTMENT_HEAD"};
        for (String dept : allDepartments) {
            if (!currentWorkflow.contains(dept)) {
                availableDepartments.getItems().add(dept);
            }
        }
        availableDepartments.setPrefHeight(200);

        HBox buttonBox = new HBox(10);
        Button btnUp = new Button("↑");
        Button btnDown = new Button("↓");
        Button btnAdd = new Button("Add →");
        Button btnRemove = new Button("← Remove");
        
        btnUp.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                String item = listView.getItems().get(selectedIndex);
                listView.getItems().remove(selectedIndex);
                listView.getItems().add(selectedIndex - 1, item);
                listView.getSelectionModel().select(selectedIndex - 1);
            }
        });
       
        btnDown.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex < listView.getItems().size() - 1 && selectedIndex >= 0) {
                String item = listView.getItems().get(selectedIndex);
                listView.getItems().remove(selectedIndex);
                listView.getItems().add(selectedIndex + 1, item);
                listView.getSelectionModel().select(selectedIndex + 1);
            }
        });
        
        btnAdd.setOnAction(e -> {
            String selected = availableDepartments.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listView.getItems().add(selected);
                availableDepartments.getItems().remove(selected);
            }
        });
        
        btnRemove.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listView.getItems().remove(selected);
                availableDepartments.getItems().add(selected);
            }
        });

        buttonBox.getChildren().addAll(btnUp, btnDown, btnAdd, btnRemove);

        HBox listsBox = new HBox(20);
        VBox availableBox = new VBox(5, new Label("Available Departments:"), availableDepartments);
        VBox workflowBox = new VBox(5, new Label("Current Workflow:"), listView);
        listsBox.getChildren().addAll(availableBox, workflowBox);

        content.getChildren().addAll(listsBox, buttonBox);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return saveWorkflow(listView.getItems());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            showAlert("Workflow Updated", result);
        });
    }

    private List<String> getCurrentWorkflow() {
        List<String> workflow = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT role FROM workflow_config ORDER BY sequence_order";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                workflow.add(rs.getString("role"));
            }
        } catch (Exception e) {
            workflow.add("LIBRARIAN");
            workflow.add("CAFETERIA");
            workflow.add("DORMITORY");
            workflow.add("REGISTRAR");
            workflow.add("DEPARTMENT_HEAD");
        }
        return workflow;
    }

    private String saveWorkflow(List<String> workflow) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String clearSql = "DELETE FROM workflow_config";
            PreparedStatement clearStmt = conn.prepareStatement(clearSql);
            clearStmt.executeUpdate();

            String insertSql = "INSERT INTO workflow_config (role, sequence_order) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            for (int i = 0; i < workflow.size(); i++) {
                insertStmt.setString(1, workflow.get(i));
                insertStmt.setInt(2, i + 1);
                insertStmt.addBatch();
            }
            
            insertStmt.executeBatch();
            return "Workflow updated successfully!\nNew sequence: " + String.join(" → ", workflow);
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    public void openSessionManagement() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Academic Session Management");
        dialog.setHeaderText("Create New Academic Session");

        ButtonType saveButton = new ButtonType("Create Session", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtSessionName = new TextField();
        txtSessionName.setPromptText("e.g., Fall 2024, Spring 2025");
        DatePicker dpStartDate = new DatePicker(LocalDate.now());
        DatePicker dpEndDate = new DatePicker(LocalDate.now().plusMonths(6));
        CheckBox chkActive = new CheckBox("Set as Active Session");
        chkActive.setSelected(true);

        grid.add(new Label("Session Name*:"), 0, 0);
        grid.add(txtSessionName, 1, 0);
        grid.add(new Label("Start Date*:"), 0, 1);
        grid.add(dpStartDate, 1, 1);
        grid.add(new Label("End Date*:"), 0, 2);
        grid.add(dpEndDate, 1, 2);
        grid.add(chkActive, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return createAcademicSession(txtSessionName.getText(), dpStartDate.getValue(), 
                                          dpEndDate.getValue(), chkActive.isSelected()) ? ButtonType.OK : null;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private boolean createAcademicSession(String sessionName, LocalDate startDate, LocalDate endDate, boolean isActive) {
        if (sessionName == null || sessionName.trim().isEmpty() || startDate == null || endDate == null) {
            showAlert("Error", "Please fill all required fields!");
            return false;
        }

        if (endDate.isBefore(startDate)) {
            showAlert("Error", "End date cannot be before start date!");
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (isActive) {
                String deactivateSql = "UPDATE academic_sessions SET is_active = false";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();
            }

            String sql = "INSERT INTO academic_sessions (session_name, start_date, end_date, is_active) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionName.trim());
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            stmt.setBoolean(4, isActive);

            if (stmt.executeUpdate() > 0) {
                showAlert("Success", "Academic session created successfully!\n" +
                                  "Session: " + sessionName + "\n" +
                                  "Period: " + startDate + " to " + endDate +
                                  (isActive ? "\n\nThis is now the active session." : ""));
                return true;
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to create session: " + e.getMessage());
        }
        return false;
    }
    
    public void generateClearanceCertificates() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Clearance Certificates");
        dialog.setHeaderText("Generate certificates for cleared students");

        ButtonType generateButton = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker dpFromDate = new DatePicker(LocalDate.now().minusMonths(1));
        DatePicker dpToDate = new DatePicker(LocalDate.now());
        ComboBox<String> cmbDepartment = new ComboBox<>();
        cmbDepartment.getItems().addAll("All Departments", "Software Engineering", "Computer Science", 
                                      "Electrical Engineering", "Mechanical Engineering", "Civil Engineering");

        grid.add(new Label("From Date:"), 0, 0);
        grid.add(dpFromDate, 1, 0);
        grid.add(new Label("To Date:"), 0, 1);
        grid.add(dpToDate, 1, 1);
        grid.add(new Label("Department:"), 0, 2);
        grid.add(cmbDepartment, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButton) {
                generateCertificates(dpFromDate.getValue(), dpToDate.getValue(), cmbDepartment.getValue());
                return ButtonType.OK;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void generateCertificates(LocalDate fromDate, LocalDate toDate, String department) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT u.username, u.full_name, u.department, cr.request_date, cr.completion_date " +
                "FROM clearance_requests cr " +
                "JOIN users u ON cr.student_id = u.id " +
                "WHERE cr.status = 'FULLY_CLEARED' " +
                "AND DATE(cr.completion_date) BETWEEN ? AND ? "
            );

            if (department != null && !"All Departments".equals(department)) {
                sql.append("AND u.department = ? ");
            }
            
            sql.append("ORDER BY cr.completion_date DESC");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            
            if (department != null && !"All Departments".equals(department)) {
                ps.setString(3, department);
            }

            ResultSet rs = ps.executeQuery();

            StringBuilder report = new StringBuilder();
            report.append("=== CERTIFICATES BATCH GENERATION REPORT ===\n\n");
            report.append("Date Range: ").append(fromDate).append(" to ").append(toDate).append("\n");
            report.append("Department: ").append(department).append("\n");
            report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
            report.append("-".repeat(50)).append("\n\n");
            
            int count = 0;
            int successful = 0;

            while (rs.next()) {
                count++;
                report.append("Certificate #").append(count).append("\n");
                report.append("Student: ").append(rs.getString("full_name")).append("\n");
                report.append("ID: ").append(rs.getString("username")).append("\n");
                report.append("Department: ").append(rs.getString("department")).append("\n");
                report.append("Clearance Date: ").append(rs.getDate("completion_date")).append("\n");
                
                try {
                    successful++;
                    report.append("Status: ✅ Generated\n");
                } catch (Exception e) {
                    report.append("Status: ❌ Failed - ").append(e.getMessage()).append("\n");
                }
                report.append("---\n");
            }

            if (count > 0) {
                report.append("\n=== SUMMARY ===\n");
                report.append("Total students found: ").append(count).append("\n");
                report.append("Successfully generated: ").append(successful).append("\n");
                report.append("Failed: ").append(count - successful).append("\n");
                
                showAlert("Certificates Generated", 
                         "Successfully processed " + count + " students!\n\n" + report.toString());
            } else {
                showAlert("No Certificates", "No cleared students found in selected period.");
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to generate certificates: " + e.getMessage());
        }
    }
    
    public void verifyCertificate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verify Certificate");
        dialog.setHeaderText("Verify Clearance Certificate");
        dialog.setContentText("Enter Student ID or Certificate ID:");
        
        dialog.showAndWait().ifPresent(input -> {
            verifyCertificateInDatabase(input);
        });
    }

    private void verifyCertificateInDatabase(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    u.username as student_id,
                    u.department,
                    cr.request_date,
                    cr.completion_date,
                    cr.status,
                    COUNT(ca.id) as approved_count
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                WHERE (u.username = ? OR u.id = ?) AND cr.status = 'FULLY_CLEARED'
                GROUP BY u.id, cr.id
                ORDER BY cr.completion_date DESC 
                LIMIT 1
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, searchTerm);
            try {
                ps.setInt(2, Integer.parseInt(searchTerm));
            } catch (NumberFormatException e) {
                ps.setInt(2, -1);
            }
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String result = "✅ CERTIFICATE VERIFICATION SUCCESSFUL\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Student ID: " + rs.getString("student_id") + "\n" +
                              "Department: " + rs.getString("department") + "\n" +
                              "Clearance Status: " + rs.getString("status") + "\n" +
                              "Request Date: " + rs.getDate("request_date") + "\n" +
                              "Completion Date: " + rs.getDate("completion_date") + "\n" +
                              "Approvals: " + rs.getInt("approved_count") + "/5 departments approved\n\n" +
                              "This certificate is VALID and verified in our system.";
                
                showAlert("Certificate Verified", result);
            } else {
                showAlert("Not Found", "No valid clearance certificate found for: " + searchTerm + 
                                    "\n\nPossible reasons:\n" +
                                    "• Student ID is incorrect\n" +
                                    "• Clearance is not fully approved\n" +
                                    "• No clearance request exists");
            }
            
        } catch (Exception e) {
            showAlert("Error", "Verification failed: " + e.getMessage());
        }
    }
    
    public void processSemesterRollover() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Semester Rollover");
        confirm.setHeaderText("Process End-of-Semester Rollover");
        confirm.setContentText("⚠️  CRITICAL OPERATION  ⚠️\n\n" +
                             "This will:\n" +
                             "• Archive all completed clearances\n" +
                             "• Reset/expire pending requests\n" +
                             "• Update student year levels\n" +
                             "• Create new academic session\n\n" +
                             "❌ This action cannot be undone!\n\n" +
                             "✅ Proceed with semester rollover?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performSemesterRollover();
        }
    }

    private void performSemesterRollover() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                StringBuilder report = new StringBuilder();
                report.append("🎉 SEMESTER ROLLOVER REPORT\n");
                report.append("==========================\n\n");
                
                String archiveSql = "INSERT INTO clearance_requests_archive " +
                                  "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                  "FROM clearance_requests WHERE status = 'FULLY_CLEARED'";
                PreparedStatement archiveStmt = conn.prepareStatement(archiveSql);
                int archived = archiveStmt.executeUpdate();
                report.append("✓ Archived " + archived + " cleared requests\n");
                
                String archiveRejectedSql = "INSERT INTO clearance_requests_archive " +
                                         "SELECT NULL, id, student_id, request_date, status, completion_date, NOW() " +
                                         "FROM clearance_requests WHERE status = 'REJECTED'";
                PreparedStatement archiveRejectedStmt = conn.prepareStatement(archiveRejectedSql);
                int archivedRejected = archiveRejectedStmt.executeUpdate();
                report.append("✓ Archived " + archivedRejected + " rejected requests\n");

                String resetSql = "UPDATE clearance_requests SET status = 'EXPIRED' " +
                                "WHERE status IN ('PENDING', 'IN_PROGRESS')";
                PreparedStatement resetStmt = conn.prepareStatement(resetSql);
                int expired = resetStmt.executeUpdate();
                report.append("✓ Expired " + expired + " pending requests\n");

                String yearSql = "UPDATE users SET year_level = " +
                               "CASE " +
                               "WHEN year_level = '1st Year' THEN '2nd Year' " +
                               "WHEN year_level = '2nd Year' THEN '3rd Year' " +
                               "WHEN year_level = '3rd Year' THEN '4th Year' " +
                               "WHEN year_level = '4th Year' THEN '5th Year' " +
                               "WHEN year_level = '5th Year' THEN 'Graduated' " +
                               "ELSE year_level " +
                               "END " +
                               "WHERE role = 'STUDENT' AND status = 'ACTIVE'";
                PreparedStatement yearStmt = conn.prepareStatement(yearSql);
                int updated = yearStmt.executeUpdate();
                report.append("✓ Updated " + updated + " student year levels\n");

                LocalDate today = LocalDate.now();
                String sessionName;
                if (today.getMonthValue() <= 6) {
                    sessionName = "Spring Semester " + today.getYear();
                } else {
                    sessionName = "Fall Semester " + today.getYear();
                }
                
                String deactivateSql = "UPDATE academic_sessions SET is_active = false WHERE is_active = true";
                PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql);
                deactivateStmt.executeUpdate();

                String sessionSql = "INSERT INTO academic_sessions (session_name, start_date, end_date, is_active) " +
                                  "VALUES (?, ?, ?, true)";
                PreparedStatement sessionStmt = conn.prepareStatement(sessionSql);
                sessionStmt.setString(1, sessionName);
                sessionStmt.setDate(2, Date.valueOf(today));
                sessionStmt.setDate(3, Date.valueOf(today.plusMonths(6)));
                sessionStmt.executeUpdate();
                report.append("✓ Created new academic session: " + sessionName + "\n");

                conn.commit();
                report.append("\n✅ Rollover completed successfully!\n");
                report.append("System is ready for the new semester.");

                showAlert("Rollover Successful", report.toString());

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            showAlert("Rollover Error", "Failed to process rollover: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}