package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {
    private String eventId;
    private String eventType;
    private String occurredAt;
    private String orderId;
    private String userId;
    private T payload;



    public static <T> BaseEvent<T> create(String eventType, String orderId, String userId, T payload) {
        return new BaseEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                orderId,
                userId,
                payload
        );
    }
}
