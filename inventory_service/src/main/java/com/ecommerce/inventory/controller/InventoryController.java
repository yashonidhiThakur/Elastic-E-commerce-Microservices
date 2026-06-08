package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryStockResponse;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public ResponseEntity<?> getInventory() {
        List<Inventory> items = inventoryRepository.findAll();
        Map<String, Integer> response = items.stream()
                .collect(Collectors.toMap(Inventory::getItem, Inventory::getStock));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock")
    public ResponseEntity<?> getInventoryStock() {
        List<Inventory> items = inventoryRepository.findAll();
        Map<String, InventoryStockResponse> response = items.stream()
                .collect(Collectors.toMap(
                        Inventory::getItem,
                        item -> new InventoryStockResponse(
                                Math.max(0, item.getStock() - item.getReserved()),
                                item.getPrice()
                        )
                ));
        return ResponseEntity.ok(response);
    }
}
