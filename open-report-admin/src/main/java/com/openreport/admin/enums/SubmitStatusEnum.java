package com.openreport.admin.enums;

import lombok.Getter;

@Getter
public enum SubmitStatusEnum {

    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    PARTIAL("PARTIAL", "部分成功"),
    FAIL("FAIL", "失败"),
    PENDING("PENDING", "待执行");

    private final String code;
    private final String desc;

    SubmitStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SubmitStatusEnum getByCode(String code) {
        for (SubmitStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
