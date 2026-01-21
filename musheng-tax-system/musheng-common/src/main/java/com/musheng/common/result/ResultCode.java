package com.musheng.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 * P0需求: code=0表示成功
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功 - 状态码必须为0
     */
    SUCCESS(0, "success"),

    /**
     * 通用失败
     */
    FAILED(1, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    INTERNAL_ERROR(500, "系统内部错误");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态消息
     */
    private final String message;
}
