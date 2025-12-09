package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

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
            
            // Get current librarian info
            currentUserId = getCurrentUserId();
            currentLibrarianName = getCurrentUserName();
            
            // Set welcome label
            if (lblWelcome != null) {
                lblWelcome.setText("Welcome, " + currentLibrarianName);
            }
            
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
            
            // Apply CSS for active tab indicator
            applyActiveTabStyling();
            
            // Load initial data
            Platform.runLater(() -> {
                refreshAllData();
                showToastNotification("Dashboard loaded successfully");
            });
            
            // Set up search filtering
            setupSearchFilter();
            
            // Add keyboard shortcuts
            Platform.runLater(() -> setupKeyboardShortcuts());
            
            // Add tooltips
            Platform.runLater(() -> setupTooltips());
            
        } catch (Exception e) {
            showErrorAlert("Initialization Error", "Cannot initialize dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Apply CSS styling for active tab
    private void applyActiveTabStyling() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                // This will work with the CSS we'll add
                newTab.getStyleClass().add("active-tab");
            }
            if (oldTab != null) {
                oldTab.getStyleClass().remove("active-tab");
            }
        });
    }
    
    public void setCurrentUser(int userId, String userName) {
        this.currentUserId = userId;
        this.currentLibrarianName = userName;
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + currentLibrarianName);
        }
    }
    
    private void setupKeyboardShortcuts() {
        tableRequests.getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case R:
                        refreshRequests();
                        event.consume();
                        break;
                    case F:
                        if (mainTabPane.getSelectionModel().getSelectedIndex() == 1) {
                            txtSearchStudent.requestFocus();
                            event.consume();
                        }
                        break;
                    case A:
                        if (mainTabPane.getSelectionModel().getSelectedIndex() == 2 && 
                            btnApproveFromDetails.isVisible() && !btnApproveFromDetails.isDisabled()) {
                            approveFromDetails();
                            event.consume();
                        }
                        break;
                }
            }
        });
    }
    
    private void setupTooltips() {
        btnApproveFromDetails.setTooltip(new Tooltip("Approve this student's library clearance (Ctrl+A)"));
        btnRejectFromDetails.setTooltip(new Tooltip("Reject this student's library clearance"));
        txtSearchStudent.setTooltip(new Tooltip("Search by student ID, name, or book title"));
        cmbStatusFilter.setTooltip(new Tooltip("Filter borrowing records by status"));
    }
    
    private int getCurrentUserId() {
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
        return 2;
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
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colBookStatus.setCellValueFactory(new PropertyValueFactory<>("bookStatus"));
        
        colBookStatus.setCellFactory(column -> new TableCell<ClearanceRequest, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(status);
                    switch (status) {
                        case "CLEAR":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            setTooltip(new Tooltip("No borrowed books, overdue items, or fines"));
                            break;
                        case "WARNING":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            setTooltip(new Tooltip("Has borrowed books but no overdue items or fines"));
                            break;
                        case "ISSUE":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            setTooltip(new Tooltip("Has overdue books and/or unpaid fines"));
                            break;
                        default:
                            setStyle("");
                            setTooltip(null);
                    }
                }
            }
        });
        
        colActions.setCellFactory(new Callback<TableColumn<ClearanceRequest, Void>, TableCell<ClearanceRequest, Void>>() {
            @Override
            public TableCell<ClearanceRequest, Void> call(TableColumn<ClearanceRequest, Void> param) {
                return new TableCell<ClearanceRequest, Void>() {
                    private final Button btnDetails = new Button("📋 Details");
                    private final Button btnApprove = new Button("✅ Approve");
                    private final Button btnReject = new Button("❌ Reject");
                    private final HBox buttonBox = new HBox(5, btnDetails, btnApprove, btnReject);
                    
                    {
                        btnDetails.getStyleClass().add("details-button");
                        btnApprove.getStyleClass().add("approve-button");
                        btnReject.getStyleClass().add("reject-button");
                        
                        btnDetails.setTooltip(new Tooltip("View detailed student information and borrowing records"));
                        btnApprove.setTooltip(new Tooltip("Approve library clearance for this student"));
                        btnReject.setTooltip(new Tooltip("Reject library clearance for this student"));
                        
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
        
        colHistoryFine.setCellFactory(column -> new TableCell<BorrowingRecord, Double>() {
            @Override
            protected void updateItem(Double fine, boolean empty) {
                super.updateItem(fine, empty);
                if (empty || fine == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("₦%.2f", fine));
                    if (fine > 0) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60;");
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
        
        colDetailFine.setCellFactory(column -> new TableCell<BookDetail, Double>() {
            @Override
            protected void updateItem(Double fine, boolean empty) {
                super.updateItem(fine, empty);
                if (empty || fine == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("₦%.2f", fine));
                    if (fine > 0) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60;");
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
        showToastNotification("Requests refreshed successfully");
    }
    
    @FXML
    private void refreshBorrowingHistory() {
        loadBorrowingHistory();
        updateHistoryStats();
        showToastNotification("Borrowing history refreshed");
    }
    
    private void refreshAllData() {
        refreshRequests();
        refreshBorrowingHistory();
    }
    
    private void loadClearanceRequests() {
        clearanceRequests.clear();
        try {
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
            
            lblTotalRequests.setText(String.valueOf(clearanceRequests.size()));
            lblClearCount.setText("Clear: " + clearCount);
            lblWarningCount.setText("Warning: " + warningCount);
            lblIssueCount.setText("Issue: " + issueCount);
            lblPendingCount.setText("Pending: " + clearanceRequests.size());
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load clearance requests: " + e.getMessage());
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
            showErrorAlert("Database Error", "Failed to load borrowing history: " + e.getMessage());
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
            
            lblDetailTotalBorrowed.setText(String.valueOf(totalBorrowed));
            lblDetailCurrentBorrowed.setText(String.valueOf(currentBorrowed));
            lblDetailOverdue.setText(String.valueOf(overdue));
            lblDetailTotalFines.setText(String.format("₦%.2f", totalFines));
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load student book details: " + e.getMessage());
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
            showErrorAlert("Database Error", "Failed to load student information: " + e.getMessage());
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
        
        loadStudentInfo(request.getStudentId());
        loadStudentBookDetails(currentStudentId);
        
        if ("PENDING".equals(selectedRequest.getApprovalStatus()) || 
            "IN_PROGRESS".equals(selectedRequest.getApprovalStatus())) {
            btnApproveFromDetails.setDisable(false);
            btnRejectFromDetails.setDisable(false);
        } else {
            btnApproveFromDetails.setDisable(true);
            btnRejectFromDetails.setDisable(true);
        }
        
        if (!mainTabPane.getTabs().contains(tabStudentDetails)) {
            mainTabPane.getTabs().add(tabStudentDetails);
        }
        mainTabPane.getSelectionModel().select(tabStudentDetails);
        
        showToastNotification("Loaded details for " + request.getStudentName());
    }
    
    @FXML
    private void approveFromDetails() {
        if (selectedRequest != null) {
            approveClearance(selectedRequest);
        } else {
            showWarningAlert("No Selection", "Please select a student first.");
        }
    }
    
    @FXML
    private void rejectFromDetails() {
        if (selectedRequest != null) {
            rejectClearance(selectedRequest);
        } else {
            showWarningAlert("No Selection", "Please select a student first.");
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
            String studentName = request.getStudentName();
            String studentId = request.getStudentId();
            
            if ("ISSUE".equals(request.getBookStatus())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning - Outstanding Issues");
                alert.setHeaderText("⚠ Student has outstanding library issues!");
                alert.setContentText(String.format(
                    "Student: %s (%s)\n\nIssues Found:\n• Overdue books: %d\n• Total fines: ₦%.2f\n\nThis student has overdue books or unpaid fines. Are you sure you want to approve?",
                    studentName, studentId, request.getOverdueCount(), request.getTotalFine()
                ));
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        showProcessingAlert("Approving clearance...");
                        updateClearanceStatus(request.getRequestId(), "APPROVED", 
                            "Library clearance approved with outstanding issues");
                        showSuccessAlert("Clearance Approved", 
                            String.format("Clearance for %s (%s) has been approved despite outstanding issues.", 
                            studentName, studentId));
                    }
                });
            } else if ("WARNING".equals(request.getBookStatus())) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Approval");
                alert.setHeaderText("⚠ Student has borrowed books");
                alert.setContentText(String.format(
                    "Student: %s (%s)\n\nWarning:\n• Currently borrowed books: %d\n\nNote: Student has borrowed books but no overdue items or fines. Approve clearance?",
                    studentName, studentId, request.getTotalBorrowed()
                ));
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        showProcessingAlert("Approving clearance...");
                        updateClearanceStatus(request.getRequestId(), "APPROVED", 
                            "Library clearance approved - borrowed books present");
                        showSuccessAlert("Clearance Approved", 
                            String.format("Clearance for %s (%s) has been approved.", studentName, studentId));
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Approval");
                alert.setHeaderText("✅ Student is clear");
                alert.setContentText(String.format(
                    "Student: %s (%s)\n\nStatus: No borrowed books, overdue items, or fines.\n\nApprove library clearance?",
                    studentName, studentId
                ));
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        showProcessingAlert("Approving clearance...");
                        updateClearanceStatus(request.getRequestId(), "APPROVED", "Library clearance approved - clear status");
                        showSuccessAlert("Clearance Approved", 
                            String.format("Clearance for %s (%s) has been approved.", studentName, studentId));
                    }
                });
            }
        } catch (Exception e) {
            showErrorAlert("Approval Error", "Failed to approve clearance: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void rejectClearance(ClearanceRequest request) {
        String studentName = request.getStudentName();
        String studentId = request.getStudentId();
        
        StringBuilder rejectionReasons = new StringBuilder();
        if (request.getOverdueCount() > 0) {
            rejectionReasons.append("• ").append(request.getOverdueCount()).append(" overdue book(s)\n");
        }
        if (request.getTotalFine() > 0) {
            rejectionReasons.append("• Unpaid fines: ₦").append(String.format("%.2f", request.getTotalFine())).append("\n");
        }
        if (request.getTotalBorrowed() > 0) {
            rejectionReasons.append("• ").append(request.getTotalBorrowed()).append(" book(s) still borrowed\n");
        }
        
        Alert confirmationAlert = new Alert(Alert.AlertType.WARNING);
        confirmationAlert.setTitle("Reject Clearance Request");
        confirmationAlert.setHeaderText("Confirm Rejection");
        confirmationAlert.setContentText(String.format(
            "Student: %s (%s)\n\nIssues detected:\n%s\nAre you sure you want to reject this clearance request?",
            studentName, studentId, 
            rejectionReasons.length() > 0 ? rejectionReasons.toString() : "No specific issues detected"
        ));
        
        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Reject Clearance");
                dialog.setHeaderText("Please specify reason for rejection");
                dialog.setContentText(String.format(
                    "Student: %s (%s)\n\nReason for rejection:",
                    studentName, studentId
                ));
                
                String defaultReason = "";
                if (request.getOverdueCount() > 0) {
                    defaultReason = "Overdue books: " + request.getOverdueCount();
                }
                if (request.getTotalFine() > 0) {
                    if (!defaultReason.isEmpty()) defaultReason += ", ";
                    defaultReason += "Unpaid fines: ₦" + String.format("%.2f", request.getTotalFine());
                }
                if (defaultReason.isEmpty()) {
                    defaultReason = "Library clearance not approved";
                }
                dialog.getEditor().setText(defaultReason);
                
                dialog.showAndWait().ifPresent(reason -> {
                    if (!reason.trim().isEmpty()) {
                        showProcessingAlert("Processing rejection...");
                        updateClearanceStatus(request.getRequestId(), "REJECTED", reason);
                        showInfoAlert("Clearance Rejected", 
                            String.format("Clearance for %s (%s) has been rejected.\nReason: %s", 
                            studentName, studentId, reason));
                    } else {
                        showWarningAlert("No Reason Provided", "Please provide a reason for rejection.");
                    }
                });
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
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                if ("APPROVED".equals(status)) {
                    checkIfAllApproved(requestId);
                }
                
                showToastNotification("Clearance " + status + " successfully!");
                
            } else {
                showErrorAlert("Update Failed", "Failed to update clearance status. Please try again.");
                return;
            }
            
            refreshRequests();
            
            if (currentStudentId > 0 && selectedRequest != null && selectedRequest.getRequestId() == requestId) {
                lblDetailStatus.setText(status);
                updateStatusLabelStyle(lblDetailStatus, status);
                btnApproveFromDetails.setDisable(true);
                btnRejectFromDetails.setDisable(true);
                
                String color = "APPROVED".equals(status) ? "#27ae60" : "#e74c3c";
                lblDetailStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, " + color + ", 10, 0.5, 0, 0);");
                
                Timeline flash = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(lblDetailStatus.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(500), new KeyValue(lblDetailStatus.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.millis(1000), new KeyValue(lblDetailStatus.opacityProperty(), 1.0))
                );
                flash.setCycleCount(3);
                flash.play();
            }
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to update clearance status: " + e.getMessage());
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
        showToastNotification("Filters cleared");
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
            String totalBooksQuery = "SELECT COUNT(*) as count FROM book_borrowings";
            PreparedStatement pstmt1 = connection.prepareStatement(totalBooksQuery);
            ResultSet rs1 = pstmt1.executeQuery();
            if (rs1.next()) {
                lblTotalBooks.setText(String.valueOf(rs1.getInt("count")));
            }
            
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
            showErrorAlert("Database Error", "Failed to load dashboard stats: " + e.getMessage());
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
    
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("⚠ Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("ℹ Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showProcessingAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Processing");
        alert.setHeaderText("⏳ Please wait...");
        alert.setContentText(message);
        alert.getDialogPane().getButtonTypes().clear();
        alert.show();
        
        new Thread(() -> {
            try {
                Thread.sleep(800);
                Platform.runLater(() -> alert.close());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void showToastNotification(String message) {
        if (tableRequests == null || tableRequests.getScene() == null) {
            System.out.println("[Toast]: " + message);
            return;
        }
        
        Label toast = new Label(message);
        toast.getStyleClass().add("toast-notification");
        
        Stage stage = (Stage) tableRequests.getScene().getWindow();
        if (stage == null) {
            return;
        }
        
        Stage toastStage = new Stage();
        toastStage.initOwner(stage);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.initModality(Modality.NONE);
        
        StackPane root = new StackPane(toast);
        root.getStyleClass().add("toast-container");
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/university/clearance/resources/css/dashboard.css").toExternalForm());
        toastStage.setScene(scene);
        
        toastStage.setX(stage.getX() + stage.getWidth() / 2 - 150);
        toastStage.setY(stage.getY() + stage.getHeight() - 100);
        
        toastStage.show();
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    if (toastStage.isShowing()) {
                        toastStage.close();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to logout from the library dashboard?");
        
        ButtonType yesButton = new ButtonType("Yes, Logout", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                try {
                    // Perform logout without showing saving session alert
                    performLogout();
                } catch (Exception e) {
                    showErrorAlert("Logout Error", "Failed to logout: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void performLogout() {
        try {
            // Close database connection
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            Scene currentScene = btnLogout.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/clearance/resources/views/Login.fxml"));
            Parent root = loader.load();
            
            Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
            stage.setScene(newScene);
            
            stage.setTitle("Debre Birhan University - Clearance System Login");
            stage.centerOnScreen();
            
            showToastNotification("Successfully logged out");
            
        } catch (IOException e) {
            showErrorAlert("Error", "Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Model classes (same as before)
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
        
        public String getRequestDateFormatted() {
            return requestDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
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
        
        public String getBorrowDateFormatted() {
            return borrowDate != null ? borrowDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
        
        public String getDueDateFormatted() {
            return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
        
        public String getReturnDateFormatted() {
            return returnDate != null ? returnDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
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
        
        public String getBookTitle() { return bookTitle; }
        public String getAuthor() { return author; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
        public double getFine() { return fine; }
        
        public String getBorrowDateFormatted() {
            return borrowDate != null ? borrowDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
        
        public String getDueDateFormatted() {
            return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
        
        public String getReturnDateFormatted() {
            return returnDate != null ? returnDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        }
    }
}