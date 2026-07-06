package com.ecommerce.inventory.messaging;

import com.ecommerce.inventory.dto.SagaEvents;
import com.ecommerce.inventory.model.PendingReservation;
import com.ecommerce.inventory.model.ProcessedEvent;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.PendingReservationRepository;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import com.ecommerce.inventory.sharded.ShardedCounterProperties;
import com.ecommerce.inventory.sharded.ShardedCounterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SagaConsumers {

    private static final Logger log = LoggerFactory.getLogger(SagaConsumers.class);
    private static final String CONSUMER_NAME = "inventory_service";

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ShardedCounterService shardedCounterService;
    private final ShardedCounterProperties shardedCounterProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean sagaEnabled;

    private final PendingReservationRepository pendingReservationRepository;

    public SagaConsumers(
            InventoryRepository inventoryRepository,
            ProcessedEventRepository processedEventRepository,
            PendingReservationRepository pendingReservationRepository,
            ShardedCounterService shardedCounterService,
            ShardedCounterProperties shardedCounterProperties,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${saga.enabled:false}") boolean sagaEnabled) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
        this.pendingReservationRepository = pendingReservationRepository;
        this.shardedCounterService = shardedCounterService;
        this.shardedCounterProperties = shardedCounterProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.sagaEnabled = sagaEnabled;
    }

    @KafkaListener(topics = "orders.created", groupId = "inventory-service-group", autoStartup = "${saga.enabled:false}")
    @Transactional
    public void onOrdersCreated(String message) {
        if (!sagaEnabled) return;
        try {
            SagaEvents.BaseEvent<SagaEvents.OrdersCreatedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            if (checkIdempotency(event.eventId)) return;

            List<SagaEvents.InventoryReservedPayload.ReservedItem> reservedItems = new ArrayList<>();
            List<String> failedItems = new ArrayList<>();

            for (SagaEvents.OrdersCreatedPayload.Item item : event.payload.items) {
                if (shardedCounterProperties.isEnabledFor(item.productId)) {
                    int cellId = shardedCounterService.reserve(item.productId, item.quantity, event.userId, 1800);
                    if (cellId >= 0) {
                        reservedItems.add(new SagaEvents.InventoryReservedPayload.ReservedItem(item.productId, item.quantity, List.of(cellId)));
                    } else {
                        failedItems.add(item.productId);
                    }
                } else {
                    int updated = inventoryRepository.reserveStock(item.productId, item.quantity);
                    if (updated > 0) {
                        reservedItems.add(new SagaEvents.InventoryReservedPayload.ReservedItem(item.productId, item.quantity, List.of()));
                    } else {
                        failedItems.add(item.productId);
                    }
                }
            }

            if (!failedItems.isEmpty()) {
                // Rollback reservations
                for (SagaEvents.InventoryReservedPayload.ReservedItem ri : reservedItems) {
                    if (shardedCounterProperties.isEnabledFor(ri.productId)) {
                        shardedCounterService.release(ri.productId, ri.quantity, ri.cellsUsed, event.userId);
                    } else {
                        inventoryRepository.releaseStock(ri.productId, ri.quantity);
                    }
                }
                publishReservationFailed(event.orderId, event.userId, "Out of stock", failedItems);
            } else {
                pendingReservationRepository.save(new PendingReservation(event.orderId, objectMapper.writeValueAsString(reservedItems)));
                publishReserved(event.orderId, event.userId, reservedItems, event.payload.totalAmount);
            }

            markProcessed(event.eventId);

        } catch (Exception e) {
            log.error("Error processing orders.created", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.processed", groupId = "inventory-service-group", autoStartup = "${saga.enabled:false}")
    @Transactional
    public void onPaymentProcessed(String message) {
        if (!sagaEnabled) return;
        try {
            SagaEvents.BaseEvent<SagaEvents.PaymentProcessedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            if (checkIdempotency(event.eventId)) return;

            pendingReservationRepository.findById(event.orderId).ifPresent(pending -> {
                try {
                    List<SagaEvents.InventoryReservedPayload.ReservedItem> reservedItems = objectMapper.readValue(pending.getPayloadJson(), new TypeReference<>() {});
                    for (SagaEvents.InventoryReservedPayload.ReservedItem ri : reservedItems) {
                        if (shardedCounterProperties.isEnabledFor(ri.productId)) {
                            shardedCounterService.commit(ri.productId, ri.quantity, ri.cellsUsed, event.userId);
                        } else {
                            inventoryRepository.commitStock(ri.productId, ri.quantity);
                        }
                    }
                    pendingReservationRepository.delete(pending);
                } catch (Exception e) {
                    log.error("Failed to parse pending reservation", e);
                }
            });

            markProcessed(event.eventId);
        } catch (Exception e) {
            log.error("Error processing payment.processed", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "inventory-service-group", autoStartup = "${saga.enabled:false}")
    @Transactional
    public void onPaymentFailed(String message) {
        if (!sagaEnabled) return;
        try {
            SagaEvents.BaseEvent<SagaEvents.PaymentFailedPayload> event = objectMapper.readValue(message, new TypeReference<>() {});
            if (checkIdempotency(event.eventId)) return;

            pendingReservationRepository.findById(event.orderId).ifPresent(pending -> {
                try {
                    List<SagaEvents.InventoryReservedPayload.ReservedItem> reservedItems = objectMapper.readValue(pending.getPayloadJson(), new TypeReference<>() {});
                    for (SagaEvents.InventoryReservedPayload.ReservedItem ri : reservedItems) {
                        if (shardedCounterProperties.isEnabledFor(ri.productId)) {
                            shardedCounterService.release(ri.productId, ri.quantity, ri.cellsUsed, event.userId);
                        } else {
                            inventoryRepository.releaseStock(ri.productId, ri.quantity);
                        }
                    }
                    pendingReservationRepository.delete(pending);
                } catch (Exception e) {
                    log.error("Failed to parse pending reservation", e);
                }
            });

            markProcessed(event.eventId);
        } catch (Exception e) {
            log.error("Error processing payment.failed", e);
            throw new RuntimeException(e);
        }
    }

    private boolean checkIdempotency(String eventId) {
        return processedEventRepository.existsByEventIdAndConsumerName(eventId, CONSUMER_NAME);
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_NAME));
    }

    private void publishReserved(String orderId, String userId, List<SagaEvents.InventoryReservedPayload.ReservedItem> items, Double totalAmount) {
        try {
            SagaEvents.InventoryReservedPayload payload = new SagaEvents.InventoryReservedPayload(items, totalAmount);
            SagaEvents.BaseEvent<SagaEvents.InventoryReservedPayload> ev = new SagaEvents.BaseEvent<>(
                    UUID.randomUUID().toString(), "inventory.reserved", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    orderId, userId, payload);
            kafkaTemplate.send("inventory.reserved", orderId, objectMapper.writeValueAsString(ev)).get();
        } catch (Exception e) {
            log.error("Failed to publish inventory.reserved", e);
        }
    }

    private void publishReservationFailed(String orderId, String userId, String reason, List<String> failedItems) {
        try {
            SagaEvents.InventoryReservationFailedPayload payload = new SagaEvents.InventoryReservationFailedPayload(reason, failedItems);
            SagaEvents.BaseEvent<SagaEvents.InventoryReservationFailedPayload> ev = new SagaEvents.BaseEvent<>(
                    UUID.randomUUID().toString(), "inventory.reservation_failed", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    orderId, userId, payload);
            kafkaTemplate.send("inventory.reservation_failed", orderId, objectMapper.writeValueAsString(ev)).get();
        } catch (Exception e) {
            log.error("Failed to publish inventory.reservation_failed", e);
        }
    }
}
