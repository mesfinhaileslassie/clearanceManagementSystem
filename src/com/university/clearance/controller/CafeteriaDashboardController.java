package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;

public class CafeteriaDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblStudentInfo;
    @FXML private Label lblCafeteriaStatus;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colMealPlan;
    @FXML private TableColumn<ClearanceRequest, String> colCafeteriaStatus;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<CafeteriaRecord> tableCafeteriaRecords;
    @FXML private TableColumn<CafeteriaRecord, String> colRecordType;
    @FXML private TableColumn<CafeteriaRecord, String> colDescription;
    @FXML private TableColumn<CafeteriaRecord, String> colAmount;
    @FXML private TableColumn<CafeteriaRecord, String> colTransactionDate;
    @FXML private TableColumn<CafeteriaRecord, String> colRecordStatus;


    @FXML private TabPane mainTabPane;
    
    
    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<CafeteriaRecord> cafeteriaData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupCafeteriaTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Cafeteria Office");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colMealPlan.setCellValueFactory(new PropertyValueFactory<>("mealPlan"));
        colCafeteriaStatus.setCellValueFactory(new PropertyValueFactory<>("cafeteriaStatus"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        
        // Actions column with Approve/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject = new Button("❌ Reject");
            private final Button btnViewDetails = new Button("🍽️ View Records");
            private final HBox buttons = new HBox(5, btnViewDetails, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                btnViewDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                
                btnViewDetails.setOnAction(event -> {
                    ClearanceRequest request = getTableView().getItems().get(getIndex());
                    viewStudentCafeteriaRecords(request);
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
                    // Only show buttons if pending
                    if ("Pending Cafeteria Check".equals(request.getCafeteriaStatus()) || 
                        request.getCafeteriaStatus().contains("Pending")) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupCafeteriaTableColumns() {
        colRecordType.setCellValueFactory(new PropertyValueFactory<>("recordType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTransactionDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Color code status
        colRecordStatus.setCellFactory(column -> new TableCell<CafeteriaRecord, String>() {
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
        
        // Color code amounts (red for outstanding balances)
        colAmount.setCellFactory(column -> new TableCell<CafeteriaRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (!item.equals("$0.00") && !item.equals("-") && !item.startsWith("+")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        loadPendingRequests();
        showAlert("Refreshed", "Cafeteria clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    cr.request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                JOIN clearance_approvals ca ON cr.id = ca.request_id 
                WHERE ca.officer_role = 'CAFETERIA' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String cafeteriaStatus = checkStudentCafeteriaStatus(conn, studentId);
                String mealPlan = getStudentMealPlan(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    mealPlan,
                    rs.getTimestamp("request_date").toString(),
                    cafeteriaStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Cafeteria Clearances: " + pendingCount);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String checkStudentCafeteriaStatus(Connection conn, String studentId) throws SQLException {
        // Check if student has any cafeteria issues
        createCafeteriaRecordsIfNeeded(conn, studentId);
        
        String cafeteriaSql = """
            SELECT 
                COUNT(*) as total_records,
                SUM(CASE WHEN record_type = 'OUTSTANDING_BALANCE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_balances,
                SUM(CASE WHEN record_type = 'MEAL_PLAN_FEE' AND status != 'Paid' THEN 1 ELSE 0 END) as unpaid_meal_plans,
                SUM(CASE WHEN record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND status != 'Paid' THEN amount ELSE 0 END) as total_balance,
                SUM(CASE WHEN record_type = 'MEAL_SWIPES' AND status = 'Active' THEN amount ELSE 0 END) as remaining_meals
            FROM cafeteria_records cr
            JOIN users u ON cr.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement ps = conn.prepareStatement(cafeteriaSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int unpaidBalances = rs.getInt("unpaid_balances");
            int unpaidMealPlans = rs.getInt("unpaid_meal_plans");
            double totalBalance = rs.getDouble("total_balance");
            int remainingMeals = rs.getInt("remaining_meals");
            
            if (unpaidBalances > 0 && totalBalance > 0) {
                return "❌ Outstanding Balance: $" + String.format("%.2f", totalBalance);
            } else if (unpaidMealPlans > 0) {
                return "❌ Unpaid Meal Plan Fee";
            } else if (remainingMeals > 0) {
                return "⚠️ " + remainingMeals + " Unused Meals Remaining";
            } else {
                return "✅ Cafeteria Clear";
            }
        }
        
        return "Pending Cafeteria Check";
    }

    private String getStudentMealPlan(Connection conn, String studentId) throws SQLException {
        String mealPlanSql = """
            SELECT meal_plan_type 
            FROM cafeteria_records 
            JOIN users u ON cafeteria_records.student_id = u.id 
            WHERE u.username = ? 
            AND record_type = 'MEAL_PLAN' 
            ORDER BY transaction_date DESC 
            LIMIT 1
            """;
            
        PreparedStatement ps = conn.prepareStatement(mealPlanSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return rs.getString("meal_plan_type");
        }
        
        return "No Meal Plan";
    }

    private void createCafeteriaRecordsIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if cafeteria_records table exists
        try {
            String checkTableSql = "SELECT 1 FROM cafeteria_records LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS cafeteria_records (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    record_type VARCHAR(50),
                    description TEXT,
                    amount DECIMAL(10,2),
                    transaction_date DATE,
                    status VARCHAR(20),
                    meal_plan_type VARCHAR(50),
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
        }
        
        // Check if student has cafeteria records, if not insert sample data
        String checkRecordsSql = """
            SELECT COUNT(*) FROM cafeteria_records cr 
            JOIN users u ON cr.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Insert sample cafeteria records
            String[][] records = generateSampleCafeteriaRecords();
            
            String insertSql = """
                INSERT INTO cafeteria_records (student_id, record_type, description, amount, transaction_date, status, meal_plan_type)
                SELECT id, ?, ?, ?, ?, ?, ? FROM users WHERE username = ?
                """;
            
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            
            // Assign a meal plan
            String[] mealPlans = {"Gold Plan (21 meals/week)", "Silver Plan (14 meals/week)", "Bronze Plan (7 meals/week)"};
            String mealPlan = mealPlans[(int)(Math.random() * mealPlans.length)];
            
            insertStmt.setString(1, "MEAL_PLAN");
            insertStmt.setString(2, "Semester Meal Plan - " + mealPlan);
            insertStmt.setDouble(3, getMealPlanPrice(mealPlan));
            insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusMonths(2)));
            insertStmt.setString(5, Math.random() > 0.3 ? "Paid" : "Pending"); // 70% paid, 30% pending
            insertStmt.setString(6, mealPlan);
            insertStmt.setString(7, studentId);
            insertStmt.executeUpdate();
            
            // Insert remaining meals
            int remainingMeals = (int)(Math.random() * 15);
            if (remainingMeals > 0) {
                insertStmt.setString(1, "MEAL_SWIPES");
                insertStmt.setString(2, "Remaining Meal Swipes");
                insertStmt.setDouble(3, remainingMeals);
                insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now()));
                insertStmt.setString(5, "Active");
                insertStmt.setString(6, null);
                insertStmt.setString(7, studentId);
                insertStmt.executeUpdate();
            }
            
            // Insert random outstanding balances (25% chance)
            if (Math.random() < 0.25) {
                insertStmt.setString(1, "OUTSTANDING_BALANCE");
                insertStmt.setString(2, "Cafeteria A La Carte Charges");
                insertStmt.setDouble(3, 25 + (Math.random() * 100));
                insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusDays(15)));
                insertStmt.setString(5, "Pending");
                insertStmt.setString(6, null);
                insertStmt.setString(7, studentId);
                insertStmt.executeUpdate();
            }
            
            // Insert payment records
            insertStmt.setString(1, "PAYMENT");
            insertStmt.setString(2, "Meal Plan Payment");
            insertStmt.setDouble(3, -getMealPlanPrice(mealPlan) * 0.7); // Negative for payments
            insertStmt.setDate(4, Date.valueOf(java.time.LocalDate.now().minusMonths(1)));
            insertStmt.setString(5, "Processed");
            insertStmt.setString(6, null);
            insertStmt.setString(7, studentId);
            insertStmt.executeUpdate();
        }
    }

    private double getMealPlanPrice(String mealPlan) {
        return switch (mealPlan) {
            case "Gold Plan (21 meals/week)" -> 2500.00;
            case "Silver Plan (14 meals/week)" -> 1800.00;
            case "Bronze Plan (7 meals/week)" -> 1200.00;
            default -> 1500.00;
        };
    }

    private String[][] generateSampleCafeteriaRecords() {
        return new String[][]{
            {"MEAL_PLAN", "Semester Meal Plan - Gold Plan", "2500.00", java.time.LocalDate.now().minusMonths(2).toString(), "Paid"},
            {"OUTSTANDING_BALANCE", "A La Carte Dining Charges", "45.75", java.time.LocalDate.now().minusDays(30).toString(), "Pending"},
            {"MEAL_SWIPES", "Remaining Weekly Meals", "8", java.time.LocalDate.now().toString(), "Active"},
            {"PAYMENT", "Meal Plan Payment", "-1750.00", java.time.LocalDate.now().minusMonths(1).toString(), "Processed"},
            {"MEAL_PLAN_FEE", "Additional Dining Dollars", "100.00", java.time.LocalDate.now().minusDays(15).toString(), "Pending"}
        };
    }

    private void viewStudentCafeteriaRecords(ClearanceRequest request) {
        loadStudentCafeteriaRecords(request.getStudentId());
        lblStudentInfo.setText("Cafeteria Records for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - Meal Plan: " + request.getMealPlan());
        
        // Update cafeteria status summary
        updateCafeteriaSummary(request.getStudentId());
    }

    private void loadStudentCafeteriaRecords(String studentId) {
        cafeteriaData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    record_type,
                    description,
                    amount,
                    transaction_date,
                    status,
                    meal_plan_type
                FROM cafeteria_records cr
                JOIN users u ON cr.student_id = u.id
                WHERE u.username = ?
                ORDER BY 
                    CASE 
                        WHEN status IN ('Pending', 'Active') THEN 1
                        ELSE 2
                    END,
                    transaction_date DESC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String amount;
                double amountValue = rs.getDouble("amount");
                
                if (rs.getString("record_type").equals("MEAL_SWIPES")) {
                    amount = String.format("%d meals", (int)amountValue);
                } else if (amountValue < 0) {
                    amount = String.format("+$%.2f", Math.abs(amountValue));
                } else {
                    amount = String.format("$%.2f", amountValue);
                }
                
                String transactionDate = rs.getDate("transaction_date") != null ? 
                    rs.getDate("transaction_date").toString() : "N/A";
                
                CafeteriaRecord record = new CafeteriaRecord(
                    formatRecordType(rs.getString("record_type")),
                    rs.getString("description"),
                    amount,
                    transactionDate,
                    rs.getString("status")
                );
                cafeteriaData.add(record);
            }
            
            tableCafeteriaRecords.setItems(cafeteriaData);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load cafeteria records: " + e.getMessage());
        }
    }

    private String formatRecordType(String recordType) {
        return switch (recordType) {
            case "OUTSTANDING_BALANCE" -> "💰 Outstanding Balance";
            case "MEAL_PLAN" -> "🍽️ Meal Plan";
            case "MEAL_PLAN_FEE" -> "💳 Meal Plan Fee";
            case "MEAL_SWIPES" -> "🎫 Meal Swipes";
            case "PAYMENT" -> "✅ Payment";
            default -> recordType;
        };
    }

    private void updateCafeteriaSummary(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    COUNT(*) as total_records,
                    SUM(CASE WHEN status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as pending_items,
                    SUM(CASE WHEN record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND status != 'Paid' THEN amount ELSE 0 END) as total_balance,
                    SUM(CASE WHEN record_type = 'MEAL_SWIPES' AND status = 'Active' THEN amount ELSE 0 END) as remaining_meals
                FROM cafeteria_records cr
                JOIN users u ON cr.student_id = u.id
                WHERE u.username = ?
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int pendingItems = rs.getInt("pending_items");
                double totalBalance = rs.getDouble("total_balance");
                int remainingMeals = rs.getInt("remaining_meals");
                
                String summary;
                if (pendingItems == 0) {
                    summary = "✅ No pending cafeteria issues";
                    lblCafeteriaStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if (totalBalance > 0) {
                    summary = String.format("❌ %d pending items | Outstanding Balance: $%.2f", pendingItems, totalBalance);
                    lblCafeteriaStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else if (remainingMeals > 0) {
                    summary = String.format("⚠️ %d unused meals remaining", remainingMeals);
                    lblCafeteriaStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    summary = String.format("ℹ️ %d pending items to review", pendingItems);
                    lblCafeteriaStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                }
                
                lblCafeteriaStatus.setText(summary);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        // Check if student has any cafeteria issues
        if (request.getCafeteriaStatus().contains("❌") || request.getCafeteriaStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Cafeteria Clearance Issue");
            warning.setHeaderText("Student Has Cafeteria Issues");
            warning.setContentText("This student has outstanding cafeteria issues:\n\n" + 
                                 request.getCafeteriaStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Cafeteria Clearance");
        confirmation.setHeaderText("Approve Cafeteria Clearance");
        confirmation.setContentText("Approve cafeteria clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\nMeal Plan: " + request.getMealPlan());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Cafeteria clearance approved. All charges cleared.");
                loadPendingRequests(); // Refresh table to remove approved request
                showAlert("Approved", "Cafeteria clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Cafeteria Clearance");
        dialog.setHeaderText("Reject Cafeteria Clearance");
        dialog.setContentText("Enter reason for rejecting cafeteria clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Cafeteria clearance rejected: " + result.get().trim());
            loadPendingRequests(); // Refresh table to remove rejected request
            showAlert("Rejected", "Cafeteria clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'CAFETERIA'
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, remarks);
            ps.setInt(3, currentUser.getId());
            ps.setInt(4, requestId);
            
            ps.executeUpdate();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void generateCafeteriaReport() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to generate cafeteria report.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    cr.meal_plan_type,
                    COUNT(cr.id) as total_records,
                    SUM(CASE WHEN cr.status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as pending_items,
                    SUM(CASE WHEN cr.record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND cr.status != 'Paid' THEN cr.amount ELSE 0 END) as total_balance,
                    SUM(CASE WHEN cr.record_type = 'MEAL_SWIPES' AND cr.status = 'Active' THEN cr.amount ELSE 0 END) as remaining_meals
                FROM users u
                LEFT JOIN cafeteria_records cr ON u.id = cr.student_id
                WHERE u.username = ?
                GROUP BY u.id, cr.meal_plan_type
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, selected.getStudentId());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String report = "🍽️ CAFETERIA CLEARANCE REPORT\n\n" +
                              "Student: " + rs.getString("full_name") + "\n" +
                              "Meal Plan: " + rs.getString("meal_plan_type") + "\n" +
                              "Total Records: " + rs.getInt("total_records") + "\n" +
                              "Pending Items: " + rs.getInt("pending_items") + "\n" +
                              "Outstanding Balance: $" + String.format("%.2f", rs.getDouble("total_balance")) + "\n" +
                              "Remaining Meals: " + rs.getInt("remaining_meals") + "\n\n" +
                              "Generated by: " + currentUser.getFullName() + 
                              " (Cafeteria Office)";
                
                showAlert("Cafeteria Report", report);
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to generate cafeteria report: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner classes for table data
    public static class ClearanceRequest {
        private final String studentId;
        private final String studentName;
        private final String department;
        private final String mealPlan;
        private final String requestDate;
        private final String cafeteriaStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String mealPlan, String requestDate, String cafeteriaStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.mealPlan = mealPlan;
            this.requestDate = requestDate;
            this.cafeteriaStatus = cafeteriaStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getMealPlan() { return mealPlan; }
        public String getRequestDate() { return requestDate; }
        public String getCafeteriaStatus() { return cafeteriaStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class CafeteriaRecord {
        private final String recordType;
        private final String description;
        private final String amount;
        private final String transactionDate;
        private final String status;

        public CafeteriaRecord(String recordType, String description, String amount, String transactionDate, String status) {
            this.recordType = recordType;
            this.description = description;
            this.amount = amount;
            this.transactionDate = transactionDate;
            this.status = status;
        }

        public String getRecordType() { return recordType; }
        public String getDescription() { return description; }
        public String getAmount() { return amount; }
        public String getTransactionDate() { return transactionDate; }
        public String getStatus() { return status; }
    }
}