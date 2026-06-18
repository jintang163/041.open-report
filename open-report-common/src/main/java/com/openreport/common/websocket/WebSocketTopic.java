package com.openreport.common.websocket;

public interface WebSocketTopic {

    String ALL = "ALL";

    String REPORT_PREFIX = "REPORT:";

    String REPORT_LIST = "REPORT:LIST";

    String APPROVAL_LIST = "APPROVAL:LIST";

    static String report(Long templateId) {
        return REPORT_PREFIX + templateId;
    }

    static String approval(Long templateId) {
        return "APPROVAL:" + templateId;
    }
}
