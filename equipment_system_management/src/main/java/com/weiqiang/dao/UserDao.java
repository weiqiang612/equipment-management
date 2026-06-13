package com.weiqiang.dao;

import com.weiqiang.pojo.User;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户数据访问对象
 * 对应数据库表 sys_user
 */
@Repository
public class UserDao extends BasicDao<User> {

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息，未找到返回 null
     */
    public User getByUsername(final String username) {
        final String sql = "SELECT id, username, password, real_name AS realName, role, " +
                "create_time AS createTime, update_time AS updateTime, unit_code AS unitCode FROM sys_user WHERE username = ?";
        return selectOne(sql, User.class, username);
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息，未找到返回 null
     */
    public User getById(final Integer id) {
        final String sql = "SELECT id, username, password, real_name AS realName, role, " +
                "create_time AS createTime, update_time AS updateTime, unit_code AS unitCode FROM sys_user WHERE id = ?";
        return selectOne(sql, User.class, id);
    }

    /**
     * 插入新用户
     *
     * @param user 用户实体
     * @return 影响行数
     */
    public int insert(final User user) {
        final String sql = "INSERT INTO sys_user (username, password, real_name, role, create_time, update_time, unit_code) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        return update(sql,
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getRole(),
                user.getCreateTime(),
                user.getUpdateTime(),
                user.getUnitCode());
    }

    /**
     * 更新用户角色与所属单位
     *
     * @param id 用户ID
     * @param role 新角色值
     * @param unitCode 新所属单位代码
     * @return 影响行数
     */
    public int updateRoleAndDept(final Integer id, final Integer role, final String unitCode) {
        final String sql = "UPDATE sys_user SET role = ?, unit_code = ?, update_time = NOW() WHERE id = ?";
        return update(sql, role, unitCode, id);
    }

    /**
     * 更新用户资料
     *
     * @param id 用户ID
     * @param realName 真实姓名
     * @param role 角色
     * @param unitCode 所属单位
     * @return 影响行数
     */
    public int updateUserProfile(final Integer id, final String realName, final Integer role, final String unitCode) {
        final String sql = "UPDATE sys_user SET real_name = ?, role = ?, unit_code = ?, update_time = NOW() WHERE id = ?";
        return update(sql, realName, role, unitCode, id);
    }

    /**
     * 更新用户密码
     *
     * @param id 用户ID
     * @param password 密码MD5
     * @return 影响行数
     */
    public int updatePassword(final Integer id, final String password) {
        final String sql = "UPDATE sys_user SET password = ?, update_time = NOW() WHERE id = ?";
        return update(sql, password, id);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    public List<User> listAll() {
        final String sql = "SELECT id, username, password, real_name AS realName, role, " +
                "create_time AS createTime, update_time AS updateTime, unit_code AS unitCode FROM sys_user";
        return mutiSelect(sql, User.class, (Object[]) null);
    }
}
