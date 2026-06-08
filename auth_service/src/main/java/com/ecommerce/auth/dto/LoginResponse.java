package com.ecommerce.auth.dto;

public record LoginResponse(String token, Long user_id, String username) {
}
