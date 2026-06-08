package com.ecommerce.inventory.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishDepleted(String item) {
        try {
            String event = objectMapper.writeValueAsString(Map.of("item", item));
            kafkaTemplate.send("inventory-events", event);
            System.out.println(" [x] Published inventory.depleted for " + item);
        } catch (Exception e) {
            System.err.println(" [!] Failed to publish depleted event: " + e.getMessage());
        }
    }
}
