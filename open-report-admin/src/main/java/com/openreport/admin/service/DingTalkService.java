package com.openreport.admin.service;

import java.util.Map;

public interface DingTalkService {

    boolean sendMarkdown(String webhook, String secret, String title, String content);

    boolean sendText(String webhook, String secret, String content);

    boolean sendActionCard(String webhook, String secret, String title, String content, String singleTitle, String singleURL);

    boolean sendToUser(String dingtalkUserId, String title, String content);

    boolean sendMarkdownToUser(String phone, String title, String content, Map<String, Object> extra);
}
