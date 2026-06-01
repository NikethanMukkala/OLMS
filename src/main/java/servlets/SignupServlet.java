package servlets;

import utils.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("signup.jsp");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String loginName = request.getParameter("login_name");
        String rollNo = request.getParameter("roll_no");
        String email = request.getParameter("email");
        String mobile = request.getParameter("mobile");
        String password = request.getParameter("password");
        String role = "STUDENT";
        
        try (Connection conn = DBConnection.getConnection()) {
            // Check for duplicates (Roll No or Email)
            PreparedStatement checkPs = conn.prepareStatement("SELECT * FROM users WHERE roll_no = ? OR email = ?");
            checkPs.setString(1, rollNo);
            checkPs.setString(2, email);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next()) {
                request.setAttribute("error", "An account already exists with this Roll Number or Email.");
                request.getRequestDispatcher("signup.jsp").forward(request, response);
                return;
            }
            
            // Generate OTP and store details in session
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));
            String hashedOtp = utils.HashUtil.hashPassword(otp);
            
            HttpSession session = request.getSession();
            session.setAttribute("signup_otpHash", hashedOtp);
            session.setAttribute("signup_otpExpiry", System.currentTimeMillis() + (5 * 60 * 1000));
            session.setAttribute("debug_signup_otp", otp);
            
            session.setAttribute("signup_loginName", loginName);
            session.setAttribute("signup_rollNo", rollNo);
            session.setAttribute("signup_email", email);
            session.setAttribute("signup_mobile", mobile);
            session.setAttribute("signup_password", password);
            session.setAttribute("signup_role", role);
            
            // Send OTP via Email
            utils.EmailUtility.sendOTP(email, otp);
            
            // dispatch OTP (handled above)
            
            // Redirect to OTP verification servlet (which forwards to JSP)
            response.sendRedirect(request.getContextPath() + "/verify-signup-otp");
            
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Database/System error: " + e.getMessage());
            request.getRequestDispatcher("signup.jsp").forward(request, response);
        }
    }
}
