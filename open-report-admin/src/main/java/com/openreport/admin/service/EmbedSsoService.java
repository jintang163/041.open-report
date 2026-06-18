package com.openreport.admin.service;

import java.util.Map;

public interface EmbedSsoService {

    String SSO_TOKEN_KEY_PREFIX = "openreport:embed:sso:";
    long DEFAULT_SSO_EXPIRE_SECONDS = 3600;
    long SSO_HEARTBEAT_INTERVAL_SECONDS = 300;

    String generateSsoToken(Long reportId, Long userId, String username, Long expireSeconds);

    boolean validateSsoToken(String token, Long reportId);

    boolean renewSsoToken(String token, Long expireSeconds);

    boolean revokeSsoToken(String token);

    Map<String, Object> getSsoTokenInfo(String token);
}
