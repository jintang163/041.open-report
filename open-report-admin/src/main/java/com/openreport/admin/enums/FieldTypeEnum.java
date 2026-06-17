package com.openreport.admin.enums;

import lombok.Getter;

@Getter
public enum FieldTypeEnum {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数值"),
    DATE("DATE", "日期"),
    DATETIME("DATETIME", "日期时间"),
    BOOLEAN("BOOLEAN", "布尔值");

    private final String code;
    private final String desc;

    FieldTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FieldTypeEnum getByCode(String code) {
        for (FieldTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return STRING;
    }
}
