package com.trainticket.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

/** Session-bound synchronizer token for all unsafe HTTP methods, including login/logout. */
@WebFilter("/*")
public class CsrfFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrfToken");
        if (!SAFE.contains(req.getMethod())) {
            String actual = req.getParameter("csrfToken");
            if (actual == null) { actual = req.getHeader("X-CSRF-Token"); }
            if (expected == null || actual == null || actual.length() > 128
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                            actual.getBytes(StandardCharsets.US_ASCII))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or expired form. Reload the page and try again.");
                return;
            }
        }
        if (session == null) { session = req.getSession(); }
        ensureToken(session);
        res.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
    public static String ensureToken(HttpSession session) {
        synchronized (session) {
            if (session.getAttribute("csrfToken") == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                session.setAttribute("csrfToken", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            }
            return (String) session.getAttribute("csrfToken");
        }
    }
}
