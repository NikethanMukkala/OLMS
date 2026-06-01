package servlets;

import utils.DBConnection;
import utils.HashUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String role     = request.getParameter("role");
        String loginName = request.getParameter("login_name");
        String rollNo   = request.getParameter("roll_no");
        String password = request.getParameter("password");

        if (password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Password is required.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            PreparedStatement ps;

            if ("STUDENT".equals(role)) {
                ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE roll_no = ? AND role = 'STUDENT'");
                ps.setString(1, rollNo);
            } else {
                ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE login_name = ? AND role = 'LIBRARIAN'");
                ps.setString(1, loginName);
            }

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                request.setAttribute("error",
                    "STUDENT".equals(role)
                        ? "No account found with this Roll Number. Please sign up first."
                        : "Librarian account not found.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }

            // ── Password check ────────────────────────────────────────────────
            String storedHash = rs.getString("password");
            boolean passwordOk;

            // Support plain-text legacy password ("admin") for the default librarian
            if (storedHash != null && storedHash.startsWith("$2")) {
                passwordOk = HashUtil.checkPassword(password, storedHash);
            } else {
                passwordOk = password.equals(storedHash); // legacy plain-text
            }

            if (!passwordOk) {
                request.setAttribute("error", "Incorrect password. Please try again.");
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }

            // ── Status check (students only) ──────────────────────────────────
            if ("STUDENT".equals(role)) {
                String status = rs.getString("status");
                if ("PENDING".equals(status)) {
                    request.setAttribute("error",
                        "Your account is pending librarian approval. Please wait.");
                    request.getRequestDispatcher("login.jsp").forward(request, response);
                    return;
                }
                if ("REJECTED".equals(status)) {
                    request.setAttribute("error",
                        "Your registration was rejected. Please contact the librarian.");
                    request.getRequestDispatcher("login.jsp").forward(request, response);
                    return;
                }
            }

            // ── Success – set session ─────────────────────────────────────────
            String resolvedLoginName = rs.getString("login_name");
            String csrfToken = java.util.UUID.randomUUID().toString();

            HttpSession session = request.getSession(true);
            session.setAttribute("user_id",   resolvedLoginName);
            session.setAttribute("role",      role);
            session.setAttribute("csrfToken", csrfToken);

            if ("STUDENT".equals(role)) {
                response.sendRedirect("student-dashboard.jsp");
            } else {
                response.sendRedirect("librarian-dashboard");
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "System error: " + e.getMessage());
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}
