package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import com.weiqiang.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Result login(@RequestBody final User user) {
        log.info("接收到登录请求，用户名: {}", user.getUsername());
        return userService.login(user.getUsername(), user.getPassword());
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public Result register(@RequestBody final User user) {
        log.info("接收到注册请求，用户名: {}", user.getUsername());
        return userService.register(user);
    }

    /**
     * 修改用户角色接口（仅限系统管理员 role=3 访问）
     */
    @PutMapping("/role")
    @RequiresRoles(3)
    public Result updateRole(@RequestBody final User user) {
        log.info("修改用户角色请求，目标用户ID: {}, 期望修改的角色: {}", user.getId(), user.getRole());
        return userService.updateRole(user.getId(), user.getRole());
    }

    /**
     * 获取用户列表接口（仅限系统管理员 role=3 访问）
     */
    @GetMapping
    @RequiresRoles(3)
    public Result getUsers() {
        log.info("接收到获取用户列表请求");
        final List<User> users = userService.listAll();
        return Result.success(users);
    }

    /**
     * 删除用户接口（仅限系统管理员 role=3 访问）
     */
    @DeleteMapping("/{id}")
    @RequiresRoles(3)
    public Result deleteUser(@PathVariable("id") final Integer id) {
        log.info("接收到删除用户请求，用户ID: {}", id);
        return userService.deleteUser(id);
    }

    /**
     * 获取所有维修工程师列表 (已登录用户均可访问)
     */
    @GetMapping("/maintainers")
    public Result getMaintainers() {
        log.info("接收到获取维修工列表请求");
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
}
