package com.openreport.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalTypeEnum {

    PUBLISH(1, "发布审批"),
    OFFLINE(2, "下线审批"),
    MODIFY(3, "修改审批");

    private final Integer code;
    private final String name;

    public static ApprovalTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApprovalTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
