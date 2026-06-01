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

@WebServlet("/downloadReceipt")
public class DownloadReceiptServlet extends HttpServlet {
    private BorrowDAO borrowDAO;

    @Override
    public void init() {
        borrowDAO = new BorrowDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"STUDENT".equals(session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please login as student");
            return;
        }

        String userId = (String) session.getAttribute("user_id");
        String borrowIdParam = request.getParameter("borrowId");
        
        if (borrowIdParam == null || borrowIdParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing borrow ID");
            return;
        }

        try {
            int borrowId = Integer.parseInt(borrowIdParam);
            Borrow borrow = borrowDAO.getBorrowById(borrowId);

            if (borrow == null || !borrow.getUserId().equals(userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied or record not found");
                return;
            }

            if (!borrow.isFinePaid()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot download receipt for unpaid fine");
                return;
            }

            // Generate PDF
            String logoPath = getServletContext().getRealPath("/images/library_logo.png");
            byte[] pdfBytes = utils.ReceiptGenerator.generateReceiptPdf(borrow, logoPath);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"Receipt_" + borrowId + ".pdf\"");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating receipt");
        }
    }
}
