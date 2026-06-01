<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OLMS | Signup</title>
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
            <div id="quote-text" class="quotes-text">"A reader lives a thousand lives before he dies."</div>
            <div id="quote-author" class="quotes-author">George R.R. Martin</div>
        </div>

        <div class="login-form-container card animate-section">
            <h2>Create Account</h2>
            <p>Sign up as a student to access the library</p>
            
            <% 
               String error = (String) request.getAttribute("error");
               if (error == null) error = request.getParameter("error");
               if (error != null) { %>
                <div class="alert alert-danger"><%= utils.Sanitize.html(error) %></div>
            <% } %>

            <form id="student-signup-form" class="form-container" action="<%= request.getContextPath() %>/signup" method="POST" onsubmit="return validateMobile()">
                <div class="form-group">
                    <label class="form-label">Full Name</label>
                    <input type="text" class="form-control" name="login_name" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Roll No</label>
                    <input type="text" class="form-control" name="roll_no" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Email Address</label>
                    <input type="email" class="form-control" name="email" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Mobile Number</label>
                    <input type="tel" class="form-control" id="mobile" name="mobile" required placeholder="10 digits only">
                    <div id="mobile-error" class="error-msg">Mobile must be exactly 10 digits</div>
                </div>
                <div class="form-group">
                    <label class="form-label">Create Password</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Confirm Password</label>
                    <input type="password" class="form-control" id="confirm_password" name="confirm_password" required>
                    <div id="password-error" class="error-msg" style="display:none; color: var(--danger-color); margin-top: 5px;">Passwords do not match</div>
                </div>
                
                <button type="submit" class="btn btn-primary w-full mb-4">Sign Up</button>
                <div class="text-center mt-3">
                    <a href="login.jsp" class="signup-link">Already have an account? Login</a>
                </div>
            </form>
        </div>
    </div>

    <script>
        document.getElementById('student-signup-form').addEventListener('submit', function(e) {
            const pwd = document.getElementById('password').value;
            const confirm = document.getElementById('confirm_password').value;
            const errorDiv = document.getElementById('password-error');
            
            if (pwd !== confirm) {
                e.preventDefault();
                errorDiv.style.display = 'block';
            } else {
                errorDiv.style.display = 'none';
            }
        });
    </script>
</body>
</html>
