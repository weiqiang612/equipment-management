package com.weiqiang.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRoles {
    /**
     * 允许访问的角色列表
     * 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员
     */
    int[] value();
}
