package com.weiqiang.service.impl;

import com.weiqiang.dao.UserDao;
import com.weiqiang.exception.BusinessException;
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
    private final com.weiqiang.dao.EquipmentDao equipmentDao;
    private final com.weiqiang.dao.EquipmentClaimDao equipmentClaimDao;

    private static final int DEFAULT_ROLE = 0;
    private static final int CLAIMS_MAP_CAPACITY = 5;

    @Override
    public Result login(final String username, final String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("用户名或密码不能为空");
        }

        final User dbUser = userDao.getByUsername(username);
        if (dbUser == null) {
            log.warn("登录失败：用户 {} 不存在", username);
            return Result.error("用户名或密码错误");
        }

        final String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!Objects.equals(dbUser.getPassword(), md5Password)) {
            log.warn("登录失败：用户 {} 密码不匹配", username);
            return Result.error("用户名或密码错误");
        }

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

        final User dbUser = userDao.getByUsername(user.getUsername());
        if (dbUser != null) {
            log.warn("注册失败：用户名 {} 已存在", user.getUsername());
            return Result.error("用户名已存在");
        }

        final String rawPassword = user.getPassword();
        final String md5Password = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        user.setPassword(md5Password);
        user.setRole(DEFAULT_ROLE);

        final LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteUser(final Integer id) {
        if (id == null) {
            return Result.error("用户ID不能为空");
        }
        
        final User user = userDao.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 1. 级联清退保管的设备并写审计日志（去除保管阻断）
        List<com.weiqiang.pojo.Equipment> equipmentList = equipmentDao.mutiSelect(
                "SELECT equip_id equipId, custodian FROM equipment WHERE custodian = ?",
                com.weiqiang.pojo.Equipment.class, user.getUsername());

        if (equipmentList != null && !equipmentList.isEmpty()) {
            userDao.update("UPDATE equipment SET custodian = NULL WHERE custodian = ?", user.getUsername());
            for (com.weiqiang.pojo.Equipment eq : equipmentList) {
                com.weiqiang.pojo.EquipmentClaim claim = new com.weiqiang.pojo.EquipmentClaim();
                claim.setEquipId(eq.getEquipId());
                claim.setApplicant(user.getUsername());
                claim.setApprover(null);
                claim.setStatus(4); // 已退还
                claim.setRemark("用户被删除导致保管关系自动清退");
                equipmentClaimDao.addClaim(claim);
            }
        }

        // 2. 级联校验未完结工单 (作为报修人且工单状态 != 2，或作为被指派人且工单状态 != 2)
        final String checkMaintSql = "SELECT COUNT(*) FROM maintenance_record WHERE (reporter = ? OR maint_person_id = ?) AND maint_status != 2";
        final Long maintCount = (Long) userDao.singleSelect(checkMaintSql, user.getUsername(), id);
        if (maintCount != null && maintCount > 0) {
            log.warn("级联校验拦截：用户 {} 尚有未完结的维保工单，拒绝删除", user.getUsername());
            throw new BusinessException("操作失败：该用户尚有未完结的检修工单，无法删除！");
        }

        // 3. 校验通过，执行逻辑删除或物理删除
        final String deleteSql = "DELETE FROM sys_user WHERE id = ?";
        final int rows = userDao.update(deleteSql, id);
        if (rows > 0) {
            log.info("删除用户成功，用户ID: {}, 用户名: {}", id, user.getUsername());
            return Result.success();
        }
        return Result.error("删除用户失败");
    }
}
