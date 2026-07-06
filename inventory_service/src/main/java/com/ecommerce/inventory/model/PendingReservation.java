package com.ecommerce.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_reservations")
public class PendingReservation {

    @Id
    private String orderId;

    @Column(columnDefinition = "TEXT")
    private String payloadJson; // Serialized list of items with cellsUsed

    public PendingReservation() {}

    public PendingReservation(String orderId, String payloadJson) {
        this.orderId = orderId;
        this.payloadJson = payloadJson;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
