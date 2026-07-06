package com.ecommerce.inventory.sharded;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CellSelectorTest {

    private final CellSelector cellSelector = new CellSelector();

    @Test
    void testDeterminism() {
        String userId = "user123";
        int cell1 = cellSelector.selectInitialCell(userId, 10);
        int cell2 = cellSelector.selectInitialCell(userId, 10);
        assertEquals(cell1, cell2, "Cell selection must be deterministic");
    }

    @Test
    void testDistribution() {
        int n = 10;
        int samples = 10000;
        Map<Integer, Integer> counts = new HashMap<>();

        for (int i = 0; i < samples; i++) {
            String userId = UUID.randomUUID().toString();
            int cell = cellSelector.selectInitialCell(userId, n);
            counts.put(cell, counts.getOrDefault(cell, 0) + 1);
        }

        double expected = (double) samples / n;
        double chiSquare = 0;

        for (int i = 0; i < n; i++) {
            double observed = counts.getOrDefault(i, 0);
            chiSquare += Math.pow(observed - expected, 2) / expected;
        }

        // For df = 9 (n-1), critical value for p=0.05 is 16.919
        // If chiSquare < 16.919, then p > 0.05 (fail to reject null hypothesis of uniform distribution)
        assertTrue(chiSquare < 16.919, "Distribution is not uniform, chi-square: " + chiSquare);
    }
}
