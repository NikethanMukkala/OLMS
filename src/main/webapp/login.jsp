<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OLMS | Login</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <script src="js/theme.js"></script>
    <script src="js/auth-animations.js"></script>
</head>
<body>

    <!-- Gradient mesh layer -->
    <div class="auth-bg-mesh"></div>

    <!-- Floating books & sparkle canvas -->
    <div class="books-bg-canvas" id="books-bg"></div>

    <div class="login-wrapper">
        <div class="quotes-section animate-section">
            <div id="quote-text" class="quotes-text">"A room without books is like a body without a soul."</div>
            <div id="quote-author" class="quotes-author">Cicero</div>
        </div>

        <div class="login-form-container card animate-section">
            <h2>Welcome Back</h2>
            <p>Sign in to access the library</p>
            
            <% String error = (String) request.getAttribute("error");
               if (error != null) { %>
                <div class="alert alert-danger"><%= error %></div>
            <% } %>
            <% String success = (String) request.getSession().getAttribute("success");
               if (success != null) { 
                   request.getSession().removeAttribute("success");
            %>
                <div class="alert alert-success"><%= success %></div>
            <% } %>

            <div class="tabs">
                <button class="tab-btn active" data-tab="student" type="button">Student</button>
                <button class="tab-btn" data-tab="librarian" type="button">Librarian</button>
            </div>

            <form id="student-form" class="tab-content active" action="<%= request.getContextPath() %>/login" method="POST">
                <input type="hidden" name="role" value="STUDENT">
                <div class="form-group">
                    <label class="form-label">Roll No</label>
                    <input type="text" class="form-control" name="roll_no" required placeholder="Enter your registered Roll Number">
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" class="form-control" name="password" required placeholder="Enter your password">
                </div>
                <button type="submit" class="btn btn-primary w-full mb-4">Login</button>
                <div class="text-center mt-3">
                    <a href="signup.jsp" class="signup-link">Don't have an account? Sign up</a>
                </div>
            </form>

            <form id="librarian-form" class="tab-content" action="<%= request.getContextPath() %>/login" method="POST">
                <input type="hidden" name="role" value="LIBRARIAN">
                <div class="form-group">
                    <label class="form-label">Librarian ID</label>
                    <input type="text" class="form-control" name="login_name" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" class="form-control" name="password" required>
                </div>
                <button type="submit" class="btn btn-primary w-full">Login</button>
            </form>
        </div>
    </div>
</body>
</html>
