package com.ecommerce.order.saga;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.ProcessedEvent;
import com.ecommerce.order.events.*;
import com.ecommerce.order.persistence.OrderRepository;
import com.ecommerce.order.persistence.ProcessedEventRepository;
import com.ecommerce.order.producers.OrderEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class OrderStateMachine {
    private static final Logger log = LoggerFactory.getLogger(OrderStateMachine.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OrderStateMachine(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository, OrderEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    private static final String CONSUMER_NAME = "order_service";

    @Transactional
    public void processInventoryReserved(BaseEvent<InventoryReservedPayload> event) {
        if (checkIdempotency(event.getEventId())) return;

        orderRepository.findById(java.util.UUID.fromString(event.getOrderId())).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.RESERVED);
                orderRepository.save(order);
                log.info("Order {} transitioned to RESERVED", order.getId());
            }
        });

        markProcessed(event.getEventId());
    }

    @Transactional
    public void processPaymentProcessed(BaseEvent<PaymentProcessedPayload> event) {
        if (checkIdempotency(event.getEventId())) return;

        orderRepository.findById(java.util.UUID.fromString(event.getOrderId())).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.RESERVED) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Order {} transitioned to PAID", order.getId());

                // Emits order.confirmed
                OrderConfirmedPayload payload = new OrderConfirmedPayload(
                        order.getItems().stream()
                                .map(item -> new OrderConfirmedPayload.Item(item.getProductId(), item.getQuantity()))
                                .collect(Collectors.toList()),
                        order.getTotalAmount()
                );
                eventPublisher.publish("order.confirmed", BaseEvent.create("order.confirmed", order.getId().toString(), order.getUserId(), payload));
            }
        });

        markProcessed(event.getEventId());
    }

    @Transactional
    public void processCartCleared(BaseEvent<CartClearedPayload> event) {
        if (checkIdempotency(event.getEventId())) return;

        orderRepository.findById(java.util.UUID.fromString(event.getOrderId())).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PAID) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                log.info("Order {} transitioned to CONFIRMED", order.getId());
            }
        });

        markProcessed(event.getEventId());
    }

    @Transactional
    public void processInventoryFailed(BaseEvent<InventoryReservationFailedPayload> event) {
        if (checkIdempotency(event.getEventId())) return;
        cancelOrder(event.getOrderId());
        markProcessed(event.getEventId());
    }

    @Transactional
    public void processPaymentFailed(BaseEvent<PaymentFailedPayload> event) {
        if (checkIdempotency(event.getEventId())) return;
        cancelOrder(event.getOrderId());
        markProcessed(event.getEventId());
    }

    private void cancelOrder(String orderId) {
        orderRepository.findById(java.util.UUID.fromString(orderId)).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("Order {} transitioned to CANCELLED", order.getId());
            }
        });
    }

    private boolean checkIdempotency(String eventId) {
        return processedEventRepository.existsByEventIdAndConsumerName(eventId, CONSUMER_NAME);
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_NAME));
    }
}
