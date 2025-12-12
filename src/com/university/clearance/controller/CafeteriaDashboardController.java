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
        System.out.println("=== DEBUG: CafeteriaDashboardController initialized ===");
        setupTableColumns();
        setupCafeteriaTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("=== DEBUG: Setting current user for Cafeteria ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Full Name: " + user.getFullName());
        System.out.println("Role: " + user.getRole());
        
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Cafeteria Office");
        loadPendingRequests();
        
        // Create test data if no requests exist
        checkAndCreateTestData();
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
                    if (request != null) {
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
        System.out.println("=== DEBUG: Refresh button clicked ===");
        loadPendingRequests();
        showAlert("Refreshed", "Cafeteria clearance requests refreshed successfully!");
    }

    private void loadPendingRequests() {
        System.out.println("\n=== DEBUG: Loading pending cafeteria requests ===");
        requestData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Database connection established");
            
            // Debug: Check what's in the database
            debugDatabaseStatus(conn);
            
            // FIXED QUERY: Use LEFT JOIN and table aliases to avoid ambiguous columns
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    u.year_level,
                    DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date,
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
                    AND ca.officer_role = 'CAFETERIA'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND (ca.status IS NULL OR ca.status = 'PENDING')
                ORDER BY cr.request_date ASC
                """;
                
            System.out.println("SQL Query: " + sql);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int pendingCount = 0;
            System.out.println("Query executed, processing results...");
            
            while (rs.next()) {
                pendingCount++;
                String studentId = rs.getString("student_id");
                System.out.println("\nFound student #" + pendingCount + ": " + studentId);
                System.out.println("Student Name: " + rs.getString("student_name"));
                System.out.println("Department: " + rs.getString("department"));
                System.out.println("Request Date: " + rs.getString("request_date"));
                System.out.println("Approval Status: " + rs.getString("approval_status"));
                
                String cafeteriaStatus = checkStudentCafeteriaStatus(conn, studentId);
                String mealPlan = getStudentMealPlan(conn, studentId);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("department"),
                    mealPlan,
                    rs.getString("request_date"),
                    cafeteriaStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
            }
            
            System.out.println("\n=== DEBUG: Query Results Summary ===");
            System.out.println("Total records found: " + pendingCount);
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Cafeteria Clearances: " + pendingCount);
            
            if (pendingCount == 0) {
                System.out.println("⚠️ WARNING: No cafeteria clearance requests found!");
                showAlert("No Requests", "No pending cafeteria clearance requests found.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR in loadPendingRequests:");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
        }
    }
    
    private void debugDatabaseStatus(Connection conn) throws SQLException {
        System.out.println("\n=== DEBUG: Database Status Check ===");
        
        // Check clearance_requests
        System.out.println("\n1. Total PENDING/IN_PROGRESS clearance_requests:");
        String checkRequestsSql = "SELECT COUNT(*) as count FROM clearance_requests WHERE status IN ('PENDING', 'IN_PROGRESS')";
        PreparedStatement requestsStmt = conn.prepareStatement(checkRequestsSql);
        ResultSet requestsRs = requestsStmt.executeQuery();
        if (requestsRs.next()) {
            System.out.println("   Count: " + requestsRs.getInt("count"));
        }
        
        // Check clearance_approvals for CAFETERIA
        System.out.println("\n2. clearance_approvals for CAFETERIA:");
        String checkApprovalsSql = """
            SELECT ca.request_id, ca.status, u.username as student_id
            FROM clearance_approvals ca
            LEFT JOIN clearance_requests cr ON ca.request_id = cr.id
            LEFT JOIN users u ON cr.student_id = u.id
            WHERE ca.officer_role = 'CAFETERIA'
            """;
        PreparedStatement approvalsStmt = conn.prepareStatement(checkApprovalsSql);
        ResultSet approvalsRs = approvalsStmt.executeQuery();
        int approvalCount = 0;
        while (approvalsRs.next()) {
            approvalCount++;
            System.out.println("   Approval " + approvalCount + ": Request ID=" + approvalsRs.getInt("request_id") +
                             ", Student=" + approvalsRs.getString("student_id") +
                             ", Status=" + approvalsRs.getString("status"));
        }
        
        // Check requests missing CAFETERIA approvals
        System.out.println("\n3. Requests missing CAFETERIA approvals:");
        String missingSql = """
            SELECT cr.id, u.username, cr.status as request_status
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
            AND NOT EXISTS (
                SELECT 1 FROM clearance_approvals ca 
                WHERE ca.request_id = cr.id 
                AND ca.officer_role = 'CAFETERIA'
            )
            """;
        PreparedStatement missingStmt = conn.prepareStatement(missingSql);
        ResultSet missingRs = missingStmt.executeQuery();
        int missingCount = 0;
        while (missingRs.next()) {
            missingCount++;
            System.out.println("   Missing approval " + missingCount + ": Request ID=" + missingRs.getInt("id") +
                             ", Student=" + missingRs.getString("username") +
                             ", Status=" + missingRs.getString("request_status"));
        }
        
        System.out.println("\n=== DEBUG SUMMARY ===");
        System.out.println("Total CAFETERIA approvals: " + approvalCount);
        System.out.println("Requests missing CAFETERIA approvals: " + missingCount);
    }

    private String checkStudentCafeteriaStatus(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking cafeteria status for student: " + studentId);
        
        // CHANGED: Only check, don't create
        checkCafeteriaRecords(conn, studentId);
        
        // FIXED: Added table alias 'cr' to specify which table's 'status' column
        String cafeteriaSql = """
            SELECT 
                COUNT(*) as total_records,
                SUM(CASE WHEN cr.record_type = 'OUTSTANDING_BALANCE' AND cr.status != 'Paid' THEN 1 ELSE 0 END) as unpaid_balances,
                SUM(CASE WHEN cr.record_type = 'MEAL_PLAN_FEE' AND cr.status != 'Paid' THEN 1 ELSE 0 END) as unpaid_meal_plans,
                SUM(CASE WHEN cr.record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND cr.status != 'Paid' THEN cr.amount ELSE 0 END) as total_balance,
                SUM(CASE WHEN cr.record_type = 'MEAL_SWIPES' AND cr.status = 'Active' THEN cr.amount ELSE 0 END) as remaining_meals
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
            
            System.out.println("  Cafeteria issues - Unpaid balances: " + unpaidBalances + 
                             ", Unpaid meal plans: " + unpaidMealPlans +
                             ", Remaining meals: " + remainingMeals +
                             ", Total balance: $" + totalBalance);
            
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
            SELECT cr.meal_plan_type 
            FROM cafeteria_records cr
            JOIN users u ON cr.student_id = u.id 
            WHERE u.username = ? 
            AND cr.record_type = 'MEAL_PLAN' 
            ORDER BY cr.transaction_date DESC 
            LIMIT 1
            """;
            
        PreparedStatement ps = conn.prepareStatement(mealPlanSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            String mealPlan = rs.getString("meal_plan_type");
            System.out.println("  Meal plan: " + mealPlan);
            return mealPlan;
        }
        
        System.out.println("  No meal plan found");
        return "No Meal Plan";
    }

    private void checkCafeteriaRecords(Connection conn, String studentId) throws SQLException {
        System.out.println("DEBUG: Checking cafeteria records for: " + studentId);
        
        String checkRecordsSql = """
            SELECT COUNT(*) as record_count,
                   MAX(CASE WHEN cr.status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as has_pending_items,
                   SUM(CASE WHEN cr.record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND cr.status != 'Paid' THEN cr.amount ELSE 0 END) as total_balance
            FROM cafeteria_records cr
            JOIN users u ON cr.student_id = u.id
            WHERE u.username = ?
            """;
            
        PreparedStatement checkStmt = conn.prepareStatement(checkRecordsSql);
        checkStmt.setString(1, studentId);
        ResultSet rs = checkStmt.executeQuery();
        
        if (rs.next()) {
            int recordCount = rs.getInt("record_count");
            System.out.println("  Found " + recordCount + " cafeteria records");
            
            if (recordCount == 0) {
                System.out.println("  ⚠️ No cafeteria records exist for this student.");
                System.out.println("  Cafeteria officer should manually create records if needed.");
            }
        }
    }
    private void checkAndCreateTestData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("\n=== DEBUG: Checking if we need test data ===");
            
            // Check how many students have CAFETERIA approvals
            String checkApprovalsSql = """
                SELECT COUNT(*) as count 
                FROM clearance_approvals 
                WHERE officer_role = 'CAFETERIA' 
                AND status = 'PENDING'
                """;
            PreparedStatement checkStmt = conn.prepareStatement(checkApprovalsSql);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int pendingCount = rs.getInt("count");
            
            System.out.println("Pending CAFETERIA approvals: " + pendingCount);
            
            if (pendingCount == 0) {
                System.out.println("⚠️ No pending CAFETERIA approvals found. Creating test data...");
                createTestClearanceRequests();
            }
        } catch (Exception e) {
            System.err.println("Error checking test data: " + e.getMessage());
        }
    }
    
    private void createTestClearanceRequests() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("\n=== DEBUG: Creating test clearance requests for Cafeteria ===");
            
            // Get some random students
            String studentsSql = """
                SELECT u.id, u.username, u.full_name 
                FROM users u 
                WHERE u.role = 'STUDENT' 
                LIMIT 2
                """;
            
            PreparedStatement studentsStmt = conn.prepareStatement(studentsSql);
            ResultSet studentsRs = studentsStmt.executeQuery();
            
            int createdCount = 0;
            while (studentsRs.next()) {
                int studentId = studentsRs.getInt("id");
                String username = studentsRs.getString("username");
                
                // Check if student already has a pending clearance request
                String checkRequestSql = """
                    SELECT cr.id 
                    FROM clearance_requests cr 
                    WHERE cr.student_id = ? 
                    AND cr.status IN ('PENDING', 'IN_PROGRESS')
                    LIMIT 1
                    """;
                
                PreparedStatement checkRequestStmt = conn.prepareStatement(checkRequestSql);
                checkRequestStmt.setInt(1, studentId);
                ResultSet requestRs = checkRequestStmt.executeQuery();
                
                if (!requestRs.next()) {
                    // Create clearance request
                    String insertRequestSql = """
                        INSERT INTO clearance_requests (student_id, request_date, status, remarks)
                        VALUES (?, NOW(), 'PENDING', 'Test clearance request for cafeteria')
                        """;
                    
                    PreparedStatement insertRequestStmt = conn.prepareStatement(insertRequestSql, Statement.RETURN_GENERATED_KEYS);
                    insertRequestStmt.setInt(1, studentId);
                    insertRequestStmt.executeUpdate();
                    
                    ResultSet keys = insertRequestStmt.getGeneratedKeys();
                    if (keys.next()) {
                        int requestId = keys.getInt(1);
                        
                        // Create CAFETERIA approval record
                        String insertApprovalSql = """
                            INSERT INTO clearance_approvals (request_id, officer_role, officer_id, status, remarks)
                            VALUES (?, 'CAFETERIA', NULL, 'PENDING', 'Awaiting cafeteria clearance')
                            """;
                        
                        PreparedStatement insertApprovalStmt = conn.prepareStatement(insertApprovalSql);
                        insertApprovalStmt.setInt(1, requestId);
                        insertApprovalStmt.executeUpdate();
                        
                        createdCount++;
                        System.out.println("✅ Created test request for: " + username + " (ID: " + requestId + ")");
                    }
                }
            }
            
            System.out.println("✅ Created " + createdCount + " test clearance requests");
            
            if (createdCount > 0) {
                // Refresh the table
                loadPendingRequests();
                showAlert("Test Data Created", "Created " + createdCount + " test clearance requests for cafeteria review.");
            }
            
        } catch (Exception e) {
            System.err.println("Error creating test clearance requests: " + e.getMessage());
            e.printStackTrace();
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

    private void viewStudentCafeteriaRecords(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Viewing cafeteria records for: " + request.getStudentId());
        loadStudentCafeteriaRecords(request.getStudentId());
        
        if (lblStudentInfo != null) {
            lblStudentInfo.setText("Cafeteria Records for: " + request.getStudentName() + 
                              " (" + request.getStudentId() + ") - Meal Plan: " + request.getMealPlan());
        }
        
        updateCafeteriaSummary(request.getStudentId());
        
        // Switch to Cafeteria Records tab
        if (mainTabPane != null) {
            for (Tab tab : mainTabPane.getTabs()) {
                if (tab.getText() != null && tab.getText().contains("Records")) {
                    mainTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    private void loadStudentCafeteriaRecords(String studentId) {
        System.out.println("Loading cafeteria records for: " + studentId);
        cafeteriaData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    cr.record_type,
                    cr.description,
                    cr.amount,
                    cr.transaction_date,
                    cr.status,
                    cr.meal_plan_type
                FROM cafeteria_records cr
                JOIN users u ON cr.student_id = u.id
                WHERE u.username = ?
                ORDER BY 
                    CASE 
                        WHEN cr.status IN ('Pending', 'Active') THEN 1
                        ELSE 2
                    END,
                    cr.transaction_date DESC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
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
            System.out.println("Loaded " + count + " cafeteria records");
            
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
                    SUM(CASE WHEN cr.status IN ('Pending', 'Active') THEN 1 ELSE 0 END) as pending_items,
                    SUM(CASE WHEN cr.record_type IN ('OUTSTANDING_BALANCE', 'MEAL_PLAN_FEE') AND cr.status != 'Paid' THEN cr.amount ELSE 0 END) as total_balance,
                    SUM(CASE WHEN cr.record_type = 'MEAL_SWIPES' AND cr.status = 'Active' THEN cr.amount ELSE 0 END) as remaining_meals
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
                    if (lblCafeteriaStatus != null) {
                        lblCafeteriaStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                } else if (totalBalance > 0) {
                    summary = String.format("❌ %d pending items | Outstanding Balance: $%.2f", pendingItems, totalBalance);
                    if (lblCafeteriaStatus != null) {
                        lblCafeteriaStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                } else if (remainingMeals > 0) {
                    summary = String.format("⚠️ %d unused meals remaining", remainingMeals);
                    if (lblCafeteriaStatus != null) {
                        lblCafeteriaStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                } else {
                    summary = String.format("ℹ️ %d pending items to review", pendingItems);
                    if (lblCafeteriaStatus != null) {
                        lblCafeteriaStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    }
                }
                
                if (lblCafeteriaStatus != null) {
                    lblCafeteriaStatus.setText(summary);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Approving cafeteria clearance for: " + request.getStudentId());
        
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
                System.out.println("Approval cancelled due to cafeteria issues");
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
                System.out.println("Approving cafeteria clearance for: " + request.getStudentId());
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Cafeteria clearance approved. All charges cleared.");
                loadPendingRequests();
                showAlert("Approved", "Cafeteria clearance approved for " + request.getStudentName());
            } else {
                System.out.println("Approval cancelled by user");
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        System.out.println("\n=== DEBUG: Rejecting cafeteria clearance for: " + request.getStudentId());
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Cafeteria Clearance");
        dialog.setHeaderText("Reject Cafeteria Clearance");
        dialog.setContentText("Enter reason for rejecting cafeteria clearance for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            System.out.println("Rejection reason: " + result.get());
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Cafeteria clearance rejected: " + result.get().trim());
            loadPendingRequests();
            showAlert("Rejected", "Cafeteria clearance rejected for " + request.getStudentName());
        } else {
            System.out.println("Rejection cancelled");
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        System.out.println("\n=== DEBUG: Updating cafeteria clearance status ===");
        System.out.println("Request ID: " + requestId);
        System.out.println("Status: " + status);
        System.out.println("Remarks: " + remarks);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = """
                SELECT COUNT(*) FROM clearance_approvals 
                WHERE request_id = ? AND officer_role = 'CAFETERIA'
                """;
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            System.out.println("Existing CAFETERIA approval records: " + count);
            
            if (count > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE clearance_approvals 
                    SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                    WHERE request_id = ? AND officer_role = 'CAFETERIA'
                    """;
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setString(1, status);
                ps.setString(2, remarks);
                ps.setInt(3, currentUser.getId());
                ps.setInt(4, requestId);
                int updated = ps.executeUpdate();
                System.out.println("Updated " + updated + " record(s)");
            } else {
                // Insert new record
                String insertSql = """
                    INSERT INTO clearance_approvals (request_id, officer_role, officer_id, status, remarks, approval_date)
                    VALUES (?, 'CAFETERIA', ?, ?, ?, NOW())
                    """;
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setInt(1, requestId);
                ps.setInt(2, currentUser.getId());
                ps.setString(3, status);
                ps.setString(4, remarks);
                int inserted = ps.executeUpdate();
                System.out.println("Inserted " + inserted + " record(s)");
            }
            
            System.out.println("Cafeteria clearance status updated successfully");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR updating clearance status: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
        }
    }

    @FXML
    private void generateCafeteriaReport() {
        System.out.println("\n=== DEBUG: Generating cafeteria report ===");
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No student selected");
            showAlert("Selection Required", "Please select a student first to generate cafeteria report.");
            return;
        }
        
        System.out.println("Generating report for: " + selected.getStudentId());
        
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
                
                System.out.println("Report generated successfully");
                showAlert("Cafeteria Report", report);
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR generating report: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to generate cafeteria report: " + e.getMessage());
        }
    }

    @FXML
    private void createTestData() {
        System.out.println("\n=== DEBUG: Manual test data creation ===");
        createTestClearanceRequests();
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