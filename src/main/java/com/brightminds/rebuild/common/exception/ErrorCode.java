package com.brightminds.rebuild.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一维护 HTTP 状态和业务错误码，避免散落魔法数字。
 */
public enum ErrorCode {
    INVALID_REQUEST(40001, HttpStatus.BAD_REQUEST, "请求参数不合法"),
    MALFORMED_REQUEST(40002, HttpStatus.BAD_REQUEST, "请求体格式错误"),
    SESSION_ALREADY_EXISTS(40901, HttpStatus.CONFLICT, "相同儿童和主题的学习会话已存在"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
