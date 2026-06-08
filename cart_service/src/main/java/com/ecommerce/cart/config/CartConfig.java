package com.ecommerce.cart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CartConfig {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Bean
    public RestClient inventoryRestClient() {
        return RestClient.builder()
                .baseUrl(inventoryServiceUrl)
                .build();
    }
}
