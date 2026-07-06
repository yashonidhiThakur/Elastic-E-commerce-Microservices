package com.ecommerce.order.producers;

import com.ecommerce.order.domain.OutboxEvent;
import com.ecommerce.order.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll.interval:1000}")
    public void processOutbox() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        for (OutboxEvent event : events) {
            try {
                // Partition by orderId
                kafkaTemplate.send(event.getTopic(), event.getOrderId(), event.getPayload()).get();
                outboxEventRepository.delete(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id: {}", event.getId(), e);
            }
        }
    }
}
