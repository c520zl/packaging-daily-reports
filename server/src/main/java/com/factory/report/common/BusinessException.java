package com.factory.report.common;

import lombok.Getter;

/** 业务异常：message 面向用户展示，code 对应 API.md §1.2 错误码 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }
}
