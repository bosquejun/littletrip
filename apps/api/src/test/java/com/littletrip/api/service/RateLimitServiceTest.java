package com.littletrip.api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void tryConsume_firstRequest_allowsRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = rateLimitService.tryConsume("test-key", 10, Duration.ofMinutes(1));

        assertThat(result).isTrue();
    }

    @Test
    void tryConsume_underLimit_allowsRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        boolean result = rateLimitService.tryConsume("test-key", 10, Duration.ofMinutes(1));

        assertThat(result).isTrue();
    }

    @Test
    void tryConsume_atLimit_deniesRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        boolean result = rateLimitService.tryConsume("test-key", 10, Duration.ofMinutes(1));

        assertThat(result).isFalse();
    }

    @Test
    void tryConsume_redisException_allowsRequest() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        boolean result = rateLimitService.tryConsume("test-key", 10, Duration.ofMinutes(1));

        assertThat(result).isTrue();
    }

    @Test
    void tryConsumeTokenBucket_underLimit_allowsRequest() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

        boolean result = rateLimitService.tryConsumeTokenBucket("test-key", limit);

        assertThat(result).isTrue();
    }

    @Test
    void tryConsumeTokenBucket_atLimit_deniesRequest() {
        Bandwidth limit = Bandwidth.classic(1, Refill.greedy(1, Duration.ofMinutes(1)));

        rateLimitService.tryConsumeTokenBucket("test-key", limit);

        boolean result = rateLimitService.tryConsumeTokenBucket("test-key", limit);

        assertThat(result).isFalse();
    }

    @Test
    void tryConsumeTokenBucket_redisException_allowsRequest() {
        Bandwidth limit = Bandwidth.classic(1, Refill.greedy(1, Duration.ofMinutes(1)));

        boolean result = rateLimitService.tryConsumeTokenBucket("test-key", limit);

        assertThat(result).isTrue();
    }

    @Test
    void getRemainingRequests_someRequests_returnsCorrectCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("3");

        long remaining = rateLimitService.getRemainingRequests("test-key", 10, Duration.ofMinutes(1));

        assertThat(remaining).isEqualTo(7);
    }

    @Test
    void getRemainingRequests_noRequests_returnsFullLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        long remaining = rateLimitService.getRemainingRequests("test-key", 10, Duration.ofMinutes(1));

        assertThat(remaining).isEqualTo(10);
    }

    @Test
    void getRemainingRequests_redisException_returnsFullLimit() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        long remaining = rateLimitService.getRemainingRequests("test-key", 10, Duration.ofMinutes(1));

        assertThat(remaining).isEqualTo(10);
    }

    @Test
    void getResetTime_returnsNextWindowTimestamp() {
        Duration window = Duration.ofMinutes(1);
        long resetTime = rateLimitService.getResetTime(window);

        assertThat(resetTime).isGreaterThan(0);
    }
}