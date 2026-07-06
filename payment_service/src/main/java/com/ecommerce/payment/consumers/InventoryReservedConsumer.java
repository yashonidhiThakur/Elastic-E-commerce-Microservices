package com.ecommerce.payment.consumers;

import com.ecommerce.payment.events.SagaEvents;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class InventoryReservedConsumer {

    private final DataSource authDataSource;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean sagaEnabled;

    private static final String CONSUMER_NAME = "payment_service";

    public InventoryReservedConsumer(
            @Qualifier("authDataSource") DataSource authDataSource,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${saga.enabled:false}") boolean sagaEnabled) {
        this.authDataSource = authDataSource;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.sagaEnabled = sagaEnabled;

        if (sagaEnabled) {
            initTable();
        }
    }

    private void initTable() {
        try (Connection conn = authDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS processed_events (" +
                             "event_id TEXT PRIMARY KEY, " +
                             "consumer_name TEXT, " +
                             "processed_at TEXT)")) {
            ps.execute();
        } catch (Exception e) {
            System.err.println("Failed to init processed_events table: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service-group", autoStartup = "${saga.enabled:false}")
    public void onInventoryReserved(String message) {
        if (!sagaEnabled) return;

        try {
            SagaEvents.BaseEvent<SagaEvents.InventoryReservedPayload> event = 
                objectMapper.readValue(message, new TypeReference<>() {});
            
            try (Connection conn = authDataSource.getConnection()) {
                conn.setAutoCommit(false);

                // Check idempotency
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM processed_events WHERE event_id = ? AND consumer_name = ?")) {
                    check.setString(1, event.eventId);
                    check.setString(2, CONSUMER_NAME);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            // Already processed
                            conn.rollback();
                            return;
                        }
                    }
                }

                // Deduct balance
                Double totalAmount = event.payload.totalAmount;
                try (PreparedStatement psUpdate = conn.prepareStatement(
                        "UPDATE users SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?")) {
                    psUpdate.setDouble(1, totalAmount);
                    psUpdate.setString(2, event.userId);
                    psUpdate.setDouble(3, totalAmount);
                    int updated = psUpdate.executeUpdate();

                    if (updated == 0) {
                        // Insufficient balance or user not found
                        publishFailed(event.orderId, event.userId, "Insufficient wallet balance");
                        conn.rollback();
                        return;
                    }
                }

                // Insert idempotency record
                try (PreparedStatement psInsert = conn.prepareStatement(
                        "INSERT INTO processed_events (event_id, consumer_name, processed_at) VALUES (?, ?, ?)")) {
                    psInsert.setString(1, event.eventId);
                    psInsert.setString(2, CONSUMER_NAME);
                    psInsert.setString(3, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                    psInsert.executeUpdate();
                }

                // Commit SQLite Transaction
                conn.commit();

                // Publish payment.processed (At-least-once via KafkaTemplate)
                publishProcessed(event.orderId, event.userId, totalAmount);

            } catch (Exception ex) {
                // Any other DB failure -> rollback and publish payment.failed
                publishFailed(event.orderId, event.userId, "Database error: " + ex.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process inventory.reserved", e);
        }
    }

    private void publishProcessed(String orderId, String userId, Double amount) {
        try {
            SagaEvents.PaymentProcessedPayload payload = new SagaEvents.PaymentProcessedPayload(UUID.randomUUID().toString(), amount);
            SagaEvents.BaseEvent<SagaEvents.PaymentProcessedPayload> ev = new SagaEvents.BaseEvent<>(
                    UUID.randomUUID().toString(), "payment.processed", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    orderId, userId, payload);
            kafkaTemplate.send("payment.processed", orderId, objectMapper.writeValueAsString(ev)).get();
        } catch (Exception e) {
            System.err.println("Failed to publish payment.processed: " + e.getMessage());
        }
    }

    private void publishFailed(String orderId, String userId, String reason) {
        try {
            SagaEvents.PaymentFailedPayload payload = new SagaEvents.PaymentFailedPayload(reason);
            SagaEvents.BaseEvent<SagaEvents.PaymentFailedPayload> ev = new SagaEvents.BaseEvent<>(
                    UUID.randomUUID().toString(), "payment.failed", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    orderId, userId, payload);
            kafkaTemplate.send("payment.failed", orderId, objectMapper.writeValueAsString(ev)).get();
        } catch (Exception e) {
            System.err.println("Failed to publish payment.failed: " + e.getMessage());
        }
    }
}
