package servlets;

import dao.BorrowDAO;
import utils.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/rate-book")
public class RatingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null || !"STUDENT".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        String userId = (String) session.getAttribute("user_id");
        String action = request.getParameter("action");
        int borrowId = Integer.parseInt(request.getParameter("borrowId"));
        int bookId = Integer.parseInt(request.getParameter("bookId"));

        BorrowDAO borrowDAO = new BorrowDAO();

        if ("submit_rating".equals(action)) {
            int rating = Integer.parseInt(request.getParameter("rating"));
            
            // Insert into user_ratings
            String sql = "INSERT INTO user_ratings (user_id, book_id, rating) VALUES (?, ?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                stmt.setInt(2, bookId);
                stmt.setInt(3, rating);
                stmt.executeUpdate();
                session.setAttribute("successMessage", "Thank you for your rating!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } 
        
        // Regardless of submitting or skipping, mark it as prompted
        borrowDAO.markRatingPrompted(borrowId);

        response.sendRedirect("books?view=overview");
    }
}
