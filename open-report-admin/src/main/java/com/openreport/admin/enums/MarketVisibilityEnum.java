package com.openreport.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketVisibilityEnum {

    PRIVATE(0, "私有"),
    PUBLIC(1, "公开");

    private final Integer code;
    private final String desc;
}
