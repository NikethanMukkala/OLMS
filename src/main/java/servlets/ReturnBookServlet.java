package servlets;

import dao.BorrowDAO;
import utils.DBConnection;
import utils.EmailUtility;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/returnBook")
public class ReturnBookServlet extends HttpServlet {
    private BorrowDAO borrowDAO;

    @Override
    public void init() {
        borrowDAO = new BorrowDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"STUDENT".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        // Add CSRF validation
        String sessionCsrf = (String) session.getAttribute("csrfToken");
        String requestCsrf = request.getParameter("csrfToken");
        if (sessionCsrf == null || requestCsrf == null || !sessionCsrf.equals(requestCsrf)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            return;
        }

        String userId = (String) session.getAttribute("user_id");
        String borrowIdParam = request.getParameter("borrowId");
        
        if (borrowIdParam != null && !borrowIdParam.trim().isEmpty()) {
            try {
                int borrowId = Integer.parseInt(borrowIdParam);
                boolean success = borrowDAO.returnBook(borrowId, userId);
                if (success) {
                    session.setAttribute("successMessage", "Book returned successfully!");
                    
                    // Fetch user's email to send confirmation
                    String email = null;
                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE login_name = ?")) {
                        stmt.setString(1, userId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                email = rs.getString("email");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (email != null) {
                        EmailUtility.sendEmail(email, "OLMS - Book Return Confirmation", 
                            "Dear Student,\n\nYou have successfully returned your book. Thank you for using OLMS.\n\nRegards,\nOLMS Admin");
                    }
                } else {
                    session.setAttribute("errorMessage", "Failed to return the book. You may have already returned it.");
                }
            } catch (NumberFormatException e) {
                session.setAttribute("errorMessage", "Invalid borrow ID format.");
            }
        }
        response.sendRedirect("books?view=dues");
    }
}
