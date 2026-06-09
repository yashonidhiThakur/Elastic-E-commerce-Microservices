package com.ecommerce.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

@RestController
public class GatewayController {

    @Value("${app.services.auth}")
    private String authUrl;

    @Value("${app.services.cart}")
    private String cartUrl;

    @Value("${app.services.payment}")
    private String paymentUrl;

    @Value("${app.services.inventory}")
    private String inventoryUrl;

    @Value("${app.services.order}")
    private String orderUrl;

    private final RestClient restClient = RestClient.create();

    @RequestMapping(value = {"/auth", "/auth/**"})
    public ResponseEntity<byte[]> handleAuth(HttpServletRequest request) {
        return forwardRequest(request, authUrl, "/auth");
    }

    @RequestMapping(value = {"/cart", "/cart/**"})
    public ResponseEntity<byte[]> handleCart(HttpServletRequest request) {
        return forwardRequest(request, cartUrl, "/cart");
    }

    @RequestMapping(value = {"/payment", "/payment/**"})
    public ResponseEntity<byte[]> handlePayment(HttpServletRequest request) {
        return forwardRequest(request, paymentUrl, "/payment");
    }

    @RequestMapping(value = {"/inventory", "/inventory/**"})
    public ResponseEntity<byte[]> handleInventory(HttpServletRequest request) {
        return forwardRequest(request, inventoryUrl, "/inventory");
    }

    // Keep the old routes for backward compatibility with order-service (until it's retired)
    @RequestMapping(value = "/api/orders")
    public ResponseEntity<byte[]> handleApiOrders(HttpServletRequest request) {
        return forwardRequest(request, orderUrl, "/orders");
    }

    @RequestMapping(value = "/api/inventory")
    public ResponseEntity<byte[]> handleApiInventory(HttpServletRequest request) {
        return forwardRequest(request, inventoryUrl, "/inventory");
    }

    @RequestMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private ResponseEntity<byte[]> forwardRequest(HttpServletRequest request, String targetServiceUrl, String originalPrefix) {
        try {
            String requestUri = request.getRequestURI();
            String path = requestUri;
            
            // For exact mappings like /api/orders mapping to /orders, originalPrefix contains the target path.
            if (requestUri.equals("/api/orders")) {
                path = "/orders";
            } else if (requestUri.equals("/api/inventory")) {
                path = "/inventory";
            }

            String queryString = request.getQueryString();
            String url = targetServiceUrl + path + (queryString != null ? "?" + queryString : "");

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(url)
                    .headers(httpHeaders -> {
                        Enumeration<String> headerNames = request.getHeaderNames();
                        while (headerNames.hasMoreElements()) {
                            String headerName = headerNames.nextElement();
                            if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                                httpHeaders.add(headerName, request.getHeader(headerName));
                            }
                        }

                        // Add x-user-id from state if present
                        if (request.getAttribute("user_id") != null) {
                            httpHeaders.set("x-user-id", request.getAttribute("user_id").toString());
                        }
                    });

            if (body.length > 0) {
                spec.body(body);
            }

            return spec.retrieve().toEntity(byte[].class);

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (IOException e) {
            return ResponseEntity.status(500).body(String.format("{\"detail\": \"%s\"}", e.getMessage()).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(String.format("{\"detail\": \"%s\"}", e.getMessage()).getBytes());
        }
    }
}
