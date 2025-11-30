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

public class LibrarianDashboardController implements Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblBookStatus;
    
    @FXML private TableView<ClearanceRequest> tableRequests;
    @FXML private TableColumn<ClearanceRequest, String> colStudentId;
    @FXML private TableColumn<ClearanceRequest, String> colStudentName;
    @FXML private TableColumn<ClearanceRequest, String> colDepartment;
    @FXML private TableColumn<ClearanceRequest, String> colRequestDate;
    @FXML private TableColumn<ClearanceRequest, String> colBookStatus;
    @FXML private TableColumn<ClearanceRequest, String> colActions;

    @FXML private TableView<BookBorrowing> tableBookDetails;
    @FXML private TableColumn<BookBorrowing, String> colBookTitle;
    @FXML private TableColumn<BookBorrowing, String> colBorrowDate;
    @FXML private TableColumn<BookBorrowing, String> colDueDate;
    @FXML private TableColumn<BookBorrowing, String> colStatus;
    @FXML private TableColumn<BookBorrowing, String> colFine;

    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<BookBorrowing> bookData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupBookTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName() + " - Library Clearance");
        loadPendingRequests();
    }

    private void setupTableColumns() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colBookStatus.setCellValueFactory(new PropertyValueFactory<>("bookStatus"));
        
        // Actions column with Approve/Reject buttons
        colActions.setCellFactory(param -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("✅ Approve");
            private final Button btnReject = new Button("❌ Reject");
            private final HBox buttons = new HBox(5, btnApprove, btnReject);

            {
                buttons.setPadding(new Insets(5));
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                
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
                    if ("Pending".equals(request.getBookStatus()) || request.getBookStatus().contains("Pending")) {
                        setGraphic(buttons);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupBookTableColumns() {
        colBookTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFine.setCellValueFactory(new PropertyValueFactory<>("fine"));
        
        // Color code status
        colStatus.setCellFactory(column -> new TableCell<BookBorrowing, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "BORROWED":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "OVERDUE":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "RETURNED":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }

    @FXML
    private void refreshRequests() {
        loadPendingRequests();
        showAlert("Refreshed", "Library clearance requests refreshed successfully!");
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
                WHERE ca.officer_role = 'LIBRARIAN' 
                AND (ca.status IS NULL OR ca.status = 'PENDING')  -- Only show pending/null status
                AND cr.status IN ('PENDING', 'IN_PROGRESS')       -- Only show active requests
                ORDER BY cr.request_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

        int pendingCount = 0;
        
        while (rs.next()) {
            String studentId = rs.getString("student_id");
            String bookStatus = checkStudentBookStatus(conn, studentId);
            
            ClearanceRequest request = new ClearanceRequest(
                rs.getString("student_id"),
                rs.getString("student_name"),
                rs.getString("department"),
                rs.getTimestamp("request_date").toString(),
                bookStatus,
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

    // ADD THIS MISSING METHOD
    private String checkStudentBookStatus(Connection conn, String studentId) throws SQLException {
        // First, check if book_borrowings table exists, if not create sample data
        createSampleBookDataIfNeeded(conn, studentId);
        
        // Check if student has any borrowed books
        String borrowSql = """
            SELECT 
                COUNT(*) as borrowed_count,
                SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_count,
                SUM(COALESCE(fine_amount, 0)) as total_fine
            FROM book_borrowings bb
            JOIN users u ON bb.student_id = u.id
            WHERE u.username = ? AND bb.status IN ('BORROWED', 'OVERDUE')
            """;
            
        PreparedStatement ps = conn.prepareStatement(borrowSql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int borrowedCount = rs.getInt("borrowed_count");
            int overdueCount = rs.getInt("overdue_count");
            double totalFine = rs.getDouble("total_fine");
            
            if (overdueCount > 0) {
                return "❌ Has " + overdueCount + " overdue book(s) - Fine: $" + totalFine;
            } else if (borrowedCount > 0) {
                return "⚠️ Has " + borrowedCount + " borrowed book(s)";
            } else {
                return "✅ No books borrowed";
            }
        }
        
        return "✅ No books borrowed";
    }

    // ADD THIS METHOD TO CREATE SAMPLE BOOK DATA
    private void createSampleBookDataIfNeeded(Connection conn, String studentId) throws SQLException {
        // Check if book_borrowings table exists
        try {
            String checkTableSql = "SELECT 1 FROM book_borrowings LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            checkStmt.executeQuery();
        } catch (SQLException e) {
            // Table doesn't exist, create it
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS book_borrowings (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT,
                    book_id INT,
                    book_title VARCHAR(100),
                    borrow_date DATE,
                    due_date DATE,
                    return_date DATE,
                    status VARCHAR(20),
                    fine_amount DECIMAL(10,2),
                    FOREIGN KEY (student_id) REFERENCES users(id)
                )
                """;
            PreparedStatement createStmt = conn.prepareStatement(createTableSql);
            createStmt.executeUpdate();
        }
        
        // Check if student has book records, if not insert sample data
        String checkRecordsSql = """
            SELECT COUNT(*) FROM book_borrowings bb 
            JOIN users u ON bb.student_id = u.id 
            WHERE u.username = ?
            """;
        PreparedStatement checkRecordsStmt = conn.prepareStatement(checkRecordsSql);
        checkRecordsStmt.setString(1, studentId);
        ResultSet rs = checkRecordsStmt.executeQuery();
        rs.next();
        
        if (rs.getInt(1) == 0) {
            // Insert sample book records (50% chance of having borrowed books)
            if (Math.random() > 0.5) {
                String insertSql = """
                    INSERT INTO book_borrowings (student_id, book_title, borrow_date, due_date, status, fine_amount)
                    SELECT id, ?, CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 1 DAY, 'OVERDUE', 5.00 
                    FROM users WHERE username = ?
                    """;
                
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, "Advanced Java Programming");
                insertStmt.setString(2, studentId);
                insertStmt.executeUpdate();
            }
        }
    }

    private void loadStudentBookDetails(String studentId) {
        bookData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    bb.book_title as title,
                    bb.borrow_date,
                    bb.due_date,
                    bb.status,
                    bb.fine_amount
                FROM book_borrowings bb
                JOIN users u ON bb.student_id = u.id
                WHERE u.username = ? AND bb.status IN ('BORROWED', 'OVERDUE')
                ORDER BY bb.due_date ASC
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                BookBorrowing book = new BookBorrowing(
                    rs.getString("title"),
                    rs.getDate("borrow_date").toString(),
                    rs.getDate("due_date").toString(),
                    rs.getString("status"),
                    "$" + rs.getDouble("fine_amount")
                );
                bookData.add(book);
            }
            
            tableBookDetails.setItems(bookData);
            
            // Update book status label
            if (bookData.isEmpty()) {
                lblBookStatus.setText("📚 Student has no borrowed books");
                lblBookStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                lblBookStatus.setText("📚 Student has " + bookData.size() + " borrowed book(s)");
                lblBookStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void approveClearance(ClearanceRequest request) {
        // Check if student has any book issues
        if (request.getBookStatus().contains("❌") || request.getBookStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Book Clearance Issue");
            warning.setHeaderText("Student Has Book Issues");
            warning.setContentText("This student has outstanding book issues:\n\n" + 
                                 request.getBookStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.YES) {
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Clearance");
        confirmation.setHeaderText("Approve Library Clearance");
        confirmation.setContentText("Approve clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId());
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                    "Library clearance approved. All books cleared.");
                loadPendingRequests(); // Refresh table to remove approved request
                showAlert("Approved", "Clearance approved for " + request.getStudentName());
            }
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Clearance");
        dialog.setHeaderText("Reject Library Clearance");
        dialog.setContentText("Enter rejection reason for " + request.getStudentName() + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            updateClearanceStatus(request.getRequestId(), "REJECTED", 
                                "Library clearance rejected: " + result.get().trim());
            loadPendingRequests(); // Refresh table to remove rejected request
            showAlert("Rejected", "Clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE clearance_approvals 
                SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                WHERE request_id = ? AND officer_role = 'LIBRARIAN'
                """;
                
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, remarks);
            ps.setInt(3, currentUser.getId());
            ps.setInt(4, requestId);
            
            ps.executeUpdate();
            
            // Update overall request status if needed
            updateOverallRequestStatus(conn, requestId);
            
        } catch (Exception e) {
            showAlert("Error", "Failed to update clearance status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateOverallRequestStatus(Connection conn, int requestId) throws SQLException {
        // Check if all approvals are done
        String checkSql = """
            SELECT 
                SUM(CASE WHEN status = 'PENDING' OR status IS NULL THEN 1 ELSE 0 END) as pending_count,
                SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count
            FROM clearance_approvals 
            WHERE request_id = ?
            """;
        
        PreparedStatement checkPs = conn.prepareStatement(checkSql);
        checkPs.setInt(1, requestId);
        ResultSet rs = checkPs.executeQuery();
        
        if (rs.next()) {
            int pendingCount = rs.getInt("pending_count");
            int rejectedCount = rs.getInt("rejected_count");
            
            if (rejectedCount > 0) {
                // Any rejection fails the request
                String updateSql = "UPDATE clearance_requests SET status = 'REJECTED' WHERE id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setInt(1, requestId);
                updatePs.executeUpdate();
            } else if (pendingCount == 0) {
                // All departments have responded (no pending) and no rejections
                String updateSql = "UPDATE clearance_requests SET status = 'FULLY_CLEARED', completion_date = CURDATE() WHERE id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setInt(1, requestId);
                updatePs.executeUpdate();
            }
            // Otherwise, status remains IN_PROGRESS
        }
    }

    @FXML
    private void viewBookDetails() {
        ClearanceRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a student first to view book details.");
            return;
        }
        
        loadStudentBookDetails(selected.getStudentId());
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
        private final String requestDate;
        private final String bookStatus;
        private final int requestId;

        public ClearanceRequest(String studentId, String studentName, String department, 
                               String requestDate, String bookStatus, int requestId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.requestDate = requestDate;
            this.bookStatus = bookStatus;
            this.requestId = requestId;
        }

        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public String getRequestDate() { return requestDate; }
        public String getBookStatus() { return bookStatus; }
        public int getRequestId() { return requestId; }
    }

    public static class BookBorrowing {
        private final String title;
        private final String borrowDate;
        private final String dueDate;
        private final String status;
        private final String fine;

        public BookBorrowing(String title, String borrowDate, String dueDate, String status, String fine) {
            this.title = title;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.status = status;
            this.fine = fine;
        }

        public String getTitle() { return title; }
        public String getBorrowDate() { return borrowDate; }
        public String getDueDate() { return dueDate; }
        public String getStatus() { return status; }
        public String getFine() { return fine; }
    }
}