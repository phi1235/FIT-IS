package com.example.keycloak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Example Service demonstrating Redis Cache usage
 * Đây là service mẫu để demo cách sử dụng Spring Cache với Redis
 * 
 * Các annotation quan trọng:
 * - @Cacheable: Cache kết quả của method, lần sau gọi sẽ lấy từ cache
 * - @CachePut: Luôn execute method và update cache
 * - @CacheEvict: Xóa data từ cache
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheExampleService {

    // Giả lập database
    private final Map<String, String> database = new HashMap<>();
    
    private final RedisService redisService;

    /**
     * Method này sẽ cache kết quả trong Redis
     * Lần đầu gọi sẽ thực thi method, các lần sau sẽ lấy từ cache
     * 
     * Cache name: "users"
     * Cache key: userId (parameter)
     * TTL: 30 minutes (defined in RedisConfig)
     */
    @Cacheable(value = "users", key = "#userId")
    public String getUserById(String userId) {
        log.info("Fetching user from database: userId={}", userId);
        // Simulate database query
        String user = database.getOrDefault(userId, "User not found");
        return user;
    }

    /**
     * Method này sẽ luôn thực thi và update cache
     * Dùng khi muốn update data trong cache
     */
    @CachePut(value = "users", key = "#userId")
    public String updateUser(String userId, String userData) {
        log.info("Updating user in database: userId={}", userId);
        database.put(userId, userData);
        return userData;
    }

    /**
     * Method này sẽ xóa entry trong cache
     * Dùng khi xóa hoặc invalidate data
     */
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(String userId) {
        log.info("Deleting user from database: userId={}", userId);
        database.remove(userId);
    }

    /**
     * Xóa toàn bộ cache của "users"
     */
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsersCache() {
        log.info("Clearing all users cache");
    }

    /**
     * Example sử dụng RedisService trực tiếp (không dùng annotation)
     * Dùng khi cần control cache một cách chi tiết hơn
     */
    public String getUserByIdManual(String userId) {
        String cacheKey = "manual:user:" + userId;
        
        // Try to get from cache first
        Object cached = redisService.get(cacheKey);
        if (cached != null) {
            log.info("Cache hit: userId={}", userId);
            return (String) cached;
        }
        
        // Cache miss, fetch from database
        log.info("Cache miss, fetching from database: userId={}", userId);
        String user = database.getOrDefault(userId, "User not found");
        
        // Save to cache with custom TTL (5 minutes)
        redisService.set(cacheKey, user, java.time.Duration.ofMinutes(5));
        
        return user;
    }

    /**
     * Example sử dụng Redis Set
     */
    public void addUserToActiveSet(String userId) {
        redisService.addToSet("active:users", userId);
        log.info("Added user to active set: userId={}", userId);
    }

    /**
     * Example sử dụng Redis Hash
     */
    public void saveUserProfile(String userId, String profileKey, Object profileValue) {
        String hashKey = "profile:" + userId;
        redisService.putHash(hashKey, profileKey, profileValue);
        log.info("Saved user profile: userId={}, key={}", userId, profileKey);
    }

    /**
     * Example increment counter
     */
    public Long incrementLoginCount(String userId) {
        String key = "login:count:" + userId;
        Long count = redisService.increment(key, 1);
        log.info("Incremented login count for user: userId={}, count={}", userId, count);
        return count;
    }
}

