package com.ecommerce.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 12;
    private static final int REPLICAS = 1; // dev

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (r, e) -> new org.apache.kafka.common.TopicPartition(r.topic() + ".dlq", r.partition()));
        // N=3 retries, fixed 1 second backoff
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    @Bean
    public NewTopic ordersCreatedTopic() { return TopicBuilder.name("orders.created").partitions(PARTITIONS).replicas(REPLICAS).build(); }
    
    @Bean
    public NewTopic ordersCreatedDlqTopic() { return TopicBuilder.name("orders.created.dlq").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic inventoryReservedTopic() { return TopicBuilder.name("inventory.reserved").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic inventoryReservedDlqTopic() { return TopicBuilder.name("inventory.reserved.dlq").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic paymentProcessedTopic() { return TopicBuilder.name("payment.processed").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic paymentProcessedDlqTopic() { return TopicBuilder.name("payment.processed.dlq").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic orderConfirmedTopic() { return TopicBuilder.name("order.confirmed").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic cartClearedTopic() { return TopicBuilder.name("cart.cleared").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic cartClearedDlqTopic() { return TopicBuilder.name("cart.cleared.dlq").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic inventoryFailedTopic() { return TopicBuilder.name("inventory.reservation_failed").partitions(PARTITIONS).replicas(REPLICAS).build(); }

    @Bean
    public NewTopic paymentFailedTopic() { return TopicBuilder.name("payment.failed").partitions(PARTITIONS).replicas(REPLICAS).build(); }
}
