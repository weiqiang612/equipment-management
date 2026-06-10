package com.weiqiang.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程上下文工具类，用于在同个请求线程内传递当前登录用户信息
 */
public class BaseContext {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置线程变量
     */
    public static void set(final String key, final Object value) {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new HashMap<>();
            THREAD_LOCAL.set(map);
        }
        map.put(key, value);
    }

    /**
     * 获取线程变量
     */
    public static Object get(final String key) {
        final Map<String, Object> map = THREAD_LOCAL.get();
        return map == null ? null : map.get(key);
    }

    /**
     * 清理线程变量，防止内存溢出
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static Integer getCurrentId() {
        return (Integer) get("id");
    }

    public static void setCurrentId(final Integer id) {
        set("id", id);
    }

    public static String getCurrentName() {
        return (String) get("username");
    }

    public static void setCurrentName(final String username) {
        set("username", username);
    }

    public static Integer getCurrentRole() {
        return (Integer) get("role");
    }

    public static void setCurrentRole(final Integer role) {
        set("role", role);
    }

    public static String getCurrentUnitCode() {
        return (String) get("unitCode");
    }

    public static void setCurrentUnitCode(final String unitCode) {
        set("unitCode", unitCode);
    }
}
