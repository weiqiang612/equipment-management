package com.weiqiang.service;

import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;

import java.util.List;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 用户登录校验，成功后返回包含 JWT 令牌的 Result
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录结果
     */
    Result login(final String username, final String password);

    /**
     * 用户注册业务，进行用户名防重校验、密码 MD5 加密，强制角色默认为 0
     *
     * @param user 用户注册信息
     * @return 注册结果
     */
    Result register(final User user);

    /**
     * 修改用户角色
     *
     * @param id   用户ID
     * @param role 新角色值
     * @return 操作结果
     */
    Result updateRole(final Integer id, final Integer role);

    /**
     * 获取所有用户列表
     *
     * @return 用户列表
     */
    List<User> listAll();
}
