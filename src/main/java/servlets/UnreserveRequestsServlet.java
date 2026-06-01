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

@WebServlet("/unreserve-requests")
public class UnreserveRequestsServlet extends HttpServlet {
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

        List<Borrow> unreserveRequests = borrowDAO.getAllUnreserveRequests();
        request.setAttribute("unreserveRequests", unreserveRequests);
        request.setAttribute("view", "unreserve_requests");

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
        if ("approve_unreserve".equals(action)) {
            int borrowId = Integer.parseInt(request.getParameter("borrowId"));
            boolean success = borrowDAO.approveUnreserve(borrowId);
            if (success) {
                session.setAttribute("success", "Unreserve request approved successfully.");
            } else {
                session.setAttribute("error", "Failed to approve unreserve request.");
            }
        }
        
        response.sendRedirect("unreserve-requests");
    }
}
