package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.openreport.admin.dto.TemplateEditLockInfo;
import com.openreport.admin.service.TemplateEditLockService;
import com.openreport.admin.utils.RedisDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TemplateEditLockServiceImpl implements TemplateEditLockService {

    @Autowired
    private RedisDistributedLock distributedLock;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String buildLockKey(Long templateId) {
        return TEMPLATE_LOCK_KEY_PREFIX + templateId;
    }

    @Override
    public TemplateEditLockInfo acquireLock(Long templateId, Long userId, String userName) {
        String lockKey = buildLockKey(templateId);
        String lockToken = UUID.randomUUID().toString().replace("-", "");

        TemplateEditLockInfo existingLock = getLockInfo(templateId);
        if (existingLock != null) {
            if (existingLock.getUserId().equals(userId)) {
                renewLock(templateId, userId, existingLock.getLockToken());
                existingLock.setExpireTime(System.currentTimeMillis() + DEFAULT_LOCK_EXPIRE_SECONDS * 1000);
                return existingLock;
            }
            return null;
        }

        long lockTime = System.currentTimeMillis();
        long expireTime = lockTime + DEFAULT_LOCK_EXPIRE_SECONDS * 1000;

        TemplateEditLockInfo lockInfo = TemplateEditLockInfo.builder()
                .templateId(templateId)
                .userId(userId)
                .userName(userName)
                .lockTime(lockTime)
                .expireTime(expireTime)
                .lockToken(lockToken)
                .build();

        String lockValue = JSON.toJSONString(lockInfo);
        boolean locked = distributedLock.tryLock(lockKey, lockValue, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (locked) {
            return lockInfo;
        }

        return getLockInfo(templateId);
    }

    @Override
    public boolean releaseLock(Long templateId, Long userId, String lockToken) {
        String lockKey = buildLockKey(templateId);
        TemplateEditLockInfo lockInfo = getLockInfo(templateId);
        if (lockInfo == null) {
            return true;
        }
        if (!lockInfo.getUserId().equals(userId) || !lockInfo.getLockToken().equals(lockToken)) {
            return false;
        }
        String lockValue = JSON.toJSONString(lockInfo);
        return distributedLock.unlock(lockKey, lockValue);
    }

    @Override
    public boolean renewLock(Long templateId, Long userId, String lockToken) {
        String lockKey = buildLockKey(templateId);
        TemplateEditLockInfo lockInfo = getLockInfo(templateId);
        if (lockInfo == null) {
            return false;
        }
        if (!lockInfo.getUserId().equals(userId) || !lockInfo.getLockToken().equals(lockToken)) {
            return false;
        }
        String lockValue = JSON.toJSONString(lockInfo);
        boolean renewed = distributedLock.renew(lockKey, lockValue, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (renewed) {
            lockInfo.setExpireTime(System.currentTimeMillis() + DEFAULT_LOCK_EXPIRE_SECONDS * 1000);
            redisTemplate.opsForValue().set(lockKey, JSON.toJSONString(lockInfo),
                    DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        return renewed;
    }

    @Override
    public TemplateEditLockInfo getLockInfo(Long templateId) {
        String lockKey = buildLockKey(templateId);
        Object value = distributedLock.getLockValue(lockKey);
        if (value == null) {
            return null;
        }
        try {
            TemplateEditLockInfo lockInfo = JSON.parseObject(value.toString(), TemplateEditLockInfo.class);
            Long expire = distributedLock.getLockExpire(lockKey);
            if (expire != null && expire > 0) {
                lockInfo.setExpireTime(System.currentTimeMillis() + expire);
            }
            return lockInfo;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isLockOwner(Long templateId, Long userId, String lockToken) {
        TemplateEditLockInfo lockInfo = getLockInfo(templateId);
        if (lockInfo == null) {
            return true;
        }
        return lockInfo.getUserId().equals(userId) && lockInfo.getLockToken().equals(lockToken);
    }
}
