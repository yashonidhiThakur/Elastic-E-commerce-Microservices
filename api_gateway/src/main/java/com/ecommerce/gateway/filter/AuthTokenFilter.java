package com.ecommerce.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Value("${app.services.auth}")
    private String authUrl;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/cart") || path.startsWith("/payment") || path.startsWith("/checkout") || path.startsWith("/orders") || path.startsWith("/api/orders")) {
            String token = request.getHeader("token");
            if (token == null || token.isEmpty()) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing token");
                return;
            }

            try {
                // Call Auth Service
                RestClient.ResponseSpec responseSpec = restClient.get()
                        .uri(authUrl + "/auth/me")
                        .header("token", token)
                        .retrieve();
                
                String responseBody = responseSpec.body(String.class);
                JsonNode userNode = objectMapper.readTree(responseBody);
                
                String userId = userNode.get("user_id").asText();
                String username = userNode.get("username").asText();
                
                request.setAttribute("user_id", userId);
                request.setAttribute("username", username);

            } catch (Exception e) {
                // Determine if it was a 401 from downstream or some other error
                if (e.getMessage() != null && e.getMessage().contains("401")) {
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token");
                } else {
                    sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Auth service unavailable");
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"detail\": \"%s\"}", message);
        response.getWriter().write(json);
    }
}
