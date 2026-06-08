package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RestClient restClient;

    @Test
    void testAddItemSuccess() throws Exception {
        String requestBody = "{\"item\": \"laptop\", \"quantity\": 2}";

        when(cartService.addItem(anyString(), anyString(), anyInt()))
                .thenReturn(Map.of("laptop", 2));

        mockMvc.perform(post("/cart/add")
                        .header("x-user-id", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.cart.laptop", is(2)));
    }

    @Test
    void testAddItemMissingHeader() throws Exception {
        String requestBody = "{\"item\": \"laptop\", \"quantity\": 2}";

        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("Missing user_id")));
    }

    @Test
    void testRemoveItemSuccess() throws Exception {
        String requestBody = "{\"item\": \"laptop\"}";

        when(cartService.removeItem(anyString(), anyString()))
                .thenReturn(Map.of());

        mockMvc.perform(post("/cart/remove")
                        .header("x-user-id", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testClearCartSuccess() throws Exception {
        mockMvc.perform(delete("/cart/clear")
                        .header("x-user-id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testViewCartSuccess() throws Exception {
        CartResponse cartResponse = new CartResponse(
                List.of(new CartItemResponse("laptop", 2, 1000.0, true)),
                2000.0
        );

        when(cartService.getEnrichedCart("123")).thenReturn(cartResponse);

        mockMvc.perform(get("/cart")
                        .header("x-user-id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(2000.0)))
                .andExpect(jsonPath("$.items[0].item", is("laptop")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].price", is(1000.0)))
                .andExpect(jsonPath("$.items[0].available", is(true)));
    }

    @Test
    void testViewCartInventoryServiceUnavailable() throws Exception {
        when(cartService.getEnrichedCart("123"))
                .thenThrow(new RuntimeException("Inventory service unavailable"));

        mockMvc.perform(get("/cart")
                        .header("x-user-id", "123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", is("Inventory service unavailable")));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }
}
