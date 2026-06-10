package com.weiqiang.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.pojo.Result;
import com.weiqiang.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器，用于拦截非白名单请求，验证请求头中的 JWT 令牌
 */
@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final String HEADER_TOKEN_NAME = "token";
    private static final String NOT_LOGIN_MSG = "NOT_LOGIN";
    private static final String METHOD_OPTIONS = "OPTIONS";

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求以避免跨域预检问题
        if (METHOD_OPTIONS.equals(request.getMethod())) {
            return true;
        }

        // 2. 获取请求头中的 token
        final String token = request.getHeader(HEADER_TOKEN_NAME);

        // 3. 验证 token 存在性
        if (!StringUtils.hasText(token)) {
            log.warn("请求头缺少 token，请求路径: {}，拦截处理", request.getRequestURI());
            return handleUnauthenticated(response);
        }

        // 4. 验证并解析 token
        try {
            JwtUtils.parseToken(token);
            return true;
        } catch (final Exception e) {
            log.warn("token 验证失败: {}，请求路径: {}，拦截处理", token, request.getRequestURI(), e);
            return handleUnauthenticated(response);
        }
    }

    /**
     * 处理未授权情况，返回 HTTP 401 和自定义 Result 错误信息
     */
    private boolean handleUnauthenticated(final HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");
        final Result errorResult = Result.error(NOT_LOGIN_MSG);
        final ObjectMapper objectMapper = new ObjectMapper();
        final String json = objectMapper.writeValueAsString(errorResult);
        response.getWriter().write(json);
        return false;
    }
}
