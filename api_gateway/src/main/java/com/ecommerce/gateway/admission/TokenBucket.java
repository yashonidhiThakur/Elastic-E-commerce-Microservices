package com.ecommerce.gateway.admission;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class TokenBucket {

    private final StringRedisTemplate redisTemplate;
    private final AdmissionProperties admissionProperties;
    private final DefaultRedisScript<Long> bucketConsumeScript;

    public TokenBucket(StringRedisTemplate redisTemplate, AdmissionProperties admissionProperties) {
        this.redisTemplate = redisTemplate;
        this.admissionProperties = admissionProperties;
        
        this.bucketConsumeScript = new DefaultRedisScript<>();
        this.bucketConsumeScript.setLocation(new ClassPathResource("lua/bucket_consume.lua"));
        this.bucketConsumeScript.setResultType(Long.class);
    }

    /**
     * Attempts to consume one token for the given product ID.
     * @param productId the ID of the product
     * @return true if token was consumed (allow), false if bucket is empty (deny)
     */
    public boolean tryConsume(String productId) {
        AdmissionProperties.ProductLimit limit = admissionProperties.getProducts() != null ? 
            admissionProperties.getProducts().get(productId) : null;
            
        if (limit == null) {
            limit = admissionProperties.getDefault();
        }

        if (limit == null) {
            // Fallback if completely misconfigured
            limit = new AdmissionProperties.ProductLimit();
            limit.setBucketCapacity(100);
            limit.setRefillPerSecond(50);
        }

        String bucketKey = "bucket:" + productId;
        long now = System.currentTimeMillis();

        Long result = redisTemplate.execute(
                bucketConsumeScript,
                Collections.singletonList(bucketKey),
                String.valueOf(limit.getBucketCapacity()),
                String.valueOf(limit.getRefillPerSecond()),
                String.valueOf(now)
        );

        return result != null && result == 1L;
    }
}
