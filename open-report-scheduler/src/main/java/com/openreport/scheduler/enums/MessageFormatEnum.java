package com.openreport.scheduler.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageFormatEnum {

    MARKDOWN("MARKDOWN", "Markdown"),
    CARD("CARD", "卡片消息"),
    TEXT("TEXT", "纯文本");

    private final String code;
    private final String name;

    public static MessageFormatEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (MessageFormatEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
