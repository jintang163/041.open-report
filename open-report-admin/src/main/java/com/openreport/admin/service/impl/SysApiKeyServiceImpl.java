package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.SysApiKey;
import com.openreport.admin.mapper.SysApiKeyMapper;
import com.openreport.admin.service.SysApiKeyService;
import com.openreport.common.result.ResultCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyMapper, SysApiKey>
        implements SysApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int API_KEY_LENGTH = 32;
    private static final int API_SECRET_LENGTH = 48;
    private static final int DEFAULT_RATE_LIMIT = 100;

    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysApiKey createKey(Long userId, String userName, String appName, String description,
                                Integer rateLimit, String ipWhitelist, Integer expireDays) {
        LocalDateTime now = LocalDateTime.now();
        SysApiKey apiKey = new SysApiKey();
        apiKey.setApiKey("op_" + generateRandomString(API_KEY_LENGTH));
        apiKey.setApiSecret(generateRandomString(API_SECRET_LENGTH));
        apiKey.setAppName(appName);
        apiKey.setDescription(description);
        apiKey.setUserId(userId);
        apiKey.setUserName(userName);
        apiKey.setStatus(1);
        apiKey.setRateLimit(rateLimit != null ? rateLimit : DEFAULT_RATE_LIMIT);
        apiKey.setIpWhitelist(ipWhitelist);
        if (expireDays != null && expireDays > 0) {
            apiKey.setExpireTime(now.plusDays(expireDays));
        }
        apiKey.setCallCount(0L);
        apiKey.setCreateTime(now);
        apiKey.setUpdateTime(now);
        apiKey.setDeleted(0);
        save(apiKey);
        return apiKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeKey(Long id, Long userId) {
        SysApiKey key = getById(id);
        if (key == null || !key.getUserId().equals(userId)) {
            return false;
        }
        key.setStatus(0);
        key.setUpdateTime(LocalDateTime.now());
        return updateById(key);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rotateKey(Long id, Long userId) {
        SysApiKey key = getById(id);
        if (key == null || !key.getUserId().equals(userId)) {
            return false;
        }
        key.setApiSecret(generateRandomString(API_SECRET_LENGTH));
        key.setUpdateTime(LocalDateTime.now());
        return updateById(key);
    }

    @Override
    public SysApiKey validateKey(String apiKey, String clientIp) {
        if (StringUtils.isBlank(apiKey)) {
            throw new RuntimeException(ResultCode.API_KEY_INVALID.getMessage());
        }
        LambdaQueryWrapper<SysApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysApiKey::getApiKey, apiKey);
        SysApiKey key = getOne(wrapper);
        if (key == null) {
            throw new RuntimeException(ResultCode.API_KEY_INVALID.getMessage());
        }
        if (key.getStatus() == null || key.getStatus() != 1) {
            throw new RuntimeException(ResultCode.API_KEY_DISABLED.getMessage());
        }
        if (key.getExpireTime() != null && key.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(ResultCode.API_KEY_EXPIRED.getMessage());
        }
        if (!isIpAllowed(key, clientIp)) {
            throw new RuntimeException(ResultCode.IP_NOT_ALLOWED.getMessage());
        }
        return key;
    }

    @Override
    public Page<SysApiKey> pageMyKeys(Long userId, Integer pageNum, Integer pageSize, String keyword) {
        Page<SysApiKey> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysApiKey::getUserId, userId);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysApiKey::getAppName, keyword)
                    .or().like(SysApiKey::getApiKey, keyword));
        }
        wrapper.orderByDesc(SysApiKey::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCallCount(Long id) {
        LambdaUpdateWrapper<SysApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysApiKey::getId, id)
                .setSql("call_count = call_count + 1")
                .set(SysApiKey::getLastCallTime, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    public boolean isIpAllowed(SysApiKey apiKey, String clientIp) {
        if (apiKey == null || StringUtils.isBlank(apiKey.getIpWhitelist())) {
            return true;
        }
        if (StringUtils.isBlank(clientIp)) {
            return false;
        }
        String[] allowedIps = apiKey.getIpWhitelist().split("[,;\\s]+");
        for (String allowedIp : allowedIps) {
            if (StringUtils.isNotBlank(allowedIp)) {
                allowedIp = allowedIp.trim();
                if (allowedIp.equals(clientIp)) {
                    return true;
                }
                if (allowedIp.endsWith(".*")) {
                    String prefix = allowedIp.substring(0, allowedIp.length() - 2);
                    if (clientIp.startsWith(prefix)) {
                        return true;
                    }
                }
                if (allowedIp.contains("/")) {
                    try {
                        String[] parts = allowedIp.split("/");
                        String networkAddress = parts[0];
                        int prefixLen = Integer.parseInt(parts[1]);
                        if (isIpInRange(clientIp, networkAddress, prefixLen)) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return false;
    }

    private boolean isIpInRange(String ip, String networkAddress, int prefixLength) {
        try {
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkAddress);
            long mask = prefixLength == 0 ? 0 : (0xFFFFFFFFL << (32 - prefixLength));
            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + Integer.parseInt(parts[i]);
        }
        return result;
    }
}
