package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddItemRequest;
import com.ecommerce.cart.dto.ErrorResponse;
import com.ecommerce.cart.dto.RemoveItemRequest;
import com.ecommerce.cart.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(
            @RequestHeader(value = "x-user-id", required = false) String userId,
            @RequestBody AddItemRequest req) {

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing user_id"));
        }

        Map<String, Integer> cart = cartService.addItem(userId, req.item(), req.quantity());
        return ResponseEntity.ok(Map.of("success", true, "cart", cart));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeItem(
            @RequestHeader(value = "x-user-id", required = false) String userId,
            @RequestBody RemoveItemRequest req) {

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing user_id"));
        }

        Map<String, Integer> cart = cartService.removeItem(userId, req.item());
        return ResponseEntity.ok(Map.of("success", true, "cart", cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(
            @RequestHeader(value = "x-user-id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing user_id"));
        }

        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping
    public ResponseEntity<?> viewCart(
            @RequestHeader(value = "x-user-id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing user_id"));
        }

        try {
            return ResponseEntity.ok(cartService.getEnrichedCart(userId));
        } catch (RuntimeException e) {
            if ("Inventory service unavailable".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("Inventory service unavailable"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
