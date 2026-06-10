package com.weiqiang.exception;

/**
 * 权限不足异常 (HTTP 403)
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(final String message) {
        super(message);
    }
}
