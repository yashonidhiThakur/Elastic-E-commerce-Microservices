package com.ecommerce.inventory.sharded;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "inventory.sharded")
public class ShardedCounterProperties {

    private Map<String, Boolean> enabled = new HashMap<>();
    private int defaultCells = 10;
    private Map<String, Integer> cells = new HashMap<>();

    public Map<String, Boolean> getEnabled() { return enabled; }
    public void setEnabled(Map<String, Boolean> enabled) { this.enabled = enabled; }

    public int getDefaultCells() { return defaultCells; }
    public void setDefaultCells(int defaultCells) { this.defaultCells = defaultCells; }

    public Map<String, Integer> getCells() { return cells; }
    public void setCells(Map<String, Integer> cells) { this.cells = cells; }

    public boolean isEnabledFor(String productId) {
        return enabled.getOrDefault(productId, false);
    }

    public int getCellsFor(String productId) {
        return cells.getOrDefault(productId, defaultCells);
    }
}
