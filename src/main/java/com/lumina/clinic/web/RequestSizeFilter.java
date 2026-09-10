package com.lumina.clinic.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bound ordinary JSON submissions; the reverse proxy should also enforce an upload limit. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestSizeFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/") && request.getContentLengthLong() > 32768) {
            response.setStatus(413);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":413,\"code\":\"REQUEST_TOO_LARGE\",\"detail\":\"Request exceeds the 32 KB limit.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
