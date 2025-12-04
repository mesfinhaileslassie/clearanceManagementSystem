package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
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

                return createPDF(fullName, studentIdStr, department, yearLevel,
                        requestDate, completionDate, totalApprovals);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createPDF(String fullName, String studentId, String department,
                             String yearLevel, Date requestDate, Date completionDate,
                             int totalApprovals) {

        String fileName = System.getProperty("user.home") + "/Downloads/Debre_Birhan_University_Clearance_"
                + studentId + "_" + System.currentTimeMillis() + ".pdf";

        try {
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;

            // Add logo
            Image logo = getLogoImage();
            if (logo != null) {
                logo.setHeight(1);
                logo.setWidth(3);
                logo.setAutoScale(true);
                document.add(logo.setHorizontalAlignment(com.itextpdf.layout.property.HorizontalAlignment.CENTER));
            }

            // University header
            Paragraph header = new Paragraph("DEBRE BIRHAN UNIVERSITY")
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.RED);
            document.add(header);

            Paragraph motto = new Paragraph("Practical Knowledge for better success")
                    .setItalic()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY);
            document.add(motto);

//            Paragraph title = new Paragraph("OFFICIAL CLEARANCE CERTIFICATE")
//                    .setBold()
//                    .setFontSize(16)
//                    .setTextAlignment(TextAlignment.CENTER)
//                    .setMarginTop(10);
//            document.add(title);

            // Certificate info
            Paragraph certInfo = new Paragraph()
                    .add("Certificate No.: " + certificateId + "\n")
                    .add("Issue Date: " + currentDate + "\n")
                    .setMarginTop(20)
                    .setTextAlignment(TextAlignment.RIGHT);
            document.add(certInfo);

            // Student details
            Paragraph studentInfo = new Paragraph()
                    .add("This is to formally certify that:\n\n")
                    .add("Student Name: " + fullName + "\n")
                    .add("Student ID: " + studentId + "\n")
                    .add("Department: " + department + "\n")
                    .add("Faculty / College: Faculty of " + department + "\n")
                    .add("Academic Year / Level: " + yearLevel + "\n")
                    .setFontSize(12);
            document.add(studentInfo);

            // Declaration
            Paragraph declaration = new Paragraph()
                    .add("has satisfactorily fulfilled all academic, financial, and administrative obligations required by Debre Birhan University.\n")
                    .add("The student has been duly examined and confirmed by the relevant university offices to have no outstanding liabilities or responsibilities.\n\n")
                    .add("Accordingly, the student is hereby declared CLEARED and is eligible to receive all official academic credentials from Debre Birhan University.\n\n")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.JUSTIFIED);
            document.add(declaration);

            // Clearance details
            Paragraph clearanceDetails = new Paragraph()
                    .add("CLEARANCE DETAILS:\n")
                    .add("Request Date: " + requestDateStr + " \n Completion Date: " + completionDateStr
                            + " \n| This clearance certificate is approved by : ")
                    .setFontSize(12);
                    //.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            document.add(clearanceDetails);

            // Signatures
            Paragraph signatures = new Paragraph()
                    .add("University Registrar: ____________________  Date: " + currentDate + "\n")
                    .add("Department Head: ______________________  Date: " + currentDate + "\n")
                    .add("Dormitory: _______________  Date: " + currentDate + "\n")
                    .add("Library: ______________________  Date: " + currentDate + "\n")
                    .add("Cafeteria: ______________________  Date: " + currentDate + "\n");
            document.add(signatures);

//            // Official stamp
//            Paragraph stamp = new Paragraph("DEBRE BIRHAN UNIVERSITY - OFFICIALLY CLEARED\nCertificate ID: " + certificateId + "\nDate: " + currentDate)
//                    .setBold()
//                    .setTextAlignment(TextAlignment.CENTER)
//                    .setFontColor(ColorConstants.RED);
//            document.add(stamp);

            document.close();
            return fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Load logo image
    private Image getLogoImage() {
        try {
            // Use the same path as in your HTML
            String[] possiblePaths = {
                    "/com/university/clearance/resources/images/dbu_logo.png",
                    "resources/images/dbu_logo.png"
            };

            for (String path : possiblePaths) {
                InputStream logoStream = getClass().getResourceAsStream(path);
                if (logoStream != null) {
                    byte[] bytes = logoStream.readAllBytes();
                    ImageData imageData = ImageDataFactory.create(bytes);
                    return new Image(imageData);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading logo: " + e.getMessage());
        }
        return null;
    }
}
