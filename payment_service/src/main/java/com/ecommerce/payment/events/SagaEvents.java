package com.ecommerce.payment.events;

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
    public static class InventoryReservedPayload {
        public List<ReservedItem> itemsReserved;
        public Double totalAmount;
        public static class ReservedItem {
            public String productId;
            public Integer quantity;
            public List<Integer> cellsUsed;
        }
    }

    public static class PaymentProcessedPayload {
        public String paymentId;
        public Double amountCharged;
        public PaymentProcessedPayload() {}
        public PaymentProcessedPayload(String paymentId, Double amountCharged) {
            this.paymentId = paymentId;
            this.amountCharged = amountCharged;
        }
    }

    public static class PaymentFailedPayload {
        public String reason;
        public PaymentFailedPayload() {}
        public PaymentFailedPayload(String reason) {
            this.reason = reason;
        }
    }
}
