package com.openreport.scheduler.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscribeChannelEnum {

    DINGTALK("DINGTALK", "钉钉"),
    WECOM("WECOM", "企业微信"),
    EMAIL("EMAIL", "邮件");

    private final String code;
    private final String name;

    public static SubscribeChannelEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SubscribeChannelEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
