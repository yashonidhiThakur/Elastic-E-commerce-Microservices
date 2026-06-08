package com.ecommerce.inventory.config;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryDataSeeder implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    public InventoryDataSeeder(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (inventoryRepository.count() == 0) {
            System.out.println("[-] Inventory database is empty. Seeding default items...");
            inventoryRepository.saveAll(List.of(
                    new Inventory("laptop", 100, 1000.0, 0),
                    new Inventory("mouse", 100, 25.0, 0),
                    new Inventory("keyboard", 100, 75.0, 0)
            ));
            System.out.println("[✓] Default inventory seeded successfully.");
        }
    }
}
