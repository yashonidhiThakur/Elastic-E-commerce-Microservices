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

    public BaseEvent() {}

    public BaseEvent(String eventId, String eventType, String occurredAt, String orderId, String userId, T payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.orderId = orderId;
        this.userId = userId;
        this.payload = payload;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }

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
