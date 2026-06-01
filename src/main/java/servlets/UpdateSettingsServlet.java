package servlets;

import dao.SettingsDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/updateSettings")
public class UpdateSettingsServlet extends HttpServlet {
    private SettingsDAO settingsDAO;

    @Override
    public void init() {
        settingsDAO = new SettingsDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp?error=Unauthorized");
            return;
        }

        String upiId = request.getParameter("upi_id");
        String upiName = request.getParameter("upi_name");

        boolean success = true;
        if (upiId != null) {
            success &= settingsDAO.updateSetting("library_upi_id", upiId);
        }
        if (upiName != null) {
            success &= settingsDAO.updateSetting("library_upi_name", upiName);
        }

        if (success) {
            response.sendRedirect("librarian-dashboard.jsp?success=Settings updated successfully");
        } else {
            response.sendRedirect("librarian-dashboard.jsp?error=Failed to update settings");
        }
    }
}
