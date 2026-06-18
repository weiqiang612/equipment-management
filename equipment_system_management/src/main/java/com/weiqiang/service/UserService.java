package com.weiqiang.service;

import com.weiqiang.common.Result;
import com.weiqiang.entity.User;
import com.weiqiang.dto.UserProfileUpdateRequest;

import java.util.List;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 用户登录校验
     */
    Result login(final String username, final String password);

    /**
     * 用户注册业务
     */
    Result register(final User user);

    /**
     * 修改用户角色与所属单位
     */
    Result updateRole(final Integer id, final Integer role, final String unitCode);

    /**
     * 统一更新用户资料
     */
    Result updateUserProfile(final Integer id, final UserProfileUpdateRequest request);

    /**
     * 获取所有用户列表
     */
    List<User> listAll();

    /**
     * 删除用户（执行级联资产和未完结工单的安全校验拦截）
     */
    Result deleteUser(final Integer id);

    /**
     * 管理员重置用户密码
     */
    Result resetUserPassword(final Integer id, final String newPassword);

    /**
     * 当前登录用户修改本人密码
     */
    Result changeCurrentUserPassword(final String oldPassword, final String newPassword);
}
