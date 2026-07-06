package com.ecommerce.cart.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentProcessedPayload {
        public String paymentId;
        public Double amountCharged;
    }
}
