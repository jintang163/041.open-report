package com.openreport.admin.service;

import com.openreport.admin.dto.TemplateEditLockInfo;

public interface TemplateEditLockService {

    String TEMPLATE_LOCK_KEY_PREFIX = "openreport:template:edit:lock:";
    long DEFAULT_LOCK_EXPIRE_SECONDS = 5 * 60;
    long HEARTBEAT_INTERVAL_SECONDS = 60;

    TemplateEditLockInfo acquireLock(Long templateId, Long userId, String userName);

    boolean releaseLock(Long templateId, Long userId, String lockToken);

    boolean renewLock(Long templateId, Long userId, String lockToken);

    TemplateEditLockInfo getLockInfo(Long templateId);

    boolean isLockOwner(Long templateId, Long userId, String lockToken);
}
