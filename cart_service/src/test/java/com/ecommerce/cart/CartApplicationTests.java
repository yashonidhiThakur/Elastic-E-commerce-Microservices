package com.ecommerce.cart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

@SpringBootTest
class CartApplicationTests {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RestClient restClient;

    @Test
    void contextLoads() {
    }
}
