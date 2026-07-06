package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedPayload {
    private List<ReservedItem> itemsReserved;
    private Double totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private String productId;
        private Integer quantity;
        private List<Integer> cellsUsed;
    }
}
