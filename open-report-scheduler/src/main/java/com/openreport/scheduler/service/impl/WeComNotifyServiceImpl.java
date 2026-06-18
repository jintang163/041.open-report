package com.openreport.scheduler.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openreport.scheduler.service.ChannelNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class WeComNotifyServiceImpl implements ChannelNotifyService {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send";

    @Override
    public String getChannel() {
        return "WECOM";
    }

    @Override
    public boolean send(String webhook, String secret, String messageFormat, String title, String content, Map<String, Object> extra) {
        try {
            String url = buildUrl(webhook);
            JSONObject body = buildMessageBody(messageFormat, title, content, extra);

            HttpResponse response = HttpRequest.post(url)
                    .body(body.toJSONString(), "application/json")
                    .timeout(10000)
                    .execute();

            String responseBody = response.body();
            log.info("企微推送响应：{}", responseBody);

            JSONObject result = JSON.parseObject(responseBody);
            return result.getIntValue("errcode") == 0;
        } catch (Exception e) {
            log.error("企微推送失败，webhook: {}", webhook, e);
            return false;
        }
    }

    private String buildUrl(String webhook) {
        if (webhook != null && webhook.startsWith("https://")) {
            return webhook;
        }
        return BASE_URL + "?key=" + webhook;
    }

    private JSONObject buildMessageBody(String messageFormat, String title, String content, Map<String, Object> extra) {
        JSONObject body = new JSONObject();

        switch (messageFormat != null ? messageFormat : "MARKDOWN") {
            case "MARKDOWN":
                body.put("msgtype", "markdown");
                JSONObject markdown = new JSONObject();
                markdown.put("content", content);
                body.put("markdown", markdown);
                break;
            case "CARD":
                body.put("msgtype", "interactive");
                JSONObject card = new JSONObject();
                card.put("header", buildCardHeader(title));
                card.put("elements", buildCardElements(content, extra));
                body.put("card", card);
                break;
            default:
                body.put("msgtype", "text");
                JSONObject text = new JSONObject();
                text.put("content", content);
                body.put("text", text);
                break;
        }

        return body;
    }

    private JSONObject buildCardHeader(String title) {
        JSONObject header = new JSONObject();
        header.put("title", title);
        header.put("theme", "blue");
        JSONObject template = new JSONObject();
        template.put("header_type", 1);
        return header;
    }

    private JSONObject[] buildCardElements(String content, Map<String, Object> extra) {
        JSONObject element = new JSONObject();
        element.put("tag", "markdown");
        element.put("content", content);
        return new JSONObject[]{element};
    }
}
