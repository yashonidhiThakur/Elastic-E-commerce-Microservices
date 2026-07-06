package com.ecommerce.order.api;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    private List<Item> items;
    private Double totalAmount;
    private String idempotencyKey;
    private String userId;

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Data
    public static class Item {
        private String productId;
        private Integer quantity;
        private Double priceAtAdd;
        
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPriceAtAdd() { return priceAtAdd; }
        public void setPriceAtAdd(Double priceAtAdd) { this.priceAtAdd = priceAtAdd; }
    }
}
