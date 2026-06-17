package com.openreport.admin.enums;

import lombok.Getter;

@Getter
public enum RowStatusEnum {

    INSERT("INSERT", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String desc;

    RowStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RowStatusEnum getByCode(String code) {
        for (RowStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
