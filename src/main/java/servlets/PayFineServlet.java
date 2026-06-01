package servlets;

import dao.BorrowDAO;

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
import utils.DBConnection;
import utils.EmailUtility;

@WebServlet("/payFine")
public class PayFineServlet extends HttpServlet {
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

        String userId = (String) session.getAttribute("user_id");
        String borrowIdParam = request.getParameter("borrowId");
        String paymentMethod = request.getParameter("paymentMethod"); // 'CASH', 'PHONEPE', 'GPAY'
        
        if (borrowIdParam != null && !borrowIdParam.trim().isEmpty() && paymentMethod != null) {
            try {
                int borrowId = Integer.parseInt(borrowIdParam);
                boolean success;
                
                if ("CASH".equalsIgnoreCase(paymentMethod)) {
                    // For cash, just request it. Librarian must approve.
                    success = borrowDAO.requestCashPayment(borrowId, userId);
                    if (success) {
                        session.setAttribute("successMessage", "Cash payment requested. Please pay at the desk.");
                    }
                } else {
                    success = borrowDAO.payFine(borrowId, userId, paymentMethod.toUpperCase());
                    if (success) {
                        session.setAttribute("successMessage", "Fine paid successfully via " + paymentMethod.toUpperCase() + "!");
                        
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
                            try {
                                models.Borrow borrow = borrowDAO.getBorrowById(borrowId);
                                String logoPath = getServletContext().getRealPath("/images/library_logo.png");
                                byte[] receiptPdf = utils.ReceiptGenerator.generateReceiptPdf(borrow, logoPath);
                                EmailUtility.sendEmailWithAttachment(email, "OLMS - Payment Received", 
                                    "Dear Student,\n\nWe have successfully received your fine payment for borrow ID: " + borrowId + ".\n" +
                                    "Your payment receipt is attached to this email.\n\nRegards,\nOLMS Admin",
                                    receiptPdf, "Receipt_" + borrowId + ".pdf");
                            } catch (Exception e) {
                                e.printStackTrace();
                                EmailUtility.sendEmail(email, "OLMS - Payment Received", 
                                    "Dear Student,\n\nWe have successfully received your fine payment for borrow ID: " + borrowId + ".\n" +
                                    "You can now download your receipt directly from the Borrowed Books & Dues section of your dashboard.\n\nRegards,\nOLMS Admin");
                            }
                        }
                    }
                }
                
                if (!success) {
                    session.setAttribute("errorMessage", "Failed to process payment. Please try again.");
                }
            } catch (NumberFormatException e) {
                session.setAttribute("errorMessage", "Invalid borrow ID format.");
            }
        }
        response.sendRedirect("books?view=dues");
    }
}
