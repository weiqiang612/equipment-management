package com.weiqiang.service.impl;

import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import com.weiqiang.service.UserService;
import com.weiqiang.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    // 常量定义，避免魔法值
    private static final int DEFAULT_ROLE = 0;
    private static final int CLAIMS_MAP_CAPACITY = 5; // (3 expected / 0.75) + 1 = 5

    @Override
    public Result login(final String username, final String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("用户名或密码不能为空");
        }

        // 1. 查询用户
        final User dbUser = userDao.getByUsername(username);
        if (dbUser == null) {
            log.warn("登录失败：用户 {} 不存在", username);
            return Result.error("用户名或密码错误");
        }

        // 2. 对比密码 (MD5 哈希比较)
        final String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!Objects.equals(dbUser.getPassword(), md5Password)) {
            log.warn("登录失败：用户 {} 密码不匹配", username);
            return Result.error("用户名或密码错误");
        }

        // 3. 生成 JWT Token，设置自定义 Claims
        final Map<String, Object> claims = new HashMap<>(CLAIMS_MAP_CAPACITY);
        claims.put("id", dbUser.getId());
        claims.put("username", dbUser.getUsername());
        claims.put("role", dbUser.getRole());
        claims.put("realName", dbUser.getRealName());

        final String token = JwtUtils.generateToken(claims);
        log.info("用户 {} 登录成功，下发 Token", username);
        return Result.success(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(final User user) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }

        // 1. 防重校验
        final User dbUser = userDao.getByUsername(user.getUsername());
        if (dbUser != null) {
            log.warn("注册失败：用户名 {} 已存在", user.getUsername());
            return Result.error("用户名已存在");
        }

        // 2. 密码 MD5 转换
        final String rawPassword = user.getPassword();
        final String md5Password = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        user.setPassword(md5Password);

        // 3. 强制角色默认为 0 (设备操作员)
        user.setRole(DEFAULT_ROLE);

        // 4. 设置时间属性
        final LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        // 5. 写入数据库
        final int rows = userDao.insert(user);
        if (rows > 0) {
            log.info("用户 {} 注册成功，默认分配角色为操作员", user.getUsername());
            return Result.success();
        } else {
            log.error("用户 {} 注册时数据库插入记录失败", user.getUsername());
            return Result.error("注册失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateRole(final Integer id, final Integer role) {
        if (id == null || role == null) {
            return Result.error("用户ID或角色值不能为空");
        }

        // 校验角色范围合法性 (0-3)
        if (role < 0 || role > 3) {
            return Result.error("非法的角色值");
        }

        final int rows = userDao.updateRole(id, role);
        if (rows > 0) {
            log.info("修改用户角色成功，用户ID: {}, 新角色: {}", id, role);
            return Result.success();
        }
        log.warn("修改用户角色失败：未找到ID为 {} 的用户", id);
        return Result.error("修改用户角色失败，用户不存在");
    }

    @Override
    public List<User> listAll() {
        return userDao.listAll();
    }
}
