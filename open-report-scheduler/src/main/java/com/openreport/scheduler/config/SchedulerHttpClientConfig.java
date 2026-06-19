package com.openreport.scheduler.config;

import com.openreport.common.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SchedulerHttpClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerHttpClientConfig.class);

    @Value("${report.admin.base-url:http://localhost:8080/api}")
    private String adminBaseUrl;

    @Value("${report.admin.service-key:openreport-scheduler-service-key}")
    private String serviceKey;

    @Value("${report.admin.service-secret:openreport-scheduler-secret-2024}")
    private String serviceSecret;

    @Value("${report.admin.service-user-id:1}")
    private Long serviceUserId;

    @Value("${report.admin.service-username:scheduler}")
    private String serviceUsername;

    @Bean
    public RestTemplate schedulerRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(300000);
        return new RestTemplate(factory);
    }

    @Bean
    public SchedulerApiClient schedulerApiClient(RestTemplate schedulerRestTemplate, JwtUtils jwtUtils) {
        return new SchedulerApiClient(schedulerRestTemplate, adminBaseUrl, jwtUtils,
                serviceUserId, serviceUsername, serviceKey, serviceSecret);
    }

    public static class SchedulerApiClient {

        private final RestTemplate restTemplate;
        private final String baseUrl;
        private final JwtUtils jwtUtils;
        private final Long serviceUserId;
        private final String serviceUsername;
        private final String serviceKey;
        private final String serviceSecret;

        private volatile String cachedToken;
        private volatile long tokenExpireTime;

        public SchedulerApiClient(RestTemplate restTemplate, String baseUrl, JwtUtils jwtUtils,
                                  Long serviceUserId, String serviceUsername,
                                  String serviceKey, String serviceSecret) {
            this.restTemplate = restTemplate;
            this.baseUrl = baseUrl;
            this.jwtUtils = jwtUtils;
            this.serviceUserId = serviceUserId;
            this.serviceUsername = serviceUsername;
            this.serviceKey = serviceKey;
            this.serviceSecret = serviceSecret;
        }

        private String getToken() {
            long now = System.currentTimeMillis();
            if (cachedToken != null && tokenExpireTime > now + 60000) {
                return cachedToken;
            }
            synchronized (this) {
                if (cachedToken != null && tokenExpireTime > now + 60000) {
                    return cachedToken;
                }
                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", serviceUserId);
                claims.put("username", serviceUsername);
                claims.put("type", "service");
                claims.put("serviceKey", serviceKey);
                claims.put("serviceSecret", serviceSecret);
                cachedToken = jwtUtils.generateToken(claims);
                tokenExpireTime = now + 86400000L;
                logger.debug("生成服务间调用 Token, userId: {}, username: {}", serviceUserId, serviceUsername);
                return cachedToken;
            }
        }

        private HttpHeaders buildHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + getToken());
            headers.set("X-Service-Key", serviceKey);
            headers.set("X-Service-Source", "scheduler");
            return headers;
        }

        public <T> T get(String path, Class<T> responseType) {
            String url = baseUrl + path;
            HttpHeaders headers = buildHeaders();
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<T> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, responseType);
            return response.getBody();
        }

        public <T> T post(String path, Object body, Class<T> responseType) {
            String url = baseUrl + path;
            HttpHeaders headers = buildHeaders();
            org.springframework.http.HttpEntity<Object> entity = new org.springframework.http.HttpEntity<>(body, headers);
            return restTemplate.postForObject(url, entity, responseType);
        }

        public Map<String, Object> getForMap(String path) {
            String url = baseUrl + path;
            HttpHeaders headers = buildHeaders();
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, Map.class);
            return parseResponse(response.getBody());
        }

        public Map<String, Object> postForMap(String path, Object body) {
            String url = baseUrl + path;
            HttpHeaders headers = buildHeaders();
            org.springframework.http.HttpEntity<Object> entity = new org.springframework.http.HttpEntity<>(body, headers);
            Map response = restTemplate.postForObject(url, entity, Map.class);
            return parseResponse(response);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parseResponse(Map response) {
            if (response == null) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("success", false);
                fail.put("message", "响应为空");
                fail.put("code", -1);
                return fail;
            }
            Object code = response.get("code");
            boolean success = code != null && "200".equals(String.valueOf(code));
            Map<String, Object> result = new HashMap<>(response);
            result.put("success", success);
            if (!success && response.get("message") == null) {
                result.put("message", "请求失败");
            }
            return result;
        }
    }
}
