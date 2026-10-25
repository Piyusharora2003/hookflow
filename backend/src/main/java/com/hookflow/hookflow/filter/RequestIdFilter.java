package com.hookflow.hookflow.filter;

import com.hookflow.hookflow.publisher.constant.PublisherConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts or generates an X-Request-Id and puts it into MDC for logging.
 * Cleans up MDC upon completion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(PublisherConstants.X_REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 10) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "").trim().substring(0,6);
        }

        response.setHeader(PublisherConstants.X_REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
