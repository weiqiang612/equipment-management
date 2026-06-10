package com.weiqiang.service;

import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;

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
     * 修改用户角色
     */
    Result updateRole(final Integer id, final Integer role);

    /**
     * 获取所有用户列表
     */
    List<User> listAll();

    /**
     * 删除用户（执行级联资产和未完结工单的安全校验拦截）
     */
    Result deleteUser(final Integer id);
}
