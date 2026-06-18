package com.weiqiang.exception;

import com.weiqiang.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获 403 越权异常，设置 HTTP 状态码为 403
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result handleForbiddenException(final ForbiddenException e, final HttpServletResponse response) {
        log.warn("越权访问拦截：{}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 设置 403 状态码
        return Result.error("权限不足");
    }

    /**
     * 捕获业务流转约束异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(final BusinessException e) {
        log.warn("业务流转约束拦截：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 捕获全局未处理的通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(final Exception e) {
        log.error("系统运行异常：", e);

        // 判断是否为数据库主键重复异常
        if (e.getCause() instanceof SQLIntegrityConstraintViolationException ||
                (e.getMessage() != null && e.getMessage().contains("Duplicate entry"))) {
            return Result.error("操作失败：唯一编号已存在，请勿重复添加！");
        }

        return Result.error("操作失败：" + e.getMessage());
    }
}
