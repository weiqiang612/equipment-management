package com.weiqiang.controller;

import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import com.weiqiang.service.UserService;
import com.weiqiang.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 常量定义，避开魔法值
    private static final String HEADER_TOKEN = "token";
    private static final String CLAIM_ROLE = "role";
    private static final int ADMIN_ROLE_VALUE = 3;

    /**
     * 用户登录接口
     *
     * @param user 包含 username 和 password 的请求体
     * @return 登录结果（成功则携带 Token）
     */
    @PostMapping("/login")
    public Result login(@RequestBody final User user) {
        log.info("接收到登录请求，用户名: {}", user.getUsername());
        return userService.login(user.getUsername(), user.getPassword());
    }

    /**
     * 用户注册接口
     *
     * @param user 包含注册信息的请求体
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody final User user) {
        log.info("接收到注册请求，用户名: {}", user.getUsername());
        return userService.register(user);
    }

    /**
     * 修改用户角色接口（仅限系统管理员 role=3 访问）
     *
     * @param user 包含 id 和 role 的请求体
     * @param token 请求头中的 token
     * @param response 用于设置 403 状态码
     * @return 修改结果
     */
    @PutMapping("/role")
    public Result updateRole(@RequestBody final User user,
                             @RequestHeader(value = HEADER_TOKEN, required = false) final String token,
                             final HttpServletResponse response) {
        log.info("修改用户角色请求，目标用户ID: {}, 期望修改的角色: {}", user.getId(), user.getRole());

        // 鉴权逻辑：解析 Token 检查 role
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        try {
            final Claims claims = JwtUtils.parseToken(token);
            final Integer currentUserRole = claims.get(CLAIM_ROLE, Integer.class);

            if (currentUserRole == null || currentUserRole != ADMIN_ROLE_VALUE) {
                log.warn("越权访问拦截：用户角色为 {}，尝试调用修改用户角色接口", currentUserRole);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Result.error("权限不足");
            }
        } catch (final Exception e) {
            log.error("Token 解析失败或过期", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        return userService.updateRole(user.getId(), user.getRole());
    }

    /**
     * 获取用户列表接口（仅限系统管理员 role=3 访问）
     *
     * @param token 请求头中的 token
     * @param response 用于设置 403 状态码
     * @return 用户列表数据
     */
    @GetMapping
    public Result getUsers(@RequestHeader(value = HEADER_TOKEN, required = false) final String token,
                           final HttpServletResponse response) {
        log.info("接收到获取用户列表请求");

        // 鉴权逻辑：解析 Token 检查 role
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        try {
            final Claims claims = JwtUtils.parseToken(token);
            final Integer currentUserRole = claims.get(CLAIM_ROLE, Integer.class);

            if (currentUserRole == null || currentUserRole != ADMIN_ROLE_VALUE) {
                log.warn("越权访问拦截：用户角色为 {}，尝试获取用户列表", currentUserRole);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Result.error("权限不足");
            }
        } catch (final Exception e) {
            log.error("Token 解析失败或过期", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        final List<User> users = userService.listAll();
        return Result.success(users);
    }

    /**
     * 获取所有维修工程师列表 (仅限已登录用户访问)
     *
     * @param token 请求头中的 token
     * @param response 用于设置 403 状态码
     * @return 维修工程师列表
     */
    @GetMapping("/maintainers")
    public Result getMaintainers(@RequestHeader(value = HEADER_TOKEN, required = false) final String token,
                                 final HttpServletResponse response) {
        log.info("接收到获取维修工列表请求");

        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        try {
            final Claims claims = JwtUtils.parseToken(token);
            final Integer currentUserRole = claims.get(CLAIM_ROLE, Integer.class);

            if (currentUserRole == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Result.error("权限不足");
            }
        } catch (final Exception e) {
            log.error("Token 解析失败或过期", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("未登录");
        }

        final List<User> users = userService.listAll();
        final List<User> maintainers = new java.util.ArrayList<>();
        for (final User u : users) {
            if (u.getRole() != null && u.getRole() == 1) {
                maintainers.add(u);
            }
        }
        return Result.success(maintainers);
    }
}
