package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OperationLogAndDetailTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String opTokenD98;
    private String opTokenD99;
    private String mgrTokenD98;
    private String maintTokenD98;
    private String adminToken;

    private Integer opD98Id;
    private Integer opD99Id;
    private Integer mgrD98Id;
    private Integer maintD98Id;

    @BeforeEach
    public void setup() throws Exception {
        // 确保 operation_log 表存在
        userDao.update("CREATE TABLE IF NOT EXISTS `operation_log` (" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键'," +
                "  `operator` varchar(50) NOT NULL COMMENT '操作人用户名'," +
                "  `operator_role` tinyint(4) NOT NULL COMMENT '操作人角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员'," +
                "  `op_type` varchar(50) NOT NULL COMMENT '操作类型'," +
                "  `target_type` varchar(50) NOT NULL COMMENT '业务对象类型'," +
                "  `target_id` varchar(50) NOT NULL COMMENT '业务对象ID'," +
                "  `op_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'," +
                "  `summary` varchar(500) NOT NULL COMMENT '操作摘要'," +
                "  `status` tinyint(4) NOT NULL COMMENT '状态: 1-成功, 0-失败'," +
                "  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败错误信息'," +
                "  PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';");
        // 确保建表
        userDao.update("CREATE TABLE IF NOT EXISTS `operation_log` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键',\n" +
                "  `operator` varchar(50) NOT NULL COMMENT '操作人用户名',\n" +
                "  `operator_role` tinyint(4) NOT NULL COMMENT '操作人角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员',\n" +
                "  `op_type` varchar(50) NOT NULL COMMENT '操作类型',\n" +
                "  `target_type` varchar(50) NOT NULL COMMENT '业务对象类型',\n" +
                "  `target_id` varchar(50) NOT NULL COMMENT '业务对象ID',\n" +
                "  `op_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',\n" +
                "  `summary` varchar(500) NOT NULL COMMENT '操作摘要',\n" +
                "  `status` tinyint(4) NOT NULL COMMENT '状态: 1-成功, 0-失败',\n" +
                "  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败错误信息',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';");

        // 1. 清理垃圾数据
        // 1. 清理数据
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM transfer_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM scrap_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM equipment WHERE unit_code IN ('D98', 'D99') OR equip_id LIKE 'TE%'");
        userDao.update("UPDATE sys_user SET unit_code = NULL WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'audit_op_%' OR username LIKE 'audit_mgr_%' OR username LIKE 'audit_maint_%'");
        userDao.update("DELETE FROM department WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM category WHERE category_id = 'C99'");
        userDao.update("DELETE FROM operation_log");

        // 2. 初始化部门与分类
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D98', '审计测试部门A', '审计经理A'), ('D99', '审计测试部门B', '审计经理B')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C99', '审计测试分类', 5, 0.05)");

        // 3. 注册并准备 Token
        registerUser("audit_op_d98", "审计操作员A");
        registerUser("audit_op_d99", "审计操作员B");
        registerUser("audit_mgr_d98", "审计管理员A");
        registerUser("audit_maint_d98", "审计维修工A");

        opD98Id = userDao.getByUsername("audit_op_d98").getId();
        opD99Id = userDao.getByUsername("audit_op_d99").getId();
        mgrD98Id = userDao.getByUsername("audit_mgr_d98").getId();
        maintD98Id = userDao.getByUsername("audit_maint_d98").getId();

        userDao.update("UPDATE sys_user SET unit_code = 'D98', role = 0 WHERE id = ?", opD98Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D99', role = 0 WHERE id = ?", opD99Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D98', role = 2 WHERE id = ?", mgrD98Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D98', role = 1 WHERE id = ?", maintD98Id);

        opTokenD98 = loginUser("audit_op_d98");
        opTokenD99 = loginUser("audit_op_d99");
        mgrTokenD98 = loginUser("audit_mgr_d98");
        maintTokenD98 = loginUser("audit_maint_d98");

        // 系统管理员 Token
        final User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("123456");
        MvcResult adminLoginRes = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = (String) objectMapper.readValue(adminLoginRes.getResponse().getContentAsString(), Result.class).getData();
    }

    private void registerUser(String username, String realName) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("123456");
        user.setRealName(realName);
        user.setUnitCode("D98");
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    private String loginUser(String username) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("123456");
        MvcResult res = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn();
        return (String) objectMapper.readValue(res.getResponse().getContentAsString(), Result.class).getData();
    }

    @Test
    public void testOperationLogOnEquipmentAdd() throws Exception {
        // 1. 系统中本来审计日志为 0
        Long initialCount = (Long) userDao.singleSelect("SELECT COUNT(*) FROM operation_log");
        assertEquals(0L, initialCount);

        // 2. 用管理员 Token 新增一个设备
        Equipment equip = new Equipment();
        equip.setEquipId("TE-AUDIT-01");
        equip.setEquipName("审计测试笔记本");
        equip.setModel("ThinkPad X1");
        equip.setStatus("在用");
        equip.setPurchaseDate(LocalDate.now());
        equip.setOriginalValue(new BigDecimal("10000.00"));
        equip.setUnitCode("D98");
        equip.setCategoryId("C99");

        mockMvc.perform(post("/equipments")
                        .header("token", mgrTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isOk());

        // 3. 校验审计日志是否成功写入
        Long afterCount = (Long) userDao.singleSelect("SELECT COUNT(*) FROM operation_log");
        assertEquals(1L, afterCount);

        List<com.weiqiang.pojo.OperationLog> logs = userDao.mutiSelect(
                "SELECT id, operator, operator_role AS operatorRole, op_type AS opType, target_type AS targetType, target_id AS targetId, op_time AS opTime, summary, status, error_msg AS errorMsg FROM operation_log", com.weiqiang.pojo.OperationLog.class, (Object[]) null);
        assertFalse(logs.isEmpty());
        com.weiqiang.pojo.OperationLog log = logs.get(0);
        assertEquals("audit_mgr_d98", log.getOperator());
        assertEquals(2, log.getOperatorRole());
        assertEquals("设备新增", log.getOpType());
        assertEquals("equipment", log.getTargetType());
        assertEquals("TE-AUDIT-01", log.getTargetId());
        assertEquals(1, log.getStatus());

        // 校验日志列表接口是否能正确返回 operatorRealName
        mockMvc.perform(get("/system/log/list")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows[0].operatorRealName").value("审计管理员A"));
    }

    @Test
    public void testAuditLogListAccessControl() throws Exception {
        // 普通用户访问日志列表被拒绝 (Requires Role 3)
        mockMvc.perform(get("/system/log/list")
                        .header("token", opTokenD98))
                .andExpect(status().isForbidden());

        // 资产管理员访问日志列表被拒绝
        mockMvc.perform(get("/system/log/list")
                        .header("token", mgrTokenD98))
                .andExpect(status().isForbidden());

        // 系统管理员访问日志列表成功
        mockMvc.perform(get("/system/log/list")
                        .header("token", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testEquipmentDetailRBAC() throws Exception {
        // 1. 初始化两台设备，一台 D98 (保管人为 audit_op_d98 )，另一台 D99 (无保管人)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) " +
                "VALUES ('TE-AUDIT-02', '设备B', 'M2', '在用', '2026-06-01', 5000.00, 'D98', 'C99', 'audit_op_d98')");
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) " +
                "VALUES ('TE-AUDIT-03', '设备C', 'M3', '在用', '2026-06-01', 6000.00, 'D99', 'C99', NULL)");

        // 2. 验证普通用户 (Role 0) 的水平越权校验
        // 普通用户 A (D98) 查看自己保管 of 设备 TE-AUDIT-02：成功
        mockMvc.perform(get("/equipments/detail/TE-AUDIT-02")
                        .header("token", opTokenD98))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.equipId").value("TE-AUDIT-02"))
                .andExpect(jsonPath("$.data.custodianRealName").value("审计操作员A"))
                .andExpect(jsonPath("$.data.netValue").exists());

        // 普通用户 A (D98) 查看自己未保管的同部门设备（哪怕是同部门也禁止查看，除非是自己保管）：被拒绝 403
        // 另外，普通用户 A 查看 D99 部门的设备 TE-AUDIT-03 (未保管)：被拒绝 403
        mockMvc.perform(get("/equipments/detail/TE-AUDIT-03")
                        .header("token", opTokenD98))
                .andExpect(status().isForbidden());

        // 3. 验证维修工 (Role 1) 和资产管理员 (Role 2) 的部门隔离校验
        // 管理员 A (D98) 查看 D98 的设备 TE-AUDIT-02：成功
        mockMvc.perform(get("/equipments/detail/TE-AUDIT-02")
                        .header("token", mgrTokenD98))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 管理员 A (D98) 查看 D99 的设备 TE-AUDIT-03 (垂直与水平越权)：被拒绝 403
        mockMvc.perform(get("/equipments/detail/TE-AUDIT-03")
                        .header("token", mgrTokenD98))
                .andExpect(status().isForbidden());

        // 4. 验证系统管理员 (Role 3) 可以看全部
        mockMvc.perform(get("/equipments/detail/TE-AUDIT-03")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.equipId").value("TE-AUDIT-03"));
    }
}
