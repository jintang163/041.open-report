package com.openreport.scheduler.service;

import java.util.Map;

public interface ChannelNotifyService {

    String getChannel();

    boolean send(String webhook, String secret, String messageFormat, String title, String content, Map<String, Object> extra);
}
