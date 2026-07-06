package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdersCreatedPayload {
    private List<Item> items;
    private Double totalAmount;
    private String idempotencyKey;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private Integer quantity;
        private Double priceAtAdd;
    }
}
