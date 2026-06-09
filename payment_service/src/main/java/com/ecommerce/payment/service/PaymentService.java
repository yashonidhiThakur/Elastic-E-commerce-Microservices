package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.CartItem;
import com.ecommerce.payment.dto.CheckoutRequest;
import com.ecommerce.payment.dto.OrderPaidEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final DataSource authDataSource;
    private final DataSource inventoryDataSource;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${cart.url}")
    private String cartUrl;

    public PaymentService(
            @Qualifier("authDataSource") DataSource authDataSource,
            @Qualifier("inventoryDataSource") DataSource inventoryDataSource,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.authDataSource = authDataSource;
        this.inventoryDataSource = inventoryDataSource;
        this.kafkaTemplate = kafkaTemplate;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public Double getBalance(String xUserId) throws Exception {
        try (Connection conn = authDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT wallet_balance FROM users WHERE id = ?")) {
            ps.setString(1, xUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("wallet_balance");
                }
                return null;
            }
        }
    }

    public Map<String, Object> checkout(CheckoutRequest req, String xUserId) {
        List<CartItem> reservedItems = new ArrayList<>();
        double totalCost = 0.0;

        try (Connection invConn = inventoryDataSource.getConnection();
             Connection authConn = authDataSource.getConnection()) {

            invConn.setAutoCommit(false);
            authConn.setAutoCommit(false);

            try {
                // STEP 1: Atomic inventory reservation
                for (CartItem cartItem : req.getCartItems()) {
                    try (PreparedStatement psSelect = invConn.prepareStatement("SELECT price FROM inventory WHERE item = ?")) {
                        psSelect.setString(1, cartItem.getItem());
                        try (ResultSet rs = psSelect.executeQuery()) {
                            if (!rs.next()) {
                                throw new Exception("Item " + cartItem.getItem() + " not found");
                            }
                            double price = rs.getDouble("price");
                            totalCost += price * cartItem.getQuantity();
                        }
                    }

                    try (PreparedStatement psUpdate = invConn.prepareStatement(
                            "UPDATE inventory SET reserved = reserved + ? WHERE item = ? AND (stock - reserved) >= ?")) {
                        psUpdate.setInt(1, cartItem.getQuantity());
                        psUpdate.setString(2, cartItem.getItem());
                        psUpdate.setInt(3, cartItem.getQuantity());
                        int updated = psUpdate.executeUpdate();
                        if (updated == 0) {
                            throw new Exception("Item " + cartItem.getItem() + " is out of stock");
                        }
                    }
                    reservedItems.add(cartItem);
                }

                // STEP 2: Atomic wallet deduction
                try (PreparedStatement psAuthUpdate = authConn.prepareStatement(
                        "UPDATE users SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?")) {
                    psAuthUpdate.setDouble(1, totalCost);
                    psAuthUpdate.setString(2, xUserId);
                    psAuthUpdate.setDouble(3, totalCost);
                    int authUpdated = psAuthUpdate.executeUpdate();
                    if (authUpdated == 0) {
                        throw new Exception("Insufficient wallet balance");
                    }
                }

                // STEP 3: Both succeeded - commit everything
                authConn.commit();

                // Now execute the final update on inventory
                for (CartItem cartItem : req.getCartItems()) {
                    try (PreparedStatement psFinalInv = invConn.prepareStatement(
                            "UPDATE inventory SET stock = stock - ?, reserved = reserved - ? WHERE item = ?")) {
                        psFinalInv.setInt(1, cartItem.getQuantity());
                        psFinalInv.setInt(2, cartItem.getQuantity());
                        psFinalInv.setString(3, cartItem.getItem());
                        psFinalInv.executeUpdate();
                    }
                }
                invConn.commit();

                // Publish order.paid to Kafka
                for (CartItem cartItem : req.getCartItems()) {
                    OrderPaidEvent event = new OrderPaidEvent(cartItem.getItem(), cartItem.getQuantity(), xUserId);
                    String eventJson = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send("orders", eventJson);
                }

                // Call cart-service DELETE /cart/clear
                try {
                    restClient.delete()
                            .uri(cartUrl + "/cart/clear")
                            .header("x-user-id", xUserId)
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception e) {
                    System.err.println("Failed to clear cart: " + e.getMessage());
                }

                // Fetch new balance
                Double newBalance = null;
                try (PreparedStatement ps = authConn.prepareStatement("SELECT wallet_balance FROM users WHERE id = ?")) {
                    ps.setString(1, xUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            newBalance = rs.getDouble("wallet_balance");
                        }
                    }
                }

                return Map.of("success", true, "new_balance", newBalance != null ? newBalance : 0.0);

            } catch (Exception e) {
                // Rollback wallet
                authConn.rollback();
                
                // Rollback inventory: release reservations
                for (CartItem cartItem : reservedItems) {
                    try (PreparedStatement psRollbackInv = invConn.prepareStatement(
                            "UPDATE inventory SET reserved = reserved - ? WHERE item = ?")) {
                        psRollbackInv.setInt(1, cartItem.getQuantity());
                        psRollbackInv.setString(2, cartItem.getItem());
                        psRollbackInv.executeUpdate();
                    }
                }
                invConn.commit();

                return Map.of("success", false, "error", e.getMessage());
            }

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
