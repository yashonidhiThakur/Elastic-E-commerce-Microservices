package com.ecommerce.order.consumers;

import com.ecommerce.order.events.*;
import com.ecommerce.order.saga.OrderStateMachine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaConsumers {
    private static final Logger log = LoggerFactory.getLogger(SagaConsumers.class);

    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    public SagaConsumers(OrderStateMachine stateMachine, ObjectMapper objectMapper) {
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    public void onInventoryReserved(String message) {
        try {
            BaseEvent<InventoryReservedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            stateMachine.processInventoryReserved(event);
        } catch (Exception e) {
            log.error("Failed to process inventory.reserved", e);
            throw new RuntimeException(e); // Let it DLQ
        }
    }

    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    public void onPaymentProcessed(String message) {
        try {
            BaseEvent<PaymentProcessedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            stateMachine.processPaymentProcessed(event);
        } catch (Exception e) {
            log.error("Failed to process payment.processed", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "cart.cleared", groupId = "order-service-group")
    public void onCartCleared(String message) {
        try {
            BaseEvent<CartClearedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            stateMachine.processCartCleared(event);
        } catch (Exception e) {
            log.error("Failed to process cart.cleared", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "inventory.reservation_failed", groupId = "order-service-group")
    public void onInventoryFailed(String message) {
        try {
            BaseEvent<InventoryReservationFailedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            stateMachine.processInventoryFailed(event);
        } catch (Exception e) {
            log.error("Failed to process inventory.reservation_failed", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(String message) {
        try {
            BaseEvent<PaymentFailedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            stateMachine.processPaymentFailed(event);
        } catch (Exception e) {
            log.error("Failed to process payment.failed", e);
            throw new RuntimeException(e);
        }
    }
}
