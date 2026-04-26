package com.littletrip.api.config;

import com.littletrip.api.service.RateLimitService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RateLimitService rateLimitService(StringRedisTemplate stringRedisTemplate) {
        return new RateLimitService(stringRedisTemplate);
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return mock(MeterRegistry.class);
    }
}
