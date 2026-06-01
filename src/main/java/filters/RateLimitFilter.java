package filters;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@WebFilter(urlPatterns = {"/chatbot", "/login"}, asyncSupported = true)
public class RateLimitFilter implements Filter {

    private static final long MAX_REQUESTS = 10;
    private static final long WINDOW_IN_MS = TimeUnit.MINUTES.toMillis(1);
    
    private ConcurrentHashMap<String, RequestData> clientRequests;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        clientRequests = new ConcurrentHashMap<>();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Use IP or session ID
        String clientIp = req.getRemoteAddr();
        final long now = System.currentTimeMillis();

        clientRequests.compute(clientIp, (ip, data) -> {
            if (data == null || (now - data.startTime > WINDOW_IN_MS)) {
                return new RequestData(now, 1);
            } else {
                data.count++;
                return data;
            }
        });

        RequestData data = clientRequests.get(clientIp);

        if (data.count > MAX_REQUESTS) {
            res.setStatus(429); // Too Many Requests
            res.setContentType("application/json");
            res.getWriter().write("{\"reply\":\"Rate limit exceeded. Please wait a minute before trying again.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        clientRequests.clear();
    }

    private static class RequestData {
        long startTime;
        long count;

        RequestData(long startTime, long count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
