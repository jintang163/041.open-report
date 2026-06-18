package com.openreport.admin.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private static final String RENEW_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "else " +
            "    return 0 " +
            "end";

    public boolean tryLock(String key, String requestId, long expireTime, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, requestId, expireTime, timeUnit);
        return Boolean.TRUE.equals(result);
    }

    public boolean lock(String key, String requestId, long expireTime, TimeUnit timeUnit,
                        long retryInterval, int maxRetryCount) {
        int retryCount = 0;
        while (retryCount < maxRetryCount) {
            if (tryLock(key, requestId, expireTime, timeUnit)) {
                return true;
            }
            retryCount++;
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public boolean unlock(String key, String requestId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), requestId);
        return result != null && result == 1;
    }

    public boolean renew(String key, String requestId, long expireTime, TimeUnit timeUnit) {
        long expireMillis = timeUnit.toMillis(expireTime);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RENEW_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), requestId, String.valueOf(expireMillis));
        return result != null && result == 1;
    }

    public Object getLockValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Long getLockExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }
}
