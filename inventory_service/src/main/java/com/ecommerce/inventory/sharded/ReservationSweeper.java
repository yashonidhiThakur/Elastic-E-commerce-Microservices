package com.ecommerce.inventory.sharded;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);

    private final StringRedisTemplate redisTemplate;
    private final LuaScripts luaScripts;
    private final Counter releasedCounter;

    public ReservationSweeper(StringRedisTemplate redisTemplate, LuaScripts luaScripts, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.luaScripts = luaScripts;
        this.releasedCounter = Counter.builder("inv.shard.sweeper.released")
                .description("Number of expired reservations released by sweeper")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 30000)
    public void sweep() {
        log.info("Starting reservation sweeper...");
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(redisConnection ->
                redisConnection.scan(ScanOptions.scanOptions().match("inv:*:cell:*:res:*").count(100).build())
        )) {
            
            long currentTime = System.currentTimeMillis() / 1000;

            while (cursor != null && cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                if (rawKey == null) continue;
                String resKey = new String(rawKey);
                String value = redisTemplate.opsForValue().get(resKey);
                if (value != null && value.contains(":")) {
                    String[] parts = value.split(":");
                    int qty = Integer.parseInt(parts[0]);
                    long expiryTimestamp = Long.parseLong(parts[1]);

                    if (currentTime > expiryTimestamp) {
                        releaseExpiredReservation(resKey, qty);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during reservation sweeping", e);
        }
    }

    private void releaseExpiredReservation(String resKey, int qty) {
        // resKey format: inv:{productId}:cell:{cellIndex}:res:{userId}:{nonce}
        String[] keyParts = resKey.split(":");
        if (keyParts.length >= 6) {
            String productId = keyParts[1];
            String cellIndex = keyParts[3];
            String cellKey = String.format("inv:%s:cell:%s", productId, cellIndex);

            Long result = redisTemplate.execute(
                    luaScripts.getReleaseScript(),
                    List.of(cellKey, resKey),
                    String.valueOf(qty)
            );

            if (result != null && result == 1L) {
                releasedCounter.increment();
                log.info("Released expired reservation for product {}, cell {}, qty {}", productId, cellIndex, qty);
            }
        }
    }
}
