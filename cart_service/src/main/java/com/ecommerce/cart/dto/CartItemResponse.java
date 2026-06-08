package com.ecommerce.cart.dto;

public record CartItemResponse(String item, Integer quantity, Double price, Boolean available) {
}
