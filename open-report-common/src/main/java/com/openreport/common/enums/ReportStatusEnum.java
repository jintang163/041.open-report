package com.openreport.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatusEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线"),
    DELETED(9, "已删除");

    private final Integer code;
    private final String name;

    public static ReportStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReportStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
