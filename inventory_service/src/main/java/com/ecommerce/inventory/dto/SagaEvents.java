package com.ecommerce.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class SagaEvents {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseEvent<T> {
        public String eventId;
        public String eventType;
        public String occurredAt;
        public String orderId;
        public String userId;
        public T payload;

        public BaseEvent() {}
        public BaseEvent(String eventId, String eventType, String occurredAt, String orderId, String userId, T payload) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.occurredAt = occurredAt;
            this.orderId = orderId;
            this.userId = userId;
            this.payload = payload;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrdersCreatedPayload {
        public List<Item> items;
        public Double totalAmount;
        public String idempotencyKey;

        public static class Item {
            public String productId;
            public Integer quantity;
            public Double priceAtAdd;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InventoryReservedPayload {
        public List<ReservedItem> itemsReserved;
        public Double totalAmount;

        public InventoryReservedPayload() {}
        public InventoryReservedPayload(List<ReservedItem> itemsReserved, Double totalAmount) {
            this.itemsReserved = itemsReserved;
            this.totalAmount = totalAmount;
        }

        public static class ReservedItem {
            public String productId;
            public Integer quantity;
            public List<Integer> cellsUsed;
            
            public ReservedItem() {}
            public ReservedItem(String productId, Integer quantity, List<Integer> cellsUsed) {
                this.productId = productId;
                this.quantity = quantity;
                this.cellsUsed = cellsUsed;
            }
        }
    }

    public static class InventoryReservationFailedPayload {
        public String reason;
        public List<String> failedItems;
        public InventoryReservationFailedPayload() {}
        public InventoryReservationFailedPayload(String reason, List<String> failedItems) {
            this.reason = reason;
            this.failedItems = failedItems;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentProcessedPayload {
        public String paymentId;
        public Double amountCharged;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentFailedPayload {
        public String reason;
    }
}
