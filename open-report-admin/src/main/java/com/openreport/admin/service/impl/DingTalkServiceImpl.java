package com.openreport.admin.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openreport.admin.service.DingTalkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DingTalkServiceImpl implements DingTalkService {

    private static final String BASE_URL = "https://oapi.dingtalk.com/robot/send";

    @Value("${open-report.dingtalk.timeout:10000}")
    private int timeout;

    @Override
    public boolean sendMarkdown(String webhook, String secret, String title, String content) {
        return send(webhook, secret, "MARKDOWN", title, content, null);
    }

    @Override
    public boolean sendText(String webhook, String secret, String content) {
        return send(webhook, secret, "TEXT", content, content, null);
    }

    @Override
    public boolean sendActionCard(String webhook, String secret, String title, String content, String singleTitle, String singleURL) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("singleTitle", singleTitle);
        extra.put("singleURL", singleURL);
        return send(webhook, secret, "CARD", title, content, extra);
    }

    @Override
    public boolean sendToUser(String dingtalkUserId, String title, String content) {
        log.info("发送钉钉消息给用户: {}, 标题: {}", dingtalkUserId, title);
        return true;
    }

    @Override
    public boolean sendMarkdownToUser(String phone, String title, String content, Map<String, Object> extra) {
        log.info("发送钉钉消息给手机号: {}, 标题: {}, 内容: {}", phone, title, content);
        return true;
    }

    private boolean send(String webhook, String secret, String messageFormat, String title, String content, Map<String, Object> extra) {
        if (webhook == null || webhook.isEmpty()) {
            log.warn("钉钉webhook为空，跳过发送");
            return false;
        }
        try {
            String url = buildUrl(webhook, secret);
            JSONObject body = buildMessageBody(messageFormat, title, content, extra);

            HttpResponse response = HttpRequest.post(url)
                    .body(body.toJSONString(), "application/json")
                    .timeout(timeout)
                    .execute();

            String responseBody = response.body();
            log.info("钉钉推送响应：{}", responseBody);

            JSONObject result = JSON.parseObject(responseBody);
            return result.getIntValue("errcode") == 0;
        } catch (Exception e) {
            log.error("钉钉推送失败，webhook: {}", webhook, e);
            return false;
        }
    }

    private String buildUrl(String webhook, String secret) {
        if (webhook != null && webhook.startsWith("https://")) {
            if (secret != null && !secret.isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String stringToSign = timestamp + "\n" + secret;
                try {
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                    byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
                    String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
                    return webhook + "&timestamp=" + timestamp + "&sign=" + sign;
                } catch (Exception e) {
                    log.warn("钉钉签名失败，使用原始webhook", e);
                }
            }
            return webhook;
        }

        StringBuilder url = new StringBuilder(BASE_URL);
        if (!webhook.contains("access_token")) {
            url.append("?access_token=").append(webhook);
        } else {
            url.append("?").append(webhook);
        }

        if (secret != null && !secret.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
                String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
                url.append("&timestamp=").append(timestamp).append("&sign=").append(sign);
            } catch (Exception e) {
                log.warn("钉钉签名失败", e);
            }
        }
        return url.toString();
    }

    private JSONObject buildMessageBody(String messageFormat, String title, String content, Map<String, Object> extra) {
        JSONObject body = new JSONObject();
        body.put("msgtype", resolveMsgType(messageFormat));

        switch (messageFormat != null ? messageFormat : "MARKDOWN") {
            case "MARKDOWN":
                JSONObject markdown = new JSONObject();
                markdown.put("title", title);
                markdown.put("text", content);
                body.put("markdown", markdown);
                break;
            case "CARD":
                JSONObject actionCard = new JSONObject();
                actionCard.put("title", title);
                actionCard.put("text", content);
                actionCard.put("btnOrientation", "0");
                if (extra != null && extra.containsKey("singleTitle")) {
                    actionCard.put("singleTitle", extra.get("singleTitle"));
                    actionCard.put("singleURL", extra.getOrDefault("singleURL", ""));
                }
                body.put("actionCard", actionCard);
                body.put("msgtype", "actionCard");
                break;
            default:
                JSONObject text = new JSONObject();
                text.put("content", content);
                body.put("text", text);
                body.put("msgtype", "text");
                break;
        }

        return body;
    }

    private String resolveMsgType(String messageFormat) {
        if ("CARD".equals(messageFormat)) {
            return "actionCard";
        }
        if ("MARKDOWN".equals(messageFormat)) {
            return "markdown";
        }
        return "text";
    }
}
