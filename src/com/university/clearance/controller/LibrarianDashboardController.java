package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.io.IOException;

public class LibrarianDashboardController implements Initializable {
    
    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblTotalBooks;
    @FXML private Label lblActiveBorrowers;
    @FXML private Label lblOverdueCount;
    @FXML private Label lblTotalRequests;
    @FXML private Label lblClearCount;
    @FXML private Label lblWarningCount;
    @FXML private Label lblIssueCount;
    @FXML private Label lblTotalBorrowed;
    @FXML private Label lblCurrentlyBorrowed;
    @FXML private Label lblTotalFines;
    @FXML private Label lblDetailStudentId;
    @FXML private Label lblDetailStudentName;
    @FXML private Label lblDetailDepartment;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailEmail;
    @FXML private Label lblDetailPhone;
    @FXML private Label lblDetailTotalBorrowed;
    @FXML private Label lblDetailCurrentBorrowed;
    @FXML private Label lblDetailOverdue;
    @FXML private Label lblDetailTotalFines;
    
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabStudentDetails;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colBookStatus;
    @FXML private TableColumn<ClearanceRequest, Void> colActions;
    
    @FXML private TableView<BorrowingRecord> tableBorrowingHistory;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryStudentId;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryStudentName;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryDepartment;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryBookTitle;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryBorrowDate;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryDueDate;
    @FXML private TableColumn<BorrowingRecord, String> colHistoryStatus;
    @FXML private TableColumn<BorrowingRecord, Double> colHistoryFine;
    
    @FXML private TableView<BookDetail> tableStudentBookDetails;
    @FXML private TableColumn<BookDetail, String> colDetailBookTitle;
    @FXML private TableColumn<BookDetail, String> colDetailAuthor;
    @FXML private TableColumn<BookDetail, String> colDetailBorrowDate;
    @FXML private TableColumn<BookDetail, String> colDetailDueDate;
    @FXML private TableColumn<BookDetail, String> colDetailReturnDate;
    @FXML private TableColumn<BookDetail, String> colDetailStatus;
    @FXML private TableColumn<BookDetail, Double> colDetailFine;
    
    @FXML private TextField txtSearchStudent;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Button btnApproveFromDetails;
    @FXML private Button btnRejectFromDetails;
    @FXML private Button btnLogout;
    
    private ObservableList<ClearanceRequest> clearanceRequests;
    private ObservableList<BorrowingRecord> borrowingHistory;
    private ObservableList<BookDetail> studentBookDetails;
    private FilteredList<BorrowingRecord> filteredBorrowingHistory;
    
    private Connection connection;
    private int currentUserId;
    private String currentLibrarianName;
    private ClearanceRequest selectedRequest;
    private int currentStudentId;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Get database connection
            connection = DatabaseConnection.getConnection();
            
            // Get current librarian info (from session)
            currentUserId = getCurrentUserId(); // This should be set during login
            currentLibrarianName = getCurrentUserName();
            lblWelcome.setText("Welcome, " + currentLibrarianName);
            
            // Initialize observable lists
            clearanceRequests = FXCollections.observableArrayList();
            borrowingHistory = FXCollections.observableArrayList();
            studentBookDetails = FXCollections.observableArrayList();
            
            // Setup combo box for filtering
            cmbStatusFilter.getItems().addAll("All", "BORROWED", "RETURNED", "OVERDUE");
            cmbStatusFilter.setValue("All");
            
            // Configure tables
            configureClearanceTable();
            configureBorrowingHistoryTable();
            configureStudentDetailsTable();
            
            // Load initial data
            refreshAllData();
            
