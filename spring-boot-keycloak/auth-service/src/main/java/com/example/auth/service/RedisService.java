package com.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for Redis operations
 */
@Slf4j
@Service
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Set value with expiry time in seconds
     */
    public void set(String key, Object value, long expirySeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, expirySeconds, TimeUnit.SECONDS);
            log.debug("Redis SET: key={}, expirySeconds={}", key, expirySeconds);
        } catch (Exception e) {
            log.error("Error setting Redis value", e);
        }
    }
    
    /**
     * Get value
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting Redis value", e);
            return null;
        }
    }
    
    /**
     * Delete key
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis DELETE: key={}", key);
        } catch (Exception e) {
            log.error("Error deleting Redis key", e);
        }
    }
    
    /**
     * Delete keys matching pattern
     */
    public void deletePattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Redis DELETE PATTERN: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("Error deleting Redis pattern", e);
        }
    }
    
    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking Redis key existence", e);
            return false;
        }
    }
    
    /**
     * Get TTL in seconds
     */
    public long getTTL(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting Redis TTL", e);
            return -1;
        }
    }
}
