package com.university.clearance.controller;

import com.university.clearance.DatabaseConnection;
import com.university.clearance.model.User;
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
    private User currentUser; // CHANGED: Use User object
    private ClearanceRequest selectedRequest;
    private int currentStudentId;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Get database connection
            connection = DatabaseConnection.getConnection();
            
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
            
            // Set up search filtering
            setupSearchFilter();
            
            // Add tooltips
            Platform.runLater(() -> setupTooltips());
            
            // Show welcome message after user is set
            Platform.runLater(() -> {
                if (currentUser != null && lblWelcome != null) {
                    lblWelcome.setText("Welcome, " + currentUser.getFullName());
                }
            });
            
        } catch (Exception e) {
            showErrorAlert("Initialization Error", "Cannot initialize dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ADD THIS METHOD: Set current user
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
        }
        
        // Load data after user is set
        Platform.runLater(() -> {
            refreshAllData();
            showToastNotification("Dashboard loaded successfully");
        });
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
    
    private void setupKeyboardShortcuts() {
        if (tableRequests != null && tableRequests.getScene() != null) {
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
                                btnApproveFromDetails != null && btnApproveFromDetails.isVisible() && !btnApproveFromDetails.isDisabled()) {
                                approveFromDetails();
                                event.consume();
                            }
                            break;
                    }
                }
            });
        }
    }
    
    private void setupTooltips() {
        if (btnApproveFromDetails != null) {
            btnApproveFromDetails.setTooltip(new Tooltip("Approve this student's library clearance (Ctrl+A)"));
        }
        if (btnRejectFromDetails != null) {
            btnRejectFromDetails.setTooltip(new Tooltip("Reject this student's library clearance"));
        }
        if (txtSearchStudent != null) {
            txtSearchStudent.setTooltip(new Tooltip("Search by student ID, name, or book title"));
        }
        if (cmbStatusFilter != null) {
            cmbStatusFilter.setTooltip(new Tooltip("Filter borrowing records by status"));
        }
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
                    private final Button btnDetails = new Button("Details");
                    private final Button btnApprove = new Button("Approve");
                    private final Button btnReject = new Button(" Reject");
                    private final HBox buttonBox = new HBox(5, btnDetails, btnApprove, btnReject);
                    
                    {
                        btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        
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
                    setText(String.format("ETB%.2f", fine));
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
                    setText(String.format("ETB%.2f", fine));
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
    
    
    private void loadBorrowingHistory() {
        borrowingHistory.clear();
        try {
            // Query borrowed_books with proper joins
            String query = """
                SELECT 
                    bb.id,
                    u.username as student_id,
                    u.full_name,
                    u.department,
                    b.title as book_title,
                    DATE(bb.borrow_date) as borrow_date,
                    DATE(bb.due_date) as due_date,
                    DATE(bb.return_date) as return_date,
                    bb.status,
                    bb.fine
                FROM borrowed_books bb
                JOIN users u ON bb.student_id = u.id
                JOIN book_copies bc ON bb.book_copy_id = bc.id
                JOIN books b ON bc.book_id = b.id
                ORDER BY bb.borrow_date DESC
                LIMIT 50
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
                    rs.getDouble("fine")
                );
                
                borrowingHistory.add(record);
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading borrowing history: " + e.getMessage());
            e.printStackTrace();
        }
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
                    ca.status as approval_status
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.officer_role = 'LIBRARIAN'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                  AND (ca.status IS NULL OR ca.status = 'PENDING')
                ORDER BY cr.request_date DESC
                """;
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            int clearCount = 0, warningCount = 0, issueCount = 0;
            
            while (rs.next()) {
                String studentId = rs.getString("username");
                
                // Get book borrowing info for this student
                int[] bookStats = getBookStatsForStudent(rs.getInt("student_id"));
                int totalBorrowed = bookStats[0];
                int overdueCount = bookStats[1];
                double totalFine = bookStats[2];
                
                String bookStatus = determineBookStatus(totalBorrowed, overdueCount, totalFine);
                
                ClearanceRequest request = new ClearanceRequest(
                    rs.getInt("request_id"),
                    studentId,
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getDate("request_date") != null ? rs.getDate("request_date").toLocalDate() : LocalDate.now(),
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
            
            if (lblTotalRequests != null) lblTotalRequests.setText(String.valueOf(clearanceRequests.size()));
            if (lblClearCount != null) lblClearCount.setText("Clear: " + clearCount);
            if (lblWarningCount != null) lblWarningCount.setText("Warning: " + warningCount);
            if (lblIssueCount != null) lblIssueCount.setText("Issue: " + issueCount);
            if (lblPendingCount != null) lblPendingCount.setText("Pending: " + clearanceRequests.size());
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper method to get book stats for student
    private int[] getBookStatsForStudent(int studentId) throws SQLException {
        int totalBorrowed = 0;
        int overdueCount = 0;
        double totalFine = 0;
        
        // Check if borrowed_books table exists (this is the correct table based on your BorrowDAO)
        try {
            String checkTableQuery = "SHOW TABLES LIKE 'borrowed_books'";
            PreparedStatement checkStmt = connection.prepareStatement(checkTableQuery);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                // Query the borrowed_books table
                String query = """
                    SELECT 
                        COUNT(*) as total_borrowed,
                        SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_count,
                        COALESCE(SUM(fine), 0) as total_fine
                    FROM borrowed_books 
                    WHERE student_id = ?
                    """;
                
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setInt(1, studentId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    totalBorrowed = rs.getInt("total_borrowed");
                    overdueCount = rs.getInt("overdue_count");
                    totalFine = rs.getDouble("total_fine");
                }
            } else {
                System.out.println("Note: borrowed_books table not found");
            }
        } catch (Exception e) {
            System.out.println("Error checking borrowed_books table: " + e.getMessage());
        }
        
        return new int[] {totalBorrowed, overdueCount, (int) totalFine};
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
    
   
    
    private void loadStudentBookDetails(int studentId) {
        studentBookDetails.clear();
        currentStudentId = studentId;
        
        try {
            String query = """
                SELECT 
                    b.title as book_title,
                    b.author,
                    DATE(bb.borrow_date) as borrow_date,
                    DATE(bb.due_date) as due_date,
                    DATE(bb.return_date) as return_date,
                    bb.status,
                    bb.fine
                FROM borrowed_books bb
                JOIN book_copies bc ON bb.book_copy_id = bc.id
                JOIN books b ON bc.book_id = b.id
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
                    rs.getDouble("fine")
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
            
            if (lblDetailTotalBorrowed != null) lblDetailTotalBorrowed.setText(String.valueOf(totalBorrowed));
            if (lblDetailCurrentBorrowed != null) lblDetailCurrentBorrowed.setText(String.valueOf(currentBorrowed));
            if (lblDetailOverdue != null) lblDetailOverdue.setText(String.valueOf(overdue));
            if (lblDetailTotalFines != null) lblDetailTotalFines.setText(String.format("ETB%.2f", totalFines));
            
        } catch (SQLException e) {
            System.out.println("Error loading student book details: " + e.getMessage());
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
                if (lblDetailStudentId != null) lblDetailStudentId.setText(rs.getString("username"));
                if (lblDetailStudentName != null) lblDetailStudentName.setText(rs.getString("full_name"));
                if (lblDetailDepartment != null) lblDetailDepartment.setText(rs.getString("department"));
                if (lblDetailEmail != null) lblDetailEmail.setText(rs.getString("email"));
                if (lblDetailPhone != null) lblDetailPhone.setText(rs.getString("phone"));
                
                String status = rs.getString("clearance_status");
                if (lblDetailStatus != null) {
                    lblDetailStatus.setText(status != null ? status : "Not Applied");
                    updateStatusLabelStyle(lblDetailStatus, status);
                }
            }
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load student information: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateStatusLabelStyle(Label label, String status) {
        if (status == null || label == null) return;
        
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
        
        if (btnApproveFromDetails != null && btnRejectFromDetails != null) {
            if ("PENDING".equals(selectedRequest.getApprovalStatus()) || 
                "IN_PROGRESS".equals(selectedRequest.getApprovalStatus())) {
                btnApproveFromDetails.setDisable(false);
                btnRejectFromDetails.setDisable(false);
            } else {
                btnApproveFromDetails.setDisable(true);
                btnRejectFromDetails.setDisable(true);
            }
        }
        
        if (mainTabPane != null && !mainTabPane.getTabs().contains(tabStudentDetails)) {
            mainTabPane.getTabs().add(tabStudentDetails);
        }
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(tabStudentDetails);
        }
        
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
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0);
        }
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
                    "Student: %s (%s)\n\nIssues Found:\n• Overdue books: %d\n• Total fines: ETB%.2f\n\nThis student has overdue books or unpaid fines. Are you sure you want to approve?",
                    studentName, studentId, request.getOverdueCount(), request.getTotalFine()
                ));
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
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
                        updateClearanceStatus(request.getRequestId(), "APPROVED", 
                            "Library clearance approved - borrowed books present");
                        showSuccessAlert("Clearance Approved", 
                            String.format("Clearance for %s (%s) has been approved.", studentName, studentId));
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Approval");
                alert.setHeaderText("Student is clear");
                alert.setContentText(String.format(
                    "Student: %s (%s)\n\nStatus: No borrowed books, overdue items, or fines.\n\nApprove library clearance?",
                    studentName, studentId
                ));
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
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
            rejectionReasons.append("• Unpaid fines: ETB").append(String.format("%.2f", request.getTotalFine())).append("\n");
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
                    defaultReason += "Unpaid fines: ETB" + String.format("%.2f", request.getTotalFine());
                }
                if (defaultReason.isEmpty()) {
                    defaultReason = "Library clearance not approved";
                }
                dialog.getEditor().setText(defaultReason);
                
                dialog.showAndWait().ifPresent(reason -> {
                    if (!reason.trim().isEmpty()) {
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
            if (currentUser == null) {
                showErrorAlert("Error", "No librarian logged in!");
                return;
            }
            
            // First, check if approval record exists
            String checkQuery = "SELECT id FROM clearance_approvals WHERE request_id = ? AND officer_role = 'LIBRARIAN'";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setInt(1, requestId);
            ResultSet rs = checkStmt.executeQuery();
            
            int rowsAffected = 0;
            
            if (rs.next()) {
                // Update existing record
                String updateQuery = """
                    UPDATE clearance_approvals 
                    SET status = ?, 
                        officer_id = ?,
                        remarks = ?, 
                        approval_date = NOW() 
                    WHERE request_id = ? 
                      AND officer_role = 'LIBRARIAN'
                    """;
                
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setString(1, status);
                updateStmt.setInt(2, currentUser.getId());
                updateStmt.setString(3, remarks);
                updateStmt.setInt(4, requestId);
                rowsAffected = updateStmt.executeUpdate();
            } else {
                // Insert new record
                String insertQuery = """
                    INSERT INTO clearance_approvals 
                    (request_id, officer_role, officer_id, status, remarks, approval_date)
                    VALUES (?, 'LIBRARIAN', ?, ?, ?, NOW())
                    """;
                
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
                insertStmt.setInt(1, requestId);
                insertStmt.setInt(2, currentUser.getId());
                insertStmt.setString(3, status);
                insertStmt.setString(4, remarks);
                rowsAffected = insertStmt.executeUpdate();
            }
            
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
                if (lblDetailStatus != null) {
                    lblDetailStatus.setText(status);
                    updateStatusLabelStyle(lblDetailStatus, status);
                }
                if (btnApproveFromDetails != null) btnApproveFromDetails.setDisable(true);
                if (btnRejectFromDetails != null) btnRejectFromDetails.setDisable(true);
                
                if (lblDetailStatus != null) {
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
        if (filteredBorrowingHistory == null) return;
        
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
        if (filteredBorrowingHistory != null) {
            filteredBorrowingHistory.setPredicate(null);
        }
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
            String totalBooksQuery = """
                SELECT COUNT(*) as count 
                FROM borrowed_books bb
                WHERE bb.return_date IS NULL
                """;
            PreparedStatement pstmt1 = connection.prepareStatement(totalBooksQuery);
            ResultSet rs1 = pstmt1.executeQuery();
            if (rs1.next() && lblTotalBooks != null) {
                lblTotalBooks.setText(String.valueOf(rs1.getInt("count")));
            }
            
            String activeBorrowersQuery = """
                SELECT COUNT(DISTINCT student_id) as count 
                FROM borrowed_books 
                WHERE status IN ('BORROWED', 'OVERDUE') AND return_date IS NULL
                """;
            PreparedStatement pstmt2 = connection.prepareStatement(activeBorrowersQuery);
            ResultSet rs2 = pstmt2.executeQuery();
            if (rs2.next() && lblActiveBorrowers != null) {
                lblActiveBorrowers.setText(String.valueOf(rs2.getInt("count")));
            }
            
            String overdueQuery = """
                SELECT COUNT(*) as count 
                FROM borrowed_books 
                WHERE status = 'OVERDUE' AND return_date IS NULL
                """;
            PreparedStatement pstmt3 = connection.prepareStatement(overdueQuery);
            ResultSet rs3 = pstmt3.executeQuery();
            if (rs3.next() && lblOverdueCount != null) {
                lblOverdueCount.setText(String.valueOf(rs3.getInt("count")));
            }
            
        } catch (SQLException e) {
            System.out.println("Error updating dashboard stats: " + e.getMessage());
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
        
        if (lblTotalBorrowed != null) lblTotalBorrowed.setText(String.valueOf(totalBorrowed));
        if (lblCurrentlyBorrowed != null) lblCurrentlyBorrowed.setText(String.valueOf(currentlyBorrowed));
        if (lblTotalFines != null) lblTotalFines.setText(String.format("ETB%.2f", totalFines));
    }
    
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success");
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
        alert.setHeaderText("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showToastNotification(String message) {
        // Simple console notification for now
        System.out.println("[Toast]: " + message);
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