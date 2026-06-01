package servlets;

import dao.BorrowDAO;
import dao.SettingsDAO;
import dao.UserDAO;

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

@WebServlet("/managePayments")
public class ManagePaymentsServlet extends HttpServlet {
    private BorrowDAO borrowDAO;
    private SettingsDAO settingsDAO;
    private UserDAO userDAO;

    @Override
    public void init() {
        borrowDAO = new BorrowDAO();
        settingsDAO = new SettingsDAO();
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        String loginName = (String) session.getAttribute("user_id");

        if ("approve_cash".equals(action)) {
            String borrowIdStr = request.getParameter("borrowId");
            String studentId = request.getParameter("studentId");
            if (borrowIdStr != null && studentId != null) {
                try {
                    int borrowId = Integer.parseInt(borrowIdStr);
                    boolean success = borrowDAO.payFine(borrowId, studentId, "CASH");
                    if (success) {
                        session.setAttribute("successMessage", "Cash payment approved for borrow ID: " + borrowId);
                        
                        // Fetch user's email to send confirmation
                        String email = null;
                        try (Connection conn = DBConnection.getConnection();
                             PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE login_name = ?")) {
                            stmt.setString(1, studentId);
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
                                EmailUtility.sendEmailWithAttachment(email, "OLMS - Cash Payment Approved", 
                                    "Dear Student,\n\nYour cash payment for borrow ID: " + borrowId + " has been successfully verified and approved by the Librarian.\n" +
                                    "Your payment receipt is attached to this email.\n\nRegards,\nOLMS Admin",
                                    receiptPdf, "Receipt_" + borrowId + ".pdf");
                            } catch (Exception e) {
                                e.printStackTrace();
                                EmailUtility.sendEmail(email, "OLMS - Cash Payment Approved", 
                                    "Dear Student,\n\nYour cash payment for borrow ID: " + borrowId + " has been successfully verified and approved by the Librarian.\n" +
                                    "You can now download your receipt directly from the Borrowed Books & Dues section of your dashboard.\n\nRegards,\nOLMS Admin");
                            }
                        }
                    } else {
                        session.setAttribute("errorMessage", "Failed to approve cash payment.");
                    }
                } catch (NumberFormatException e) {
                    session.setAttribute("errorMessage", "Invalid borrow ID format.");
                }
            }
            response.sendRedirect("librarian-dashboard?view=transactions");
            return;
        } 
        else if ("update_upi".equals(action)) {
            String newUpiId = request.getParameter("upi_id");
            String newUpiName = request.getParameter("upi_name");
            String password = request.getParameter("password");

            if (newUpiId == null || newUpiName == null || password == null) {
                session.setAttribute("errorMessage", "All fields are required to update settings.");
                response.sendRedirect("librarian-dashboard?view=settings");
                return;
            }

            if (!userDAO.verifyPassword(loginName, password)) {
                session.setAttribute("errorMessage", "Invalid password! Settings not updated.");
                response.sendRedirect("librarian-dashboard?view=settings");
                return;
            }

            settingsDAO.updateSetting("library_upi_id", newUpiId);
            settingsDAO.updateSetting("library_upi_name", newUpiName);
            session.setAttribute("successMessage", "UPI details updated successfully.");
            response.sendRedirect("librarian-dashboard?view=settings");
            return;
        }

        response.sendRedirect("librarian-dashboard");
    }
}
