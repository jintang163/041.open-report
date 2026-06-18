package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.service.EmbedSsoService;
import com.openreport.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmbedSsoServiceImpl implements EmbedSsoService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String generateSsoToken(Long reportId, Long userId, String username, Long expireSeconds) {
        if (expireSeconds == null || expireSeconds <= 0) {
            expireSeconds = DEFAULT_SSO_EXPIRE_SECONDS;
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("reportId", reportId);
        claims.put("type", "embed");
        claims.put("createBy", userId);
        claims.put("createByUsername", username);
        claims.put("expireSeconds", expireSeconds);

        String token = jwtUtils.generateToken(claims);

        Map<String, String> ssoInfo = new HashMap<>();
        ssoInfo.put("reportId", String.valueOf(reportId));
        ssoInfo.put("userId", String.valueOf(userId));
        ssoInfo.put("username", username);
        ssoInfo.put("createTime", String.valueOf(System.currentTimeMillis()));
        ssoInfo.put("expireSeconds", String.valueOf(expireSeconds));

        String redisKey = SSO_TOKEN_KEY_PREFIX + token;
        stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(ssoInfo),
                expireSeconds, TimeUnit.SECONDS);

        return token;
    }

    @Override
    public boolean validateSsoToken(String token, Long reportId) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        try {
            if (!jwtUtils.validateToken(token)) {
                return false;
            }
            if (jwtUtils.isTokenExpired(token)) {
                return false;
            }

            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
            String type = claims.get("type") != null ? claims.get("type").toString() : null;
            if (!"embed".equals(type)) {
                return false;
            }

            Object tokenReportId = claims.get("reportId");
            if (tokenReportId == null) {
                return false;
            }
            if (reportId != null && !Long.valueOf(tokenReportId.toString()).equals(reportId)) {
                return false;
            }

            String redisKey = SSO_TOKEN_KEY_PREFIX + token;
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            return StringUtils.isNotBlank(cached);
        } catch (Exception e) {
            log.error("验证SSO Token失败", e);
            return false;
        }
    }

    @Override
    public boolean renewSsoToken(String token, Long expireSeconds) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        if (expireSeconds == null || expireSeconds <= 0) {
            expireSeconds = DEFAULT_SSO_EXPIRE_SECONDS;
        }

        try {
            if (!jwtUtils.validateToken(token)) {
                return false;
            }

            String redisKey = SSO_TOKEN_KEY_PREFIX + token;
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(cached)) {
                return false;
            }

            Map<String, String> ssoInfo = JSON.parseObject(cached, new TypeReference<Map<String, String>>() {});
            ssoInfo.put("expireSeconds", String.valueOf(expireSeconds));
            ssoInfo.put("lastRenewTime", String.valueOf(System.currentTimeMillis()));

            stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(ssoInfo),
                    expireSeconds, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            log.error("续期SSO Token失败", e);
            return false;
        }
    }

    @Override
    public boolean revokeSsoToken(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        String redisKey = SSO_TOKEN_KEY_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.delete(redisKey));
    }

    @Override
    public Map<String, Object> getSsoTokenInfo(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        try {
            String redisKey = SSO_TOKEN_KEY_PREFIX + token;
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(cached)) {
                return null;
            }

            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

            Map<String, String> ssoInfo = JSON.parseObject(cached, new TypeReference<Map<String, String>>() {});
            Map<String, Object> result = new HashMap<>();
            result.put("reportId", Long.valueOf(ssoInfo.get("reportId")));
            result.put("userId", Long.valueOf(ssoInfo.get("userId")));
            result.put("username", ssoInfo.get("username"));
            result.put("createTime", Long.valueOf(ssoInfo.get("createTime")));
            result.put("expireSeconds", Long.valueOf(ssoInfo.get("expireSeconds")));
            result.put("remainingSeconds", ttl != null ? ttl : 0);
            if (ssoInfo.containsKey("lastRenewTime")) {
                result.put("lastRenewTime", Long.valueOf(ssoInfo.get("lastRenewTime")));
            }

            return result;
        } catch (Exception e) {
            log.error("获取SSO Token信息失败", e);
            return null;
        }
    }
}
