package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.CheckoutRequest;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment/balance")
    public ResponseEntity<?> getBalance(@RequestHeader(value = "x-user-id", required = false) String xUserId) {
        if (xUserId == null || xUserId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Missing user_id"));
        }

        try {
            Double balance = paymentService.getBalance(xUserId);
            if (balance == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User not found"));
            }
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/payment/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req,
                                      @RequestHeader(value = "x-user-id", required = false) String xUserId) {
        if (xUserId == null || xUserId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Missing user_id"));
        }

        Map<String, Object> result = paymentService.checkout(req, xUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
