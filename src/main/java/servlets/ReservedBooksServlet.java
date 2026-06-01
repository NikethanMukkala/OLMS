package servlets;

import dao.BorrowDAO;
import models.Borrow;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/reserved-books")
public class ReservedBooksServlet extends HttpServlet {
    private BorrowDAO borrowDAO;

    @Override
    public void init() throws ServletException {
        borrowDAO = new BorrowDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        List<Borrow> reservedBooks = borrowDAO.getAllReservedBooks();
        request.setAttribute("reservedBooks", reservedBooks);
        request.setAttribute("view", "reserved_books");

        request.getRequestDispatcher("librarian-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("mark_borrowed".equals(action)) {
            int borrowId = Integer.parseInt(request.getParameter("borrowId"));
            boolean success = borrowDAO.markAsBorrowed(borrowId);
            if (success) {
                Borrow borrow = borrowDAO.getBorrowById(borrowId);
                if (borrow != null) {
                    String email = null;
                    try (java.sql.Connection conn = utils.DBConnection.getConnection();
                         java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE login_name = ?")) {
                        stmt.setString(1, borrow.getUserId());
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) email = rs.getString("email");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (email != null) {
                        String subject = "Borrowing Confirmed: " + borrow.getBookTitle();
                        String body = "Dear " + borrow.getUserId() + ",\n\n"
                                + "You have successfully borrowed the book: '" + borrow.getBookTitle() + "'.\n"
                                + "Please return it by: " + borrow.getDueDate() + "\n\n"
                                + "Happy Reading!\nLibrary Management Team";
                        utils.EmailUtility.sendEmail(email, subject, body);
                    }
                }
                session.setAttribute("success", "Book officially marked as borrowed and email sent to student.");
            } else {
                session.setAttribute("error", "Failed to mark book as borrowed.");
            }
        }
        
        response.sendRedirect("reserved-books");
    }
}
