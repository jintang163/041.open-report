package com.openreport.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    BAD_REQUEST(400, "请求参数错误"),

    LOGIN_ERROR(1001, "用户名或密码错误"),
    TOKEN_EXPIRED(1002, "Token已过期"),
    TOKEN_INVALID(1003, "Token无效"),
    USER_NOT_FOUND(1004, "用户不存在"),
    USER_DISABLED(1005, "用户已被禁用"),
    USER_ALREADY_EXISTS(1006, "用户已存在"),

    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_ALREADY_EXISTS(2002, "数据已存在"),
    DATA_VALIDATE_ERROR(2003, "数据校验失败"),
    DATA_DELETE_ERROR(2004, "数据删除失败"),

    DATA_SOURCE_CONNECT_ERROR(3001, "数据源连接失败"),
    DATA_SOURCE_NOT_FOUND(3002, "数据源不存在"),
    REPORT_NOT_FOUND(3003, "报表不存在"),
    REPORT_EXECUTE_ERROR(3004, "报表执行失败"),
    REPORT_PUBLISH_ERROR(3005, "报表发布失败"),

    TEMPLATE_LOCKED(4001, "模板正在被编辑"),
    TEMPLATE_LOCK_ACQUIRE_FAILED(4002, "获取编辑锁失败"),
    TEMPLATE_LOCK_NOT_OWNER(4003, "您不是当前编辑者，无法操作");

    private final Integer code;
    private final String message;
}
