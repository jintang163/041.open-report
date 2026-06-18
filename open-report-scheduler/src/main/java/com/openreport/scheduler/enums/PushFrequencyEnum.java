package com.openreport.scheduler.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PushFrequencyEnum {

    DAILY("DAILY", "每天"),
    WEEKLY("WEEKLY", "每周"),
    MONTHLY("MONTHLY", "每月");

    private final String code;
    private final String name;

    public static PushFrequencyEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (PushFrequencyEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
