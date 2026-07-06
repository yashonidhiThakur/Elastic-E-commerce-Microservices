package com.ecommerce.order;

import com.ecommerce.order.api.OrderCreateRequest;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.events.BaseEvent;
import com.ecommerce.order.events.InventoryReservedPayload;
import com.ecommerce.order.persistence.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testOrderHappyPath() throws Exception {
        // 1. Create order
        OrderCreateRequest request = new OrderCreateRequest();
        request.setTotalAmount(100.0);
        OrderCreateRequest.Item item = new OrderCreateRequest.Item();
        item.setProductId("item1");
        item.setQuantity(2);
        item.setPriceAtAdd(50.0);
        request.setItems(List.of(item));

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-user-id", "user1");

        ResponseEntity<Map> response = restTemplate.postForEntity("/orders", new HttpEntity<>(request, headers), Map.class);
        assertEquals(202, response.getStatusCode().value());
        
        String orderIdStr = (String) response.getBody().get("order_id");
        assertNotNull(orderIdStr);
        UUID orderId = UUID.fromString(orderIdStr);

        // Wait for outbox to publish, then manually simulate inventory.reserved
        Thread.sleep(2000); // give time for outbox processor
        
        InventoryReservedPayload irp = new InventoryReservedPayload();
        BaseEvent<InventoryReservedPayload> irEvent = BaseEvent.create("inventory.reserved", orderId.toString(), "user1", irp);
        kafkaTemplate.send("inventory.reserved", orderId.toString(), objectMapper.writeValueAsString(irEvent)).get();

        // Wait for consumer and state transition
        Thread.sleep(1000);
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }
}
