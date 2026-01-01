package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class DormitoryDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblDormitoryStatus;
    @FXML private TabPane mainTabPane;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colDormitoryStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<DormitoryRecord> tableDormitoryRecords;
    @FXML private TableColumn<DormitoryRecord, String> colRecordType;
    @FXML private TableColumn<DormitoryRecord, String> colDescription;
    @FXML private TableColumn<DormitoryRecord, String> colAmount;
    @FXML private TableColumn<DormitoryRecord, String> colDueDate;
    @FXML private TableColumn<DormitoryRecord, String> colRecordStatus;

    // New UI Components for Dorm Details Tab
    @FXML private Tab tabDormDetails;
    @FXML private Label lblDetailStudentId;
    @FXML private Label lblDetailStudentName;
    @FXML private Label lblDetailBlockRoom;
    @FXML private Label lblDetailKeyReturned;
    @FXML private Label lblDetailDamageStatus;
    @FXML private Label lblDetailOverallStatus;
    @FXML private TextArea txtDetailRemarks;
    @FXML private Button btnSaveDetails;
    @FXML private Button btnApproveFromDetails;
    @FXML private Button btnRejectFromDetails;

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<DormitoryRecord> dormitoryData = FXCollections.observableArrayList();
    private ClearanceRequest selectedRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupDormitoryTableColumns();
        
        // Initialize dorm details tab components
        setupDormDetailsTab();
        
        // Add listener to handle row selection
        setupDoubleClickHandler();
        
        loadPendingRequests();
    }

    private void setupDormDetailsTab() {
        if (tabDormDetails != null) {
            tabDormDetails.setOnSelectionChanged(event -> {
                if (tabDormDetails.isSelected() && selectedRequest != null) {
                    loadStudentDormitoryDetails(selectedRequest.getStudentId());
                }
            });
        }
    }
    
    private void setupDoubleClickHandler() {
        tableRequests.setRowFactory(tv -> {
            TableRow<ClearanceRequest> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectedRequest = row.getItem();
                    showStudentDormDetails(selectedRequest);
                }
            });
            return row;
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + user.getFullName());
        }
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDormitoryStatus.setCellValueFactory(new PropertyValueFactory<>("dormitoryStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Enhanced Actions column with See Details button
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnSeeDetails = new Button("Details");
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button("Reject");
            private final HBox buttons = new HBox(5, btnSeeDetails, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnSeeDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                
                btnSeeDetails.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    showStudentDormDetails(request);
                });
                
                btnApprove.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    approveClearance(request);
                });
                
                btnReject.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    rejectClearance(request);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    if (request != null) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupDormitoryTableColumns() {
        colRecordType.setCellValueFactory(new PropertyValueFactory<>("recordType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colRecordStatus.setCellFactory(column -> new TableCell<DormitoryRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Paid") || item.equals("Cleared")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.equals("Pending")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        loadPendingRequests();
        showAlert("Refreshed", "Dormitory clearance requests refreshed successfully!");
    }

    @FXML
    private void logout() {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Logout Confirmation");
            confirmAlert.setHeaderText("Are you sure you want to logout?");
            confirmAlert.setContentText("You will be redirected to the login screen.");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Load the login FXML - adjust path based on your project structure
                FXMLLoader loader = new FXMLLoader();
                
                // Try different possible paths for login FXML
                URL loginUrl = getClass().getResource("/com/university/clearance/resources/views/Login.fxml");
                if (loginUrl == null) {
                    loginUrl = getClass().getResource("/com/university/clearance/view/Login.fxml");
                }
                if (loginUrl == null) {
                    loginUrl = getClass().getResource("/views/Login.fxml");
                }
                if (loginUrl == null) {
                    loginUrl = getClass().getResource("/Login.fxml");
                }
                
                if (loginUrl == null) {
                    showAlert("Error", "Login screen not found. Please check the file path.");
                    return;
                }
                
                loader.setLocation(loginUrl);
                Parent root = loader.load();
                
                // Get the current stage
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                
                // Preserve current window size
                Scene currentScene = lblWelcome.getScene();
                Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
                stage.setScene(newScene);
                
                // Center the window
                //stage.centerOnScreen();
                
              
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void loadPendingRequests() {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, ensure dormitory credentials table exists
            createDormitoryCredentialsTable(conn);
            
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
                    AND ca.officer_role = 'DORMITORY'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND (ca.status IS NULL OR ca.status = 'PENDING')
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String dormitoryStatus = getStudentDormitoryStatus(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    rs.getString("request_date"),
                    dormitoryStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createDormitoryCredentialsTable(Connection conn) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS student_dormitory_credentials (
                id INT AUTO_INCREMENT PRIMARY KEY,
                student_id INT UNIQUE NOT NULL,
                block_number VARCHAR(20) NOT NULL,
                room_number VARCHAR(20) NOT NULL,
                key_returned BOOLEAN DEFAULT FALSE,
                key_return_date DATE NULL,
                damage_description TEXT,
                damage_paid BOOLEAN DEFAULT FALSE,
                damage_amount DECIMAL(10,2) DEFAULT 0.00,
                clearance_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
                remarks TEXT,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    private String getStudentDormitoryStatus(Connection conn, String studentId) throws SQLException {
        // Check dormitory credentials for key and damage status
        String sql = """
            SELECT 
                sdc.key_returned,
                sdc.damage_paid,
                sdc.clearance_status,
                CASE 
                    WHEN sdc.key_returned = TRUE AND sdc.damage_paid = TRUE THEN '✅ Key returned & No damage'
                    WHEN sdc.key_returned = FALSE AND sdc.damage_paid = FALSE THEN '❌ Key not returned & Damage not paid'
                    WHEN sdc.key_returned = FALSE THEN '❌ Key not returned'
                    WHEN sdc.damage_paid = FALSE THEN '❌ Damage not paid'
                    WHEN sdc.clearance_status = 'APPROVED' THEN ' Approved'
                    WHEN sdc.clearance_status = 'REJECTED' THEN ' Rejected'
                    ELSE '⏳ Pending review'
                END as status_display
            FROM student_dormitory_credentials sdc
            JOIN users u ON sdc.student_id = u.id
            WHERE u.username = ?
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return rs.getString("status_display");
        } else {
            // If no record exists, create one
            createDefaultDormitoryCredentials(conn, studentId);
            return "⏳ No dormitory record - Click Details to add";
        }
    }
    
    private void createDefaultDormitoryCredentials(Connection conn, String studentId) throws SQLException {
        String insertSql = """
            INSERT INTO student_dormitory_credentials (student_id, block_number, room_number, key_returned, damage_paid, clearance_status)
            SELECT u.id, 'A', '101', FALSE, FALSE, 'PENDING'
            FROM users u WHERE u.username = ?
            """;
        
        PreparedStatement ps = conn.prepareStatement(insertSql);
        ps.setString(1, studentId);
        ps.executeUpdate();
    }

    private void showStudentDormDetails(ClearanceRequest request) {
        selectedRequest = request;
        
        // Load student dormitory information
        loadStudentDormitoryDetails(request.getStudentId());
        
        // Switch to Dorm Details tab
        if (mainTabPane != null && tabDormDetails != null) {
            if (!mainTabPane.getTabs().contains(tabDormDetails)) {
                mainTabPane.getTabs().add(tabDormDetails);
            }
            mainTabPane.getSelectionModel().select(tabDormDetails);
        }
    }

    private void loadStudentDormitoryDetails(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.username,
                    u.full_name,
                    sdc.block_number,
                    sdc.room_number,
                    sdc.key_returned,
                    sdc.key_return_date,
                    sdc.damage_description,
                    sdc.damage_paid,
                    sdc.damage_amount,
                    sdc.clearance_status,
                    sdc.remarks
                FROM users u
                LEFT JOIN student_dormitory_credentials sdc ON u.id = sdc.student_id
                WHERE u.username = ?
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                lblDetailStudentId.setText("Student ID: " + rs.getString("username"));
                lblDetailStudentName.setText("Name: " + rs.getString("full_name"));
                lblDetailBlockRoom.setText("Room: Block " + rs.getString("block_number") + " - Room " + rs.getString("room_number"));
                
                // Key returned status
                boolean keyReturned = rs.getBoolean("key_returned");
                Date keyReturnDate = rs.getDate("key_return_date");
                if (keyReturned) {
                    lblDetailKeyReturned.setText("Key Returned:  Yes" + (keyReturnDate != null ? " (on " + keyReturnDate + ")" : ""));
                    lblDetailKeyReturned.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    lblDetailKeyReturned.setText("Key Returned: ❌ No");
                    lblDetailKeyReturned.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
                
                // Damage status
                boolean damagePaid = rs.getBoolean("damage_paid");
                String damageDescription = rs.getString("damage_description");
                double damageAmount = rs.getDouble("damage_amount");
                
                if (damageDescription != null && !damageDescription.isEmpty()) {
                    if (damagePaid) {
                        lblDetailDamageStatus.setText("Damage Status:  Paid - $" + damageAmount + " (" + damageDescription + ")");
                        lblDetailDamageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        lblDetailDamageStatus.setText("Damage Status:  Unpaid - $" + damageAmount + " (" + damageDescription + ")");
                        lblDetailDamageStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                } else {
                    lblDetailDamageStatus.setText("Damage Status: No damage reported");
                    lblDetailDamageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
                
                // Overall status
                String clearanceStatus = rs.getString("clearance_status");
                String overallStatus = determineOverallStatus(keyReturned, damagePaid, clearanceStatus);
                lblDetailOverallStatus.setText("Dormitory Status: " + overallStatus);
                
                if (overallStatus.contains("✅")) {
                    lblDetailOverallStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (overallStatus.contains("❌")) {
                    lblDetailOverallStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else {
                    lblDetailOverallStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                }
                
                txtDetailRemarks.setText(rs.getString("remarks") != null ? rs.getString("remarks") : "");
                
                // Load financial records
                loadStudentDormitoryRecords(studentId);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dormitory details: " + e.getMessage());
        }
    }
    
    private String determineOverallStatus(boolean keyReturned, boolean damagePaid, String clearanceStatus) {
        if ("APPROVED".equals(clearanceStatus)) {
            return " Approved";
        } else if ("REJECTED".equals(clearanceStatus)) {
            return " Rejected";
        } else if (keyReturned && damagePaid) {
            return " Eligible for approval";
        } else if (!keyReturned && !damagePaid) {
            return " Reject (Key not returned & Damage not paid)";
        } else if (!keyReturned) {
            return " Reject (Key not returned)";
        } else if (!damagePaid) {
            return " Reject (Damage not paid)";
        } else {
            return " Pending review";
        }
    }

    @FXML
    private void saveDormitoryDetails() {
        if (selectedRequest == null) {
            showAlert("Error", "Please select a student first");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get the student ID
            int studentId = getStudentIdByUsername(selectedRequest.getStudentId());
            
            // Update dormitory credentials
            String updateSql = """
                UPDATE student_dormitory_credentials 
                SET remarks = ?, last_updated = NOW()
                WHERE student_id = ?
                """;
                
            PreparedStatement ps = conn.prepareStatement(updateSql);
            ps.setString(1, txtDetailRemarks.getText());
            ps.setInt(2, studentId);
            ps.executeUpdate();
            
            showAlert("Success", "Dormitory details saved successfully!");
            
            // Reload data
            loadStudentDormitoryDetails(selectedRequest.getStudentId());
            loadPendingRequests();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save dormitory details: " + e.getMessage());
        }
    }

    @FXML
    private void updateKeyReturnStatus() {
        if (selectedRequest == null) {
            showAlert("Error", "Please select a student first");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Yes", "Yes", "No");
        dialog.setTitle("Key Return Status");
        dialog.setHeaderText("Update Key Return Status");
        dialog.setContentText("Has the student returned the dorm key?");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                int studentId = getStudentIdByUsername(selectedRequest.getStudentId());
                boolean keyReturned = "Yes".equals(choice);
                
                String updateSql = """
                    UPDATE student_dormitory_credentials 
                    SET key_returned = ?, 
                        key_return_date = ?,
                        last_updated = NOW()
                    WHERE student_id = ?
                    """;
                    
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setBoolean(1, keyReturned);
                ps.setDate(2, keyReturned ? Date.valueOf(LocalDate.now()) : null);
                ps.setInt(3, studentId);
                ps.executeUpdate();
                
                showAlert("Success", "Key return status updated to: " + choice);
                loadStudentDormitoryDetails(selectedRequest.getStudentId());
                loadPendingRequests();
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to update key status: " + e.getMessage());
            }
        });
    }

    @FXML
    private void updateDamageStatus() {
        if (selectedRequest == null) {
            showAlert("Error", "Please select a student first");
            return;
        }
        
        // Create a dialog to update damage status
        Dialog<DamageInfo> dialog = new Dialog<>();
        dialog.setTitle("Update Damage Status");
        dialog.setHeaderText("Enter damage information");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtDamageDescription = new TextField();
        txtDamageDescription.setPromptText("e.g., Broken window, Damaged bed");
        
        TextField txtDamageAmount = new TextField();
        txtDamageAmount.setPromptText("Amount in USD");
        
        ChoiceBox<String> chkDamagePaid = new ChoiceBox<>();
        chkDamagePaid.getItems().addAll("Paid", "Not Paid");
        chkDamagePaid.setValue("Not Paid");
        
        grid.add(new Label("Damage Description:"), 0, 0);
        grid.add(txtDamageDescription, 1, 0);
        grid.add(new Label("Damage Amount:"), 0, 1);
        grid.add(txtDamageAmount, 1, 1);
        grid.add(new Label("Payment Status:"), 0, 2);
        grid.add(chkDamagePaid, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(txtDamageAmount.getText());
                    return new DamageInfo(
                        txtDamageDescription.getText(),
                        amount,
                        "Paid".equals(chkDamagePaid.getValue())
                    );
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid amount");
                    return null;
                }
            }
            return null;
        });
        
        Optional<DamageInfo> result = dialog.showAndWait();
        result.ifPresent(damageInfo -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                int studentId = getStudentIdByUsername(selectedRequest.getStudentId());
                
                String updateSql = """
                    UPDATE student_dormitory_credentials 
                    SET damage_description = ?, 
                        damage_amount = ?,
                        damage_paid = ?,
                        last_updated = NOW()
                    WHERE student_id = ?
                    """;
                    
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setString(1, damageInfo.getDescription());
                ps.setDouble(2, damageInfo.getAmount());
                ps.setBoolean(3, damageInfo.isPaid());
                ps.setInt(4, studentId);
                ps.executeUpdate();
                
                showAlert("Success", "Damage status updated successfully!");
                loadStudentDormitoryDetails(selectedRequest.getStudentId());
                loadPendingRequests();
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to update damage status: " + e.getMessage());
            }
        });
    }

    @FXML
    private void approveFromDetails() {
        if (selectedRequest != null) {
            approveClearance(selectedRequest);
        }
    }

    @FXML
    private void rejectFromDetails() {
        if (selectedRequest != null) {
            rejectClearance(selectedRequest);
        }
    }

    private void approveClearance(ClearanceRequest request) {
        // Check dormitory credentials first
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = """
                SELECT key_returned, damage_paid 
                FROM student_dormitory_credentials sdc
                JOIN users u ON sdc.student_id = u.id
                WHERE u.username = ?
                """;
                
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, request.getStudentId());
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next()) {
                boolean keyReturned = rs.getBoolean("key_returned");
                boolean damagePaid = rs.getBoolean("damage_paid");
                
                if (!keyReturned || !damagePaid) {
                    Alert warning = new Alert(Alert.AlertType.WARNING);
                    warning.setTitle("Dormitory Clearance Issue");
                    warning.setHeaderText("Student Has Dormitory Issues");
                    
                    StringBuilder issues = new StringBuilder();
                    if (!keyReturned) issues.append("• Key not returned\n");
                    if (!damagePaid) issues.append("• Damage not paid\n");
                    
                    warning.setContentText("This student has the following dormitory issues:\n\n" + 
                                         issues.toString() + 
                                         "\nAre you sure you want to approve anyway?");
                    
                    warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    
                    Optional<ButtonType> result = warning.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.YES) {
                        return;
                    }
                }
            }
            
            // Update dormitory credentials status
            String updateCredSql = """
                UPDATE student_dormitory_credentials sdc
                JOIN users u ON sdc.student_id = u.id
                SET sdc.clearance_status = 'APPROVED',
                    sdc.last_updated = NOW()
                WHERE u.username = ?
                """;
                
            PreparedStatement credPs = conn.prepareStatement(updateCredSql);
            credPs.setString(1, request.getStudentId());
            credPs.executeUpdate();
            
            // Update clearance approval
            updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                "Dormitory clearance approved. Key returned and no outstanding damage.");
            
            showAlert("Approved", "Dormitory clearance approved for " + request.getStudentName());
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to approve clearance: " + e.getMessage());
        }
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Dormitory Clearance");
        dialog.setHeaderText("Reject Dormitory Clearance");
        dialog.setContentText("Enter reason for rejecting dormitory clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Update dormitory credentials status
                String updateCredSql = """
                    UPDATE student_dormitory_credentials sdc
                    JOIN users u ON sdc.student_id = u.id
                    SET sdc.clearance_status = 'REJECTED',
                        sdc.last_updated = NOW()
                    WHERE u.username = ?
                    """;
                    
                PreparedStatement credPs = conn.prepareStatement(updateCredSql);
                credPs.setString(1, request.getStudentId());
                credPs.executeUpdate();
                
                updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                    "Dormitory clearance rejected: " + result.get().trim());
                
                showAlert("Rejected", "Dormitory clearance rejected for " + request.getStudentName());
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to reject clearance: " + e.getMessage());
            }
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = "SELECT COUNT(*) FROM clearance_approvals WHERE request_id = ? AND officer_role = 'DORMITORY'";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            
            if (rs.getInt(1) > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE clearance_approvals 
                    SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                    WHERE request_id = ? AND officer_role = 'DORMITORY'
                    """;
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setString(1, status);
                ps.setString(2, remarks);
                ps.setInt(3, currentUser.getId());
                ps.setInt(4, requestId);
                ps.executeUpdate();
            } else {
                // Insert new record
                String insertSql = """
                    INSERT INTO clearance_approvals (request_id, officer_role, officer_id, status, remarks, approval_date)
                    VALUES (?, 'DORMITORY', ?, ?, ?, NOW())
                    """;
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setInt(1, requestId);
                ps.setInt(2, currentUser.getId());
                ps.setString(3, status);
                ps.setString(4, remarks);
                ps.executeUpdate();
            }
            
            // Update the overall request status
            updateOverallRequestStatus(conn, requestId);
            
            loadPendingRequests();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateOverallRequestStatus(Connection conn, int requestId) throws SQLException {
        String checkSql = """
            SELECT 
                SUM(CASE WHEN status = 'PENDING' OR status IS NULL THEN 1 ELSE 0 END) as pending_count,
                SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count
            FROM clearance_approvals 
            WHERE request_id = ?
            AND officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
            """;
        
        PreparedStatement checkPs = conn.prepareStatement(checkSql);
        checkPs.setInt(1, requestId);
        ResultSet rs = checkPs.executeQuery();
        
        if (rs.next()) {
            int pendingCount = rs.getInt("pending_count");
            int rejectedCount = rs.getInt("rejected_count");
            
            if (rejectedCount > 0) {
                updateRequestStatus(conn, requestId, "REJECTED");
            } else if (pendingCount == 0) {
                updateRequestStatus(conn, requestId, "FULLY_CLEARED");
            }
        }
    }

    private void updateRequestStatus(Connection conn, int requestId, String status) throws SQLException {
        String sql = "UPDATE clearance_requests SET status = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, requestId);
        ps.executeUpdate();
    }

    private int getStudentIdByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Student not found: " + username);
        }
    }

    private void loadStudentDormitoryRecords(String studentId) {
        dormitoryData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    dr.record_type,
                    dr.description,
                    dr.amount,
                    dr.due_date,
                    dr.status
                FROM dormitory_records dr
                JOIN users u ON dr.student_id = u.id
                WHERE u.username = ?
                ORDER BY dr.due_date DESC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                double amountValue = rs.getDouble("amount");
                String amount = String.format("$%.2f", amountValue);
                
                String dueDate = rs.getDate("due_date") != null ? 
                    rs.getDate("due_date").toString() : "N/A";
                
                DormitoryRecord record = new DormitoryRecord(
                    formatRecordType(rs.getString("record_type")),
                    rs.getString("description"),
                    amount,
                    dueDate,
                    rs.getString("status")
                );
                dormitoryData.add(record);
            }
            
            tableDormitoryRecords.setItems(dormitoryData);
            lblDormitoryStatus.setText("Found " + dormitoryData.size() + " dormitory records");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatRecordType(String recordType) {
        return switch (recordType) {
            case "DORMITORY_FEE" -> "💰 Dormitory Fee";
            case "DAMAGE_CHARGE" -> "🔨 Damage Charge";
            case "LATE_CHECKOUT" -> "⏰ Late Checkout";
            case "SECURITY_DEPOSIT" -> "🏦 Security Deposit";
            case "PAYMENT" -> "✅ Payment";
            default -> recordType;
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper classes
    private static class DamageInfo {
        private final String description;
        private final double amount;
        private final boolean paid;

        public DamageInfo(String description, double amount, boolean paid) {
            this.description = description;
            this.amount = amount;
            this.paid = paid;
        }

        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public boolean isPaid() { return paid; }
    }

    // Inner class for table data
    public static class ClearanceRequest {
        private final String studentId;
        private final String studentName;
        private final String department;
        private final String requestDate;
        private final String dormitoryStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String requestDate, String dormitoryStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.requestDate = requestDate;
            this.dormitoryStatus = dormitoryStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getRequestDate() { return requestDate; }
        public String getDormitoryStatus() { return dormitoryStatus; }
        public int getRequestId() { return requestId; }
    }
    
    public static class DormitoryRecord {
        private final String recordType;
        private final String description;
        private final String amount;
        private final String dueDate;
        private final String status;

        public DormitoryRecord(String recordType, String description, String amount, String dueDate, String status) {
            this.recordType = recordType;
            this.description = description;
            this.amount = amount;
            this.dueDate = dueDate;
            this.status = status;
        }

        public String getRecordType() { return recordType; }
        public String getDescription() { return description; }
        public String getAmount() { return amount; }
        public String getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}