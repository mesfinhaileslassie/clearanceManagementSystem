package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for generating the final, professionally formatted 
 * clearance certificate in PDF format using the Apache PDFBox library.
 * This version uses a formal, minimalist black and white design with no colored 
 * backgrounds, borders, or watermarks.
 */
public class PDFCertificateService {
    
    // ==================== DESIGN CONSTANTS ====================
    // Increased margin for a cleaner, more spacious look
    private static final float MARGIN = 60; 
    private static final float LINE_HEIGHT = 16;
    // FIX: Switched to a more robust placeholder service known for direct PNG output 
    // to solve server-side loading issues. (60x60 B/W placeholder)
    private static final String LOGO_URL = "https://via.placeholder.com/60/FFFFFF/000000?text=DBU";
    
    // Black and White Color Scheme
    private static final float[] BLACK = {0.0f, 0.0f, 0.0f};
    private static final float[] DARK_GREY = {0.2f, 0.2f, 0.2f};
    private static final float[] MEDIUM_GREY = {0.5f, 0.5f, 0.5f};
    private static final float[] LIGHT_GREY = {0.95f, 0.95f, 0.95f}; // Used for subtle table banding
    private static final float[] WHITE = {1.0f, 1.0f, 1.0f};
    
    /**
     * Generates and saves a formal PDF clearance certificate with a B&W design.
     * @param studentId The ID of the student.
     * @param requestId The ID of the completed clearance request.
     * @return The absolute path to the saved PDF file, or null on failure.
     */
    public String generatePDFCertificate(int studentId, int requestId) {
        System.out.println("\n=== FORMAL B&W PDF CERTIFICATE GENERATION ===");
        System.out.println("Student ID: " + studentId + ", Request ID: " + requestId);      
        
        try {
            // 1. Fetch all required data
            StudentData studentData = getStudentData(studentId);
            ApprovalSummary approvalSummary = getApprovalSummary(requestId);
            List<ApprovalDetail> approvalDetails = getApprovalDetails(requestId);
            
            if (studentData == null) {
                System.out.println("ERROR: Could not fetch student data");
                return null;
            }
            
            // Generate a Certificate Number
            String certificateNo = "DBU-CLR-" + requestId + studentId + System.currentTimeMillis() % 1000;
            
            // 2. Create professional PDF document
            try (PDDocument document = new PDDocument()) { 
                PDPage page = new PDPage(PDRectangle.A4); 
                document.addPage(page);
                
                // Load the logo image object
                PDImageXObject logoImage = loadImageFromUrl(document, LOGO_URL);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                
                // 3. Add formal design elements (No colored backgrounds, borders, or watermarks)
                addUniversityHeader(contentStream, page, logoImage);

                // 4. Add certificate content (The new formal declaration)
                float contentYStart = 680; // Adjusted start Y
                float approvalYStart = addFormalDeclaration(contentStream, page, contentYStart, studentData, approvalSummary, certificateNo);
                
                // Add department approvals below the declaration
                addDepartmentApprovals(contentStream, page, approvalDetails, approvalYStart);
                
                // Final elements 
                addOfficialSeal(contentStream, page);
                addFooter(contentStream, page);
                
                contentStream.close();
                
                // 5. Save PDF with unique filename
                String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = String.format("Debre_Birhan_University_Clearance_Formal_%s_%s.pdf", 
                    studentData.getStudentId(), 
                    timeStamp);
                
                String downloadsPath = System.getProperty("user.home") + "/Downloads/" + fileName;
                
                document.save(downloadsPath);
                
                System.out.println("✅ Formal B&W PDF generated successfully at: " + downloadsPath);
                return downloadsPath;
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR generating formal PDF: " + e.getMessage());
            System.err.println("HINT: Please close the PDF file if it is currently open in a reader program.");
            e.printStackTrace();
            return null;
        }
    }
    
    // ==================== PDF DRAWING UTILITIES ====================

    /**
     * Loads an image from a URL into a PDFBox PDImageXObject.
     */
    private PDImageXObject loadImageFromUrl(PDDocument document, String url) {
        try (java.io.InputStream in = new URL(url).openStream()) {
            System.out.println("Attempting to load B/W logo from URL: " + url);
            return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "Logo");
        } catch (IOException e) {
            System.err.println("WARNING: Could not load logo image from URL: " + url + " - This may be due to network restrictions or an invalid image format. Error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Draws text at a specific position.
     */
    private void drawText(PDPageContentStream contentStream, PDType1Font font, float size, float[] color, float x, float y, String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, size);
        contentStream.setNonStrokingColor(color[0], color[1], color[2]);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
    
    /**
     * Draws a key-value pair with colon separation, maintaining alignment.
     */
    private void drawKeyValuePair(PDPageContentStream contentStream, PDType1Font font, float size, float yPos, float xKey, float xValue, String key, String value) throws IOException {
        drawText(contentStream, font, size, BLACK, xKey, yPos, key);
        drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), size, DARK_GREY, xValue, yPos, value);
    }

    // ==================== PDF CONTENT METHODS ====================
    
    // All background and border methods removed to ensure B&W compliance

    private void addUniversityHeader(PDPageContentStream contentStream, PDPage page, PDImageXObject logoImage) throws IOException {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // --- Calculate positions for centered block ---
        String title = "DEBRE BIRHAN UNIVERSITY";
        float titleSize = 24;
        
        // Calculate the required width for the header block (Logo + Gap + Title)
        float logoSize = logoImage != null ? 60 : 0;
        float gap = logoImage != null ? 10 : 0;
        float titleWidth = bold.getStringWidth(title) / 1000 * titleSize;
        float totalHeaderWidth = logoSize + gap + titleWidth;
        
        // Center the entire block
        float headerXStart = (width - totalHeaderWidth) / 2;
        float currentX = headerXStart;
        float headerY = height - 80;
        
        if (logoImage != null) {
            // 1. Draw Logo
            contentStream.drawImage(logoImage, currentX, headerY - 15, logoSize, logoSize);
            currentX += logoSize + gap;
        }

        // 2. University name (Next to logo, or centered if no logo)
        float titleY = headerY;
        drawText(contentStream, bold, titleSize, BLACK, currentX, titleY, title);
        
        // 3. Official Certificate Title/Tagline
        String motto = "OFFICIAL GRADUATION CLEARANCE CERTIFICATE";
        float mottoSize = 12;
        float mottoWidth = regular.getStringWidth(motto) / 1000 * mottoSize;
        
        // Draw Motto centered beneath the combined Title/Logo block
        drawText(contentStream, regular, mottoSize, DARK_GREY, width/2 - mottoWidth/2, headerY - 25, motto);
        
        // Add a clean separator line
        contentStream.setStrokingColor(BLACK[0], BLACK[1], BLACK[2]);
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(MARGIN, headerY - 45);
        contentStream.lineTo(width - MARGIN, headerY - 45);
        contentStream.stroke();
    }
    
    /**
     * Adds the formal declaration text and student details.
     * @return The starting Y position for the next section (Department Approvals).
     */
    private float addFormalDeclaration(PDPageContentStream contentStream, PDPage page, float yStart, StudentData studentData, ApprovalSummary summary, String certificateNo) throws IOException {
        float yPos = yStart; 
        float xKey = MARGIN;
        float xValue = MARGIN + 220; // Alignment for value column
        
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = 11;
        
        // --- Header Details ---
        
        // 1. Certificate No.
        drawKeyValuePair(contentStream, bold, fontSize, yPos, xKey, xValue, "Certificate No.:", certificateNo);
        yPos -= LINE_HEIGHT;
        
        // 2. Issue Date
        String issueDate = summary.getCompletionDate() != null ? summary.getCompletionDate() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        drawKeyValuePair(contentStream, bold, fontSize, yPos, xKey, xValue, "Issue Date:", issueDate);
        
        yPos -= 3 * LINE_HEIGHT;
        
        // --- Introduction Sentence ---
        String intro = "This is to formally certify that:";
        drawText(contentStream, regular, fontSize, DARK_GREY, xKey, yPos, intro);
        
        yPos -= 2 * LINE_HEIGHT;
        
        // --- Student Details ---
        
        // Key-value pairs for student profile
        String[][] details = {
            {"Student Name:", studentData.getFullName()},
            {"Student ID:", studentData.getStudentId()},
            {"Program / Department:", studentData.getDepartment()},
            {"Academic Year / Level:", studentData.getYearLevel()},
        };
        
        for (String[] detail : details) {
            drawKeyValuePair(contentStream, bold, fontSize, yPos, xKey, xValue, detail[0], detail[1]);
            yPos -= LINE_HEIGHT;
        }
        
        yPos -= 2 * LINE_HEIGHT;
        
        // --- Declaration Text Block ---
        
        String p1 = "has satisfactorily fulfilled all academic, financial, and administrative obligations required by Debre Birhan University. The student has been duly examined and confirmed by the relevant university offices to have no outstanding liabilities or responsibilities.";
        String p2_start = "Accordingly, the student is hereby declared ";
        String p2_cleared = "CLEARED";
        String p2_end = " and is eligible to receive all official academic credentials from Debre Birhan University.";

        // Draw first paragraph (multi-line)
        yPos = drawFormalParagraph(contentStream, p1, xKey, yPos, fontSize, regular, DARK_GREY, page.getMediaBox().getWidth() - 2 * MARGIN);
        
        yPos -= LINE_HEIGHT;
        
        // Draw second paragraph (split to emphasize the CLEARED status)
        contentStream.beginText();
        contentStream.setFont(regular, fontSize);
        contentStream.setNonStrokingColor(DARK_GREY[0], DARK_GREY[1], DARK_GREY[2]);
        contentStream.newLineAtOffset(xKey, yPos);
        
        // Part 1: "Accordingly, the student is hereby declared "
        contentStream.showText(p2_start);
        
        // Part 2: "CLEARED" (Bold and Primary Color - using BLACK for formal look)
        contentStream.setFont(bold, fontSize);
        contentStream.setNonStrokingColor(BLACK[0], BLACK[1], BLACK[2]);
        contentStream.showText(p2_cleared);
        
        // Part 3: " and is eligible..." (Return to regular font and color)
        contentStream.setFont(regular, fontSize);
        contentStream.setNonStrokingColor(DARK_GREY[0], DARK_GREY[1], DARK_GREY[2]);
        contentStream.showText(p2_end);
        
        contentStream.endText();
        
        yPos -= 3 * LINE_HEIGHT;

        return yPos; // Return the position for the next section
    }

    /**
     * Handles multi-line text drawing for formal paragraphs.
     * @return The new Y position after drawing the paragraph.
     */
    private float drawFormalParagraph(PDPageContentStream contentStream, String text, float x, float yStart, float fontSize, PDType1Font font, float[] color, float maxWidth) throws IOException {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float yPos = yStart;

        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color[0], color[1], color[2]);
        
        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String potentialLine = currentLine + " " + word;
                float potentialWidth = font.getStringWidth(potentialLine) / 1000 * fontSize;

                if (potentialWidth < maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    drawText(contentStream, font, fontSize, color, x, yPos, currentLine.toString());
                    yPos -= LINE_HEIGHT;
                    currentLine = new StringBuilder(word);
                }
            }
        }
        
        // Draw the last line
        if (currentLine.length() > 0) {
            drawText(contentStream, font, fontSize, color, x, yPos, currentLine.toString());
        }

        return yPos - LINE_HEIGHT; // Return Y position below the last line drawn
    }
    
    private void addDepartmentApprovals(PDPageContentStream contentStream, PDPage page, List<ApprovalDetail> approvals, float yStart) throws IOException {
        float yPos = yStart;
        float width = page.getMediaBox().getWidth();
        
        // Section Title
        drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14, BLACK, MARGIN, yPos, "Clearance Status Summary by Office");
        
        yPos -= 25;
        
        // Table Parameters
        String[] headers = {"OFFICE / DEPARTMENT", "STATUS", "OFFICER NAME", "APPROVAL DATE", "REMARKS"};
        float[] columnWidths = {160, 70, 110, 80, 110};
        float tableWidth = width - 2 * MARGIN;
        float rowHeight = 20;
        float currentX = MARGIN;
        float tableTopY = yPos;
        
        // Table grid settings
        contentStream.setStrokingColor(MEDIUM_GREY[0], MEDIUM_GREY[1], MEDIUM_GREY[2]);
        contentStream.setLineWidth(0.5f);

        // Draw top horizontal line (Thick for header separation)
        contentStream.setLineWidth(1.0f);
        contentStream.moveTo(MARGIN, yPos);
        contentStream.lineTo(width - MARGIN, yPos);
        contentStream.stroke();
        
        // Table header row background (Light Grey Banding)
        contentStream.setNonStrokingColor(LIGHT_GREY[0], LIGHT_GREY[1], LIGHT_GREY[2]); 
        contentStream.addRect(MARGIN, yPos - rowHeight, tableWidth, rowHeight);
        contentStream.fill();
        
        // Header text (BLACK text)
        for (int i = 0; i < headers.length; i++) {
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8, BLACK, currentX + 5, yPos - rowHeight/2 - 3, headers[i]);
            currentX += columnWidths[i];
        }

        // Draw header bottom line
        yPos -= rowHeight;
        contentStream.setLineWidth(1.0f);
        contentStream.moveTo(MARGIN, yPos);
        contentStream.lineTo(width - MARGIN, yPos);
        contentStream.stroke();

        // Prepare for thinner row dividers
        contentStream.setLineWidth(0.5f);
        
        // Table rows
        for (ApprovalDetail detail : approvals) {
            currentX = MARGIN;
            
            // Draw background for row stripe (Minimalistic banding: White/Light Grey)
            if (approvals.indexOf(detail) % 2 == 0) {
                contentStream.setNonStrokingColor(WHITE[0], WHITE[1], WHITE[2]);
            } else {
                contentStream.setNonStrokingColor(LIGHT_GREY[0], LIGHT_GREY[1], LIGHT_GREY[2]); 
            }
            contentStream.addRect(MARGIN, yPos - rowHeight, tableWidth, rowHeight);
            contentStream.fill();
            
            // Status text color (Neutral Dark Grey)
            float[] statusColor = DARK_GREY; // All text neutral
            
            // 1. Department
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, DARK_GREY, currentX + 5, yPos - rowHeight/2 - 3, detail.getDepartment());
            currentX += columnWidths[0];
            
            // 2. Status (Bold and Neutral)
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9, statusColor, currentX + 5, yPos - rowHeight/2 - 3, detail.getStatus());
            currentX += columnWidths[1];
            
            // 3. Approved By
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, DARK_GREY, currentX + 5, yPos - rowHeight/2 - 3, detail.getOfficerName());
            currentX += columnWidths[2];
            
            // 4. Date
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, MEDIUM_GREY, currentX + 5, yPos - rowHeight/2 - 3, detail.getApprovalDate());
            currentX += columnWidths[3];
            
            // 5. Remarks (truncated)
            String remarks = detail.getRemarks();
            if (remarks.length() > 30) {
                remarks = remarks.substring(0, 27) + "...";
            }
            drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8, MEDIUM_GREY, currentX + 5, yPos - rowHeight/2 - 3, remarks);
            
            yPos -= rowHeight;
            
            // Draw row separator line
            contentStream.moveTo(MARGIN, yPos);
            contentStream.lineTo(width - MARGIN, yPos);
            contentStream.stroke();
        }
        
        // Draw vertical lines (after all horizontal strokes are done for better overlay)
        currentX = MARGIN;
        contentStream.setLineWidth(0.5f);
        float tableBottomY = yPos;
        
        for (int i = 0; i < columnWidths.length - 1; i++) { // Skip the last column line
            currentX += columnWidths[i];
            contentStream.moveTo(currentX, tableTopY);
            contentStream.lineTo(currentX, tableBottomY);
            contentStream.stroke();
        }
    }

    private void addOfficialSeal(PDPageContentStream contentStream, PDPage page) throws IOException {
        float width = page.getMediaBox().getWidth();
        float xStart = width - 200;
        float yPos = 160;
        
        // Signature Line
        contentStream.setStrokingColor(MEDIUM_GREY[0], MEDIUM_GREY[1], MEDIUM_GREY[2]);
        contentStream.setLineWidth(1f);
        contentStream.moveTo(xStart, yPos);
        contentStream.lineTo(xStart + 150, yPos);
        contentStream.stroke();
        
        // Title below signature line
        String signatureTitle = "Registrar Office Head (Seal & Signature)";
        float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(signatureTitle) / 1000 * 10;
        drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10, DARK_GREY, xStart + 75 - titleWidth/2, yPos - 15, signatureTitle);
    }
    
    private void addFooter(PDPageContentStream contentStream, PDPage page) throws IOException {
        float width = page.getMediaBox().getWidth();
        float yPos = 55;
        
        // Footer line
        contentStream.setStrokingColor(BLACK[0], BLACK[1], BLACK[2]);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPos);
        contentStream.lineTo(width - MARGIN, yPos);
        contentStream.stroke();
        
        // Disclaimer (Left aligned)
        String disclaimer = "This certificate is electronically generated by Debre Birhan University. For verification, check online system.";
        drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8, MEDIUM_GREY, MARGIN, yPos - 15, disclaimer);
        
        // Generation timestamp (Right aligned)
        String timeStamp = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy @ HH:mm:ss"));
        float stampWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(timeStamp) / 1000 * 8;
        drawText(contentStream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8, MEDIUM_GREY, width - MARGIN - stampWidth, yPos - 15, timeStamp);
    }

    // ==================== DATA MODELS (Unchanged) ====================
    
    private static class StudentData {
        private String studentId;
        private String fullName;
        private String department;
        private String yearLevel;
        private String email;
        private String phone;
        
        public StudentData(String studentId, String fullName, String department, 
                          String yearLevel, String email, String phone) {
            this.studentId = studentId;
            this.fullName = fullName;
            this.department = department;
            this.yearLevel = yearLevel;
            this.email = email;
            this.phone = phone;
        }
        
        public String getStudentId() { return studentId; }
        public String getFullName() { return fullName; }
        public String getDepartment() { return department; }
        public String getYearLevel() { return yearLevel; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }
    
    private static class ApprovalSummary {
        private String overallStatus;
        private String requestDate;
        private String completionDate;
        private int totalDepartments;
        private int approvedCount;
        private int rejectedCount;
        private int pendingCount;
        
        public String getOverallStatus() { return overallStatus; }
        public String getRequestDate() { return requestDate; }
        public String getCompletionDate() { return completionDate; }
        public int getTotalDepartments() { return totalDepartments; }
        public int getApprovedCount() { return approvedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getPendingCount() { return pendingCount; }
        
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        public void setRequestDate(String requestDate) { this.requestDate = requestDate; }
        public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }
        public void setTotalDepartments(int totalDepartments) { this.totalDepartments = totalDepartments; }
        public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    }
    
    private static class ApprovalDetail {
        private String department;
        private String status;
        private String officerName;
        private String approvalDate;
        private String remarks;
        
        public ApprovalDetail(String department, String status, String officerName, 
                             String approvalDate, String remarks) {
            this.department = department;
            this.status = status;
            this.officerName = officerName;
            this.approvalDate = approvalDate;
            this.remarks = remarks;
        }
        
        public String getDepartment() { return department; }
        public String getStatus() { return status; }
        public String getOfficerName() { return officerName; }
        public String getApprovalDate() { return approvalDate; }
        public String getRemarks() { return remarks; }
    }
    
    // ==================== DATABASE METHODS (Unchanged) ====================
    
    private StudentData getStudentData(int studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT username, full_name, department, year_level, email, phone 
                FROM users WHERE id = ?
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new StudentData(
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("year_level"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
            }
        } catch (Exception e) {
            System.err.println("DB Error (getStudentData): " + e.getMessage());
        }
        return null;
    }
    
    private ApprovalSummary getApprovalSummary(int requestId) {
        ApprovalSummary summary = new ApprovalSummary();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get request details
            String requestSql = """
                SELECT status, request_date, completion_date 
                FROM clearance_requests WHERE id = ?
                """;
            PreparedStatement requestPs = conn.prepareStatement(requestSql);
            requestPs.setInt(1, requestId);
            ResultSet requestRs = requestPs.executeQuery();
            
            if (requestRs.next()) {
                summary.setOverallStatus(requestRs.getString("status"));
                summary.setRequestDate(formatDate(requestRs.getDate("request_date")));
                summary.setCompletionDate(formatDate(requestRs.getDate("completion_date")));
            }
            
            // Get approval counts
            String countSql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
                    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected,
                    SUM(CASE WHEN status = 'PENDING' OR status IS NULL THEN 1 ELSE 0 END) as pending
                FROM clearance_approvals 
                WHERE request_id = ?
                AND officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
            
            PreparedStatement countPs = conn.prepareStatement(countSql);
            countPs.setInt(1, requestId);
            ResultSet countRs = countPs.executeQuery();
            
            if (countRs.next()) {
                summary.setTotalDepartments(countRs.getInt("total"));
                summary.setApprovedCount(countRs.getInt("approved"));
                summary.setRejectedCount(countRs.getInt("rejected"));
                summary.setPendingCount(countRs.getInt("pending"));
            }
            
        } catch (Exception e) {
            System.err.println("DB Error (getApprovalSummary): " + e.getMessage());
        }
        return summary;
    }
    
    private List<ApprovalDetail> getApprovalDetails(int requestId) {
        List<ApprovalDetail> details = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    ca.officer_role,
                    ca.status,
                    u.full_name as officer_name,
                    ca.approval_date,
                    ca.remarks
                FROM clearance_approvals ca
                LEFT JOIN users u ON ca.officer_id = u.id
                WHERE ca.request_id = ?
                AND ca.officer_role IN ('LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                ORDER BY FIELD(ca.officer_role, 
                    'LIBRARIAN', 'CAFETERIA', 'DORMITORY', 'REGISTRAR', 'DEPARTMENT_HEAD')
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                details.add(new ApprovalDetail(
                    formatDepartmentName(rs.getString("officer_role")),
                    rs.getString("status"),
                    rs.getString("officer_name") != null ? rs.getString("officer_name") : "Not Assigned",
                    formatDate(rs.getDate("approval_date")),
                    rs.getString("remarks") != null ? rs.getString("remarks") : "No remarks"
                ));
            }
            
        } catch (Exception e) {
            System.err.println("DB Error (getApprovalDetails): " + e.getMessage());
        }
        return details;
    }
    
    private String formatDate(Date date) {
        if (date == null) return "N/A";
        return date.toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    private String formatDepartmentName(String role) {
        return switch (role) {
            case "LIBRARIAN" -> "Library Department";
            case "CAFETERIA" -> "Cafeteria Office";
            case "DORMITORY" -> "Dormitory Office";
            case "REGISTRAR" -> "Registrar Office";
            case "DEPARTMENT_HEAD" -> "Department Head";
            default -> role;
        };
    }
}