package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.admission.TokenBucket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucket tokenBucket;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(TokenBucket tokenBucket, ObjectMapper objectMapper) {
        this.tokenBucket = tokenBucket;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (!"/checkout".equals(path) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        byte[] body = StreamUtils.copyToByteArray(cachedRequest.getInputStream());

        if (body.length > 0) {
            try {
                JsonNode json = objectMapper.readTree(body);
                JsonNode items = json.get("items");

                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        JsonNode productIdNode = item.get("productId");
                        if (productIdNode != null) {
                            String productId = productIdNode.asText();
                            boolean allowed = tokenBucket.tryConsume(productId);
                            if (!allowed) {
                                response.setStatus(429); // Too Many Requests
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"High traffic volume. Please try again.\"}");
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing fails, allow it to pass through to be rejected by downstream validation
            }
        }

        filterChain.doFilter(cachedRequest, response);
    }
}
