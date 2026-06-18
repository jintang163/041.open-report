package com.openreport.admin.filter;

import com.alibaba.fastjson.JSON;
import com.openreport.admin.entity.SysApiKey;
import com.openreport.admin.service.SysApiKeyService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SysApiKeyService apiKeyService;

    private static final String RATE_LIMIT_KEY_PREFIX = "openreport:api:ratelimit:";
    private static final long WINDOW_SECONDS = 60;
    private static final int DEFAULT_LIMIT = 100;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/open/") && !path.startsWith("/api/embed/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String apiKey = resolveApiKey(request);
        String rateLimitKey;
        int limit;

        if (StringUtils.isNotBlank(apiKey)) {
            try {
                SysApiKey key = apiKeyService.validateKey(apiKey, clientIp);
                rateLimitKey = RATE_LIMIT_KEY_PREFIX + "key:" + apiKey;
                limit = key.getRateLimit() != null ? key.getRateLimit() : DEFAULT_LIMIT;
                request.setAttribute("apiKeyObj", key);
                request.setAttribute("apiKey", apiKey);
            } catch (RuntimeException e) {
                writeErrorResponse(response, resolveResultCode(e.getMessage()));
                return;
            }
        } else {
            rateLimitKey = RATE_LIMIT_KEY_PREFIX + "ip:" + clientIp;
            limit = 20;
        }

        if (!checkRateLimit(rateLimitKey, limit)) {
            writeErrorResponse(response, ResultCode.TOO_MANY_REQUESTS);
            return;
        }

        filterChain.doFilter(request, response);

        if (StringUtils.isNotBlank(apiKey)) {
            SysApiKey key = (SysApiKey) request.getAttribute("apiKeyObj");
            if (key != null) {
                apiKeyService.incrementCallCount(key.getId());
            }
        }
    }

    private boolean checkRateLimit(String key, int limit) {
        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - WINDOW_SECONDS * 1000;
            ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

            zSetOps.removeRangeByScore(key, 0, windowStart);

            Long currentCount = zSetOps.zCard(key);
            if (currentCount != null && currentCount >= limit) {
                return false;
            }

            String member = now + ":" + ThreadLocalRandomHolder.current().nextInt(10000);
            zSetOps.add(key, member, now);
            stringRedisTemplate.expire(key, WINDOW_SECONDS + 1, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private String resolveApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.isBlank(apiKey)) {
            apiKey = request.getParameter("apiKey");
        }
        if (StringUtils.isBlank(apiKey)) {
            apiKey = request.getHeader("Authorization");
            if (StringUtils.isNotBlank(apiKey) && apiKey.startsWith("ApiKey ")) {
                apiKey = apiKey.substring(7);
            } else {
                apiKey = null;
            }
        }
        return apiKey;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            }
            return ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private ResultCode resolveResultCode(String message) {
        for (ResultCode rc : ResultCode.values()) {
            if (rc.getMessage().equals(message)) {
                return rc;
            }
        }
        return ResultCode.API_KEY_INVALID;
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Result<Object> result = Result.failure(resultCode);
        response.getWriter().write(JSON.toJSONString(result));
    }

    private static class ThreadLocalRandomHolder {
        static java.util.concurrent.ThreadLocalRandom current() {
            return java.util.concurrent.ThreadLocalRandom.current();
        }
    }
}
