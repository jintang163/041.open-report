package com.openreport.scheduler.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotifyStatusEnum {

    PENDING("PENDING", "待推送"),
    SUCCESS("SUCCESS", "推送成功"),
    FAIL("FAIL", "推送失败"),
    RETRY("RETRY", "重试中");

    private final String code;
    private final String name;

    public static NotifyStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotifyStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
