package com.ecommerce.cart.dto;

import java.util.List;

public record CartResponse(List<CartItemResponse> items, Double total) {
}
