package com.openreport.admin.service.impl;

import com.openreport.admin.dto.TemplateEditLockInfo;
import com.openreport.admin.service.TemplateEditLockService;
import com.openreport.admin.utils.RedisDistributedLock;
import com.openreport.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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

    private String buildInfoKey(Long templateId) {
        return TEMPLATE_LOCK_INFO_PREFIX + templateId;
    }

    @Override
    public TemplateEditLockInfo acquireLock(Long templateId, Long userId, String userName) {
        String lockKey = buildLockKey(templateId);
        String infoKey = buildInfoKey(templateId);

        TemplateEditLockInfo existingLock = getLockInfo(templateId);
        if (existingLock != null) {
            if (existingLock.getUserId().equals(userId)) {
                renewLock(templateId, userId, existingLock.getLockToken());
                existingLock.setExpireTime(System.currentTimeMillis() + DEFAULT_LOCK_EXPIRE_SECONDS * 1000);
                return existingLock;
            }
            return null;
        }

        String lockToken = UUID.randomUUID().toString().replace("-", "");
        long lockTime = System.currentTimeMillis();
        long expireTime = lockTime + DEFAULT_LOCK_EXPIRE_SECONDS * 1000;

        boolean locked = distributedLock.tryLock(lockKey, lockToken, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (locked) {
            Map<String, Object> infoMap = new HashMap<>();
            infoMap.put("templateId", String.valueOf(templateId));
            infoMap.put("userId", String.valueOf(userId));
            infoMap.put("userName", userName);
            infoMap.put("lockTime", String.valueOf(lockTime));
            infoMap.put("expireTime", String.valueOf(expireTime));
            infoMap.put("lockToken", lockToken);
            redisTemplate.opsForHash().putAll(infoKey, infoMap);
            redisTemplate.expire(infoKey, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            return TemplateEditLockInfo.builder()
                    .templateId(templateId)
                    .userId(userId)
                    .userName(userName)
                    .lockTime(lockTime)
                    .expireTime(expireTime)
                    .lockToken(lockToken)
                    .build();
        }

        return getLockInfo(templateId);
    }

    @Override
    public boolean releaseLock(Long templateId, Long userId, String lockToken) {
        String lockKey = buildLockKey(templateId);
        String infoKey = buildInfoKey(templateId);

        if (!isLockOwner(templateId, userId, lockToken)) {
            return false;
        }

        boolean unlocked = distributedLock.unlock(lockKey, lockToken);
        if (unlocked) {
            redisTemplate.delete(infoKey);
        }
        return unlocked;
    }

    @Override
    public boolean renewLock(Long templateId, Long userId, String lockToken) {
        String lockKey = buildLockKey(templateId);
        String infoKey = buildInfoKey(templateId);

        if (!isLockOwner(templateId, userId, lockToken)) {
            return false;
        }

        boolean renewed = distributedLock.renew(lockKey, lockToken, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (renewed) {
            long expireTime = System.currentTimeMillis() + DEFAULT_LOCK_EXPIRE_SECONDS * 1000;
            Map<String, Object> infoMap = new HashMap<>();
            infoMap.put("expireTime", String.valueOf(expireTime));
            redisTemplate.opsForHash().putAll(infoKey, infoMap);
            redisTemplate.expire(infoKey, DEFAULT_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        return renewed;
    }

    @Override
    public TemplateEditLockInfo getLockInfo(Long templateId) {
        String infoKey = buildInfoKey(templateId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(infoKey);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        try {
            Long tplId = entries.get("templateId") != null ? Long.valueOf(entries.get("templateId").toString()) : null;
            Long uid = entries.get("userId") != null ? Long.valueOf(entries.get("userId").toString()) : null;
            String uname = entries.get("userName") != null ? entries.get("userName").toString() : null;
            Long ltime = entries.get("lockTime") != null ? Long.valueOf(entries.get("lockTime").toString()) : null;
            Long etime = entries.get("expireTime") != null ? Long.valueOf(entries.get("expireTime").toString()) : null;
            String token = entries.get("lockToken") != null ? entries.get("lockToken").toString() : null;

            return TemplateEditLockInfo.builder()
                    .templateId(tplId)
                    .userId(uid)
                    .userName(uname)
                    .lockTime(ltime)
                    .expireTime(etime)
                    .lockToken(token)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isLockOwner(Long templateId, Long userId, String lockToken) {
        if (lockToken == null || lockToken.trim().isEmpty()) {
            return false;
        }
        TemplateEditLockInfo lockInfo = getLockInfo(templateId);
        if (lockInfo == null) {
            return false;
        }
        return lockInfo.getUserId().equals(userId) && lockToken.equals(lockInfo.getLockToken());
    }

    @Override
    public boolean isLocked(Long templateId) {
        String lockKey = buildLockKey(templateId);
        return distributedLock.getLockValue(lockKey) != null;
    }

    @Override
    public boolean checkLockOrThrow(Long templateId, Long userId, String lockToken) {
        if (templateId == null) {
            return true;
        }

        if (lockToken == null || lockToken.trim().isEmpty()) {
            TemplateEditLockInfo lockInfo = getLockInfo(templateId);
            if (lockInfo != null) {
                throw new RuntimeException(ResultCode.TEMPLATE_LOCKED.getCode() + ":" +
                        lockInfo.getUserName() + "正在编辑该模板，请稍后再试");
            }
            return true;
        }

        if (!isLockOwner(templateId, userId, lockToken)) {
            TemplateEditLockInfo lockInfo = getLockInfo(templateId);
            if (lockInfo != null) {
                throw new RuntimeException(ResultCode.TEMPLATE_LOCKED.getCode() + ":" +
                        lockInfo.getUserName() + "正在编辑该模板，请稍后再试");
            }
            throw new RuntimeException(ResultCode.TEMPLATE_LOCK_NOT_OWNER.getMessage());
        }

        return true;
    }
}
