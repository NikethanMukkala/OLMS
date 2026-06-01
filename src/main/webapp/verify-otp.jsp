<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OLMS | Verify OTP</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="js/theme.js"></script>
</head>
<body>
    <div class="orbs-container"> 
        <div class="orb orb-1"></div> 
        <div class="orb orb-2"></div> 
    </div>

    <div class="login-wrapper" style="justify-content: center;">
        <div class="login-form-container card" style="text-align: center;">
            <h2>OTP Verification</h2>
            <p>An OTP has been sent to your email. Enter it below.</p>
            <p style="font-size: 0.8rem; color: var(--primary-color);">* Check server console if Gmail credentials aren't configured yet</p>
            
            <% String error = (String) request.getAttribute("error");
               if (error != null) { %>
                <div style="color: var(--danger-color); background: rgba(239, 68, 68, 0.1); padding: 10px; border-radius: 8px; margin-bottom: 15px; border: 1px solid var(--danger-color);"><%= utils.Sanitize.html(error) %></div>
            <% } %>

            <% String debugOtp = (String) session.getAttribute("debug_otp");
               if (debugOtp != null) { %>
                <div style="background: rgba(16, 185, 129, 0.1); padding: 10px; border-radius: 8px; margin-bottom: 15px; border: 1px solid var(--secondary-color); color: var(--secondary-color);">
                    <strong>[Dev Mode] Generated OTP:</strong> <span style="font-size: 1.2rem; letter-spacing: 2px;"><%= utils.Sanitize.html(debugOtp) %></span>
                </div>
            <% } %>

            <form action="<%= request.getContextPath() %>/verify-otp" method="POST" style="margin-top: 20px;">
                <div class="form-group">
                    <input type="text" class="form-control" name="otp" required maxlength="6" style="text-align: center; font-size: 2rem; letter-spacing: 12px; padding: 15px;">
                </div>
                <button type="submit" class="btn btn-primary" style="margin-top: 10px;">Verify & Login</button>
            </form>
            
            <div style="margin-top: 20px;">
                <a href="login.jsp" style="color: var(--text-secondary); text-decoration: underline;">Cancel and return to login</a>
            </div>
        </div>
    </div>
</body>
</html>
