package com.example.keycloak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Service
 * Service để thực hiện các thao tác với Redis cache
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Set a value in Redis
     * @param key the key
     * @param value the value
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Saved to Redis: key={}", key);
        } catch (Exception e) {
            log.error("Error saving to Redis: key={}", key, e);
        }
    }

    /**
     * Set a value in Redis with expiration time
     * @param key the key
     * @param value the value
     * @param timeout the timeout duration
     */
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
            log.debug("Saved to Redis with TTL: key={}, ttl={}", key, timeout);
        } catch (Exception e) {
            log.error("Error saving to Redis: key={}", key, e);
        }
    }

    /**
     * Get a value from Redis
     * @param key the key
     * @return the value, or null if not found
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Retrieved from Redis: key={}, found={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Error getting from Redis: key={}", key, e);
            return null;
        }
    }

    /**
     * Get a value from Redis with type casting
     * @param key the key
     * @param clazz the class to cast to
     * @param <T> the type
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting from Redis: key={}", key, e);
            return null;
        }
    }

    /**
     * Delete a key from Redis
     * @param key the key
     * @return true if deleted, false otherwise
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Deleted from Redis: key={}, success={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error deleting from Redis: key={}", key, e);
            return false;
        }
    }

    /**
     * Delete multiple keys from Redis
     * @param keys the keys to delete
     * @return number of keys deleted
     */
    public long delete(Set<String> keys) {
        try {
            Long result = redisTemplate.delete(keys);
            log.debug("Deleted from Redis: count={}", result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Error deleting from Redis: keys={}", keys, e);
            return 0;
        }
    }

    /**
     * Check if a key exists in Redis
     * @param key the key
     * @return true if exists, false otherwise
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking existence in Redis: key={}", key, e);
            return false;
        }
    }

    /**
     * Set expiration time for a key
     * @param key the key
     * @param timeout the timeout duration
     * @return true if successful, false otherwise
     */
    public boolean expire(String key, Duration timeout) {
        try {
            Boolean result = redisTemplate.expire(key, timeout);
            log.debug("Set expiration for key: key={}, ttl={}", key, timeout);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error setting expiration: key={}", key, e);
            return false;
        }
    }

    /**
     * Get time to live for a key
     * @param key the key
     * @return TTL in seconds, or -1 if key doesn't exist, -2 if no TTL
     */
    public long getTimeToLive(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL: key={}", key, e);
            return -1;
        }
    }

    /**
     * Increment a value in Redis
     * @param key the key
     * @param delta the increment value
     * @return the new value
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Error incrementing in Redis: key={}", key, e);
            return null;
        }
    }

    /**
     * Decrement a value in Redis
     * @param key the key
     * @param delta the decrement value
     * @return the new value
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Error decrementing in Redis: key={}", key, e);
            return null;
        }
    }

    /**
     * Get all keys matching a pattern
     * @param pattern the pattern (e.g., "user:*")
     * @return set of matching keys
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Error getting keys from Redis: pattern={}", pattern, e);
            return Set.of();
        }
    }

    /**
     * Add value to a Set in Redis
     * @param key the key
     * @param values the values to add
     * @return number of elements added
     */
    public Long addToSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("Error adding to set in Redis: key={}", key, e);
            return 0L;
        }
    }

    /**
     * Get all members of a Set in Redis
     * @param key the key
     * @return set of members
     */
    public Set<Object> getSetMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Error getting set members from Redis: key={}", key, e);
            return Set.of();
        }
    }

    /**
     * Remove value from a Set in Redis
     * @param key the key
     * @param values the values to remove
     * @return number of elements removed
     */
    public Long removeFromSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("Error removing from set in Redis: key={}", key, e);
            return 0L;
        }
    }

    /**
     * Put value in a Hash in Redis
     * @param key the key
     * @param hashKey the hash key
     * @param value the value
     */
    public void putHash(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            log.debug("Saved to Redis hash: key={}, hashKey={}", key, hashKey);
        } catch (Exception e) {
            log.error("Error saving to Redis hash: key={}, hashKey={}", key, hashKey, e);
        }
    }

    /**
     * Get value from a Hash in Redis
     * @param key the key
     * @param hashKey the hash key
     * @return the value
     */
    public Object getHash(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("Error getting from Redis hash: key={}, hashKey={}", key, hashKey, e);
            return null;
        }
    }

    /**
     * Delete hash key from Redis
     * @param key the key
     * @param hashKeys the hash keys to delete
     * @return number of fields deleted
     */
    public Long deleteHash(String key, Object... hashKeys) {
        try {
            return redisTemplate.opsForHash().delete(key, hashKeys);
        } catch (Exception e) {
            log.error("Error deleting from Redis hash: key={}", key, e);
            return 0L;
        }
    }
}

