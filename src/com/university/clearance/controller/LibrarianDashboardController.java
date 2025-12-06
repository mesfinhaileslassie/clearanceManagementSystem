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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class LibrarianDashboardController implements Initializable {

    // ==================== FXML INJECTIONS ====================
    @FXML private Label lblWelcome;
    @FXML private Label lblPendingCount;
    @FXML private Label lblSelectedStudent;
    @FXML private Label lblBookStatus;
    @FXML private VBox vboxBookDetails;

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

    // ==================== DATA ====================
    private User currentUser;
    private final ObservableList<ClearanceRequest> requestData = FXCollections.observableArrayList();
    private final ObservableList<BookBorrowing> bookData = FXCollections.observableArrayList();

    // ==================== INITIALIZATION ====================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
        setupRowSelection();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Welcome, " + user.getFullName());
        loadPendingRequests();
    }

    // ==================== TABLE SETUP ====================
    private void setupTables() {
        // Main clearance requests table
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colRequestDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        colBookStatus.setCellValueFactory(new PropertyValueFactory<>("bookStatus"));

        // Color-code library status
        colBookStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Clear")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item.contains("BLOCKED") || item.contains("OVERDUE")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Action buttons
        colActions.setCellFactory(column -> new TableCell<ClearanceRequest, String>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button("Reject");
            private final HBox box = new HBox(10, btnApprove, btnReject);

            {
                box.setPadding(new Insets(5));
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 4;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 4;");

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
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Book details table
        colBookTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFine.setCellValueFactory(new PropertyValueFactory<>("fine"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Returned" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "Borrowed" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "OVERDUE" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });
    }

    private void setupRowSelection() {
        tableRequests.getSelectionModel().selectedItemProperty().addListener((obs, old, newSelection) -> {
            if (newSelection != null) {
                loadStudentBookDetails(newSelection.getStudentId(), newSelection.getStudentName());
            } else {
                vboxBookDetails.setVisible(false);
                vboxBookDetails.setManaged(false);
            }
        });
    }
    // ==================== LOAD DATA ====================
    @FXML
    private void refreshRequests() {
        loadPendingRequests();
    }

    private void loadPendingRequests() {
        requestData.clear();

        String sql = """
            SELECT 
                cr.id AS request_id,
                u.username AS student_id,
                u.full_name,
                u.department,
                DATE(cr.request_date) AS req_date,
                COALESCE((
                    SELECT GROUP_CONCAT(DISTINCT bb.status ORDER BY FIELD(bb.status, 'OVERDUE','BORROWED','RETURNED') DESC SEPARATOR ', ')
                    FROM book_borrowings bb
                    WHERE bb.student_id = u.id AND bb.status != 'RETURNED'
                ), 'Clear') AS book_status_raw
            FROM clearance_requests cr
            JOIN users u ON cr.student_id = u.id
            LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.officer_role = 'LIBRARIAN'
            WHERE cr.status NOT IN ('FULLY_CLEARED', 'REJECTED')
              AND (ca.status IS NULL OR ca.status = 'PENDING')
            ORDER BY cr.request_date ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String raw = rs.getString("book_status_raw");
                String displayStatus = raw == null || raw.equals("Clear")
                    ? "Clear - Ready for Approval"
                    : raw.contains("OVERDUE")
                        ? "BLOCKED - Overdue Books Detected!"
                        : "Warning - Books Currently Borrowed";

                requestData.add(new ClearanceRequest(
                    rs.getString("student_id"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("req_date"),
                    displayStatus,
                    rs.getInt("request_id")
                ));
            }

            tableRequests.setItems(requestData);
            lblPendingCount.setText("Pending: " + requestData.size());

        } catch (Exception e) {
            showAlert("Database Error", "Failed to load requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== BOOK DETAILS (INLINE) ====================
    private void loadStudentBookDetails(String studentIdStr, String studentName) {
        bookData.clear();
        int studentId = Integer.parseInt(studentIdStr.replaceAll("\\D", ""));

        String sql = """
            SELECT book_title, borrow_date, due_date, status, fine_amount
            FROM book_borrowings 
            WHERE student_id = ?
            ORDER BY borrow_date DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            int overdueCount = 0;
            while (rs.next()) {
                String status = rs.getString("status");
                if ("OVERDUE".equals(status)) overdueCount++;

                bookData.add(new BookBorrowing(
                    rs.getString("book_title"),
                    rs.getDate("borrow_date").toString(),
                    rs.getDate("due_date").toString(),
                    status.equals("RETURNED") ? "Returned" :
                    status.equals("BORROWED") ? "Borrowed" : "OVERDUE",
                    String.format("$%.2f", rs.getDouble("fine_amount"))
                ));
            }

            tableBookDetails.setItems(bookData);
            lblSelectedStudent.setText("Selected Student: " + studentName + " (" + studentIdStr + ")");

            if (bookData.isEmpty()) {
                lblBookStatus.setText("No books borrowed – Student is clear");
                lblBookStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else if (overdueCount > 0) {
                lblBookStatus.setText("OVERDUE BOOKS: " + overdueCount + " → Approval BLOCKED");
                lblBookStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                lblBookStatus.setText("Borrowed: " + bookData.size() + " book(s) → Must return first");
                lblBookStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            }

            vboxBookDetails.setVisible(true);
            vboxBookDetails.setManaged(true);

        } catch (Exception e) {
            showAlert("Error", "Could not load book records");
            e.printStackTrace();
        }
    }

    // ==================== APPROVAL LOGIC (WITH AUTO-REJECT) ====================
    private void approveClearance(ClearanceRequest request) {
        int studentId = Integer.parseInt(request.getStudentId().replaceAll("\\D", ""));

        if (hasUnreturnedBooks(studentId)) {
            TextInputDialog dialog = new TextInputDialog(
                "Student has unreturned or overdue books. Clearance cannot be approved until all books are returned and fines paid."
            );
            dialog.setTitle("Approval Blocked");
            dialog.setHeaderText("Library Policy Violation");
            dialog.setContentText("Enter rejection reason:");
            dialog.getEditor().setEditable(true);

            dialog.showAndWait().ifPresent(remark -> {
                String reason = remark.isBlank() ? "Rejected: Unreturned library books" : remark;
                saveApproval(request.getRequestId(), "REJECTED", reason);
                showAlert("Blocked", "Approval blocked due to outstanding books.");
                loadPendingRequests();
            });
            return;
        }

        // All clear → allow approval allowed
        TextInputDialog dialog = new TextInputDialog("No outstanding books. Student is clear.");
        dialog.setTitle("Approve Clearance");
        dialog.setContentText("Optional remarks:");

        dialog.showAndWait().ifPresent(remark -> {
            String r = remark.isBlank() ? "Approved – No outstanding books" : remark;
            saveApproval(request.getRequestId(), "APPROVED", r);
            showAlert("Approved", request.getStudentName() + " has been cleared by Library");
            loadPendingRequests();
        });
    }

    private void rejectClearance(ClearanceRequest request) {
        TextInputDialog dialog = new TextInputDialog("Manual rejection");
        dialog.setTitle("Reject Clearance");
        dialog.setContentText("Reason for rejection:");

        dialog.showAndWait().ifPresent(remark -> {
            String r = remark.isBlank() ? "Rejected by Librarian" : remark;
            saveApproval(request.getRequestId(), "REJECTED", r);
            showAlert("Rejected", request.getStudentName() + " rejected");
            loadPendingRequests();
        });
    }

    private boolean hasUnreturnedBooks(int studentId) {
        String sql = "SELECT 1 FROM book_borrowings WHERE student_id = ? AND status != 'RETURNED' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeQuery().next();
        } catch (Exception e) {
            return true; // fail-safe
        }
    }

    // ==================== SAVE APPROVAL ====================
    private void saveApproval(int requestId, String status, String remarks) {
        String sql = """
            INSERT INTO clearance_approvals 
                (request_id, officer_role, officer_id, status, remarks, approval_date)
            VALUES (?, 'LIBRARIAN', ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE 
                status = VALUES(status), remarks = VALUES(remarks), approval_date = NOW()
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, requestId);
            ps.setInt(2, currentUser.getId());
            ps.setString(3, status);
            ps.setString(4, remarks);
            ps.executeUpdate();

            updateOverallRequestStatus(conn, requestId);

        } catch (Exception e) {
            showAlert("Error", "Failed to save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateOverallRequestStatus(Connection conn, int requestId) throws SQLException {
        String sql = """
            SELECT 
                SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) rejected,
                SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) approved,
                COUNT(*) total
            FROM clearance_approvals 
            WHERE request_id = ? 
              AND officer_role IN ('LIBRARIAN','CAFETERIA','DORMITORY','REGISTRAR','DEPARTMENT_HEAD')
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int rejected = rs.getInt("rejected");
                int approved = rs.getInt("approved");
                int total = rs.getInt("total");

                String newStatus = rejected > 0 ? "REJECTED" :
                                  (approved == 5 && total == 5) ? "FULLY_CLEARED" : "IN_PROGRESS";

                try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE clearance_requests SET status = ? WHERE id = ?")) {
                    upd.setString(1, newStatus);
                    upd.setInt(2, requestId);
                    upd.executeUpdate();
                }
            }
        }
    }

    // ==================== UTILS ====================
    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ==================== INNER CLASSES ====================
    public static class ClearanceRequest {
        private final String studentId, studentName, department, requestDate, bookStatus;
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
        private final String title, borrowDate, dueDate, status, fine;

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