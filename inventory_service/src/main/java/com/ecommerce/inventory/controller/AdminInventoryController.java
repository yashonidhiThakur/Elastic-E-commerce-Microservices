package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.sharded.ShardedCounterProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products")
public class AdminInventoryController {

    private final StringRedisTemplate redisTemplate;
    private final ShardedCounterProperties properties;

    public AdminInventoryController(StringRedisTemplate redisTemplate, ShardedCounterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<Void> seedStock(@PathVariable("id") String productId, @RequestParam("total") int totalStock) {
        int n = properties.getCellsFor(productId);

        int baseStock = totalStock / n;
        int remainder = totalStock % n;

        for (int i = 0; i < n; i++) {
            String cellKey = String.format("inv:%s:cell:%d", productId, i);
            int stockForCell = baseStock + (i < remainder ? 1 : 0);

            // Idempotent: overwrite available and reset reserved to 0
            redisTemplate.opsForHash().put(cellKey, "available", String.valueOf(stockForCell));
            redisTemplate.opsForHash().put(cellKey, "reserved", "0");
        }

        return ResponseEntity.ok().build();
    }
}
