package com.ecommerce.inventory.sharded;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class ShardedCounterServiceIT {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("inventory.sharded.enabled.PROD_1", () -> true);
        registry.add("inventory.sharded.enabled.PROD_2", () -> true);
        registry.add("inventory.sharded.enabled.PROD_3", () -> true);
    }

    @Autowired
    private ShardedCounterService service;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ReservationSweeper sweeper;

    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void testParallelReservationsNoOversell() throws InterruptedException {
        String productId = "PROD_1";
        int cells = 10;
        int totalStock = 50;
        
        // Seed stock evenly (5 per cell)
        for (int i = 0; i < cells; i++) {
            redisTemplate.opsForHash().put("inv:" + productId + ":cell:" + i, "available", "5");
            redisTemplate.opsForHash().put("inv:" + productId + ":cell:" + i, "reserved", "0");
        }

        assertEquals(50, service.getAvailable(productId));

        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    int cell = service.reserve(productId, 1, UUID.randomUUID().toString(), 60);
                    if (cell != -1) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(50, successCount.get());
        assertEquals(50, failureCount.get());
        assertEquals(0, service.getAvailable(productId));
    }

    @Test
    void testSweeperReleasesExpiredReservations() throws InterruptedException {
        String productId = "PROD_2";
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:0", "available", "10");
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:0", "reserved", "0");

        // Reserve with 1 second TTL
        String userId = "user1";
        int cell = service.reserve(productId, 2, userId, 1);
        assertEquals(0, cell);
        assertEquals(8, service.getAvailable(productId));

        // Wait for it to expire logically
        Thread.sleep(1500);

        // Run sweeper
        sweeper.sweep();

        assertEquals(10, service.getAvailable(productId));
    }

    @Test
    void testBoundedRetryConcentratedStock() throws InterruptedException {
        String productId = "PROD_3";
        int cells = 10;
        
        // Concentrate stock in cell 0 and cell 1
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:0", "available", "90");
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:0", "reserved", "0");
        
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:1", "available", "10");
        redisTemplate.opsForHash().put("inv:" + productId + ":cell:1", "reserved", "0");
        
        for (int i = 2; i < cells; i++) {
            redisTemplate.opsForHash().put("inv:" + productId + ":cell:" + i, "available", "0");
            redisTemplate.opsForHash().put("inv:" + productId + ":cell:" + i, "reserved", "0");
        }

        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    int cell = service.reserve(productId, 1, UUID.randomUUID().toString(), 60);
                    if (cell != -1) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(100, successCount.get());
        assertEquals(0, service.getAvailable(productId));
    }
}
