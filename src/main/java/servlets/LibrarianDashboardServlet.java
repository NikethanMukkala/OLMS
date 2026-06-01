package servlets;

import dao.ReportDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/librarian-dashboard")
public class LibrarianDashboardServlet extends HttpServlet {
    private ReportDAO reportDAO;

    @Override
    public void init() throws ServletException {
        reportDAO = new ReportDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        Map<String, Object> stats = reportDAO.getDashboardStats();
        request.setAttribute("stats", stats);
        
        dao.SettingsDAO settingsDAO = new dao.SettingsDAO();
        String currentLimit = settingsDAO.getSetting("library_limit");
        if(currentLimit == null || currentLimit.isEmpty()) currentLimit = "50";
        request.setAttribute("libraryLimit", currentLimit);

        String viewParam = request.getParameter("view");
        
        if ("transactions".equals(viewParam)) {
            dao.BorrowDAO borrowDAO = new dao.BorrowDAO();
            request.setAttribute("transactions", borrowDAO.getAllBorrows());
        } else if ("settings".equals(viewParam)) {
            dao.SettingsDAO sDAO = new dao.SettingsDAO();
            request.setAttribute("libUpiId", sDAO.getSetting("library_upi_id"));
            request.setAttribute("libUpiName", sDAO.getSetting("library_upi_name"));
        }

        request.setAttribute("view", viewParam != null ? viewParam : "dashboard");

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
        if ("update_library_limit".equals(action)) {
            String libraryLimit = request.getParameter("library_limit");
            dao.SettingsDAO settingsDAO = new dao.SettingsDAO();
            boolean success = settingsDAO.updateSetting("library_limit", libraryLimit);
            if (success) {
                session.setAttribute("success", "Library capacity updated successfully.");
            } else {
                session.setAttribute("error", "Failed to update library capacity.");
            }
        }
        response.sendRedirect("librarian-dashboard");
    }
}
