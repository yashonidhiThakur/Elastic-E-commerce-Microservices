package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedPayload {
    private List<Item> items;
    private Double totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private Integer quantity;
    }
}
