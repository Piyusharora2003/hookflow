package com.hookflow.hookflow.auth.filter;

import com.hookflow.hookflow.auth.model.ApiKeyInfo;
import com.hookflow.hookflow.auth.service.ApiKeyAuthService;
import com.hookflow.hookflow.filter.RequestIdFilter;
import com.hookflow.hookflow.publisher.constant.PublisherConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Filter that authenticates Bearer API keys strictly for the /v1/events publisher endpoint.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_API_KEY_INFO_ATTR = "HOOKFLOW_API_KEY_INFO";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyAuthService apiKeyAuthService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(
            @Qualifier("publisherApiAuthService") ApiKeyAuthService apiKeyAuthService,
            ObjectMapper objectMapper
    ) {
        this.apiKeyAuthService = apiKeyAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only apply to /v1/events endpoint
        return !path.startsWith("/v1/events");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        if (requestId == null) {
            requestId = response.getHeader(PublisherConstants.X_REQUEST_ID_HEADER);
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorized(response, requestId, "Missing or malformed Authorization header");
            return;
        }

        String rawToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (rawToken.isEmpty()) {
            sendUnauthorized(response, requestId, "Missing API key in Authorization header");
            return;
        }

        Optional<ApiKeyInfo> authenticatedKey = apiKeyAuthService.authenticate(rawToken);
        if (authenticatedKey.isEmpty()) {
            sendUnauthorized(response, requestId, "Invalid, revoked, or expired API key");
            return;
        }

        ApiKeyInfo apiKeyInfo = authenticatedKey.get();

        // Populate request attribute and Spring Security context
        request.setAttribute(AUTH_API_KEY_INFO_ATTR, apiKeyInfo);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                apiKeyInfo,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PUBLISHER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String requestId, String detail) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        Map<String, Object> problemDetail = new LinkedHashMap<>();
        problemDetail.put("type", "about:blank");
        problemDetail.put("title", "Unauthorized");
        problemDetail.put("status", HttpStatus.UNAUTHORIZED.value());
        problemDetail.put("detail", detail);
        problemDetail.put("requestId", requestId);

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
