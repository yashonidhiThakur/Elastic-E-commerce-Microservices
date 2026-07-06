package com.ecommerce.order.api;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.events.BaseEvent;
import com.ecommerce.order.events.OrdersCreatedPayload;
import com.ecommerce.order.persistence.OrderRepository;
import com.ecommerce.order.producers.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderController(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody OrderCreateRequest request,
                                         @RequestHeader(value = "x-user-id", required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing x-user-id"));
        }

        // Idempotency check can be done by querying orderId if provided, or idempotencyKey
        // Here we just create a new order since idempotencyKey is unique in DB
        
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(request.getTotalAmount());
        order.setIdempotencyKey(request.getIdempotencyKey() != null ? request.getIdempotencyKey() : UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING);

        if (request.getItems() != null) {
            for (OrderCreateRequest.Item reqItem : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(reqItem.getProductId());
                item.setQuantity(reqItem.getQuantity());
                item.setPriceAtAdd(reqItem.getPriceAtAdd());
                order.addItem(item);
            }
        }

        order = orderRepository.save(order);

        // Emit event
        OrdersCreatedPayload payload = new OrdersCreatedPayload(
                order.getItems().stream()
                        .map(i -> new OrdersCreatedPayload.Item(i.getProductId(), i.getQuantity(), i.getPriceAtAdd()))
                        .collect(Collectors.toList()),
                order.getTotalAmount(),
                order.getIdempotencyKey()
        );
        BaseEvent<OrdersCreatedPayload> event = BaseEvent.create("orders.created", order.getId().toString(), userId, payload);
        eventPublisher.publish("orders.created", event);

        return ResponseEntity.accepted().body(Map.of("order_id", order.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable UUID id) {
        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(Map.of("order_id", order.getId(), "status", order.getStatus())))
                .orElse(ResponseEntity.notFound().build());
    }
}
