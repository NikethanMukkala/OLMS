package servlets;

import dao.ReportDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@WebServlet("/librarian-reports")
public class ReportsServlet extends HttpServlet {
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

        String export = request.getParameter("export");
        List<Map<String, Object>> popularBooks = reportDAO.getPopularBooks(50);
        
        // Let's create an overdue report as well - wait, reportDAO.getOverdueBooks doesn't exist yet!
        // For simplicity, we can fetch overdue stats from BorrowDAO or just display popular books for now.
        // Actually, let's reuse BorrowDAO to get all overdue books if needed, or simply export the popular books.

        if ("csv".equals(export)) {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"popular_books_report.csv\"");
            
            PrintWriter out = response.getWriter();
            out.println("Title,Author,Borrow Count");
            
            for (Map<String, Object> map : popularBooks) {
                String title = "\"" + String.valueOf(map.get("title")).replace("\"", "\"\"") + "\"";
                String author = "\"" + String.valueOf(map.get("author")).replace("\"", "\"\"") + "\"";
                out.println(title + "," + author + "," + map.get("borrowCount"));
            }
            out.flush();
            return;
        }

        request.setAttribute("popularBooks", popularBooks);
        request.setAttribute("view", "reports");
        request.getRequestDispatcher("librarian-dashboard.jsp").forward(request, response);
    }
}
