package filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class SecurityFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // ─── Static resources – always pass through ───────────────────────────
        if (uri.contains("/css/") || uri.contains("/js/") || uri.contains("/images/")) {
            chain.doFilter(request, response);
            return;
        }

        // ─── Public pages & endpoints (no session required) ───────────────────
        boolean isPublic = uri.endsWith("login.jsp")
                || uri.endsWith("signup.jsp")
                || uri.endsWith("verify-otp.jsp")
                || uri.endsWith("verify-signup-otp.jsp")
                || uri.endsWith("index.jsp")
                || uri.endsWith("/login")
                || uri.endsWith("/signup")
                || uri.endsWith("/verifyOtp")
                || uri.endsWith("/verify-signup-otp")   // servlet path
                || uri.endsWith("/chatbot")
                || uri.endsWith("/debug.jsp");           // dev helper

        HttpSession session = req.getSession(false);
        boolean isLoggedIn = session != null && session.getAttribute("user_id") != null;

        if (isPublic) {
            // Already logged-in users visiting login/signup → redirect to dashboard
            if (isLoggedIn && (uri.endsWith("login.jsp") || uri.endsWith("signup.jsp"))) {
                String role = (String) session.getAttribute("role");
                if ("STUDENT".equals(role)) {
                    res.sendRedirect(req.getContextPath() + "/student-dashboard.jsp");
                } else {
                    res.sendRedirect(req.getContextPath() + "/librarian-dashboard");
                }
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // ─── Protected pages ──────────────────────────────────────────────────
        if (isLoggedIn) {
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/login.jsp");
        }
    }

    public void destroy() {}
}
