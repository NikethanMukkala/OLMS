package servlets;

import utils.DBConnection;
import utils.HashUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/verify-signup-otp")
public class VerifySignupOtpServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward to the OTP page (so session is still accessible)
        request.getRequestDispatcher("verify-signup-otp.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String inputOtp = request.getParameter("otp");
        HttpSession session = request.getSession(false);

        // ── Guard: session or OTP hash missing ────────────────────────────────
        if (session == null || session.getAttribute("signup_otpHash") == null) {
            response.sendRedirect("signup.jsp?error=Session+expired+please+sign+up+again");
            return;
        }

        String storedHash  = (String) session.getAttribute("signup_otpHash");
        Long   expiry      = (Long)   session.getAttribute("signup_otpExpiry");

        // ── Guard: expired OTP ────────────────────────────────────────────────
        if (expiry == null || System.currentTimeMillis() > expiry) {
            session.removeAttribute("signup_otpHash");
            request.setAttribute("error", "OTP expired. Please sign up again.");
            request.getRequestDispatcher("signup.jsp").forward(request, response);
            return;
        }

        // ── Guard: invalid OTP ────────────────────────────────────────────────
        if (!HashUtil.checkPassword(inputOtp, storedHash)) {
            request.setAttribute("error", "Invalid OTP. Please check and try again.");
            request.getRequestDispatcher("verify-signup-otp.jsp").forward(request, response);
            return;
        }

        // ── OTP correct – read signup data from session ────────────────────────
        String loginName = (String) session.getAttribute("signup_loginName");
        String rollNo    = (String) session.getAttribute("signup_rollNo");
        String email     = (String) session.getAttribute("signup_email");
        String mobile    = (String) session.getAttribute("signup_mobile");
        String password  = (String) session.getAttribute("signup_password");
        String role      = (String) session.getAttribute("signup_role");

        // ── Insert user into DB ───────────────────────────────────────────────
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO users (login_name, roll_no, email, mobile, password, role, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'APPROVED')");
            insert.setString(1, loginName);
            insert.setString(2, rollNo);
            insert.setString(3, email);
            insert.setString(4, mobile);
            insert.setString(5, HashUtil.hashPassword(password));
            insert.setString(6, role != null ? role : "STUDENT");

            insert.executeUpdate();

            // ── Clean session ─────────────────────────────────────────────────
            session.removeAttribute("signup_otpHash");
            session.removeAttribute("signup_otpExpiry");
            session.removeAttribute("signup_loginName");
            session.removeAttribute("signup_rollNo");
            session.removeAttribute("signup_email");
            session.removeAttribute("signup_mobile");
            session.removeAttribute("signup_password");
            session.removeAttribute("signup_role");
            session.removeAttribute("debug_signup_otp");

            // ── Redirect to login with success message ────────────────────────
            session.setAttribute("success",
                "Account created successfully! You can now log in.");
            response.sendRedirect("login.jsp");

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Database error: " + e.getMessage() +
                " — Please contact the administrator.");
            request.getRequestDispatcher("verify-signup-otp.jsp").forward(request, response);
        }
    }
}
