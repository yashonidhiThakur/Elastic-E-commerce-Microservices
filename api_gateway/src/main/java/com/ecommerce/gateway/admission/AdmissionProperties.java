package com.ecommerce.gateway.admission;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "admission")
public class AdmissionProperties {

    private Map<String, ProductLimit> products;
    private ProductLimit defaultLimit;

    public Map<String, ProductLimit> getProducts() {
        return products;
    }

    public void setProducts(Map<String, ProductLimit> products) {
        this.products = products;
    }

    public ProductLimit getDefault() {
        return defaultLimit;
    }

    public void setDefault(ProductLimit defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public static class ProductLimit {
        private int bucketCapacity;
        private int refillPerSecond;

        public int getBucketCapacity() {
            return bucketCapacity;
        }

        public void setBucketCapacity(int bucketCapacity) {
            this.bucketCapacity = bucketCapacity;
        }

        public int getRefillPerSecond() {
            return refillPerSecond;
        }

        public void setRefillPerSecond(int refillPerSecond) {
            this.refillPerSecond = refillPerSecond;
        }
    }
}