            // Set up search filtering for borrowing history
            setupSearchFilter();
            
        } catch (Exception e) {
            showAlert("Initialization Error", "Cannot initialize dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int getCurrentUserId() {
        // This should come from session/login. For now, hardcode librarian ID
        try {
            String query = "SELECT id FROM users WHERE username = 'librarian' AND role = 'LIBRARIAN'";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 2; // Default librarian ID from your database
    }
    
    private String getCurrentUserName() {
        try {
            String query = "SELECT full_name FROM users WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("full_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Librarian";
    }
    
    private void configureClearanceTable() {
        // Set up cell value factories
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colBookStatus.setCellValueFactory(new PropertyValueFactory<>("bookStatus"));
        
        // Set up actions column with buttons
        colActions.setCellFactory(new Callback<TableColumn<ClearanceRequest, Void>, TableCell<ClearanceRequest, Void>>() {
            @Override
            public TableCell<ClearanceRequest, Void> call(TableColumn<ClearanceRequest, Void> param) {
                return new TableCell<ClearanceRequest, Void>() {
                    private final Button btnDetails = new Button("📋 Details");
                    private final Button btnApprove = new Button("✅ Approve");
                    private final Button btnReject = new Button("❌ Reject");
                    private final HBox buttonBox = new HBox(5, btnDetails, btnApprove, btnReject);
                    
                    {
                        btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px;");
                        btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px;");
                        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
                        
                        btnDetails.setOnAction(e -> {
                            ClearanceRequest request = getTableView().getItems().get(getIndex());
                            showStudentDetails(request);
                        });
                        
                        btnApprove.setOnAction(e -> {
                            ClearanceRequest request = getTableView().getItems().get(getIndex());
                            approveClearance(request);
                        });
                        
                        btnReject.setOnAction(e -> {
                            ClearanceRequest request = getTableView().getItems().get(getIndex());
                            rejectClearance(request);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonBox);
                        }
                    }
                };
            }
        });
        
        tableRequests.setItems(clearanceRequests);
    }
    
    private void configureBorrowingHistoryTable() {
        colHistoryStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colHistoryStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colHistoryDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colHistoryBookTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        colHistoryBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        colHistoryDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colHistoryStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colHistoryFine.setCellValueFactory(new PropertyValueFactory<>("fine"));
        
        // Add styling for status column
        colHistoryStatus.setCellFactory(column -> new TableCell<BorrowingRecord, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "BORROWED":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "RETURNED":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "OVERDUE":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        tableBorrowingHistory.setItems(borrowingHistory);
    }
    
    private void configureStudentDetailsTable() {
        colDetailBookTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        colDetailAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colDetailBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        colDetailDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colDetailReturnDate.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        colDetailStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDetailFine.setCellValueFactory(new PropertyValueFactory<>("fine"));
        
        // Add styling for status column
        colDetailStatus.setCellFactory(column -> new TableCell<BookDetail, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "BORROWED":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "RETURNED":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "OVERDUE":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        tableStudentBookDetails.setItems(studentBookDetails);
    }
    
    @FXML
    private void refreshRequests() {
        loadClearanceRequests();
        updateDashboardStats();
    }
    
    @FXML
    private void refreshBorrowingHistory() {
        loadBorrowingHistory();
        updateHistoryStats();
    }
    
    private void refreshAllData() {
        refreshRequests();
        refreshBorrowingHistory();
    }
    
    private void loadClearanceRequests() {
        clearanceRequests.clear();
        try {
            // Get pending clearance requests for librarian approval
            String query = """
                SELECT 
                    cr.id as request_id,
                    u.id as student_id,
                    u.username,
                    u.full_name,
                    u.department,
                    DATE(cr.request_date) as request_date,
                    ca.status as approval_status,
                    lcs.total_borrowed,
                    lcs.overdue_count,
                    lcs.total_fine
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.officer_role = 'LIBRARIAN'
                JOIN librarian_clearance_status lcs ON u.id = lcs.student_id
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                  AND ca.status = 'PENDING'
                ORDER BY cr.request_date DESC
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            int clearCount = 0, warningCount = 0, issueCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("username");
                int totalBorrowed = rs.getInt("total_borrowed");
                int overdueCount = rs.getInt("overdue_count");
                double totalFine = rs.getDouble("total_fine");
                
                // Determine book status based on borrowing records
                String bookStatus = determineBookStatus(totalBorrowed, overdueCount, totalFine);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getInt("request_id"),
                    studentId,
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getDate("request_date").toLocalDate(),
                    bookStatus,
                    totalBorrowed,
                    overdueCount,
                    totalFine
                );
                
                clearanceRequests.add(request);
                
                // Count status types
                switch (bookStatus) {
                    case "CLEAR":
                        clearCount++;
                        break;
                    case "WARNING":
                        warningCount++;
                        break;
                    case "ISSUE":
                        issueCount++;
                        break;
                }
            }
            
            // Update counters
            lblTotalRequests.setText(String.valueOf(clearanceRequests.size()));
            lblClearCount.setText("Clear: " + clearCount);
            lblWarningCount.setText("Warning: " + warningCount);
            lblIssueCount.setText("Issue: " + issueCount);
            lblPendingCount.setText("Pending: " + clearanceRequests.size());
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String determineBookStatus(int totalBorrowed, int overdueCount, double totalFine) {
        if (totalBorrowed == 0) {
            return "CLEAR";
        } else if (overdueCount > 0 || totalFine > 0) {
            return "ISSUE";
        } else {
            return "WARNING";
        }
    }
    
    private void loadBorrowingHistory() {
        borrowingHistory.clear();
        try {
            String query = """
                SELECT 
                    bb.id,
                    u.username as student_id,
                    u.full_name,
                    u.department,
                    bb.book_title,
                    DATE(bb.borrow_date) as borrow_date,
                    DATE(bb.due_date) as due_date,
                    DATE(bb.return_date) as return_date,
                    bb.status,
                    bb.fine_amount
                FROM book_borrowings bb
                JOIN users u ON bb.student_id = u.id
                ORDER BY bb.borrow_date DESC
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BorrowingRecord record = new BorrowingRecord(
                    rs.getInt("id"),
                    rs.getString("student_id"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("book_title"),
                    rs.getDate("borrow_date") != null ? rs.getDate("borrow_date").toLocalDate() : null,
                    rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                    rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null,
                    rs.getString("status"),
                    rs.getDouble("fine_amount")
                );
                
                borrowingHistory.add(record);
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load borrowing history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadStudentBookDetails(int studentId) {
        studentBookDetails.clear();
        currentStudentId = studentId;
        
        try {
            String query = """
                SELECT 
                    bb.book_title,
                    'Unknown' as author,
                    DATE(bb.borrow_date) as borrow_date,
                    DATE(bb.due_date) as due_date,
                    DATE(bb.return_date) as return_date,
                    bb.status,
                    bb.fine_amount
                FROM book_borrowings bb
                WHERE bb.student_id = ?
                ORDER BY bb.borrow_date DESC
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            int totalBorrowed = 0, currentBorrowed = 0, overdue = 0;
            double totalFines = 0;
            
            while (rs.next()) {
                totalBorrowed++;
                
                BookDetail detail = new BookDetail(
                    rs.getString("book_title"),
                    rs.getString("author"),
                    rs.getDate("borrow_date") != null ? rs.getDate("borrow_date").toLocalDate() : null,
                    rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                    rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null,
                    rs.getString("status"),
                    rs.getDouble("fine_amount")
                );
                
                studentBookDetails.add(detail);
                
                if ("BORROWED".equals(detail.getStatus()) || "OVERDUE".equals(detail.getStatus())) {
                    currentBorrowed++;
                }
                if ("OVERDUE".equals(detail.getStatus())) {
                    overdue++;
                }
                totalFines += detail.getFine();
            }
            
            // Update summary labels
            lblDetailTotalBorrowed.setText(String.valueOf(totalBorrowed));
            lblDetailCurrentBorrowed.setText(String.valueOf(currentBorrowed));
            lblDetailOverdue.setText(String.valueOf(overdue));
            lblDetailTotalFines.setText(String.format("₦%.2f", totalFines));
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load student book details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadStudentInfo(String username) {
        try {
            String query = """
                SELECT 
                    u.id,
                    u.username,
                    u.full_name,
                    u.department,
                    u.email,
                    u.phone,
                    cr.status as clearance_status
                FROM users u
                LEFT JOIN clearance_requests cr ON u.id = cr.student_id 
                    AND cr.id = (SELECT MAX(id) FROM clearance_requests WHERE student_id = u.id)
                WHERE u.username = ?
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                currentStudentId = rs.getInt("id");
                lblDetailStudentId.setText(rs.getString("username"));
                lblDetailStudentName.setText(rs.getString("full_name"));
                lblDetailDepartment.setText(rs.getString("department"));
                lblDetailEmail.setText(rs.getString("email"));
                lblDetailPhone.setText(rs.getString("phone"));
                
                String status = rs.getString("clearance_status");
                lblDetailStatus.setText(status != null ? status : "Not Applied");
                updateStatusLabelStyle(lblDetailStatus, status);
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load student information: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateStatusLabelStyle(Label label, String status) {
        if (status == null) return;
        
        switch (status) {
            case "APPROVED":
            case "FULLY_CLEARED":
                label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                break;
            case "REJECTED":
                label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                break;
            case "PENDING":
            case "IN_PROGRESS":
                label.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                break;
            default:
                label.setStyle("");
        }
    }
    
    @FXML
    private void showStudentDetails(ClearanceRequest request) {
        selectedRequest = request;
        
        // Load student information
        loadStudentInfo(request.getStudentId());
        loadStudentBookDetails(currentStudentId);
        
        // Update button states based on request status
        if ("PENDING".equals(selectedRequest.getApprovalStatus()) || 
            "IN_PROGRESS".equals(selectedRequest.getApprovalStatus())) {
            btnApproveFromDetails.setDisable(false);
            btnRejectFromDetails.setDisable(false);
        } else {
            btnApproveFromDetails.setDisable(true);
            btnRejectFromDetails.setDisable(true);
        }
        
        // Switch to student details tab
        if (!mainTabPane.getTabs().contains(tabStudentDetails)) {
            mainTabPane.getTabs().add(tabStudentDetails);
        }
        mainTabPane.getSelectionModel().select(tabStudentDetails);
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
    
    @FXML
    private void goBackToRequests() {
        mainTabPane.getSelectionModel().select(0);
    }
    
    @FXML
    private void handleTabClosed() {
        selectedRequest = null;
        currentStudentId = 0;
        studentBookDetails.clear();
    }
    
    private void approveClearance(ClearanceRequest request) {
        try {
            // Check if student has any issues
            if ("ISSUE".equals(request.getBookStatus())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Student has book issues");
                alert.setContentText("This student has overdue books or unpaid fines. Are you sure you want to approve?");
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        updateClearanceStatus(request.getRequestId(), "APPROVED", "Library clearance approved with issues");
                    }
                });
            } else {
                updateClearanceStatus(request.getRequestId(), "APPROVED", "Library clearance approved");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to approve clearance: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Clearance");
        dialog.setHeaderText("Reason for rejection");
        dialog.setContentText("Please enter the reason:");
        
        dialog.showAndWait().ifPresent(reason -> {
            if (!reason.trim().isEmpty()) {
                updateClearanceStatus(request.getRequestId(), "REJECTED", reason);
            }
        });
    }
    
    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try {
            String query = """
                UPDATE clearance_approvals 
                SET status = ?, 
                    officer_id = ?,
                    remarks = ?, 
                    approval_date = NOW() 
                WHERE request_id = ? 
                  AND officer_role = 'LIBRARIAN'
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, status);
            pstmt.setInt(2, currentUserId);
            pstmt.setString(3, remarks);
            pstmt.setInt(4, requestId);
            pstmt.executeUpdate();
            
            // Update overall clearance request status if needed
            if ("APPROVED".equals(status)) {
                checkIfAllApproved(requestId);
            }
            
            showAlert("Success", "Clearance " + status + " successfully!");
            refreshRequests();
            
            // If we're in the details tab, update the status label
            if (currentStudentId > 0 && selectedRequest != null && selectedRequest.getRequestId() == requestId) {
                lblDetailStatus.setText(status);
                updateStatusLabelStyle(lblDetailStatus, status);
                btnApproveFromDetails.setDisable(true);
                btnRejectFromDetails.setDisable(true);
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkIfAllApproved(int requestId) {
        try {
            String query = """
                SELECT COUNT(*) as pending_count
                FROM clearance_approvals
                WHERE request_id = ? AND status != 'APPROVED'
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt("pending_count") == 0) {
                // All departments approved, update clearance request
                String updateQuery = "UPDATE clearance_requests SET status = 'FULLY_CLEARED', completion_date = NOW() WHERE id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setInt(1, requestId);
                updateStmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void searchBorrowingHistory() {
        String searchText = txtSearchStudent.getText().toLowerCase();
        String selectedStatus = cmbStatusFilter.getValue();
        
        filteredBorrowingHistory.setPredicate(record -> {
            boolean matchesSearch = searchText.isEmpty() ||
                record.getStudentId().toLowerCase().contains(searchText) ||
                record.getStudentName().toLowerCase().contains(searchText) ||
                record.getBookTitle().toLowerCase().contains(searchText);
            
            boolean matchesStatus = "All".equals(selectedStatus) ||
                record.getStatus().equals(selectedStatus);
            
            return matchesSearch && matchesStatus;
        });
    }
    
    @FXML
    private void clearFilters() {
        txtSearchStudent.clear();
        cmbStatusFilter.setValue("All");
        filteredBorrowingHistory.setPredicate(null);
    }
    
    private void setupSearchFilter() {
        filteredBorrowingHistory = new FilteredList<>(borrowingHistory, p -> true);
        SortedList<BorrowingRecord> sortedData = new SortedList<>(filteredBorrowingHistory);
        sortedData.comparatorProperty().bind(tableBorrowingHistory.comparatorProperty());
        tableBorrowingHistory.setItems(sortedData);
        
        txtSearchStudent.textProperty().addListener((observable, oldValue, newValue) -> {
            searchBorrowingHistory();
        });
        
        cmbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            searchBorrowingHistory();
        });
    }
    
    private void updateDashboardStats() {
        try {
            // Total books borrowed count
            String totalBooksQuery = "SELECT COUNT(*) as count FROM book_borrowings";
            PreparedStatement pstmt1 = connection.prepareStatement(totalBooksQuery);
            ResultSet rs1 = pstmt1.executeQuery();
            if (rs1.next()) {
                lblTotalBooks.setText(String.valueOf(rs1.getInt("count")));
            }
            
            // Active borrowers
            String activeBorrowersQuery = """
                SELECT COUNT(DISTINCT student_id) as count 
                FROM book_borrowings 
                WHERE status IN ('BORROWED', 'OVERDUE')
                """;
            PreparedStatement pstmt2 = connection.prepareStatement(activeBorrowersQuery);
            ResultSet rs2 = pstmt2.executeQuery();
            if (rs2.next()) {
                lblActiveBorrowers.setText(String.valueOf(rs2.getInt("count")));
            }
            
            // Overdue count
            String overdueQuery = """
                SELECT COUNT(*) as count 
                FROM book_borrowings 
                WHERE due_date < CURDATE() AND return_date IS NULL
                """;
            PreparedStatement pstmt3 = connection.prepareStatement(overdueQuery);
            ResultSet rs3 = pstmt3.executeQuery();
            if (rs3.next()) {
                lblOverdueCount.setText(String.valueOf(rs3.getInt("count")));
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load dashboard stats: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateHistoryStats() {
        int totalBorrowed = borrowingHistory.size();
        long currentlyBorrowed = borrowingHistory.stream()
            .filter(r -> "BORROWED".equals(r.getStatus()) || "OVERDUE".equals(r.getStatus()))
            .count();
        double totalFines = borrowingHistory.stream()
            .mapToDouble(BorrowingRecord::getFine)
            .sum();
        
        lblTotalBorrowed.setText(String.valueOf(totalBorrowed));
        lblCurrentlyBorrowed.setText(String.valueOf(currentlyBorrowed));
        lblTotalFines.setText(String.format("₦%.2f", totalFines));
    }
    
    @FXML
    private void handleLogout() {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Logout");
            alert.setHeaderText("Confirm Logout");
            alert.setContentText("Are you sure you want to logout?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Close database connection
                        if (connection != null && !connection.isClosed()) {
                            connection.close();
                        }
                        
                        // Load login screen
                        Stage stage = (Stage) btnLogout.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/clearance/resources/views/Login.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.setTitle("University Clearance Management System - Login");
                        stage.centerOnScreen();
                        
                    } catch (IOException e) {
                        showAlert("Error", "Failed to load login screen: " + e.getMessage());
                        e.printStackTrace();
                    } catch (SQLException e) {
                        showAlert("Database Error", "Failed to close database connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            
        } catch (Exception e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Model classes
    public static class ClearanceRequest {
        private final int requestId;
        private final String studentId;
        private final String studentName;
        private final String department;
        private final LocalDate requestDate;
        private final String bookStatus;
        private final int totalBorrowed;
        private final int overdueCount;
        private final double totalFine;
        private String approvalStatus;
        
        public ClearanceRequest(int requestId, String studentId, String studentName, 
                               String department, LocalDate requestDate, String bookStatus,
                               int totalBorrowed, int overdueCount, double totalFine) {
            this.requestId = requestId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.requestDate = requestDate;
            this.bookStatus = bookStatus;
            this.totalBorrowed = totalBorrowed;
            this.overdueCount = overdueCount;
            this.totalFine = totalFine;
            this.approvalStatus = "PENDING";
        }
        
        // Getters
        public int getRequestId() { return requestId; }
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public LocalDate getRequestDate() { return requestDate; }
        public String getBookStatus() { return bookStatus; }
        public int getTotalBorrowed() { return totalBorrowed; }
        public int getOverdueCount() { return overdueCount; }
        public double getTotalFine() { return totalFine; }
        public String getApprovalStatus() { return approvalStatus; }
    }
    
    public static class BorrowingRecord {
        private final int borrowId;
        private final String studentId;
        private final String studentName;
        private final String department;
        private final String bookTitle;
        private final LocalDate borrowDate;
        private final LocalDate dueDate;
        private final LocalDate returnDate;
        private final String status;
        private final double fine;
        
        public BorrowingRecord(int borrowId, String studentId, String studentName, String department,
                              String bookTitle, LocalDate borrowDate, LocalDate dueDate, 
                              LocalDate returnDate, String status, double fine) {
            this.borrowId = borrowId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.bookTitle = bookTitle;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.returnDate = returnDate;
            this.status = status;
            this.fine = fine;
        }
        
        // Getters
        public int getBorrowId() { return borrowId; }
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getBookTitle() { return bookTitle; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
        public double getFine() { return fine; }
    }
    
    public static class BookDetail {
        private final String bookTitle;
        private final String author;
        private final LocalDate borrowDate;
        private final LocalDate dueDate;
        private final LocalDate returnDate;
        private final String status;
        private final double fine;
        
        public BookDetail(String bookTitle, String author, LocalDate borrowDate, 
                         LocalDate dueDate, LocalDate returnDate, String status, double fine) {
            this.bookTitle = bookTitle;
            this.author = author;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.returnDate = returnDate;
            this.status = status;
            this.fine = fine;
        }
        
        // Getters
        public String getBookTitle() { return bookTitle; }
        public String getAuthor() { return author; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
        public double getFine() { return fine; }
    }
}