package com.demo.WiseNest.exception;

import lombok.Getter;

/**
 * 自定义错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(410, "请求参数错误"),
    NOT_LOGIN_ERROR(411, "未登录"),
    NO_AUTH_ERROR(412, "无权限"),
    NOT_FOUND_ERROR(413, "请求数据不存在"),
    FORBIDDEN_ERROR(414, "禁止访问"),
    SYSTEM_ERROR(510, "系统内部异常"),
    OPERATION_ERROR(511, "操作失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
