package com.weiqiang.exception;

/**
 * 业务逻辑流转约束异常 (HTTP 200/400, 返回 Result.error)
 */
public class BusinessException extends RuntimeException {
    public BusinessException(final String message) {
        super(message);
    }
}
