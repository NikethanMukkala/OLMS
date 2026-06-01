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

@WebServlet("/librarian-borrows")
public class BorrowedBooksServlet extends HttpServlet {
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

        List<Borrow> allBorrows = borrowDAO.getAllBorrows();
        request.setAttribute("borrows", allBorrows);
        request.setAttribute("view", "borrows");

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
        String[] borrowIds = request.getParameterValues("borrowIds");

        if (borrowIds != null && borrowIds.length > 0) {
            int successCount = 0;
            for (String idStr : borrowIds) {
                int bid = Integer.parseInt(idStr);
                if ("approve_return".equals(action)) {
                    if (borrowDAO.approveReturn(bid)) successCount++;
                } else if ("waive_fine".equals(action)) {
                    if (borrowDAO.waiveFine(bid)) successCount++;
                } else if ("approve_unreserve".equals(action)) {
                    if (borrowDAO.approveUnreserve(bid)) successCount++;
                }
            }
            request.setAttribute("success", "Successfully processed " + successCount + " record(s).");
        } else {
            request.setAttribute("error", "No records selected.");
        }

        doGet(request, response);
    }
}
