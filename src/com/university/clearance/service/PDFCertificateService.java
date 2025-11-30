package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFCertificateService {

    public String generatePDFCertificate(int studentId, int requestId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT 
                    u.full_name,
                    u.username as student_id,
                    u.department,
                    u.year_level,
                    cr.request_date,
                    cr.completion_date,
                    COUNT(ca.id) as total_approvals
                FROM users u
                JOIN clearance_requests cr ON u.id = cr.student_id
                LEFT JOIN clearance_approvals ca ON cr.id = ca.request_id AND ca.status = 'APPROVED'
                WHERE u.id = ? AND cr.id = ? AND cr.status = 'FULLY_CLEARED'
                GROUP BY u.id, cr.id
                """;
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, requestId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String studentIdStr = rs.getString("student_id");
                String department = rs.getString("department");
                String yearLevel = rs.getString("year_level");
                Date requestDate = rs.getDate("request_date");
                Date completionDate = rs.getDate("completion_date");
                int totalApprovals = rs.getInt("total_approvals");
                
                return createPDFCertificate(fullName, studentIdStr, department, yearLevel, 
                                          requestDate, completionDate, totalApprovals);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createPDFCertificate(String fullName, String studentId, String department, 
                                      String yearLevel, Date requestDate, Date completionDate, 
                                      int totalApprovals) {
        String fileName = null;
        PDDocument document = new PDDocument();
        
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            // Set up fonts with smaller sizes
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            
            // Add border first
            addCertificateBorder(contentStream, pageWidth, pageHeight);
            
            // Add university header
            addUniversityHeader(document, contentStream, pageWidth, pageHeight, titleFont);
            
            // Add certificate title
            contentStream.setFont(titleFont, 16);
            contentStream.setNonStrokingColor(new Color(0, 51, 102));
            String title = "OFFICIAL CLEARANCE CERTIFICATE";
            float titleWidth = titleFont.getStringWidth(title) / 1000 * 16;
            contentStream.beginText();
            contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 150);
            contentStream.showText(title);
            contentStream.endText();
            
            // Certificate information
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            
            contentStream.setFont(normalFont, 9);
            contentStream.setNonStrokingColor(Color.DARK_GRAY);
            
            float startY = pageHeight - 180;
            float lineHeight = 14;
            
            // Certificate number and date
            contentStream.beginText();
            contentStream.newLineAtOffset(50, startY);
            contentStream.showText("Certificate No: " + certificateId);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(pageWidth - 200, startY);
            contentStream.showText("Issue Date: " + currentDate);
            contentStream.endText();
            
            startY -= lineHeight * 2;
            
            // Certification text
            contentStream.setFont(normalFont, 10);
            contentStream.setNonStrokingColor(Color.BLACK);
            addCenteredText(contentStream, "This is to formally certify that:", normalFont, 10, pageWidth, startY);
            startY -= lineHeight * 1.5f;
            
            // Student name
            contentStream.setFont(headerFont, 14);
            contentStream.setNonStrokingColor(new Color(204, 0, 0));
            addCenteredText(contentStream, fullName.toUpperCase(), headerFont, 14, pageWidth, startY);
            startY -= lineHeight * 2;
            
            // Student details
            contentStream.setFont(normalFont, 10);
            contentStream.setNonStrokingColor(Color.BLACK);
            
            float detailsX = 80;
            contentStream.beginText();
            contentStream.newLineAtOffset(detailsX, startY);
            contentStream.showText("Student ID: " + studentId);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(detailsX, startY - lineHeight);
            contentStream.showText("Program / Department: " + department);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(detailsX, startY - lineHeight * 2);
            contentStream.showText("Faculty / College: Faculty of " + department);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(detailsX, startY - lineHeight * 3);
            contentStream.showText("Academic Year / Level: " + yearLevel);
            contentStream.endText();
            
            startY -= lineHeight * 5;
            
            // Declaration text
            contentStream.setFont(normalFont, 10);
            String[] declarationLines = {
                "has satisfactorily fulfilled all academic, financial, and administrative obligations",
                "required by Debre Birhan University. The student has been duly examined and confirmed",
                "by the relevant university offices to have no outstanding liabilities or responsibilities.",
                "",
                "Accordingly, the student is hereby declared CLEARED and is eligible to receive all",
                "official academic credentials from the University."
            };
            
            for (String line : declarationLines) {
                if (!line.isEmpty()) {
                    addCenteredText(contentStream, line, normalFont, 10, pageWidth, startY);
                }
                startY -= lineHeight;
            }
            
            startY -= lineHeight * 0.5f;
            
            // Clearance details
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;
            
            contentStream.setFont(boldFont, 9);
            contentStream.setNonStrokingColor(new Color(0, 51, 102));
            String details = String.format("Request Date: %s | Completion Date: %s | Departments Approved: %d/6", 
                                         requestDateStr, completionDateStr, totalApprovals);
            addCenteredText(contentStream, details, boldFont, 9, pageWidth, startY);
            
            startY -= lineHeight * 2;
            
            // Approved departments
            contentStream.setFont(boldFont, 11);
            contentStream.setNonStrokingColor(new Color(0, 51, 102));
            addCenteredText(contentStream, "APPROVED DEPARTMENTS", boldFont, 11, pageWidth, startY);
            startY -= lineHeight;
            
            // Use ASCII checkboxes instead of Unicode checkmarks
            String[] departments = {
                "[X] University Library",
                "[X] Cafeteria Services", 
                "[X] Dormitory Office",
                "[X] Student Association",
                "[X] Registrar Office",
                "[X] Department Head"
            };
            
            // Two columns for departments
            float col1X = pageWidth / 2 - 100;
            float col2X = pageWidth / 2 + 20;
            float deptY = startY;
            
            contentStream.setFont(normalFont, 9);
            contentStream.setNonStrokingColor(Color.BLACK);
            
            for (int i = 0; i < departments.length; i++) {
                if (i < 3) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(col1X, deptY);
                    contentStream.showText(departments[i]);
                    contentStream.endText();
                } else {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(col2X, deptY);
                    contentStream.showText(departments[i]);
                    contentStream.endText();
                }
                deptY -= lineHeight;
            }
            
            // Add signatures section
            addSignaturesSection(contentStream, pageWidth, pageHeight, normalFont, boldFont);
            
            // Add footer
            addFooter(contentStream, pageWidth, pageHeight, normalFont);
            
            contentStream.close();
            
            // Save the document
            String downloadsDir = System.getProperty("user.home") + "/Downloads/";
            fileName = downloadsDir + "DBU_Clearance_Certificate_" + studentId + "_" + System.currentTimeMillis() + ".pdf";
            document.save(fileName);
            
            System.out.println("✅ PDF certificate generated: " + fileName);
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return fileName;
    }

    private void addUniversityHeader(PDDocument document, PDPageContentStream contentStream, float pageWidth, float pageHeight, PDType1Font font) throws IOException {
        float headerY = pageHeight - 70;
        
        // Try to add logo
        try {
            InputStream logoStream = getClass().getResourceAsStream("/com/university/clearance/resources/images/dbu_logo.png");
            if (logoStream != null) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoStream.readAllBytes(), "DBU Logo");
                float logoWidth = 50;
                float logoHeight = 50;
                contentStream.drawImage(logo, 50, headerY - 10, logoWidth, logoHeight);
            }
        } catch (Exception e) {
            System.out.println("Logo not available for PDF: " + e.getMessage());
        }
        
        // University name
        contentStream.setFont(font, 16);
        contentStream.setNonStrokingColor(new Color(204, 0, 0));
        String universityName = "DEBRE BIRHAN UNIVERSITY";
        float uniWidth = font.getStringWidth(universityName) / 1000 * 16;
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - uniWidth) / 2, headerY);
        contentStream.showText(universityName);
        contentStream.endText();
        
        // University motto
        contentStream.setFont(font, 9);
        contentStream.setNonStrokingColor(Color.GRAY);
        String motto = "\"Quality Education for Transformation\"";
        float mottoWidth = font.getStringWidth(motto) / 1000 * 9;
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - mottoWidth) / 2, headerY - 12);
        contentStream.showText(motto);
        contentStream.endText();
        
        // University address
        contentStream.setFont(font, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        String address = "P.O. Box 445, Debre Birhan, Ethiopia • Tel: +251-22-111-0000 • www.dbu.edu.et";
        float addressWidth = font.getStringWidth(address) / 1000 * 7;
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - addressWidth) / 2, headerY - 24);
        contentStream.showText(address);
        contentStream.endText();
        
        // Header line
        contentStream.setStrokingColor(new Color(204, 0, 0));
        contentStream.setLineWidth(1);
        contentStream.moveTo(50, headerY - 35);
        contentStream.lineTo(pageWidth - 50, headerY - 35);
        contentStream.stroke();
    }

    private void addCenteredText(PDPageContentStream contentStream, String text, PDType1Font font, 
                               float fontSize, float pageWidth, float y) throws IOException {
        contentStream.setFont(font, fontSize);
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - textWidth) / 2, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private void addSignaturesSection(PDPageContentStream contentStream, float pageWidth, float pageHeight, 
                                    PDType1Font normalFont, PDType1Font boldFont) throws IOException {
        float signatureY = 150;
        
        // Signature section title
        contentStream.setFont(boldFont, 9);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        addCenteredText(contentStream, "AUTHORIZED SIGNATURES", boldFont, 9, pageWidth, signatureY + 30);
        
        float col1X = 80;
        float col2X = pageWidth - 200;
        
        // First row
        contentStream.setFont(boldFont, 8);
        contentStream.setNonStrokingColor(Color.BLACK);
        
        // Registrar
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY);
        contentStream.showText("_________________________");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY - 10);
        contentStream.showText("University Registrar");
        contentStream.endText();
        
        contentStream.setFont(normalFont, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY - 20);
        contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        contentStream.endText();
        
        // Department Head
        contentStream.setFont(boldFont, 8);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY);
        contentStream.showText("_________________________");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY - 10);
        contentStream.showText("Department Head");
        contentStream.endText();
        
        contentStream.setFont(normalFont, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY - 20);
        contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        contentStream.endText();
        
        // Second row
        signatureY -= 45;
        
        // Academic VP
        contentStream.setFont(boldFont, 8);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY);
        contentStream.showText("_________________________");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY - 10);
        contentStream.showText("Academic Vice President");
        contentStream.endText();
        
        contentStream.setFont(normalFont, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        contentStream.beginText();
        contentStream.newLineAtOffset(col1X, signatureY - 20);
        contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        contentStream.endText();
        
        // Dean of Students
        contentStream.setFont(boldFont, 8);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY);
        contentStream.showText("_________________________");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY - 10);
        contentStream.showText("Dean of Students");
        contentStream.endText();
        
        contentStream.setFont(normalFont, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        contentStream.beginText();
        contentStream.newLineAtOffset(col2X, signatureY - 20);
        contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        contentStream.endText();
    }

    private void addFooter(PDPageContentStream contentStream, float pageWidth, float pageHeight, PDType1Font font) throws IOException {
        float footerY = 40;
        
        // Footer line
        contentStream.setStrokingColor(new Color(204, 0, 0));
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(50, footerY + 12);
        contentStream.lineTo(pageWidth - 50, footerY + 12);
        contentStream.stroke();
        
        // Footer text
        contentStream.setFont(font, 7);
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        
        String footerText1 = "This is an electronically generated document. No physical signature is required.";
        String footerText2 = "For verification, contact: registrar@dbu.edu.et | Tel: +251-22-111-0000";
        
        float text1Width = font.getStringWidth(footerText1) / 1000 * 7;
        float text2Width = font.getStringWidth(footerText2) / 1000 * 7;
        
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - text1Width) / 2, footerY);
        contentStream.showText(footerText1);
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - text2Width) / 2, footerY - 8);
        contentStream.showText(footerText2);
        contentStream.endText();
    }

    private void addCertificateBorder(PDPageContentStream contentStream, float pageWidth, float pageHeight) throws IOException {
        // Outer border
        contentStream.setStrokingColor(new Color(0, 51, 102));
        contentStream.setLineWidth(1.5f);
        contentStream.addRect(30, 30, pageWidth - 60, pageHeight - 60);
        contentStream.stroke();
        
        // Inner border
        contentStream.setStrokingColor(new Color(204, 0, 0));
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(35, 35, pageWidth - 70, pageHeight - 70);
        contentStream.stroke();
    }
}