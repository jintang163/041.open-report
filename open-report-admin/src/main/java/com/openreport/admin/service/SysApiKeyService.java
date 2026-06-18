package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysApiKey;

public interface SysApiKeyService extends IService<SysApiKey> {

    SysApiKey createKey(Long userId, String userName, String appName, String description,
                        Integer rateLimit, String ipWhitelist, Integer expireDays);

    boolean revokeKey(Long id, Long userId);

    boolean rotateKey(Long id, Long userId);

    SysApiKey validateKey(String apiKey, String clientIp);

    Page<SysApiKey> pageMyKeys(Long userId, Integer pageNum, Integer pageSize, String keyword);

    void incrementCallCount(Long id);

    boolean isIpAllowed(SysApiKey apiKey, String clientIp);
}
