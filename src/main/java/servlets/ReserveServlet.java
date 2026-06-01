package servlets;

import dao.BookDAO;
import dao.BorrowDAO;
import models.Book;
import models.Borrow;
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
import java.sql.Date;
import java.time.LocalDate;

@WebServlet("/reserve")
public class ReserveServlet extends HttpServlet {
    private BookDAO bookDAO;
    private BorrowDAO borrowDAO;

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        borrowDAO = new BorrowDAO();
    }

    private String getUserEmail(String userId) {
        String sql = "SELECT email FROM users WHERE login_name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("email");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null
                || !"STUDENT".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        String userId = (String) session.getAttribute("user_id");
        
        String action = request.getParameter("action");
        if ("unreserve_request".equals(action)) {
            int borrowId = Integer.parseInt(request.getParameter("borrowId"));
            String unreserveReason = request.getParameter("unreserveReason");
            boolean success = borrowDAO.requestUnreserve(borrowId, unreserveReason);
            if (success) {
                session.setAttribute("successMessage", "Unreserve request submitted successfully.");
            } else {
                session.setAttribute("errorMessage", "Failed to submit unreserve request.");
            }
            response.sendRedirect("books?view=reservations");
            return;
        }

        if ("place_hold".equals(action)) {
            int holdBookId = Integer.parseInt(request.getParameter("bookId"));
            dao.ReservationDAO resDao = new dao.ReservationDAO();
            boolean success = resDao.placeHold(holdBookId, userId);
            if(success) {
                session.setAttribute("successMessage", "You have successfully been added to the waitlist for this book.");
            } else {
                session.setAttribute("errorMessage", "You are already on the waitlist for this book.");
            }
            response.sendRedirect("books");
            return;
        }

        int bookId = Integer.parseInt(request.getParameter("bookId"));
        Book book = bookDAO.getBookById(bookId);

        int currentBorrowed = borrowDAO.getBorrowCountByUser(userId);
        if (currentBorrowed >= 3) {
            session.setAttribute("errorMessage", "You cannot reserve this book. Maximum limit of 3 overlapping reservations/borrows reached.");
            response.sendRedirect("books");
            return;
        }

        boolean alreadyReserved = false;
        java.util.List<Borrow> activeBorrows = borrowDAO.getBorrowsByUser(userId, false);
        for(Borrow b : activeBorrows) {
             if (b.getBookId() == bookId) {
                  alreadyReserved = true;
                  break;
             }
        }
        if (alreadyReserved) {
            session.setAttribute("errorMessage", "You already have an active reservation or borrow for this exact book.");
            response.sendRedirect("books");
            return;
        }

        if (book != null && book.getCopiesAvailable() > 0) {
            boolean updated = bookDAO.updateCopies(bookId, -1);
            if (updated) {
                Borrow borrow = new Borrow();
                borrow.setUserId(userId);
                borrow.setBookId(bookId);
                borrow.setIssueDate(Date.valueOf(LocalDate.now())); // now
                borrow.setDueDate(Date.valueOf(LocalDate.now().plusDays(14))); // +14 days
                borrow.setStatus("RESERVED");
                borrow.setReservationExpiry(new java.sql.Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)); // +24 hours

                boolean inserted = borrowDAO.insertBorrow(borrow);

                if (inserted) {
                    String userEmail = getUserEmail(userId);
                    if (userEmail != null) {
                        String subject = "Reservation Confirmation: " + book.getTitle();
                        String body = "Dear " + userId + ",\n\n"
                                + "You have successfully reserved the book: '" + book.getTitle() + "' (ISBN: "
                                + book.getIsbn() + ").\n"
                                + "Please collect it from the library within 24 hours.\n\n"
                                + "If the book is not picked up within 24 hours, your reservation will automatically expire.\n\n"
                                + "Thank you,\nLibrary Management Team";
                        EmailUtility.sendEmail(userEmail, subject, body);
                    }
                    session.setAttribute("successMessage",
                            "Book '" + book.getTitle() + "' successfully reserved! Please collect within 24 hours.");
                } else {
                    bookDAO.updateCopies(bookId, 1);
                    session.setAttribute("errorMessage", "Failed to create reservation record.");
                }
            } else {
                session.setAttribute("errorMessage", "Failed to update book copies. Please try again.");
            }
        } else {
            session.setAttribute("errorMessage", "Sorry, this book is currently out of stock.");
        }

        response.sendRedirect("books?view=reservations");
    }
}
