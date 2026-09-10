package com.brightminds.rebuild.common.response;

/**
 * 所有 HTTP 接口共用的响应外壳。
 *
 * @param code    业务状态码
 * @param message 面向调用方的简短说明
 * @param data    实际响应数据
 * @param <T>     数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
}
