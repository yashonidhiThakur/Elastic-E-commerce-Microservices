package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderPaidEvent {
    private String item;
    private int quantity;
    @JsonProperty("user_id")
    private String userId;

    public OrderPaidEvent(String item, int quantity, String userId) {
        this.item = item;
        this.quantity = quantity;
        this.userId = userId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
