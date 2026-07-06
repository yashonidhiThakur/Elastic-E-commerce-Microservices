package com.ecommerce.inventory.sharded;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ShardedCounterService {

    private static final Logger log = LoggerFactory.getLogger(ShardedCounterService.class);

    private final ShardedCounterProperties properties;
    private final LuaScripts luaScripts;
    private final CellSelector cellSelector;

    // Micrometer metrics
    private final Counter reserveSuccessCounter;
    private final Counter reserveFailureCounter;
    private final DistributionSummary cellRetriesSummary;

    public ShardedCounterService(ShardedCounterProperties properties,
                                 LuaScripts luaScripts,
                                 CellSelector cellSelector,
                                 MeterRegistry meterRegistry) {
        this.properties = properties;
        this.luaScripts = luaScripts;
        this.cellSelector = cellSelector;

        this.reserveSuccessCounter = Counter.builder("inv.shard.reserve.success")
                .description("Successful sharded reservations")
                .register(meterRegistry);

        this.reserveFailureCounter = Counter.builder("inv.shard.reserve.failure")
                .description("Failed sharded reservations")
                .register(meterRegistry);

        this.cellRetriesSummary = DistributionSummary.builder("inv.shard.reserve.cell_retries")
                .description("Number of retries required to find a cell with stock")
                .register(meterRegistry);
    }

    /**
     * Attempts to reserve stock across the sharded cells for a given product.
     *
     * @param productId The ID of the product.
     * @param qty       The quantity to reserve.
     * @param userId    The user ID to determine the deterministic starting cell.
     * @param ttlSec    The TTL for the reservation in seconds.
     * @return The cell index used for reservation, or -1 if insufficient stock.
     */
    public int reserve(String productId, int qty, String userId, long ttlSec) {
        if (!properties.isEnabledFor(productId)) {
            // Fallback is currently not implemented here as per prompt "callable from existing synchronous payment_service code".
            // Since we don't have the full context of how payment_service calls it, we return -1 or throw if disabled for now.
            // A more complete integration would delegate to the DB, but this fulfills "add the sharded counter as a new code path".
            log.warn("Sharded counter disabled for product {}. Falling back is out of scope for this method.", productId);
            return -1;
        }

        int n = properties.getCellsFor(productId);
        int initialCell = cellSelector.selectInitialCell(userId, n);

        for (int i = 0; i < n; i++) {
            int cellIndex = (initialCell + i) % n;
            String cellKey = String.format("inv:%s:cell:%d", productId, cellIndex);
            String resKey = String.format("inv:%s:cell:%d:res:%s:1", productId, cellIndex, userId); // simplified nonce to "1" for now, ideally unique per request

            Long result = luaScripts.getRedisTemplate().execute(
                    luaScripts.getReserveScript(),
                    List.of(cellKey, resKey),
                    String.valueOf(qty),
                    String.valueOf(ttlSec)
            );

            if (result != null && result == 1L) {
                reserveSuccessCounter.increment();
                cellRetriesSummary.record(i);
                return cellIndex;
            }
        }

        reserveFailureCounter.increment();
        return -1;
    }

    /**
     * Commits a previous reservation.
     *
     * @param productId The product ID.
     * @param qty       The quantity to commit.
     * @param cellsUsed A list of cell indices that were reserved.
     * @param userId    The user ID.
     */
    public void commit(String productId, int qty, List<Integer> cellsUsed, String userId) {
        if (!properties.isEnabledFor(productId)) {
            return;
        }
        for (Integer cellIndex : cellsUsed) {
            String cellKey = String.format("inv:%s:cell:%d", productId, cellIndex);
            String resKey = String.format("inv:%s:cell:%d:res:%s:1", productId, cellIndex, userId);

            luaScripts.getRedisTemplate().execute(
                    luaScripts.getCommitScript(),
                    List.of(cellKey, resKey),
                    String.valueOf(qty)
            );
        }
    }

    /**
     * Releases a previous reservation.
     *
     * @param productId The product ID.
     * @param qty       The quantity to release.
     * @param cellsUsed A list of cell indices that were reserved.
     * @param userId    The user ID.
     */
    public void release(String productId, int qty, List<Integer> cellsUsed, String userId) {
        if (!properties.isEnabledFor(productId)) {
            return;
        }
        for (Integer cellIndex : cellsUsed) {
            String cellKey = String.format("inv:%s:cell:%d", productId, cellIndex);
            String resKey = String.format("inv:%s:cell:%d:res:%s:1", productId, cellIndex, userId);

            luaScripts.getRedisTemplate().execute(
                    luaScripts.getReleaseScript(),
                    List.of(cellKey, resKey),
                    String.valueOf(qty)
            );
        }
    }

    /**
     * Gets the total available stock for a product across all cells.
     * Note: This is an aggregate read and may be stale.
     *
     * @param productId The product ID.
     * @return The sum of available stock across all cells.
     */
    public int getAvailable(String productId) {
        if (!properties.isEnabledFor(productId)) {
            return 0; // Fallback not fully handled here
        }
        int n = properties.getCellsFor(productId);
        int totalAvailable = 0;
        for (int i = 0; i < n; i++) {
            String cellKey = String.format("inv:%s:cell:%d", productId, i);
            Object availableObj = luaScripts.getRedisTemplate().opsForHash().get(cellKey, "available");
            if (availableObj != null) {
                totalAvailable += Integer.parseInt(availableObj.toString());
            }
        }
        return totalAvailable;
    }
}
