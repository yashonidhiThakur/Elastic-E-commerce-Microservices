package com.ecommerce.inventory.dto;

public record OrderPaidEvent(String item, Integer quantity, String user_id) {
}
