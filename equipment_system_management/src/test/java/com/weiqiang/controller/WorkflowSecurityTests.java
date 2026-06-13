package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.MaintenanceRecord;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import com.weiqiang.pojo.TransferRecord;
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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 完整 RBAC 工作流与业务流转边界集成校验测试
 */
@SpringBootTest
@AutoConfigureMockMvc
public class WorkflowSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final String OP1_USERNAME = "test_op1";
    private static final String OP2_USERNAME = "test_op2";
    private static final String MAINT_USERNAME = "test_maint";
    private static final String MGR_USERNAME = "test_mgr";

    private String adminToken;
    private String op1Token;
    private String op2Token;
    private String maintToken;
    private String maintToken2;
    private String mgrToken;

    private Integer op1Id;
    private Integer op2Id;
    private Integer maintId;
    private Integer maintId2;
    private Integer mgrId;

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
        // 1. 清除历史垃圾数据
        // 1. 清理数据
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM transfer_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM scrap_record WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM equipment WHERE unit_code IN ('D98', 'D99') OR equip_id LIKE 'TE%'");
        userDao.update("UPDATE sys_user SET unit_code = NULL WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'test_%' OR username LIKE 'claim_%'");
        userDao.update("DELETE FROM department WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM category WHERE category_id = 'C99'");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE%'");

        // 2. 初始化部门与分类数据
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D98', '测试单位A', '负责人A'), ('D99', '测试单位B', '负责人B')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C99', '测试分类', 5, 0.05)");

        // 3. 登录管理员获取 Token
        final User adminLogin = new User();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("123456");
        MvcResult adminLoginRes = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = (String) objectMapper.readValue(adminLoginRes.getResponse().getContentAsString(), Result.class).getData();

        // 4. 注册测试账户
        registerTestUser(OP1_USERNAME, "测试操作员1");
        registerTestUser(OP2_USERNAME, "测试操作员2");
        registerTestUser(MAINT_USERNAME, "测试维修工");
        registerTestUser("test_maint2", "测试维修工2");
        registerTestUser(MGR_USERNAME, "测试资产管理员");

        // 5. 分配正确角色
        op1Id = userDao.getByUsername(OP1_USERNAME).getId();
        op2Id = userDao.getByUsername(OP2_USERNAME).getId();
        maintId = userDao.getByUsername(MAINT_USERNAME).getId();
        maintId2 = userDao.getByUsername("test_maint2").getId();
        mgrId = userDao.getByUsername(MGR_USERNAME).getId();

        // 操作员默认 role=0，更新部门
        userDao.update("UPDATE sys_user SET unit_code = 'D98' WHERE id = ?", op1Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D99' WHERE id = ?", op2Id);

        // 维修工 role=1
        userDao.update("UPDATE sys_user SET role = 1, unit_code = 'D98' WHERE id = ?", maintId);
        userDao.update("UPDATE sys_user SET role = 1, unit_code = 'D98' WHERE id = ?", maintId2);
        // 资产管理员 role=2
        userDao.update("UPDATE sys_user SET role = 2, unit_code = 'D98' WHERE id = ?", mgrId);

        // 6. 获取所有用户的 Token
        op1Token = loginTestUser(OP1_USERNAME);
        op2Token = loginTestUser(OP2_USERNAME);
        maintToken = loginTestUser(MAINT_USERNAME);
        maintToken2 = loginTestUser("test_maint2");
        mgrToken = loginTestUser(MGR_USERNAME);
    }

    private void registerTestUser(String username, String realName) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setRealName(realName);
        user.setUnitCode("D98");
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    private String loginTestUser(String username) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        MvcResult res = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn();
        return (String) objectMapper.readValue(res.getResponse().getContentAsString(), Result.class).getData();
    }

    private Equipment buildEquipment(String equipId, String equipName, String unitCode, String custodian) {
        final Equipment equipment = new Equipment();
        equipment.setEquipId(equipId);
        equipment.setEquipName(equipName);
        equipment.setModel("Test-Model");
        equipment.setStatus("在用");
        equipment.setPurchaseDate(LocalDate.now());
        equipment.setOriginalValue(new BigDecimal("10000.00"));
        equipment.setUnitCode(unitCode);
        equipment.setCategoryId("C99");
        equipment.setCustodian(custodian);
        return equipment;
    }

    private MaintenanceRecord buildFaultReport(String faultDescription) {
        final MaintenanceRecord record = new MaintenanceRecord();
        record.setFaultDescription(faultDescription);
        record.setMaintDate(LocalDate.now());
        return record;
    }

    @Test
    public void testFullWorkflowSecurity() throws Exception {

        // ----------------------------------------------------
        // [1] 权限准入边界 (403 校验)
        // ----------------------------------------------------
        // 设备写接口 (POST /equipments)
        // 操作员(0) 添加设备 -> 预期 403
        final Equipment equip = new Equipment();
        equip.setEquipId("TE001");
        equip.setEquipName("测试设备1");
        equip.setModel("Model-X");
        equip.setStatus("在用");
        equip.setPurchaseDate(LocalDate.now());
        equip.setOriginalValue(new BigDecimal("10000.00"));
        equip.setUnitCode("D98");
        equip.setCategoryId("C99");

        mockMvc.perform(post("/equipments")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isForbidden());

        // 维修工程师(1) 添加设备 -> 预期 403
        mockMvc.perform(post("/equipments")
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isForbidden());

        // 资产管理员(2) 添加设备 -> 预期 200 (允许操作)
        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isOk());

        // 备份还原权限拦截：资产管理员(2) 尝试备份数据库 -> 预期 403
        mockMvc.perform(post("/system/db/backup")
                        .header("token", mgrToken))
                .andExpect(status().isForbidden());

        // ----------------------------------------------------
        // [2] 设备报废状态写穿透防御
        // ----------------------------------------------------
        // 将设备修改为报废状态
        userDao.update("UPDATE equipment SET status = '报废' WHERE equip_id = 'TE001'");

        // 资产管理员(2) 尝试编辑已报废设备 -> 预期拦截并提示
        equip.setEquipName("修改后的报废设备");
        mockMvc.perform(put("/equipments/TE001")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该设备已报废，禁止进行此操作"));

        // ----------------------------------------------------
        // [3] 设备报修联动安全校验
        // ----------------------------------------------------
        // 新增在用设备，保管人为 test_op1
        final Equipment equip2 = new Equipment();
        equip2.setEquipId("TE002");
        equip2.setEquipName("测试设备2");
        equip2.setModel("Model-Y");
        equip2.setStatus("在用");
        equip2.setPurchaseDate(LocalDate.now());
        equip2.setOriginalValue(new BigDecimal("20000.00"));
        equip2.setUnitCode("D98");
        equip2.setCategoryId("C99");
        equip2.setCustodian(OP1_USERNAME);

        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip2)))
                .andExpect(status().isOk());

        // A. 用 test_op2 (不是保管人) 发起报修 -> 预期被拦截阻断
        final MaintenanceRecord maintRec = new MaintenanceRecord();
        maintRec.setFaultDescription("屏幕开裂");
        maintRec.setMaintContent("计划返厂维修");
        maintRec.setMaintCost(new BigDecimal("500.00"));
        maintRec.setMaintPerson("外协维保");

        mockMvc.perform(post("/equipments/maint/TE002")
                        .header("token", op2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("您不是该设备的保管人，无权发起报修！"));

        // B. 用 test_op1 (是保管人) 发起报修 -> 预期成功
        mockMvc.perform(post("/equipments/maint/TE002")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 校验设备状态已被联动更新为 '维修'
        final String statusSql = "SELECT status FROM equipment WHERE equip_id = 'TE002'";
        String currentStatus = (String) userDao.singleSelect(statusSql);
        assertEquals("维修", currentStatus);

        // 校验维保单已经被插入，且 reporter = 'test_op1'，maint_status = 0 (待指派)
        final String getMaintSql = "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE002' ORDER BY maint_id DESC LIMIT 1";
        final Integer maintId = (Integer) userDao.singleSelect(getMaintSql);
        assertNotNull(maintId);

        final String getMaintStatusSql = "SELECT maint_status FROM maintenance_record WHERE maint_id = ?";
        final Integer maintStatus = (Integer) userDao.singleSelect(getMaintStatusSql, maintId);
        assertEquals(0, maintStatus);

        mockMvc.perform(get("/maintenanceRecords")
                        .header("token", mgrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[?(@.equipId=='TE002')].equipName").value(hasItem("测试设备2")))
                .andExpect(jsonPath("$.data[?(@.equipId=='TE002')].faultDescription").value(hasItem("屏幕开裂")));

        // ----------------------------------------------------
        // [4] 工单指派（派工）校验
        // ----------------------------------------------------
        // 维修工程师(1) 尝试指派工单 -> 预期拦截 (400/Result.error)
        final MaintenanceRecord assignRec = new MaintenanceRecord();
        assignRec.setMaintPersonId(op1Id); // 指派给操作员

        mockMvc.perform(put("/maintenanceRecords/" + maintId)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：只有资产管理员可以指派工单！"));

        // 资产管理员(2) 指派给操作员 (role=0) -> 预期拦截阻断 (被指派人不是维修工)
        mockMvc.perform(put("/maintenanceRecords/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：被指派人不存在或不是维修工程师！"));

        // 资产管理员(2) 指派给真正维修工 test_maint -> 预期成功，工单状态流转为 1 (维修中)
        assignRec.setMaintPersonId(this.maintId);
        mockMvc.perform(put("/maintenanceRecords/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final Integer assignedStatus = (Integer) userDao.singleSelect(getMaintStatusSql, maintId);
        assertEquals(1, assignedStatus);

        // ----------------------------------------------------
        // [5] 登记维保结果联动校验
        // ----------------------------------------------------
        // 维保完结登记
        final MaintenanceRecord completeRec = new MaintenanceRecord();
        completeRec.setMaintDate(LocalDate.now());
        completeRec.setMaintContent("更换散热电容");
        completeRec.setMaintCost(new BigDecimal("100.00"));
        completeRec.setMaintPerson("测试维修工");

        // A. 另一未被指派的维修工(role=1) 尝试登记维修结果 -> 预期拦截并提示
        mockMvc.perform(put("/maintenanceRecords/" + maintId)
                        .header("token", maintToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：您没有权限登记他人的维保工单！"));

        // B. 正确被指派的维修工(1) 登记维修结果 -> 预期成功，工单流转为 2，设备依然为 '维修'
        mockMvc.perform(put("/maintenanceRecords/" + maintId)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final Integer finalMaintStatus = (Integer) userDao.singleSelect(getMaintStatusSql, maintId);
        assertEquals(2, finalMaintStatus);

        final String finalEquipStatus = (String) userDao.singleSelect(statusSql);
        assertEquals("维修", finalEquipStatus);

        // ----------------------------------------------------
        // [6] 主数据及用户级联删除安全拦截校验
        // ----------------------------------------------------
        // A. 删除分类 C99 -> 预期阻断拦截 (因为下属有 TE001, TE002)
        mockMvc.perform(delete("/categories/C99")
                        .header("token", mgrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该分类下有关联设备，无法删除！"));

        // B. 删除部门 D98 -> 预期阻断拦截
        mockMvc.perform(delete("/departments/D98")
                        .header("token", mgrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该部门下有关联设备，无法删除！"));

        // ----------------------------------------------------
        // [7] 数据隔离列表过滤校验
        // ----------------------------------------------------
        // A. 预设三台设备：
        // TE001: custodian=null, unit_code='D98' (op1所属部门)
        // TE002: custodian='test_op1', unit_code='D98'
        // TE003: custodian='test_op2', unit_code='D99' (另一部门，另一操作员)
        userDao.update("UPDATE equipment SET custodian = NULL, unit_code = 'D98' WHERE equip_id = 'TE001'");
        userDao.update("UPDATE equipment SET custodian = 'test_op1', unit_code = 'D98' WHERE equip_id = 'TE002'");
        
        final Equipment equip3 = new Equipment();
        equip3.setEquipId("TE003");
        equip3.setEquipName("测试设备3");
        equip3.setModel("Model-Z");
        equip3.setStatus("在用");
        equip3.setPurchaseDate(LocalDate.now());
        equip3.setOriginalValue(new BigDecimal("30000.00"));
        equip3.setUnitCode("D99");
        equip3.setCategoryId("C99");
        equip3.setCustodian(OP2_USERNAME);
        
        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip3)))
                .andExpect(status().isOk());

        // B. 用 test_op1 Token 查询设备列表
        // 预期仅返回 TE001(保管人空且同部门) 与 TE002(保管人自己)，不返回 TE003
        MvcResult op1ListRes = mockMvc.perform(get("/equipments?page=1&pageSize=10")
                        .header("token", op1Token))
                .andExpect(status().isOk())
                .andReturn();
        final String content = op1ListRes.getResponse().getContentAsString();
        assertTrue(content.contains("TE001"));
        assertTrue(content.contains("TE002"));
        assertFalse(content.contains("TE003"));

        // ----------------------------------------------------
        // [8] 保管人级联删除及审计清退校验
        // ----------------------------------------------------
        // C. 删除用户 test_op1 (管理员权限，即 DELETE /users/{id})
        // 因为 test_op1 名下保管有设备 TE002 -> 预期成功删除（新需求：级联清空保管人并写审计日志）
        mockMvc.perform(delete("/users/" + op1Id)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证 TE002 的保管人被自动置为 NULL
        final String checkCustodianSql = "SELECT custodian FROM equipment WHERE equip_id = 'TE002'";
        final String custodianAfterDelete = (String) userDao.singleSelect(checkCustodianSql);
        assertTrue(custodianAfterDelete == null || custodianAfterDelete.trim().isEmpty());

        // 验证 t_equipment_claim 里是否写了退还审计日志
        final String checkClaimSql = "SELECT count(*) FROM t_equipment_claim WHERE equip_id = 'TE002' AND status = 4 AND applicant = 'test_op1' AND remark = '用户被删除导致保管关系自动清退'";
        final Long claimCount = (Long) userDao.singleSelect(checkClaimSql);
        assertEquals(1L, claimCount);
    }

    @Test
    public void testMaintenanceLifecycleDeletionAndManagerProxyFlow() throws Exception {
        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE010", "同单位设备", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/equipments/maint/TE010")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("管理员巡检发现异响"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Integer proxyMaintId = (Integer) userDao.singleSelect(
                "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE010' ORDER BY maint_id DESC LIMIT 1");
        assertNotNull(proxyMaintId);
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE010'"));
        assertEquals(0, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", proxyMaintId));
        assertEquals(MGR_USERNAME, userDao.singleSelect("SELECT reporter FROM maintenance_record WHERE maint_id = ?", proxyMaintId));

        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE011", "跨单位设备", "D99", OP2_USERNAME))))
                .andExpect(status().isOk());
        userDao.update("UPDATE equipment SET unit_code = 'D99', custodian = ? WHERE equip_id = 'TE011'", OP2_USERNAME);

        mockMvc.perform(post("/equipments/maint/TE011")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("跨单位代报修"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE012", "待撤销设备", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/equipments/maint/TE012")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("误报待撤销"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Integer pendingMaintId = (Integer) userDao.singleSelect(
                "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE012' ORDER BY maint_id DESC LIMIT 1");
        assertNotNull(pendingMaintId);

        mockMvc.perform(delete("/maintenanceRecords/" + pendingMaintId)
                        .header("token", mgrToken)
                        .param("equipId", "TE012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(0L, userDao.singleSelect("SELECT COUNT(*) FROM maintenance_record WHERE maint_id = ?", pendingMaintId));
        assertEquals("在用", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE012'"));

        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE013", "维修中设备", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/equipments/maint/TE013")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("需要继续维修"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Integer inProgressMaintId = (Integer) userDao.singleSelect(
                "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE013' ORDER BY maint_id DESC LIMIT 1");
        assertNotNull(inProgressMaintId);

        final MaintenanceRecord assignRec = new MaintenanceRecord();
        assignRec.setMaintPersonId(this.maintId);
        mockMvc.perform(put("/maintenanceRecords/" + inProgressMaintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(delete("/maintenanceRecords/" + inProgressMaintId)
                        .header("token", mgrToken)
                        .param("equipId", "TE013"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：维修中的工单不允许删除，请先完工登记。"));

        assertEquals(1, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", inProgressMaintId));
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE013'"));

        final MaintenanceRecord completeRec = new MaintenanceRecord();
        completeRec.setMaintDate(LocalDate.now());
        completeRec.setMaintContent("维修完成");
        completeRec.setMaintCost(new BigDecimal("66.00"));
        completeRec.setMaintPerson("测试维修工");
        mockMvc.perform(put("/maintenanceRecords/" + inProgressMaintId)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(2, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", inProgressMaintId));
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE013'"));

        mockMvc.perform(delete("/maintenanceRecords/" + inProgressMaintId)
                        .header("token", mgrToken)
                        .param("equipId", "TE013"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：已完成的工单不允许删除。"));

        assertEquals(1L, userDao.singleSelect("SELECT COUNT(*) FROM maintenance_record WHERE maint_id = ?", inProgressMaintId));

        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE014", "工单匹配设备", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/equipments/maint/TE014")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("匹配校验"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Integer mismatchMaintId = (Integer) userDao.singleSelect(
                "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE014' ORDER BY maint_id DESC LIMIT 1");
        assertNotNull(mismatchMaintId);

        mockMvc.perform(delete("/maintenanceRecords/" + mismatchMaintId)
                        .header("token", mgrToken)
                        .param("equipId", "TE010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：工单与设备不匹配，无法撤销！"));

        assertEquals(1L, userDao.singleSelect("SELECT COUNT(*) FROM maintenance_record WHERE maint_id = ?", mismatchMaintId));
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE014'"));
    }

    @Test
    public void testMaintenanceReviewAndScrapWorkflow() throws Exception {
        // [准备工作] 创建测试设备
        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE020", "复核测试设备A", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        // [准备工作] 报修设备 TE020
        mockMvc.perform(post("/equipments/maint/TE020")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("主板烧毁"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final String getMaintSql = "SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE020' ORDER BY maint_id DESC LIMIT 1";
        final Integer maintId = (Integer) userDao.singleSelect(getMaintSql);
        assertNotNull(maintId);

        // 此时工单状态为 0 (待指派)
        assertEquals(0, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId));
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE020'"));

        // ==========================================
        // 1. 非法状态流转防御：对待指派工单(0) 直接复核
        // ==========================================
        final MaintenanceRecord reviewReq = new MaintenanceRecord();
        reviewReq.setMaintStatus(3); // 复核通过恢复在用
        reviewReq.setReviewComments("尝试对待指派工单直接复核");

        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：工单尚未登记完工，无法复核！"));

        // 对待指派工单(0) 直接转报废
        reviewReq.setMaintStatus(4); // 转报废
        reviewReq.setScrapNo("SCRAP-TEST-001");
        reviewReq.setReviewComments("尝试对待指派工单直接转报废");

        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：工单尚未登记完工，无法转报废！"));

        // ==========================================
        // 2. 指派工单给 test_maint
        // ==========================================
        final MaintenanceRecord assignRec = new MaintenanceRecord();
        assignRec.setMaintPersonId(this.maintId); // test_maint
        mockMvc.perform(put("/maintenanceRecords/assign/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 此时工单状态为 1 (维修中)
        assertEquals(1, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId));

        // ==========================================
        // 3. 非法状态流转防御：对维修中工单(1) 直接复核
        // ==========================================
        reviewReq.setMaintStatus(3);
        reviewReq.setReviewComments("尝试对维修中工单直接复核");
        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：工单尚未登记完工，无法复核！"));

        reviewReq.setMaintStatus(4);
        reviewReq.setReviewComments("尝试对维修中工单直接转报废");
        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：工单尚未登记完工，无法转报废！"));

        // ==========================================
        // 4. 越权登记他人员工单：用 maintToken2 登记 maint 的工单
        // ==========================================
        final MaintenanceRecord completeRec = new MaintenanceRecord();
        completeRec.setMaintContent("更换电容");
        completeRec.setMaintCost(new java.math.BigDecimal("150.00"));
        completeRec.setMaintPerson("测试维修工2");

        mockMvc.perform(put("/maintenanceRecords/complete/" + maintId)
                        .header("token", maintToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：您没有权限登记他人的维保工单！"));

        // ==========================================
        // 5. 正确登记完工：用 maintToken 登记完工
        // ==========================================
        completeRec.setMaintPerson("测试维修工1");
        mockMvc.perform(put("/maintenanceRecords/complete/" + maintId)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 此时工单状态为 2 (已完成/待复核)，设备依然为 '维修'
        assertEquals(2, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId));
        assertEquals("维修", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE020'"));

        // ==========================================
        // 6. 越权复核拦截：跨单位资产管理员
        //    将 test_op2 的角色临时改为 2（资产管理员），使其成为 D99 (测试单位B) 资产管理员
        // ==========================================
        userDao.update("UPDATE sys_user SET role = 2 WHERE username = 'test_op2'");
        
        reviewReq.setMaintStatus(3);
        reviewReq.setReviewComments("跨单位复核");
        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", op2Token) // op2Token 现在是 D99 资产管理员
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        // 恢复 test_op2 的角色为 0
        userDao.update("UPDATE sys_user SET role = 0 WHERE username = 'test_op2'");

        // ==========================================
        // 7. 资产管理员执行完工复核并通过
        // ==========================================
        reviewReq.setMaintStatus(3);
        reviewReq.setReviewComments("复核通过，设备状况良好");
        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 校验：工单状态为 3，设备状态恢复为 '在用'
        assertEquals(3, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId));
        assertEquals("在用", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE020'"));

        // ==========================================
        // 8. 完工复核转报废
        // ==========================================
        mockMvc.perform(post("/equipments")
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEquipment("TE021", "复核测试设备B", "D98", OP1_USERNAME))))
                .andExpect(status().isOk());

        // [准备工作] 报修设备 TE021
        mockMvc.perform(post("/equipments/maint/TE021")
                        .header("token", op1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFaultReport("无法开机"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final Integer maintId2 = (Integer) userDao.singleSelect("SELECT maint_id FROM maintenance_record WHERE equip_id = 'TE021' ORDER BY maint_id DESC LIMIT 1");
        
        // 指派
        assignRec.setMaintPersonId(this.maintId);
        mockMvc.perform(put("/maintenanceRecords/assign/" + maintId2)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRec)))
                .andExpect(status().isOk());

        // 完工登记
        mockMvc.perform(put("/maintenanceRecords/complete/" + maintId2)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk());

        // 此时工单状态为 2，设备状态为 '维修'
        assertEquals(2, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId2));

        final MaintenanceRecord reviewScrapReq = new MaintenanceRecord();
        reviewScrapReq.setMaintStatus(4); // 转报废
        reviewScrapReq.setReviewComments("修复成本过高，做报废处置");
        reviewScrapReq.setScrapNo("SCRAP-TEST-REV-001");

        mockMvc.perform(put("/maintenanceRecords/review/" + maintId2)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewScrapReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 校验 A: 工单状态为 4
        assertEquals(4, userDao.singleSelect("SELECT maint_status FROM maintenance_record WHERE maint_id = ?", maintId2));
        // 校验 B: 设备状态为 '报废'，且保管人置空
        assertEquals("报废", userDao.singleSelect("SELECT status FROM equipment WHERE equip_id = 'TE021'"));
        String custodian = (String) userDao.singleSelect("SELECT custodian FROM equipment WHERE equip_id = 'TE021'");
        assertTrue(custodian == null || custodian.trim().isEmpty());

        // 校验 C: 自动生成报废记录
        final Long scrapCount = (Long) userDao.singleSelect("SELECT COUNT(*) FROM scrap_record WHERE scrap_no = 'SCRAP-TEST-REV-001'");
        assertEquals(1L, scrapCount);

        // 校验 D: 自动清退保管人并添加领用退还流水 (status=4, applicant=test_op1)
        final Long claimCount = (Long) userDao.singleSelect("SELECT COUNT(*) FROM t_equipment_claim WHERE equip_id = 'TE021' AND status = 4 AND applicant = ?", OP1_USERNAME);
        assertEquals(1L, claimCount);

        // ==========================================
        // 9. 非法状态流转防御：对已复核工单进行二次修改拦截
        // ==========================================
        // 尝试对已复核工单(3) 登记完工
        mockMvc.perform(put("/maintenanceRecords/complete/" + maintId)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该维保工单已完成，禁止二次修改！"));

        // 尝试对已报废工单(4) 登记完工
        mockMvc.perform(put("/maintenanceRecords/complete/" + maintId2)
                        .header("token", maintToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该维保工单已完成，禁止二次修改！"));

        // 尝试对已复核工单(3) 再次复核
        mockMvc.perform(put("/maintenanceRecords/review/" + maintId)
                        .header("token", mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该工单已复核，禁止二次修改！"));
    }
}
