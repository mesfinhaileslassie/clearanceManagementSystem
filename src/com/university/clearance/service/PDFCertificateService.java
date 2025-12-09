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

    public String generatePDFCertificate(int studentId, int requestId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.full_name, u.username, u.department, u.year_level,
                       u.email, u.phone, cr.request_date, cr.completion_date
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                WHERE u.id = ? AND cr.id = ? AND cr.status = 'FULLY_CLEARED'
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

                return createCertificateWithLogo(fullName, studentIdStr, department, 
                                                yearLevel, email, phone, 
                                                requestDate, completionDate);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createCertificateWithLogo(String fullName, String studentId, String department,
                                           String yearLevel, String email, String phone,
                                           Date requestDate, Date completionDate) {

        String fileName = System.getProperty("user.home") + "/Downloads/DBU_Clearance_Certificate_" 
                        + studentId + "_" + System.currentTimeMillis() + ".pdf";

        try {
            // Create PDF document
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            
            // Set margins
            document.setMargins(30, 30, 30, 30);

            // Format dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String currentDate = dateFormat.format(new Date());
            String requestDateStr = dateFormat.format(requestDate);
            String completionDateStr = dateFormat.format(completionDate);
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();

            // ==================== MAIN CERTIFICATE DESIGN ====================
            
            // 1. Create main container with elegant border
            Div mainContainer = new Div();
            mainContainer.setBorder(new SolidBorder(UNIVERSITY_BLUE, 3));
            mainContainer.setBackgroundColor(LIGHT_BLUE);
            mainContainer.setPadding(25);
            
            // 2. HEADER SECTION WITH LOGO
            Table headerTable = new Table(3); // 3 columns for logo, title, date
            
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
                // Fallback if logo not found
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
            
//            Paragraph uniName = new Paragraph("DEBRE BIRHAN UNIVERSITY")
//                    .setBold()
//                    .setFontSize(24)
//                    .setFontColor(UNIVERSITY_RED)
//                    .setTextAlignment(TextAlignment.CENTER);
//            
            Paragraph uniMotto = new Paragraph("Practical knowledge for better success !")
                    .setItalic()
                    .setFontSize(14)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER);
            
//            Paragraph uniAddress = new Paragraph("P.O. Box 445, Debre Birhan, Ethiopia")
//                    .setFontSize(10)
//                    .setFontColor(ColorConstants.GRAY)
//                    .setTextAlignment(TextAlignment.CENTER);
//            
//            uniInfoCell.add(uniName);
//            uniInfoCell.add(uniMotto);
//            uniInfoCell.add(uniAddress);
//            headerTable.addCell(uniInfoCell);
            
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
            
            // 5. STUDENT INFORMATION - Elegant box
            Div studentInfoBox = new Div();
            studentInfoBox.setBorder(new SolidBorder(ACCENT_GOLD, 2));
            studentInfoBox.setBackgroundColor(LIGHT_GOLD);
            studentInfoBox.setPadding(25);
            studentInfoBox.setMarginBottom(30);
            
            // Student name (highlighted)
            Paragraph studentName = new Paragraph("Name:"+fullName.toUpperCase())
                    .setBold()
                    .setFontSize(20)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(10);
            studentInfoBox.add(studentName);
            
            // Student details in a neat layout
            float[] detailWidths = {40, 60}; // Two columns
            Table detailsTable = new Table(detailWidths);
            
            addDetailRow(detailsTable, "Student ID:", studentId);
            addDetailRow(detailsTable, "Department:", department);
            addDetailRow(detailsTable, "Academic Level:", yearLevel);
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
            
            // 7. CLEARANCE TIMELINE - Professional table
            Paragraph timelineTitle = new Paragraph("CLEARANCE TIMELINE")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(UNIVERSITY_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            mainContainer.add(timelineTitle);
            
            // Create timeline table
            float[] timelineWidths = {1, 1, 1}; // Three columns
            Table timelineTable = new Table(timelineWidths);
            
            // Header row
            Cell headerCell1 = createTableHeader("REQUEST DATE");
            Cell headerCell2 = createTableHeader("PROCESSING");
            Cell headerCell3 = createTableHeader("COMPLETION DATE");
            
            timelineTable.addCell(headerCell1);
            timelineTable.addCell(headerCell2);
            timelineTable.addCell(headerCell3);
            
            // Data row
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
            
            // Create signatures in a row
            float[] sigWidths = {1, 1, 1,}; // Three equal columns
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
            
            // Add main container to document
            document.add(mainContainer);
            
            // Close document
            document.close();
            
            System.out.println("✅ Professional certificate generated: " + fileName);
            return fileName;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error creating certificate: " + e.getMessage());
            return null;
        }
    }
    
    private Image loadUniversityLogo() {
        try {
            // Try different possible paths for the logo
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
            System.out.println("ℹ️ Logo not found, using text placeholder");
            return null;
            
        } catch (Exception e) {
            System.out.println("⚠️ Could not load logo: " + e.getMessage());
            return null;
        }
    }
    
    private void addDetailRow(Table table, String label, String value) {
        // Label cell
        Cell labelCell = new Cell();
        labelCell.add(new Paragraph(label).setBold());
        labelCell.setBorder(Border.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);
        
        // Value cell
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
        
        // Title
        cell.add(new Paragraph(title)
                .setFontSize(11)
                .setFontColor(UNIVERSITY_BLUE));
        
        // Signature line with space
        cell.add(new Paragraph("\n\n________________________")
                .setFontSize(10));
        
        // Date
        cell.add(new Paragraph("\nDate: " + date)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY));
        
        // Decorative dot
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
            // Create a simple verification code
            String base = studentId + "-" + certificateId.substring(certificateId.length() - 6);
            int hash = base.hashCode();
            return "DBU" + Math.abs(hash) % 10000;
        } catch (Exception e) {
            return "DBU" + System.currentTimeMillis() % 10000;
        }
    }
}