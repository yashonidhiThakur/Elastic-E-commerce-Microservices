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
    public PaymentService(@Qualifier("authDataSource") DataSource authDataSource) {
        this.authDataSource = authDataSource;
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


}
