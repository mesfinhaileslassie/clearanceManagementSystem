package com.university.clearance.service;

import com.university.clearance.DatabaseConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
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
            fileName = downloadsDir + "Debre_Birhan_University_Clearance_" + studentId + "_" + System.currentTimeMillis() + ".txt";
            
            FileWriter writer = new FileWriter(fileName);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            
            // Handle null dates safely
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;
            
            // Create certificate with Debre Birhan University branding
            StringBuilder certificate = new StringBuilder();
            certificate.append("=").append("=".repeat(70)).append("=\n");
            certificate.append("                   DEBRE BIRHAN UNIVERSITY\n");
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
            certificate.append("obligations required by Debre Birhan University. The student has been duly\n");
            certificate.append("examined and confirmed by the relevant university offices to have no outstanding\n");
            certificate.append("liabilities or responsibilities.\n\n");
            
            certificate.append("Accordingly, the student is hereby declared CLEARED and is eligible to\n");
            certificate.append("receive all official academic credentials from Debre Birhan University.\n\n");
            
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
            
            certificate.append("DEBRE BIRHAN UNIVERSITY OFFICIAL STAMP\n");
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
    
    // HTML Certificate with embedded logo
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
            fileName = downloadsDir + "Debre_Birhan_University_Clearance_" + studentId + "_" + System.currentTimeMillis() + ".html";
            
            FileWriter writer = new FileWriter(fileName);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
            String certificateId = "DBU-CLR-" + System.currentTimeMillis();
            String currentDate = dateFormat.format(new Date());
            
            // Handle null dates safely
            String requestDateStr = (requestDate != null) ? dateFormat.format(requestDate) : "Not Available";
            String completionDateStr = (completionDate != null) ? dateFormat.format(completionDate) : currentDate;
            
            // Get logo as base64
            String logoBase64 = getLogoAsBase64();
            boolean hasLogo = logoBase64 != null;
            
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Debre Birhan University - Clearance Certificate</title>
                    <style>
                        body { 
                            font-family: 'Times New Roman', serif; 
                            margin: 40px; 
                            border: 3px solid #003366;
                            padding: 30px;
                            line-height: 1.6;
                            background-color: #ffffff;
                        }
                        .header { 
                            text-align: center; 
                            color: #003366;
                            border-bottom: 2px solid #CC0000;
                            padding-bottom: 20px;
                            margin-bottom: 30px;
                            position: relative;
                        }
                        .logo-container {
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            margin-bottom: 15px;
                        }
                        .logo {
                            width: 80px;
                            height: 80px;
                            margin: 0 20px;
                            object-fit: contain;
                        }
                        .placeholder-logo {
                            width: 80px;
                            height: 80px;
                            background: #CC0000;
                            color: white;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-weight: bold;
                            border-radius: 50%%;
                            margin: 0 20px;
                            font-size: 16px;
                        }
                        .university { 
                            font-size: 24px; 
                            font-weight: bold; 
                            margin-bottom: 5px;
                            letter-spacing: 1px;
                            color: #CC0000;
                        }
                        .motto {
                            font-size: 14px;
                            font-style: italic;
                            color: #666666;
                            margin-bottom: 10px;
                        }
                        .title { 
                            font-size: 20px; 
                            color: #003366;
                            margin-bottom: 5px;
                            font-weight: bold;
                        }
                        .certificate-info {
                            text-align: right;
                            margin-bottom: 30px;
                            font-size: 14px;
                            border-bottom: 1px solid #eeeeee;
                            padding-bottom: 10px;
                        }
                        .student-info {
                            margin: 30px 0;
                            border: 1px solid #cccccc;
                            padding: 20px;
                            background-color: #f9f9f9;
                            border-left: 4px solid #CC0000;
                        }
                        .info-line {
                            margin: 10px 0;
                            font-size: 16px;
                        }
                        .underline {
                            border-bottom: 1px solid #000000;
                            display: inline-block;
                            min-width: 300px;
                            margin-left: 10px;
                            padding-bottom: 2px;
                        }
                        .declaration {
                            margin: 30px 0;
                            text-align: justify;
                            font-size: 16px;
                            line-height: 1.8;
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
                            border-top: 1px solid #000000;
                            margin: 60px 0 10px 0;
                        }
                        .official-stamp {
                            text-align: center;
                            margin: 40px 0;
                            padding: 20px;
                            border: 3px double #CC0000;
                            display: inline-block;
                            background-color: #fffafa;
                        }
                        .stamp-text {
                            color: #CC0000;
                            font-weight: bold;
                            font-size: 18px;
                            margin: 5px 0;
                        }
                        .stamp-subtext {
                            color: #003366;
                            font-size: 12px;
                            margin: 3px 0;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 40px;
                            font-size: 12px;
                            color: #666666;
                            border-top: 1px solid #eeeeee;
                            padding-top: 15px;
                        }
                        .university-address {
                            font-size: 11px;
                            color: #888888;
                            margin-top: 5px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="logo-container">
                """);
            
            // Add logo or placeholder
            if (hasLogo) {
                html += String.format("""
                            <img src="data:image/png;base64,%s" alt="DBU Logo" class="logo">
                    """, logoBase64);
            } else {
                html += """
                            <div class="placeholder-logo">DBU</div>
                """;
            }
            
            html += String.format("""
                        </div>
                        <div class="university">DEBRE BIRHAN UNIVERSITY</div>
                        <div class="motto">"Quality Education for Transformation"</div>
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
                        required by Debre Birhan University. The student has been duly examined and confirmed by the 
                        relevant university offices to have no outstanding liabilities or responsibilities.</p>
                        
                        <p>Accordingly, the student is hereby declared <strong style="color: #CC0000;">CLEARED</strong> and is eligible to 
                        receive all official academic credentials from Debre Birhan University.</p>
                    </div>
                    
                    <div style="margin: 25px 0; font-size: 14px; background: #f0f8ff; padding: 15px; border-left: 4px solid #003366;">
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
                            <strong>Academic Vice President</strong><br>
                            Date: %s
                        </div>
                        <div class="signature">
                            <div class="signature-line"></div>
                            <strong>Dean of Students</strong><br>
                            Date: %s
                        </div>
                    </div>
                    
                    <div style="text-align: center; margin-top: 40px;">
                        <div class="official-stamp">
                            <div class="stamp-text">DEBRE BIRHAN UNIVERSITY</div>
                            <div class="stamp-subtext">OFFICIALLY CLEARED</div>
                            <div class="stamp-subtext">Certificate ID: %s</div>
                            <div class="stamp-subtext">Date: %s</div>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <strong>This certificate is electronically generated and requires no physical signature</strong><br>
                        <div class="university-address">
                            Debre Birhan University | P.O. Box 445 | Debre Birhan, Ethiopia<br>
                            Tel: +251 (0)22 111 0000 | Email: registrar@dbu.edu.et | Website: www.dbu.edu.et
                        </div>
                    </div>
                </body>
                </html>
                """, 
                certificateId, currentDate,
                fullName, studentId, department, department, yearLevel,
                requestDateStr, completionDateStr, totalApprovals,
                currentDate, currentDate, currentDate, currentDate,
                certificateId, currentDate
            );
            
            writer.write(html);
            writer.close();
            
            return fileName;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
  
    // Method to get logo as base64 string
     private String getLogoAsBase64() {
        try {
            // Try multiple possible paths
            String[] possiblePaths = {
                "/images/dbu_logo.png",
                "/dbu_logo.png", 
                "images/dbu_logo.png",
                "dbu_logo.png"
            };
            
            System.out.println("=== LOGO DEBUG - CHECKING ALL PATHS ===");
            
            for (String path : possiblePaths) {
            	InputStream logoStream = getClass().getResourceAsStream("/com/university/clearance/resources/images/dbu_logo.png");
                System.out.println("Trying path: '" + path + "' -> " + (logoStream != null ? "✅ FOUND" : "❌ NOT FOUND"));
                
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    System.out.println("✅ Logo found at: " + path);
                    System.out.println("✅ Logo size: " + logoBytes.length + " bytes");
                    System.out.println("==================");
                    return Base64.getEncoder().encodeToString(logoBytes);
                }
            }
            
            System.out.println("❌ Logo not found in any path!");
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            
            // List all resources in images folder
            try {
                InputStream testStream = getClass().getResourceAsStream("/images/");
                System.out.println("Images folder stream: " + testStream);
            } catch (Exception e) {
                System.out.println("Cannot access images folder");
            }
            
            System.out.println("==================");
            
        } catch (Exception e) {
            System.out.println("❌ Error reading logo: " + e.getMessage());
            System.out.println("==================");
        }
        return null;
    }
}