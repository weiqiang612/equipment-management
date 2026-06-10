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
                "create_time AS createTime, update_time AS updateTime FROM sys_user WHERE username = ?";
        return selectOne(sql, User.class, username);
    }

    /**
     * 插入新用户
     *
     * @param user 用户实体
     * @return 影响行数
     */
    public int insert(final User user) {
        final String sql = "INSERT INTO sys_user (username, password, real_name, role, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        return update(sql,
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getRole(),
                user.getCreateTime(),
                user.getUpdateTime());
    }

    /**
     * 更新用户角色
     *
     * @param id 用户ID
     * @param role 新角色值
     * @return 影响行数
     */
    public int updateRole(final Integer id, final Integer role) {
        final String sql = "UPDATE sys_user SET role = ?, update_time = NOW() WHERE id = ?";
        return update(sql, role, id);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    public List<User> listAll() {
        final String sql = "SELECT id, username, password, real_name AS realName, role, " +
                "create_time AS createTime, update_time AS updateTime FROM sys_user";
        return mutiSelect(sql, User.class, (Object[]) null);
    }
}
