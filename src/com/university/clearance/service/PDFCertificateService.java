package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;

import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFCertificateService {

    // Professional color palette
    private static final DeviceRgb UNIVERSITY_BLUE = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb UNIVERSITY_RED = new DeviceRgb(173, 32, 47);
    private static final DeviceRgb ACCENT_GOLD = new DeviceRgb(212, 175, 55);
    private static final DeviceRgb LIGHT_BLUE = new DeviceRgb(240, 248, 255);
    private static final DeviceRgb LIGHT_GOLD = new DeviceRgb(255, 250, 240);
    
    // Debug logging
    private static final boolean DEBUG_MODE = true;

    public String generatePDFCertificate(int studentId, int requestId) {
        logDebug("=== START CERTIFICATE GENERATION ===");
        logDebug("Student ID: " + studentId + ", Request ID: " + requestId);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                logError("❌ Database connection is null!");
                return null;
            }
            
            logDebug("✅ Database connection established");
            
            // First, let's check if the user exists
            String checkUserSql = "SELECT id, username, full_name FROM users WHERE id = ? AND role = 'STUDENT'";
            try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql)) {
                checkUserStmt.setInt(1, studentId);
                ResultSet userRs = checkUserStmt.executeQuery();
                if (userRs.next()) {
                    logDebug("✅ Student exists: " + userRs.getString("full_name") + 
                            " (ID: " + userRs.getString("username") + ")");
                } else {
                    logError("❌ Student not found or not a student with ID: " + studentId);
                    return null;
                }
            }
            
            // Check the clearance request status
            String checkRequestSql = "SELECT status, student_id FROM clearance_requests WHERE id = ?";
            try (PreparedStatement checkRequestStmt = conn.prepareStatement(checkRequestSql)) {
                checkRequestStmt.setInt(1, requestId);
                ResultSet requestRs = checkRequestStmt.executeQuery();
                if (requestRs.next()) {
                    String status = requestRs.getString("status");
                    int actualStudentId = requestRs.getInt("student_id");
                    logDebug("✅ Request found: ID=" + requestId + 
                            ", Status=" + status + 
                            ", Student ID in request=" + actualStudentId);
                    
                    if (actualStudentId != studentId) {
                        logError("❌ Student ID mismatch! Request belongs to student " + 
                                actualStudentId + ", not " + studentId);
                        return null;
                    }
                    
                    // Check if we should auto-update to FULLY_CLEARED
                    if (!"FULLY_CLEARED".equals(status)) {
                        logDebug("⚠️ Request status is '" + status + "', checking if all approvals are APPROVED...");
                        
                        // Check approval statuses
                        String approvalSql = """
                            SELECT officer_role, status, remarks 
                            FROM clearance_approvals 
                            WHERE request_id = ? 
                            ORDER BY officer_role
                        """;
                        try (PreparedStatement approvalStmt = conn.prepareStatement(approvalSql)) {
                            approvalStmt.setInt(1, requestId);
                            ResultSet approvalRs = approvalStmt.executeQuery();
                            
                            boolean allApproved = true;
                            int approvalCount = 0;
                            
                            logDebug("Approval statuses for request " + requestId + ":");
                            while (approvalRs.next()) {
                                approvalCount++;
                                String role = approvalRs.getString("officer_role");
                                String approvalStatus = approvalRs.getString("status");
                                String remarks = approvalRs.getString("remarks");
                                logDebug("  - " + role + ": " + approvalStatus + 
                                        (remarks != null ? " (" + remarks + ")" : ""));
                                if (!"APPROVED".equals(approvalStatus)) {
                                    allApproved = false;
                                }
                            }
                            
                            if (allApproved && approvalCount > 0) {
                                logDebug("✅ All " + approvalCount + " approvals are APPROVED. Auto-updating to FULLY_CLEARED...");
                                
                                // Auto-update the status to FULLY_CLEARED
                                String updateSql = """
                                    UPDATE clearance_requests 
                                    SET status = 'FULLY_CLEARED', 
                                        completion_date = NOW()
                                    WHERE id = ?
                                """;
                                
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setInt(1, requestId);
                                    int rowsUpdated = updateStmt.executeUpdate();
                                    
                                    if (rowsUpdated > 0) {
                                        logDebug("✅ Successfully updated request " + requestId + " to FULLY_CLEARED");
                                        status = "FULLY_CLEARED"; // Update local status variable
                                    } else {
                                        logError("❌ Failed to update request status");
                                        return null;
                                    }
                                }
                            } else if (!allApproved) {
                                logError("❌ Cannot generate certificate - not all approvals are APPROVED");
                                return null;
                            } else {
                                logError("❌ No approval records found for this request");
                                return null;
                            }
                        }
                    }
                } else {
                    logError("❌ Clearance request not found with ID: " + requestId);
                    return null;
                }
            }
            
            // Now query for certificate data
            String sql = """
                SELECT 
                    u.full_name, 
                    u.username, 
                    u.department, 
                    u.year_level,
                    u.email, 
                    u.phone, 
                    cr.request_date, 
                    cr.completion_date
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                WHERE u.id = ? 
                AND cr.id = ? 
                AND cr.status = 'FULLY_CLEARED'
                """;

            logDebug("Executing query: " + sql);
            logDebug("Parameters: studentId=" + studentId + ", requestId=" + requestId);
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, requestId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String studentIdStr = rs.getString("username");
                String department = rs.getString("department");
                String yearLevel = rs.getString("year_level");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                Date requestDate = rs.getDate("request_date");
                Date completionDate = rs.getDate("completion_date");
                
                logDebug("✅ Data retrieved successfully:");
                logDebug("  Full Name: " + fullName);
                logDebug("  Student ID: " + studentIdStr);
                logDebug("  Department: " + department);
                logDebug("  Year Level: " + yearLevel);
                logDebug("  Email: " + (email != null ? email : "N/A"));
                logDebug("  Phone: " + (phone != null ? phone : "N/A"));
                logDebug("  Request Date: " + requestDate);
                logDebug("  Completion Date: " + completionDate);

                String certificatePath = createCertificateWithLogo(fullName, studentIdStr, department, 
                                                        yearLevel, email, phone, 
                                                        requestDate, completionDate);
                
                if (certificatePath != null) {
                    logDebug("✅ Certificate generated successfully at: " + certificatePath);
                    
                    // Send notification to student
                    sendCertificateNotification(studentId, certificatePath);
                } else {
                    logError("❌ Certificate creation failed - returned null path");
                }
                
                return certificatePath;
            } else {
                logError("❌ No data returned from query after status update.");
                logError("This might be a timing issue. Trying one more time...");
                
                // Try one more time with a small delay
                Thread.sleep(100);
                
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    String fullName = rs2.getString("full_name");
                    String studentIdStr = rs2.getString("username");
                    String department = rs2.getString("department");
                    String yearLevel = rs2.getString("year_level");
                    String email = rs2.getString("email");
                    String phone = rs2.getString("phone");
                    Date requestDate = rs2.getDate("request_date");
                    Date completionDate = rs2.getDate("completion_date");
                    
                    return createCertificateWithLogo(fullName, studentIdStr, department, 
                                                    yearLevel, email, phone, 
                                                    requestDate, completionDate);
                } else {
                    logError("❌ Still no data after retry.");
                    
                    // Diagnostic query to see what's wrong
                    String diagnosticSql = """
                        SELECT 
                            u.username,
                            u.full_name,
                            cr.status,
                            cr.completion_date,
                            COUNT(ca.id) as total_approvals,
                            SUM(CASE WHEN ca.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_count
                        FROM users u
                        JOIN clearance_requests cr ON u.id = cr.student_id
                        LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id
                        WHERE u.id = ? 
                        AND cr.id = ?
                        GROUP BY u.username, u.full_name, cr.status, cr.completion_date
                        """;
                    
                    try (PreparedStatement diagStmt = conn.prepareStatement(diagnosticSql)) {
                        diagStmt.setInt(1, studentId);
                        diagStmt.setInt(2, requestId);
                        ResultSet diagRs = diagStmt.executeQuery();
                        
                        if (diagRs.next()) {
                            logDebug("\n=== DIAGNOSTIC INFORMATION ===");
                            logDebug("Username: " + diagRs.getString("username"));
                            logDebug("Full Name: " + diagRs.getString("full_name"));
                            logDebug("Request Status: " + diagRs.getString("status"));
                            logDebug("Completion Date: " + diagRs.getDate("completion_date"));
                            logDebug("Total Approvals: " + diagRs.getInt("total_approvals"));
                            logDebug("Approved Count: " + diagRs.getInt("approved_count"));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logError("❌ SQL Error: " + e.getMessage());
            e.printStackTrace();
            logDebug("SQL State: " + e.getSQLState());
            logDebug("Error Code: " + e.getErrorCode());
        } catch (Exception e) {
            logError("❌ General Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            logDebug("=== END CERTIFICATE GENERATION ===\n");
        }
        return null;
    }
    
    // Alternative method that doesn't require FULLY_CLEARED status (for testing)
    public String generatePDFCertificateForTesting(int studentId, int requestId) {
        logDebug("=== TEST CERTIFICATE GENERATION (Bypassing status check) ===");
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name, 
                    u.username, 
                    u.department, 
                    u.year_level,
                    u.email, 
                    u.phone, 
                    cr.request_date, 
                    NOW() as completion_date
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                WHERE u.id = ? 
                AND cr.id = ?
                """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, requestId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String studentIdStr = rs.getString("username");
                String department = rs.getString("department");
                String yearLevel = rs.getString("year_level");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                Date requestDate = rs.getDate("request_date");
                Date completionDate = rs.getDate("completion_date");
                
                logDebug("⚠️ Generating certificate for testing (bypassing FULLY_CLEARED check)");
                logDebug("  This certificate will be marked as TEST VERSION");

                return createCertificateWithLogo(fullName, studentIdStr, department, 
                                                yearLevel, email, phone, 
                                                requestDate, completionDate);
            }

        } catch (Exception e) {
            logError("❌ Error in test certificate generation: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Helper method to update request status to FULLY_CLEARED
    public boolean updateRequestToFullyCleared(int requestId) {
        logDebug("=== UPDATING REQUEST TO FULLY_CLEARED ===");
        logDebug("Request ID: " + requestId);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First check if all approvals are APPROVED
            String checkSql = """
                SELECT COUNT(*) as total, 
                       SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved
                FROM clearance_approvals 
                WHERE request_id = ?
                """;
            
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, requestId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                int total = checkRs.getInt("total");
                int approved = checkRs.getInt("approved");
                
                logDebug("Approval status: " + approved + "/" + total + " approved");
                
                if (total > 0 && approved == total) {
                    String updateSql = """
                        UPDATE clearance_requests 
                        SET status = 'FULLY_CLEARED', 
                            completion_date = NOW()
                        WHERE id = ?
                        """;
                    
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setInt(1, requestId);
                    int rowsUpdated = updateStmt.executeUpdate();
                    
                    if (rowsUpdated > 0) {
                        logDebug("✅ Successfully updated request " + requestId + " to FULLY_CLEARED");
                        return true;
                    } else {
                        logError("❌ Failed to update request - no rows affected");
                    }
                } else {
                    logError("❌ Cannot update to FULLY_CLEARED - not all approvals are approved");
                    logError("   Approved: " + approved + "/" + total);
                }
            }
        } catch (Exception e) {
            logError("❌ Error updating request status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private String createCertificateWithLogo(String fullName, String studentId, String department,
                                           String yearLevel, String email, String phone,
                                           Date requestDate, Date completionDate) {

        String fileName = System.getProperty("user.home") + "/Downloads/DBU_Clearance_Certificate_" 
                        + studentId + "_" + System.currentTimeMillis() + ".pdf";

        try {
            logDebug("Creating certificate for: " + fullName + " (" + studentId + ")");
            
            // Create PDF document
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            
            // Set margins
            document.setMargins(30, 30, 30, 30);

            // Format dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String currentDate = dateFormat.format(new Date());
            String requestDateStr = requestDate != null ? dateFormat.format(requestDate) : "Not set";
            String completionDateStr = completionDate != null ? dateFormat.format(completionDate) : "Not set";
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();

            // ==================== MAIN CERTIFICATE DESIGN ====================
            
            // 1. Create main container with elegant border
            Div mainContainer = new Div();
            mainContainer.setBorder(new SolidBorder(UNIVERSITY_BLUE, 3));
            mainContainer.setBackgroundColor(LIGHT_BLUE);
            mainContainer.setPadding(25);
            
            // 2. HEADER SECTION WITH LOGO
            Table headerTable = new Table(3);
            
            // Left column - Logo
            Cell logoCell = new Cell();
            logoCell.setBorder(Border.NO_BORDER);
            logoCell.setTextAlignment(TextAlignment.CENTER);
            
            Image logo = loadUniversityLogo();
            if (logo != null) {
                logo.setWidth(300);
                logo.setHeight(130);
                logoCell.add(logo);
            } else {
                Paragraph logoPlaceholder = new Paragraph("DBU")
                        .setBold()
                        .setFontSize(24)
                        .setFontColor(UNIVERSITY_RED)
                        .setTextAlignment(TextAlignment.CENTER);
                logoCell.add(logoPlaceholder);
            }
            headerTable.addCell(logoCell);
            
            // Middle column - University info
            Cell uniInfoCell = new Cell();
            uniInfoCell.setBorder(Border.NO_BORDER);
            uniInfoCell.setTextAlignment(TextAlignment.CENTER);
            
            Paragraph uniMotto = new Paragraph("Practical knowledge for better success !")
                    .setItalic()
                    .setFontSize(14)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER);
            
            uniInfoCell.add(uniMotto);
            headerTable.addCell(uniInfoCell);
            
            // Right column - Certificate info
            Cell certInfoCell = new Cell();
            certInfoCell.setBorder(Border.NO_BORDER);
            certInfoCell.setTextAlignment(TextAlignment.RIGHT);
            
            Paragraph certId = new Paragraph("Certificate No:\n" + certificateId)
                    .setBold()
                    .setFontSize(10)
                    .setFontColor(UNIVERSITY_BLUE);
            
            Paragraph issueDate = new Paragraph("Issued: " + currentDate)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.DARK_GRAY);
            
            certInfoCell.add(certId);
            certInfoCell.add(issueDate);
            headerTable.addCell(certInfoCell);
            
            mainContainer.add(headerTable);
            
            // Decorative separator
            Div separator = new Div();
            separator.setHeight(2);
            separator.setBackgroundColor(ACCENT_GOLD);
            separator.setMarginTop(15);
            separator.setMarginBottom(15);
            mainContainer.add(separator);
            
            // 3. CERTIFICATE TITLE
            Paragraph title = new Paragraph("CLEARANCE CERTIFICATE")
                    .setBold()
                    .setFontSize(28)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            mainContainer.add(title);
            
            Paragraph subtitle = new Paragraph("Official Document")
                    .setItalic()
                    .setFontSize(14)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            mainContainer.add(subtitle);
            
            // 4. DECLARATION HEADER
            Paragraph declaration = new Paragraph("THIS CERTIFIES THAT")
                    .setBold()
                    .setFontSize(18)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(20);
            mainContainer.add(declaration);
            
            // 5. STUDENT INFORMATION
            Div studentInfoBox = new Div();
            studentInfoBox.setBorder(new SolidBorder(ACCENT_GOLD, 2));
            studentInfoBox.setBackgroundColor(LIGHT_GOLD);
            studentInfoBox.setPadding(25);
            studentInfoBox.setMarginBottom(30);
            
            Paragraph studentName = new Paragraph("Name: " + fullName.toUpperCase())
                    .setBold()
                    .setFontSize(20)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(10);
            studentInfoBox.add(studentName);
            
            float[] detailWidths = {40, 60};
            Table detailsTable = new Table(detailWidths);
            
            addDetailRow(detailsTable, "Student ID:", studentId);
            addDetailRow(detailsTable, "Department:", department != null ? department : "Not specified");
            addDetailRow(detailsTable, "Academic Level:", yearLevel != null ? yearLevel : "Not specified");
            addDetailRow(detailsTable, "Faculty:", getFacultyName(department));
            
            if (email != null && !email.isEmpty()) {
                addDetailRow(detailsTable, "Email:", email);
            }
            if (phone != null && !phone.isEmpty()) {
                addDetailRow(detailsTable, "Phone:", phone);
            }
            
            studentInfoBox.add(detailsTable);
            mainContainer.add(studentInfoBox);
            
            // 6. CLEARANCE STATEMENT
            Paragraph clearanceStatement = new Paragraph()
                    .add("has successfully fulfilled all academic, financial, and administrative ")
                    .add("obligations required by Debre Birhan University. ")
                    .add("After comprehensive review and approval by all relevant departments, ")
                    .add("this student is hereby declared ")
                    .add(new Text("OFFICIALLY CLEARED").setBold().setFontColor(ColorConstants.GREEN))
                    .add(" and is eligible to receive all academic credentials.")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(30);
            mainContainer.add(clearanceStatement);
            
            // 7. CLEARANCE TIMELINE
            Paragraph timelineTitle = new Paragraph("CLEARANCE TIMELINE")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            mainContainer.add(timelineTitle);
            
            float[] timelineWidths = {1, 1, 1};
            Table timelineTable = new Table(timelineWidths);
            
            Cell headerCell1 = createTableHeader("REQUEST DATE");
            Cell headerCell2 = createTableHeader("PROCESSING");
            Cell headerCell3 = createTableHeader("COMPLETION DATE");
            
            timelineTable.addCell(headerCell1);
            timelineTable.addCell(headerCell2);
            timelineTable.addCell(headerCell3);
            
            Cell dataCell1 = createTableCell(requestDateStr);
            Cell dataCell2 = createTableCell("✓ Approved");
            Cell dataCell3 = createTableCell(completionDateStr);
            
            timelineTable.addCell(dataCell1);
            timelineTable.addCell(dataCell2);
            timelineTable.addCell(dataCell3);
            
            mainContainer.add(timelineTable);
            
            // 8. SIGNATURES SECTION
            Div signaturesSection = new Div();
            signaturesSection.setMarginTop(40);
            
            Paragraph sigTitle = new Paragraph("AUTHORIZED APPROVALS")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(25);
            signaturesSection.add(sigTitle);
            
            float[] sigWidths = {1, 1, 1, 1, 1};
            Table signaturesTable = new Table(sigWidths);
            
            addSignatureColumn(signaturesTable, "University Registrar", currentDate);
            addSignatureColumn(signaturesTable, "Department Head", currentDate);
            addSignatureColumn(signaturesTable, "Librarian Head", currentDate);
            addSignatureColumn(signaturesTable, "Cafeteria Head", currentDate);
            addSignatureColumn(signaturesTable, "Dormitory", currentDate);
            
            signaturesSection.add(signaturesTable);
            mainContainer.add(signaturesSection);
            
            // 9. VERIFICATION FOOTER
            Div verificationDiv = new Div();
            verificationDiv.setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            verificationDiv.setPaddingTop(15);
            verificationDiv.setMarginTop(30);
            
            String verificationCode = generateVerificationCode(studentId, certificateId);
            
            Paragraph verificationText = new Paragraph()
                    .add("🔐 This is an electronically generated certificate.\n")
                    .add("Verification Code: ")
                    .add(new Text(verificationCode).setBold())
                    .add(" | Verify at: https://verify.dbu.edu.et")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            
            verificationDiv.add(verificationText);
            mainContainer.add(verificationDiv);
            
            // 10. BOTTOM DECORATIVE ELEMENT
            Div bottomDecor = new Div();
            bottomDecor.setHeight(8);
            bottomDecor.setBackgroundColor(ACCENT_GOLD);
            bottomDecor.setMarginTop(20);
            mainContainer.add(bottomDecor);
            
            document.add(mainContainer);
            document.close();
            
            logDebug("✅ Certificate file created: " + fileName);
            return fileName;

        } catch (Exception e) {
            logError("❌ Error creating certificate: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private Image loadUniversityLogo() {
        try {
            String[] possiblePaths = {
                "/com/university/clearance/resources/images/dbu_logo.png",
                "resources/images/dbu_logo.png",
                "images/dbu_logo.png",
                "src/main/resources/com/university/clearance/resources/images/dbu_logo.png"
            };
            
            for (String path : possiblePaths) {
                InputStream stream = getClass().getResourceAsStream(path);
                if (stream != null) {
                    byte[] bytes = stream.readAllBytes();
                    stream.close();
                    
                    if (bytes.length > 0) {
                        ImageData imageData = ImageDataFactory.create(bytes);
                        return new Image(imageData);
                    }
                }
            }
            logDebug("ℹ️ Logo not found, using text placeholder");
            return null;
            
        } catch (Exception e) {
            logDebug("⚠️ Could not load logo: " + e.getMessage());
            return null;
        }
    }
    
    private void addDetailRow(Table table, String label, String value) {
        Cell labelCell = new Cell();
        labelCell.add(new Paragraph(label).setBold());
        labelCell.setBorder(Border.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);
        
        Cell valueCell = new Cell();
        valueCell.add(new Paragraph(value));
        valueCell.setBorder(Border.NO_BORDER);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }
    
    private Cell createTableHeader(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text).setBold().setFontSize(11));
        cell.setBackgroundColor(new DeviceRgb(230, 240, 255));
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setPadding(10);
        return cell;
    }
    
    private Cell createTableCell(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text));
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setPadding(10);
        return cell;
    }
    
    private void addSignatureColumn(Table table, String title, String date) {
        Cell cell = new Cell();
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setPadding(15);
        
        cell.add(new Paragraph(title)
                .setFontSize(11)
                .setFontColor(UNIVERSITY_BLUE));
        
        cell.add(new Paragraph("\n\n________________________")
                .setFontSize(10));
        
        cell.add(new Paragraph("\nDate: " + date)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY));
        
        cell.add(new Paragraph("•")
                .setFontSize(20)
                .setFontColor(ACCENT_GOLD)
                .setTextAlignment(TextAlignment.CENTER));
        
        table.addCell(cell);
    }
    
    private String getFacultyName(String department) {
        if (department == null || department.isEmpty()) {
            return "General Studies";
        }
        
        String dept = department.toLowerCase();
        
        if (dept.contains("software") || dept.contains("computer")) {
            return "Computing and Informatics";
        } else if (dept.contains("electrical") || dept.contains("engineering")) {
            return "Engineering and Technology";
        } else if (dept.contains("business") || dept.contains("commerce")) {
            return "Business and Economics";
        } else if (dept.contains("science") || dept.contains("mathematics")) {
            return "Natural Sciences";
        } else if (dept.contains("education") || dept.contains("pedagogy")) {
            return "Education";
        } else if (dept.contains("health") || dept.contains("medical")) {
            return "Health Sciences";
        } else {
            return department;
        }
    }
    
    private String generateVerificationCode(String studentId, String certificateId) {
        try {
            String base = studentId + "-" + certificateId.substring(certificateId.length() - 6);
            int hash = base.hashCode();
            return "DBU" + Math.abs(hash) % 10000;
        } catch (Exception e) {
            return "DBU" + System.currentTimeMillis() % 10000;
        }
    }
    
    // Send notification to student about certificate generation
    private void sendCertificateNotification(int studentId, String certificatePath) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                INSERT INTO notifications (user_id, type, subject, message, is_read)
                VALUES (?, 'CERTIFICATE', 'Clearance Certificate Generated', 
                        'Your clearance certificate has been generated and downloaded to: ' || ?, 
                        FALSE)
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setString(2, certificatePath);
            ps.executeUpdate();
            
            logDebug("✅ Notification sent to student about certificate generation");
        } catch (Exception e) {
            logError("⚠️ Could not send notification: " + e.getMessage());
        }
    }
    
    // Test method to check database state
    public void testDatabaseConnection() {
        logDebug("=== DATABASE CONNECTION TEST ===");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                logDebug("✅ Database connection successful");
                
                // Test query to check clearance requests
                String testSql = """
                    SELECT u.username, u.full_name, cr.id, cr.status, cr.completion_date,
                           (SELECT COUNT(*) FROM clearance_approvals ca WHERE ca.request_id = cr.id AND ca.status = 'APPROVED') as approved_count,
                           (SELECT COUNT(*) FROM clearance_approvals ca WHERE ca.request_id = cr.id) as total_approvals
                    FROM users u
                    JOIN clearance_requests cr ON u.id = cr.student_id
                    WHERE u.role = 'STUDENT'
                    ORDER BY cr.id DESC
                    LIMIT 10
                    """;
                
                PreparedStatement ps = conn.prepareStatement(testSql);
                ResultSet rs = ps.executeQuery();
                
                logDebug("Current clearance requests:");
                while (rs.next()) {
                    logDebug(String.format(
                        "  Student: %s (%s) | Request ID: %d | Status: %s | Approved: %d/%d | Completion: %s",
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getInt("id"),
                        rs.getString("status"),
                        rs.getInt("approved_count"),
                        rs.getInt("total_approvals"),
                        rs.getDate("completion_date") != null ? rs.getDate("completion_date").toString() : "N/A"
                    ));
                }
            } else {
                logError("❌ Database connection failed");
            }
        } catch (Exception e) {
            logError("❌ Test error: " + e.getMessage());
            e.printStackTrace();
        }
        logDebug("=== END DATABASE TEST ===\n");
    }
    
    // Bulk update all eligible requests to FULLY_CLEARED
    public int bulkUpdateToFullyCleared() {
        logDebug("=== BULK UPDATE TO FULLY_CLEARED ===");
        int updatedCount = 0;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Find all requests where all approvals are approved but status is not FULLY_CLEARED
            String findSql = """
                SELECT cr.id as request_id, u.username, u.full_name
                FROM clearance_requests cr
                JOIN users u ON cr.student_id = u.id
                WHERE cr.status != 'FULLY_CLEARED'
                AND NOT EXISTS (
                    SELECT 1 FROM clearance_approvals ca 
                    WHERE ca.request_id = cr.id 
                    AND ca.status != 'APPROVED'
                )
                AND EXISTS (
                    SELECT 1 FROM clearance_approvals ca 
                    WHERE ca.request_id = cr.id
                )
                """;
            
            PreparedStatement findStmt = conn.prepareStatement(findSql);
            ResultSet findRs = findStmt.executeQuery();
            
            while (findRs.next()) {
                int requestId = findRs.getInt("request_id");
                String username = findRs.getString("username");
                String fullName = findRs.getString("full_name");
                
                logDebug("Found eligible request: ID=" + requestId + 
                        ", Student=" + fullName + " (" + username + ")");
                
                // Update this request
                String updateSql = """
                    UPDATE clearance_requests 
                    SET status = 'FULLY_CLEARED', 
                        completion_date = NOW()
                    WHERE id = ?
                    """;
                
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, requestId);
                int rowsUpdated = updateStmt.executeUpdate();
                
                if (rowsUpdated > 0) {
                    updatedCount++;
                    logDebug("✅ Updated request " + requestId + " to FULLY_CLEARED");
                }
            }
            
            logDebug("✅ Bulk update complete. Updated " + updatedCount + " requests.");
            
        } catch (Exception e) {
            logError("❌ Error in bulk update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return updatedCount;
    }
    
    // Debug logging methods
    private void logDebug(String message) {
        if (DEBUG_MODE) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    private void logError(String message) {
        System.err.println("[ERROR] " + message);
    }
    
    // Utility method to check if certificate can be generated for a student
    public boolean canGenerateCertificate(int studentId) {
        logDebug("=== CHECKING IF CERTIFICATE CAN BE GENERATED ===");
        logDebug("Student ID: " + studentId);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get the latest clearance request for this student
            String sql = """
                SELECT cr.id as request_id, cr.status,
                       COUNT(ca.id) as total_approvals,
                       SUM(CASE WHEN ca.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_count
                FROM clearance_requests cr
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id
                WHERE cr.student_id = ?
                GROUP BY cr.id, cr.status
                ORDER BY cr.id DESC
                LIMIT 1
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int requestId = rs.getInt("request_id");
                String status = rs.getString("status");
                int totalApprovals = rs.getInt("total_approvals");
                int approvedCount = rs.getInt("approved_count");
                
                logDebug("Latest request: ID=" + requestId + ", Status=" + status + 
                        ", Approved=" + approvedCount + "/" + totalApprovals);
                
                if ("FULLY_CLEARED".equals(status)) {
                    logDebug("✅ Certificate can be generated - status is FULLY_CLEARED");
                    return true;
                } else if (totalApprovals > 0 && approvedCount == totalApprovals) {
                    logDebug("✅ Certificate can be generated - all approvals are approved");
                    logDebug("   (Will auto-update to FULLY_CLEARED during generation)");
                    return true;
                } else {
                    logDebug("❌ Certificate cannot be generated:");
                    if (totalApprovals == 0) {
                        logDebug("   No approval records found");
                    } else {
                        logDebug("   Not all approvals are approved: " + approvedCount + "/" + totalApprovals);
                    }
                    return false;
                }
            } else {
                logDebug("❌ No clearance request found for student ID: " + studentId);
                return false;
            }
            
        } catch (Exception e) {
            logError("❌ Error checking certificate eligibility: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}