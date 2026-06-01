<%@ page import="utils.DBConnection" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="dao.SettingsDAO" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OLMS | Test Database Connection</title>
    <!-- Use Google Fonts for Typography -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="js/theme.js"></script>
</head>
<body>
    <nav class="navbar">
        <a href="index.jsp" class="navbar-brand">📚 OLMS</a>
        <div class="nav-links">
            <a href="jsp/student/dashboard.jsp">Student Portal</a>
            <a href="jsp/librarian/dashboard.jsp">Librarian Portal</a>
        </div>
    </nav>
    
    <div class="container">
        <div class="card" style="max-width: 600px; margin: 40px auto; text-align: center;">
            <h2>System Status</h2>
            <p>Testing connection to MySQL 8.0 Database (OLMS)...</p>
            <%
                boolean status = false;
                int bookCount = 0;
                try (Connection conn = DBConnection.getConnection()) {
                    if (conn != null) {
                        status = true;
                        // Check if books exist
                        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM books");
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            bookCount = rs.getInt(1);
                        }
                    }
                } catch (Exception e) {
                    out.println("<p style='color: var(--danger-color);'>" + e.getMessage() + "</p>");
                }
                
                String limitStr = new SettingsDAO().getSetting("library_limit");
                int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 50;
            %>
            
            <% if(status) { %>
                <div style="padding: 20px; background: rgba(16, 185, 129, 0.1); border: 1px solid var(--secondary-color); border-radius: 8px; margin-top: 20px;">
                    <h3 style="color: var(--secondary-color); margin-bottom: 10px;">✅ Connection Successful</h3>
                    <p>Database connected via HikariCP Pool.</p>
                    <p>Total Books found in schema: <strong><%= bookCount %></strong> / <%= limit %></p>
                </div>
            <% } else { %>
                <div style="padding: 20px; background: rgba(239, 68, 68, 0.1); border: 1px solid var(--danger-color); border-radius: 8px; margin-top: 20px;">
                    <h3 style="color: var(--danger-color); margin-bottom: 10px;">❌ Connection Failed</h3>
                    <p>Could not connect to the database. Ensure MySQL is running on port 3306 and olms schema exists.</p>
                </div>
            <% } %>
            
            <div style="margin-top: 30px;">
                <a href="index.jsp" class="btn btn-primary">Refresh Status</a>
            </div>
        </div>
    </div>
</body>
</html>
