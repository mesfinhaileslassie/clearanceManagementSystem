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
    @FXML private TabPane mainTabPane;  // Add this missing reference
    
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

    
    @FXML private Label lblPendingCountCard; 
    
    private User currentUser;
    private ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<BookBorrowing> bookData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupBookTableColumns();
        
        // Add listener to handle row selection
        tableRequests.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadStudentBookDetails(newSelection.getStudentId());
                }
            });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (lblWelcome != null) {
            lblWelcome.setText("Welcome, " + user.getFullName() + " - Library Clearance");
        }
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
                
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 80;");
                
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
                    // Always show buttons for pending requests
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
            // First, ensure required tables exist
            createRequiredTables(conn);
            
            String sql = """
                SELECT 
                    cr.id as request_id,
                    u.username as student_id,
                    u.full_name as student_name,
                    u.department,
                    DATE_FORMAT(cr.request_date, '%Y-%m-%d %H:%i') as request_date
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id 
                    AND ca.officer_role = 'LIBRARIAN'
                WHERE cr.status IN ('PENDING', 'IN_PROGRESS')
                AND (ca.status IS NULL OR ca.status = 'PENDING')
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
                    rs.getString("request_date"),
                    bookStatus,
                    rs.getInt("request_id")
                );
                
                requestData.add(request);
                pendingCount++;
            }
            
            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending Clearances: " + pendingCount);
            
            // Also update the dashboard card
            if (lblPendingCountCard != null) {
                lblPendingCountCard.setText(String.valueOf(pendingCount));
            }
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load clearance requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void createRequiredTables(Connection conn) throws SQLException {
        // Create book_borrowings table if it doesn't exist
        String createBookTable = """
            CREATE TABLE IF NOT EXISTS book_borrowings (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT NOT NULL,
                book_title VARCHAR(200) NOT NULL,
                borrow_date DATE NOT NULL,
                due_date DATE NOT NULL,
                return_date DATE,
                status VARCHAR(20) DEFAULT 'BORROWED',
                fine_amount DECIMAL(10,2) DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createBookTable);
        }
    }

    private String checkStudentBookStatus(Connection conn, String studentId) throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_books,
                SUM(CASE WHEN bb.status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_count,
                SUM(CASE WHEN bb.status = 'BORROWED' AND bb.due_date < CURDATE() THEN 1 ELSE 0 END) as newly_overdue,
                SUM(COALESCE(bb.fine_amount, 0)) as total_fine
            FROM book_borrowings bb
            JOIN users u ON bb.student_id = u.id
            WHERE u.username = ? AND bb.status IN ('BORROWED', 'OVERDUE')
            """;
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            int totalBooks = rs.getInt("total_books");
            int overdueCount = rs.getInt("overdue_count");
            double totalFine = rs.getDouble("total_fine");
            
            // Update status for newly overdue books
            int newlyOverdue = rs.getInt("newly_overdue");
            if (newlyOverdue > 0) {
                updateToOverdue(conn, studentId);
                overdueCount += newlyOverdue;
            }
            
            if (overdueCount > 0) {
                return "❌ " + overdueCount + " overdue book(s) - Fine: $" + totalFine;
            } else if (totalBooks > 0) {
                return "⚠️ " + totalBooks + " borrowed book(s)";
            } else {
                return "✅ No books borrowed";
            }
        }
        
        return "✅ No books borrowed";
    }

    private void updateToOverdue(Connection conn, String studentId) throws SQLException {
        String updateSql = """
            UPDATE book_borrowings bb
            JOIN users u ON bb.student_id = u.id
            SET bb.status = 'OVERDUE',
                bb.fine_amount = DATEDIFF(CURDATE(), bb.due_date) * 0.50
            WHERE u.username = ? 
                AND bb.status = 'BORROWED' 
                AND bb.due_date < CURDATE()
            """;
        
        PreparedStatement ps = conn.prepareStatement(updateSql);
        ps.setString(1, studentId);
        ps.executeUpdate();
    }

    private void loadStudentBookDetails(String username) {
        System.out.println("\n=== DEBUG: Loading book details for student: " + username);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Database connected successfully");
            
            // First, check what columns exist in book_borrowings table
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "book_borrowings", "%");
            
            boolean hasBookTitle = false;
            boolean hasBookId = false;
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                System.out.println("Found column: " + columnName);
                if ("book_title".equalsIgnoreCase(columnName)) {
                    hasBookTitle = true;
                }
                if ("book_id".equalsIgnoreCase(columnName)) {
                    hasBookId = true;
                }
            }
            columns.close();
            
            // Build query based on available columns
            String sql;
            if (hasBookTitle) {
                sql = """
                    SELECT
                        bb.book_title as title,
                        DATE_FORMAT(bb.borrow_date, '%Y-%m-%d') as borrow_date,
                        DATE_FORMAT(bb.due_date, '%Y-%m-%d') as due_date,
                        bb.status,
                        CASE 
                            WHEN bb.fine_amount IS NULL OR bb.fine_amount = 0 THEN 'No fine'
                            ELSE CONCAT('$', FORMAT(bb.fine_amount, 2))
                        END as fine
                    FROM book_borrowings bb
                    JOIN users u ON bb.student_id = u.id
                    WHERE u.username = ?
                    ORDER BY
                        CASE bb.status
                            WHEN 'OVERDUE' THEN 1
                            WHEN 'BORROWED' THEN 2
                            ELSE 3
                        END,
                        bb.due_date ASC
                    """;
            } else if (hasBookId) {
                sql = """
                    SELECT
                        COALESCE(b.title, CONCAT('Book #', bb.book_id)) as title,
                        DATE_FORMAT(bb.borrow_date, '%Y-%m-%d') as borrow_date,
                        DATE_FORMAT(bb.due_date, '%Y-%m-%d') as due_date,
                        bb.status,
                        CASE 
                            WHEN bb.fine_amount IS NULL OR bb.fine_amount = 0 THEN 'No fine'
                            ELSE CONCAT('$', FORMAT(bb.fine_amount, 2))
                        END as fine
                    FROM book_borrowings bb
                    LEFT JOIN books b ON bb.book_id = b.id
                    JOIN users u ON bb.student_id = u.id
                    WHERE u.username = ?
                    ORDER BY
                        CASE bb.status
                            WHEN 'OVERDUE' THEN 1
                            WHEN 'BORROWED' THEN 2
                            ELSE 3
                        END,
                        bb.due_date ASC
                    """;
            } else {
                System.out.println("❌ No book_title or book_id column found in book_borrowings table");
                lblBookStatus.setText("Database structure error: No book information columns found");
                return;
            }
            
            System.out.println("DEBUG: Using SQL: " + sql);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                
                bookData.clear(); // Clear previous data
                int recordCount = 0;
                
                while (rs.next()) {
                    recordCount++;
                    BookBorrowing book = new BookBorrowing(
                        rs.getString("title"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date"),
                        rs.getString("status"),
                        rs.getString("fine")
                    );
                    bookData.add(book);
                    
                    System.out.println("Record " + recordCount + ":");
                    System.out.println("  Title: " + rs.getString("title"));
                    System.out.println("  Borrow Date: " + rs.getString("borrow_date"));
                    System.out.println("  Due Date: " + rs.getString("due_date"));
                    System.out.println("  Status: " + rs.getString("status"));
                    System.out.println("  Fine: " + rs.getString("fine"));
                }
                
                System.out.println("DEBUG: Loaded " + recordCount + " book records for student " + username);
                
                // Update UI
                tableBookDetails.setItems(bookData);
                
                if (recordCount == 0) {
                    lblBookStatus.setText("No book records found for student: " + username);
                } else {
                    lblBookStatus.setText("Found " + recordCount + " book records for " + username);
                    
                    // Auto-switch to Book Details tab
                    for (Tab tab : mainTabPane.getTabs()) {
                        if ("Book Details".equals(tab.getText())) {
                            mainTabPane.getSelectionModel().select(tab);
                            break;
                        }
                    }
                }
                
            } catch (SQLException e) {
                System.out.println("SQL ERROR in loadStudentBookDetails:");
                System.out.println("Message: " + e.getMessage());
                e.printStackTrace();
                lblBookStatus.setText("Error loading book details: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
            lblBookStatus.setText("Database error: " + e.getMessage());
        }
    }

    private void approveClearance(ClearanceRequest request) {
        if (request.getBookStatus().contains("❌") || request.getBookStatus().contains("⚠️")) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Book Clearance Issue");
            warning.setHeaderText("Student Has Book Issues");
            warning.setContentText("This student has outstanding book issues:\n\n" + 
                                 request.getBookStatus() + 
                                 "\n\nAre you sure you want to approve anyway?");
            
            warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = warning.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.YES) {
                return;
            }
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Clearance");
        confirmation.setHeaderText("Approve Library Clearance");
        confirmation.setContentText("Approve clearance for: " + request.getStudentName() + 
                                  "\nStudent ID: " + request.getStudentId() +
                                  "\n\nBook Status: " + request.getBookStatus());
        
        Optional<ButtonType> response = confirmation.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            updateClearanceStatus(request.getRequestId(), "APPROVED", 
                                "Library clearance approved. Book status: " + request.getBookStatus());
            loadPendingRequests();
            showAlert("Approved", "Clearance approved for " + request.getStudentName());
        }
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
            loadPendingRequests();
            showAlert("Rejected", "Clearance rejected for " + request.getStudentName());
        }
    }

    private void updateClearanceStatus(int requestId, String status, String remarks) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if approval record exists
            String checkSql = "SELECT COUNT(*) FROM clearance_approvals WHERE request_id = ? AND officer_role = 'LIBRARIAN'";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, requestId);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            
            if (rs.getInt(1) > 0) {
                // Update existing record
                String updateSql = """
                    UPDATE clearance_approvals 
                    SET status = ?, remarks = ?, officer_id = ?, approval_date = NOW()
                    WHERE request_id = ? AND officer_role = 'LIBRARIAN'
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
                    VALUES (?, 'LIBRARIAN', ?, ?, ?, NOW())
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
            
            // Refresh the table
            loadPendingRequests();
            
            showAlert("Success", "Clearance request has been " + status.toLowerCase() + " successfully!");
            
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
    	        AND officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')  -- ADDED
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