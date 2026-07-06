package com.ecommerce.inventory.sharded;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CellSelector {

    /**
     * Deterministically selects a cell index based on userId and total cells (N).
     *
     * @param userId The ID of the user.
     * @param n      The total number of cells configured for the product.
     * @return A deterministic cell index between 0 and N-1.
     */
    public int selectInitialCell(String userId, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Number of cells must be > 0");
        }
        if (userId == null) {
            userId = "";
        }
        return Math.floorMod(userId.hashCode(), n);
    }
}
