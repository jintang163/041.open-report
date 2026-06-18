package com.openreport.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComponentSourceEnum {

    OFFICIAL(1, "官方"),
    COMMUNITY(2, "社区");

    private final Integer code;
    private final String desc;
}
