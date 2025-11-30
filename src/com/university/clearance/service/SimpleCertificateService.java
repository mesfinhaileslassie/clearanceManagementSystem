package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleCertificateService {

    public String generateClearanceCertificate(int studentId, int requestId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get student and clearance details
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
                
                // Generate text certificate
                return createTextCertificate(fullName, studentIdStr, department, yearLevel, 
                                           requestDate, completionDate, totalApprovals);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createTextCertificate(String fullName, String studentId, String department, 
                                       String yearLevel, Date requestDate, Date completionDate, 
                                       int totalApprovals) {
        String fileName = null;
        
        try {
            String downloadsDir = System.getProperty("user.home") + "/Downloads/";
            fileName = downloadsDir + "Clearance_Certificate_" + studentId + "_" + System.currentTimeMillis() + ".txt";
            
            FileWriter writer = new FileWriter(fileName);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            
            // Handle null dates safely
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;
            
            // Create certificate with the new structure
            StringBuilder certificate = new StringBuilder();
            certificate.append("=").append("=".repeat(70)).append("=\n");
            certificate.append("                       UNIVERSITY CLEARANCE SYSTEM\n");
            certificate.append("                     OFFICIAL CLEARANCE CERTIFICATE\n");
            certificate.append("=").append("=".repeat(70)).append("=\n\n");
            
            certificate.append("Certificate No.: ").append(certificateId).append("\n");
            certificate.append("Issue Date: ").append(currentDate).append("\n\n");
            
            certificate.append("This is to formally certify that:\n\n");
            
            certificate.append("Student Name: ").append("_".repeat(40)).append(fullName).append("\n");
            certificate.append("Student ID: ").append("_".repeat(43)).append(studentId).append("\n");
            certificate.append("Program / Department: ").append("_".repeat(32)).append(department).append("\n");
            certificate.append("Faculty / College: ").append("_".repeat(35)).append("Faculty of ").append(department).append("\n");
            certificate.append("Academic Year / Level: ").append("_".repeat(30)).append(yearLevel).append("\n\n");
            
            certificate.append("has satisfactorily fulfilled all academic, financial, and administrative\n");
            certificate.append("obligations required by the University. The student has been duly examined\n");
            certificate.append("and confirmed by the relevant university offices to have no outstanding\n");
            certificate.append("liabilities or responsibilities.\n\n");
            
            certificate.append("Accordingly, the student is hereby declared CLEARED and is eligible to\n");
            certificate.append("receive all official academic credentials from the University.\n\n");
            
            certificate.append("CLEARANCE DETAILS:\n");
            certificate.append("    Request Date: ").append(requestDateStr).append("\n");
            certificate.append("    Completion Date: ").append(completionDateStr).append("\n");
            certificate.append("    Departments Approved: ").append(totalApprovals).append("/6\n\n");
            
            certificate.append("APPROVED DEPARTMENTS:\n");
            certificate.append("    ✓ University Library\n");
            certificate.append("    ✓ Cafeteria Services\n");
            certificate.append("    ✓ Dormitory Office\n");
            certificate.append("    ✓ Student Association\n");
            certificate.append("    ✓ Registrar Office\n");
            certificate.append("    ✓ Department Head\n\n");
            
            certificate.append("OFFICIAL SIGNATURES:\n\n");
            
            certificate.append("    _________________________       _________________________\n");
            certificate.append("    University Registrar            Department Head\n");
            certificate.append("    Date: ").append(currentDate);
            certificate.append("       Date: ").append(currentDate).append("\n\n");
            
            certificate.append("    _________________________       _________________________\n");
            certificate.append("    University President           Dean of Students\n");
            certificate.append("    Date: ").append(currentDate);
            certificate.append("       Date: ").append(currentDate).append("\n\n");
            
            certificate.append("University Seal: [OFFICIAL STAMP]\n");
            certificate.append("This certificate is electronically generated and requires no physical signature.\n");
            
            certificate.append("=").append("=".repeat(70)).append("=\n");
            
            writer.write(certificate.toString());
            writer.close();
            
            return fileName;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // HTML Certificate option (can be printed as PDF from browser)
    public String generateHTMLCertificate(int studentId, int requestId) {
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
                
                return createHTMLCertificate(fullName, studentIdStr, department, yearLevel, 
                                           requestDate, completionDate, totalApprovals);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String createHTMLCertificate(String fullName, String studentId, String department, 
                                       String yearLevel, Date requestDate, Date completionDate, 
                                       int totalApprovals) {
        String fileName = null;
        
        try {
            String downloadsDir = System.getProperty("user.home") + "/Downloads/";
            fileName = downloadsDir + "Clearance_Certificate_" + studentId + "_" + System.currentTimeMillis() + ".html";
            
            FileWriter writer = new FileWriter(fileName);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            
            // Handle null dates safely
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;
            
            // Use String.format() instead of String.formatted() to avoid format specifier issues
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Official Clearance Certificate</title>
                    <style>
                        body { 
                            font-family: 'Times New Roman', serif; 
                            margin: 40px; 
                            border: 3px solid #003366;
                            padding: 30px;
                            line-height: 1.6;
                        }
                        .header { 
                            text-align: center; 
                            color: #003366;
                            border-bottom: 2px solid #003366;
                            padding-bottom: 20px;
                            margin-bottom: 30px;
                        }
                        .university { 
                            font-size: 24px; 
                            font-weight: bold; 
                            margin-bottom: 10px;
                            letter-spacing: 1px;
                        }
                        .title { 
                            font-size: 20px; 
                            color: #0066cc;
                            margin-bottom: 5px;
                        }
                        .certificate-info {
                            text-align: right;
                            margin-bottom: 30px;
                            font-size: 14px;
                        }
                        .student-info {
                            margin: 30px 0;
                            border: 1px solid #ccc;
                            padding: 20px;
                            background-color: #f9f9f9;
                        }
                        .info-line {
                            margin: 10px 0;
                            font-size: 16px;
                        }
                        .underline {
                            border-bottom: 1px solid #000;
                            display: inline-block;
                            min-width: 300px;
                            margin-left: 10px;
                        }
                        .declaration {
                            margin: 30px 0;
                            text-align: justify;
                            font-size: 16px;
                        }
                        .signature-section {
                            display: flex;
                            justify-content: space-between;
                            margin-top: 60px;
                        }
                        .signature {
                            text-align: center;
                            width: 45%%;
                        }
                        .signature-line {
                            border-top: 1px solid #000;
                            margin: 60px 0 10px 0;
                        }
                        .stamp {
                            color: red;
                            font-weight: bold;
                            text-align: center;
                            margin-top: 30px;
                            border: 2px solid red;
                            padding: 10px;
                            display: inline-block;
                            font-size: 18px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 40px;
                            font-size: 12px;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="university">UNIVERSITY CLEARANCE SYSTEM</div>
                        <div class="title">OFFICIAL CLEARANCE CERTIFICATE</div>
                    </div>
                    
                    <div class="certificate-info">
                        <strong>Certificate No.:</strong> %s<br>
                        <strong>Issue Date:</strong> %s
                    </div>
                    
                    <div style="text-align: center; margin: 20px 0; font-size: 16px;">
                        <strong>This is to formally certify that:</strong>
                    </div>
                    
                    <div class="student-info">
                        <div class="info-line">
                            <strong>Student Name:</strong> <span class="underline">%s</span>
                        </div>
                        <div class="info-line">
                            <strong>Student ID:</strong> <span class="underline">%s</span>
                        </div>
                        <div class="info-line">
                            <strong>Program / Department:</strong> <span class="underline">%s</span>
                        </div>
                        <div class="info-line">
                            <strong>Faculty / College:</strong> <span class="underline">Faculty of %s</span>
                        </div>
                        <div class="info-line">
                            <strong>Academic Year / Level:</strong> <span class="underline">%s</span>
                        </div>
                    </div>
                    
                    <div class="declaration">
                        <p>has satisfactorily fulfilled all academic, financial, and administrative obligations 
                        required by the University. The student has been duly examined and confirmed by the 
                        relevant university offices to have no outstanding liabilities or responsibilities.</p>
                        
                        <p>Accordingly, the student is hereby declared <strong>CLEARED</strong> and is eligible to 
                        receive all official academic credentials from the University.</p>
                    </div>
                    
                    <div style="margin: 25px 0; font-size: 14px;">
                        <strong>CLEARANCE DETAILS:</strong><br>
                        Request Date: %s | Completion Date: %s | Departments Approved: %d/6
                    </div>
                    
                    <div class="signature-section">
                        <div class="signature">
                            <div class="signature-line"></div>
                            <strong>University Registrar</strong><br>
                            Date: %s
                        </div>
                        <div class="signature">
                            <div class="signature-line"></div>
                            <strong>Department Head</strong><br>
                            Date: %s
                        </div>
                    </div>
                    
                    <div class="signature-section">
                        <div class="signature">
                            <div class="signature-line"></div>
                            <strong>University President</strong><br>
                            Date: %s
                        </div>
                        <div class="signature">
                            <div class="signature-line"></div>
                            <strong>Dean of Students</strong><br>
                            Date: %s
                        </div>
                    </div>
                    
                    <div style="text-align: center; margin-top: 40px;">
                        <div class="stamp">OFFICIALLY CLEARED</div>
                    </div>
                    
                    <div class="footer">
                        <strong>University Seal: [OFFICIAL STAMP]</strong><br>
                        This certificate is electronically generated and requires no physical signature.<br>
                        Verification: Contact Registrar Office | Certificate ID: %s
                    </div>
                </body>
                </html>
                """, 
                certificateId, currentDate,
                fullName, studentId, department, department, yearLevel,
                requestDateStr, completionDateStr, totalApprovals,
                currentDate, currentDate, currentDate, currentDate,
                certificateId
            );
            
            writer.write(html);
            writer.close();
            
            return fileName;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}