package servlets;

import dao.UserDAO;
import models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/student-management")
public class StudentManagementServlet extends HttpServlet {
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        List<User> students = userDAO.getAllStudents();
        List<User> pending = userDAO.getPendingStudents();

        String rollNoSearch = request.getParameter("searchRollNo");
        if (rollNoSearch != null && !rollNoSearch.trim().isEmpty()) {
            User student = userDAO.getUserByRollNo(rollNoSearch.trim());
            if (student != null) {
                request.setAttribute("searchResultUser", student);
                dao.BorrowDAO borrowDAO = new dao.BorrowDAO();
                request.setAttribute("activeBorrows", borrowDAO.getStudentBorrows(student.getLoginName()));
                request.setAttribute("totalFine", borrowDAO.getFineForUser(student.getLoginName()));
                request.setAttribute("view", "student_details");
            } else {
                request.setAttribute("error", "No student found with Roll Number: " + rollNoSearch);
                request.setAttribute("students", students);
                request.setAttribute("pendingStudents", pending);
                request.setAttribute("view", "students");
            }
        } else {
            request.setAttribute("students", students);
            request.setAttribute("pendingStudents", pending);
            request.setAttribute("view", "students");
        }

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
        String loginName = request.getParameter("login_name");

        if ("approve".equals(action)) {
            boolean success = userDAO.approveStudent(loginName);
            if (success) {
                request.setAttribute("success", "Student " + loginName + " approved successfully.");
            } else {
                request.setAttribute("error", "Failed to approve student " + loginName + ".");
            }
        } else if ("reject".equals(action)) {
            boolean success = userDAO.rejectStudent(loginName);
            if (success) {
                request.setAttribute("success", "Student " + loginName + " rejected.");
            } else {
                request.setAttribute("error", "Failed to reject student " + loginName + ".");
            }
        } else if ("approve_student_return".equals(action)) {
            int bid = Integer.parseInt(request.getParameter("borrowId"));
            dao.BorrowDAO borrowDAO = new dao.BorrowDAO();
            models.Borrow borrowInfo = borrowDAO.getBorrowById(bid);
            if (borrowDAO.approveReturn(bid)) {
                 request.getSession().setAttribute("success", "Book officially marked as returned.");
                 
                 // Send email
                 if (borrowInfo != null) {
                    String email = null;
                    try (java.sql.Connection conn = utils.DBConnection.getConnection();
                         java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE login_name = ?")) {
                        stmt.setString(1, borrowInfo.getUserId());
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) email = rs.getString("email");
                        }
                    } catch (Exception e) {}
                    if (email != null) {
                        String subject = "Return Confirmed: " + borrowInfo.getBookTitle();
                        String body = "Dear " + borrowInfo.getUserId() + ",\n\n"
                                + "You have successfully returned the book: '" + borrowInfo.getBookTitle() + "'.\n"
                                + "Thank you for returning it!\n\n"
                                + "Library Management Team";
                        utils.EmailUtility.sendEmail(email, subject, body);
                    }
                 }
            } else {
                 request.getSession().setAttribute("error", "Failed to mark book as returned.");
            }
            String rollNo = request.getParameter("searchRollNo");
            response.sendRedirect("student-management?searchRollNo=" + rollNo);
            return;
        } else if ("approve_student_unreserve".equals(action)) {
            int bid = Integer.parseInt(request.getParameter("borrowId"));
            dao.BorrowDAO borrowDAO = new dao.BorrowDAO();
            if (borrowDAO.approveUnreserve(bid)) {
                 request.getSession().setAttribute("success", "Unreserve request approved and book restocked.");
            } else {
                 request.getSession().setAttribute("error", "Failed to approve unreserve request.");
            }
            String rollNo = request.getParameter("searchRollNo");
            response.sendRedirect("student-management?searchRollNo=" + rollNo);
            return;
        }

        doGet(request, response);
    }
}
