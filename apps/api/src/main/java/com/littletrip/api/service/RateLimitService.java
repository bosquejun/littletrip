package com.littletrip.api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Bucket> buckets = new HashMap<>();

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String key, int limit, Duration window) {
        try {
            String redisKey = key + ":" + getWindowSlot(window);
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == 1) {
                redisTemplate.expire(redisKey, window);
            }
            return count <= limit;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    public boolean tryConsumeTokenBucket(String key, Bandwidth limit) {
        try {
            Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());
            return bucket.tryConsume(1);
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    public long getRemainingRequests(String key, int limit, Duration window) {
        try {
            String redisKey = key + ":" + getWindowSlot(window);
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) return limit;
            return Math.max(0, limit - Long.parseLong(value));
        } catch (Exception e) {
            return limit;
        }
    }

    public long getResetTime(Duration window) {
        long windowSeconds = window.toSeconds();
        long slot = Instant.now().getEpochSecond() / windowSeconds;
        return (slot + 1) * windowSeconds;
    }

    private long getWindowSlot(Duration window) {
        return Instant.now().getEpochSecond() / window.toSeconds();
    }
}
