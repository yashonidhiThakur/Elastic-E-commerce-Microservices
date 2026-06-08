package com.ecommerce.inventory.messaging;

import com.ecommerce.inventory.dto.OrderPaidEvent;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderConsumer {

    private final InventoryRepository inventoryRepository;
    private final InventoryPublisher inventoryPublisher;
    private final ObjectMapper objectMapper;

    public OrderConsumer(InventoryRepository inventoryRepository, InventoryPublisher inventoryPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryPublisher = inventoryPublisher;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "orders")
    public void consume(String message) {
        System.out.println(" [x] Received order.paid event: " + message);
        try {
            OrderPaidEvent event = objectMapper.readValue(message, OrderPaidEvent.class);
            String item = event.item();

            Optional<Inventory> itemOpt = inventoryRepository.findById(item);
            if (itemOpt.isPresent()) {
                Inventory inventory = itemOpt.get();
                System.out.println(" [✓] Acknowledged " + item + " payment. Current stock: " + inventory.getStock());

                if (inventory.getStock() - inventory.getReserved() <= 0) {
                    inventoryPublisher.publishDepleted(item);
                }
            } else {
                System.out.println(" [!] Item '" + item + "' not found in inventory.");
            }
        } catch (Exception e) {
            System.err.println(" [!] Error processing order.paid event: " + e.getMessage());
        }
    }
}
