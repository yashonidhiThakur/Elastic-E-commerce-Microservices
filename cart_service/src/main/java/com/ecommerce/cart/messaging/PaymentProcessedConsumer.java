package com.ecommerce.cart.messaging;

import com.ecommerce.cart.service.CartService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class PaymentProcessedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessedConsumer.class);

    private final CartService cartService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean sagaEnabled;

    public PaymentProcessedConsumer(
            CartService cartService,
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${saga.enabled:false}") boolean sagaEnabled) {
        this.cartService = cartService;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.sagaEnabled = sagaEnabled;
    }

    @KafkaListener(topics = "payment.processed", groupId = "cart-service-group", autoStartup = "${saga.enabled:false}")
    public void onPaymentProcessed(String message) {
        if (!sagaEnabled) return;
        try {
            SagaEvents.BaseEvent<SagaEvents.PaymentProcessedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            
            // Check idempotency in Redis (expires in 7 days)
            String idempotencyKey = "processed_events:cart:" + event.eventId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofDays(7));
            if (Boolean.FALSE.equals(isNew)) {
                return; // Already processed
            }

            // Clear cart
            cartService.clearCart(event.userId);

            // Publish cart.cleared
            publishCartCleared(event.orderId, event.userId);

        } catch (Exception e) {
            log.error("Failed to process payment.processed", e);
            throw new RuntimeException(e);
        }
    }

    private void publishCartCleared(String orderId, String userId) {
        try {
            SagaEvents.BaseEvent<Void> ev = new SagaEvents.BaseEvent<>();
            ev.eventId = UUID.randomUUID().toString();
            ev.eventType = "cart.cleared";
            ev.occurredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            ev.orderId = orderId;
            ev.userId = userId;
            ev.payload = null;

            kafkaTemplate.send("cart.cleared", orderId, objectMapper.writeValueAsString(ev)).get();
        } catch (Exception e) {
            log.error("Failed to publish cart.cleared", e);
        }
    }
}
