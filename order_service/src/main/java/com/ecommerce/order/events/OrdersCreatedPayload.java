package com.ecommerce.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdersCreatedPayload {
    private List<Item> items;
    private Double totalAmount;
    private String idempotencyKey;
    public OrdersCreatedPayload() {}

    public OrdersCreatedPayload(List<Item> items, Double totalAmount, String idempotencyKey) {
        this.items = items;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
    }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private Integer quantity;
        private Double priceAtAdd;
        
        public Item() {}
        public Item(String productId, Integer quantity, Double priceAtAdd) {
            this.productId = productId;
            this.quantity = quantity;
            this.priceAtAdd = priceAtAdd;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPriceAtAdd() { return priceAtAdd; }
        public void setPriceAtAdd(Double priceAtAdd) { this.priceAtAdd = priceAtAdd; }
    }
}
