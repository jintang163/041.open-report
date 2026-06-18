package com.openreport.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalStatusEnum {

    PENDING(0, "待审批"),
    APPROVED(1, "审批通过"),
    REJECTED(2, "审批拒绝"),
    CANCELLED(3, "已撤销");

    private final Integer code;
    private final String name;

    public static ApprovalStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApprovalStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
