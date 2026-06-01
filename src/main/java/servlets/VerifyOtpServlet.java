package servlets;

import utils.HashUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/verify-otp")
public class VerifyOtpServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputOtp = request.getParameter("otp");
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("otpHash") == null) {
            response.sendRedirect("login.jsp?error=Session Expired");
            return;
        }

        String storedHash = (String) session.getAttribute("otpHash");
        Long expiry = (Long) session.getAttribute("otpExpiry");
        String tempUser = (String) session.getAttribute("tempUser");
        String tempRole = (String) session.getAttribute("tempRole");

        // Validate expiry
        if (System.currentTimeMillis() > expiry) {
            session.invalidate();
            response.sendRedirect("login.jsp?error=OTP Expired. Please login again.");
            return;
        }

        // Validate Hash
        if (HashUtil.checkPassword(inputOtp, storedHash)) {
            // Success
            session.removeAttribute("otpHash");
            session.removeAttribute("otpExpiry");
            session.removeAttribute("tempUser");
            session.removeAttribute("tempRole");
            
            // Set permanent auth session
            session.setAttribute("user_id", tempUser);
            session.setAttribute("role", tempRole);
            
            if ("STUDENT".equals(tempRole)) {
                response.sendRedirect("books");
            } else {
                response.sendRedirect("librarian-dashboard");
            }
        } else {
            // Failure
            request.setAttribute("error", "Invalid OTP. Please try again.");
            request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
        }
    }
}
