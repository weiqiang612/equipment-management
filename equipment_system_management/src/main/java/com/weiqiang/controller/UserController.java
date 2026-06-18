package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.dto.AdminResetPasswordRequest;
import com.weiqiang.dto.ChangePasswordRequest;
import com.weiqiang.common.Result;
import com.weiqiang.entity.User;
import com.weiqiang.dto.UserProfileUpdateRequest;
import com.weiqiang.vo.UserVO;
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
        log.info("修改用户角色与单位请求，目标用户ID: {}, 期望修改的角色: {}, 所属单位: {}", 
                 user.getId(), user.getRole(), user.getUnitCode());
        return userService.updateRole(user.getId(), user.getRole(), user.getUnitCode());
    }

    /**
     * 统一更新用户资料接口（仅限系统管理员 role=3 访问）
     */
    @PutMapping("/{id}")
    @RequiresRoles(3)
    public Result updateUserProfile(@PathVariable("id") final Integer id,
                                    @RequestBody final UserProfileUpdateRequest request) {
        log.info("修改用户资料请求，目标用户ID: {}, 角色: {}, 所属单位: {}", id, request.getRole(), request.getUnitCode());
        return userService.updateUserProfile(id, request);
    }

    /**
     * 获取用户列表接口（仅限资产管理员和系统管理员访问）
     */
    @GetMapping
    @RequiresRoles({2, 3})
    public Result getUsers() {
        log.info("接收到获取用户列表请求");
        final List<User> users = userService.listAll();
        final List<UserVO> userVOs = new java.util.ArrayList<>();
        for (final User u : users) {
            userVOs.add(new UserVO(
                u.getId(), u.getUsername(), u.getRealName(), u.getRole(),
                u.getCreateTime(), u.getUpdateTime(), u.getUnitCode()
            ));
        }
        return Result.success(userVOs);
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
     * 管理员重置他人密码
     */
    @PutMapping("/{id}/password/reset")
    @RequiresRoles(3)
    public Result resetUserPassword(@PathVariable("id") final Integer id,
                                    @RequestBody final AdminResetPasswordRequest request) {
        log.info("管理员重置密码请求，目标用户ID: {}", id);
        return userService.resetUserPassword(id, request.getNewPassword());
    }

    /**
     * 当前登录用户修改本人密码
     */
    @PutMapping("/password")
    public Result changeCurrentPassword(@RequestBody final ChangePasswordRequest request) {
        log.info("当前登录用户修改本人密码请求");
        return userService.changeCurrentUserPassword(request.getOldPassword(), request.getNewPassword());
    }

    /**
     * 获取所有维修工程师列表 (已登录用户均可访问)
     */
    @GetMapping("/maintainers")
    public Result getMaintainers() {
        log.info("接收到获取维修工列表请求");
        final List<User> users = userService.listAll();
        final List<UserVO> maintainers = new java.util.ArrayList<>();
        for (final User u : users) {
            if (u.getRole() != null && u.getRole() == 1) {
                maintainers.add(new UserVO(
                    u.getId(), u.getUsername(), u.getRealName(), u.getRole(),
                    u.getCreateTime(), u.getUpdateTime(), u.getUnitCode()
                ));
            }
        }
        return Result.success(maintainers);
    }
}
