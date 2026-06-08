package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryRepository inventoryRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void testGetInventory() throws Exception {
        when(inventoryRepository.findAll()).thenReturn(List.of(
                new Inventory("laptop", 10, 1000.0, 2),
                new Inventory("mouse", 5, 25.0, 0)
        ));

        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laptop", is(10)))
                .andExpect(jsonPath("$.mouse", is(5)));
    }

    @Test
    void testGetInventoryStock() throws Exception {
        when(inventoryRepository.findAll()).thenReturn(List.of(
                new Inventory("laptop", 10, 1000.0, 2),
                new Inventory("mouse", 5, 25.0, 0)
        ));

        mockMvc.perform(get("/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laptop.available", is(8)))
                .andExpect(jsonPath("$.laptop.price", is(1000.0)))
                .andExpect(jsonPath("$.mouse.available", is(5)))
                .andExpect(jsonPath("$.mouse.price", is(25.0)));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }
}
