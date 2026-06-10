package com.weiqiang.aop;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.utils.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 权限拦截切面，控制基于角色的访问准入
 */
@Slf4j
@Aspect
@Component
public class SecurityAspect {

    /**
     * 在带有 @RequiresRoles 注解的方法或类执行前，校验角色
     */
    @Before("@annotation(com.weiqiang.anno.RequiresRoles) || @within(com.weiqiang.anno.RequiresRoles)")
    public void checkRole(final JoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();

        // 1. 优先获取方法上的注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        if (requiresRoles == null) {
            // 2. 如果方法上没有，再获取类上的注解
            requiresRoles = joinPoint.getTarget().getClass().getAnnotation(RequiresRoles.class);
        }

        if (requiresRoles == null) {
            return;
        }

        // 3. 从 BaseContext 获取当前用户的角色
        final Integer role = BaseContext.getCurrentRole();
        if (role == null) {
            log.warn("越权访问拦截：未检测到当前请求的用户角色");
            throw new ForbiddenException("权限不足");
        }

        // 4. 校验角色是否属于允许的列表
        final int[] allowedRoles = requiresRoles.value();
        boolean hasRole = false;
        for (final int allowedRole : allowedRoles) {
            if (allowedRole == role) {
                hasRole = true;
                break;
            }
        }

        if (!hasRole) {
            log.warn("越权访问拦截：当前用户角色为 {}，无权访问 {} 接口。允许的角色为: {}", 
                    role, method.getName(), Arrays.toString(allowedRoles));
            throw new ForbiddenException("权限不足");
        }
    }
}
