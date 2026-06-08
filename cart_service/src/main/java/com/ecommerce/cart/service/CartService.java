package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.InventoryStockResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final RestClient inventoryRestClient;
    private final ObjectMapper objectMapper;

    public CartService(StringRedisTemplate redisTemplate, RestClient inventoryRestClient) {
        this.redisTemplate = redisTemplate;
        this.inventoryRestClient = inventoryRestClient;
        this.objectMapper = new ObjectMapper();
    }

    private String getCartKey(String userId) {
        return "cart:" + userId;
    }

    public Map<String, Integer> getCart(String userId) {
        String key = getCartKey(userId);
        String cartData = redisTemplate.opsForValue().get(key);
        if (cartData == null || cartData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(cartData, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void saveCart(String userId, Map<String, Integer> cart) {
        String key = getCartKey(userId);
        try {
            String cartData = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(key, cartData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize cart", e);
        }
    }

    public Map<String, Integer> addItem(String userId, String item, Integer quantity) {
        Map<String, Integer> cart = getCart(userId);
        cart.put(item, cart.getOrDefault(item, 0) + quantity);
        saveCart(userId, cart);
        return cart;
    }

    public Map<String, Integer> removeItem(String userId, String item) {
        Map<String, Integer> cart = getCart(userId);
        if (cart.containsKey(item)) {
            cart.remove(item);
            saveCart(userId, cart);
        }
        return cart;
    }

    public void clearCart(String userId) {
        String key = getCartKey(userId);
        redisTemplate.delete(key);
    }

    public CartResponse getEnrichedCart(String userId) {
        Map<String, Integer> cart = getCart(userId);
        List<CartItemResponse> enrichedItems = new ArrayList<>();
        double totalCost = 0.0;

        Map<String, InventoryStockResponse> stockData;
        try {
            stockData = inventoryRestClient.get()
                    .uri("/inventory/stock")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, InventoryStockResponse>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Inventory service unavailable", e);
        }

        if (stockData == null) {
            stockData = new HashMap<>();
        }

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String item = entry.getKey();
            int qty = entry.getValue();

            InventoryStockResponse itemStock = stockData.get(item);
            int availableQty = 0;
            double price = 0.0;

            if (itemStock != null) {
                if (itemStock.available() != null) {
                    availableQty = itemStock.available();
                }
                if (itemStock.price() != null) {
                    price = itemStock.price();
                }
            }

            boolean isAvailable = availableQty >= qty;
            enrichedItems.add(new CartItemResponse(item, qty, price, isAvailable));

            if (isAvailable) {
                totalCost += price * qty;
            }
        }

        return new CartResponse(enrichedItems, totalCost);
    }
}
