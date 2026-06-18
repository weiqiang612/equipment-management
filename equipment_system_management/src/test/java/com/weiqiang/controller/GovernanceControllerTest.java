package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
import com.weiqiang.common.Result;
import com.weiqiang.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GovernanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String opToken;      // Role 0, Unit D98
    private String engToken;     // Role 1, Unit D98
    private String mgr1Token;    // Role 2, Unit D98 (Asset Manager 1)
    private String mgr2Token;    // Role 2, Unit D99 (Asset Manager 2)
    private String adminToken;   // Role 3, Unit NULL (Super Admin)

    @BeforeEach
    public void setup() throws Exception {
        cleanup();

        // 1. 初始化测试部门与分类
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D98', '测试部门A', '经理A'), ('D99', '测试部门B', '经理B')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C99', '测试分类', 5, 0.05)");

        // 2. 注册并准备不同角色的用户
        registerUser("gov_op", "操作员", "D98");
        registerUser("gov_eng", "工程师", "D98");
        registerUser("gov_mgr1", "管理员A", "D98");
        registerUser("gov_mgr2", "管理员B", "D99");
        registerUser("gov_admin", "超管", "D98"); // 会被置为NULL

        // 3. 直接修改用户角色与单位以满足测试需求
        userDao.update("UPDATE sys_user SET role = 0, unit_code = 'D98' WHERE username = 'gov_op'");
        userDao.update("UPDATE sys_user SET role = 1, unit_code = 'D98' WHERE username = 'gov_eng'");
        userDao.update("UPDATE sys_user SET role = 2, unit_code = 'D98' WHERE username = 'gov_mgr1'");
        userDao.update("UPDATE sys_user SET role = 2, unit_code = 'D99' WHERE username = 'gov_mgr2'");
        userDao.update("UPDATE sys_user SET role = 3, unit_code = NULL WHERE username = 'gov_admin'");

        // 4. 获取登录 Token
        opToken = loginUser("gov_op");
        engToken = loginUser("gov_eng");
        mgr1Token = loginUser("gov_mgr1");
        mgr2Token = loginUser("gov_mgr2");
        adminToken = loginUser("gov_admin");

        // 5. 准备测试用的设备数据
        // E01 - 高龄风险设备 (购入于 2010 年，已经使用超过预计寿命 5 年)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E01', '高龄电脑', 'Model-H', '在用', '2010-01-01', 5000.00, 'D98', 'C99', NULL)");

        // E02 - 高频维保风险设备 (购入于 2025 年，正常年龄，但有3次维保记录)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E02', '多修电脑', 'Model-M', '在用', '2025-01-01', 5000.00, 'D98', 'C99', NULL)");
        userDao.update("INSERT INTO maintenance_record (equip_id, maint_date, maint_content, maint_cost, maint_status, reporter) VALUES " +
                "('E02', '2025-02-01', '修理1', 100.00, 2, 'gov_op')," +
                "('E02', '2025-03-01', '修理2', 150.00, 2, 'gov_op')," +
                "('E02', '2025-04-01', '修理3', 200.00, 2, 'gov_op')");

        // E03 - 维保金额占比过大风险设备 (购入于 2025 年，维保费用 1600 / 原值 5000 = 0.32 >= 0.3, 高风险)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E03', '高费电脑', 'Model-C', '在用', '2025-01-01', 5000.00, 'D98', 'C99', NULL)");
        userDao.update("INSERT INTO maintenance_record (equip_id, maint_date, maint_content, maint_cost, maint_status, reporter) VALUES " +
                "('E03', '2025-02-01', '修理大费用', 1600.00, 2, 'gov_op')");

        // E04 - 长期空闲设备 (保管人为 NULL，状态为 '在用')
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E04', '空闲电脑', 'Model-I', '在用', '2025-01-01', 5000.00, 'D98', 'C99', NULL)");

        // E05 - 保管人与单位不匹配的设备 (设备在 D98，保管人是 gov_mgr2 其部门是 D99)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E05', '错配电脑', 'Model-X', '在用', '2025-01-01', 5000.00, 'D98', 'C99', 'gov_mgr2')");

        // E06 & E07 - 疑似重复设备 (名称、型号、单位、购入日期、原值完全一致)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E06', '重复电脑', 'Model-D', '在用', '2025-01-01', 5000.00, 'D98', 'C99', NULL)," +
                "('E07', '重复电脑', 'Model-D', '在用', '2025-01-01', 5000.00, 'D98', 'C99', NULL)");

        // E08 - 缺失分类字段设备 (数据降级容错测试)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E08', '缺失分类电脑', 'Model-E', '在用', '2025-01-01', 5000.00, 'D98', NULL, NULL)");

        // E09 - D99 的一台正常设备 (用以验证跨部门水平隔离)
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('E09', 'D99正常电脑', 'Model-N', '在用', '2025-01-01', 5000.00, 'D99', 'C99', NULL)");
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // 解除所有 sys_user 对 D98, D99 部门的外键引用
        userDao.update("UPDATE sys_user SET unit_code = NULL WHERE unit_code IN ('D98', 'D99')");
        // 清理设备相关联的所有维保单、调拨、报废、领用流转及设备自身
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'E0%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM transfer_record WHERE equip_id LIKE 'E0%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM scrap_record WHERE equip_id LIKE 'E0%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'E0%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D98', 'D99'))");
        userDao.update("DELETE FROM equipment WHERE unit_code IN ('D98', 'D99') OR equip_id LIKE 'E0%'");
        // 删除测试用户
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'gov_%'");
        // 删除测试部门与分类
        userDao.update("DELETE FROM department WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM category WHERE category_id = 'C99'");
    }

    private void registerUser(String username, String realName, String unitCode) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setRealName(realName);
        user.setUnitCode(unitCode);
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    private String loginUser(String username) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        final MvcResult res = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn();
        return (String) objectMapper.readValue(res.getResponse().getContentAsString(), Result.class).getData();
    }

    /**
     * 测试普通用户被拒绝访问
     */
    @Test
    public void testRole0AndRole1Forbidden() throws Exception {
        // Role 0 访问 summary
        mockMvc.perform(get("/governance/summary").header("token", opToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        // Role 1 访问 equipment-risks
        mockMvc.perform(get("/governance/equipment-risks").header("token", engToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));
    }

    /**
     * 测试 Role 2 资产管理员仅能访问本单位的治理数据（数据水平隔离边界测试）
     */
    @Test
    public void testRole2DataIsolation() throws Exception {
        // given: 针对部门 D98 的管理员 gov_mgr1 发起请求
        // when: 获取数据质量治理总览
        // then: 检查返回的设备总数应该不包含部门 D99 的 E09
        mockMvc.perform(get("/governance/summary").header("token", mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                // D98 一共有 E01, E02, E03, E04, E05, E06, E07, E08 共 8 台设备
                .andExpect(jsonPath("$.data.totalEquipmentCount").value(8))
                .andExpect(jsonPath("$.data.idleCount").value(7)) // e01, e02, e03, e04, e06, e07, e08
                .andExpect(jsonPath("$.data.mismatchCount").value(1)) // e05
                .andExpect(jsonPath("$.data.duplicateCount").value(2)) // e06, e07
                .andExpect(jsonPath("$.data.missingFieldsCount").value(1)); // e08

        // given: 管理员 1 (D98) 请求获取全部部门风险设备 (即使传入 unitCode = D99)
        // when: 显式调用筛选 D99
        // then: 后端强行覆盖，最终只返回其所管辖的部门 D98 的设备
        mockMvc.perform(get("/governance/equipment-risks")
                        .header("token", mgr1Token)
                        .param("unitCode", "D99")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                // 结果中每一行都必须是部门 D98 
                .andExpect(jsonPath("$.data.rows[0].unitCode").value("D98"));
    }

    /**
     * 测试 Role 3 系统管理员可查看全局数据
     */
    @Test
    public void testRole3GlobalAccess() throws Exception {
        // given: 系统管理员 gov_admin 发起请求
        // when: 获取治理总览
        // then: 设备总数应该包含全部部门 (8台 D98 + 1台 D99 = 9台)
        mockMvc.perform(get("/governance/summary").header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.totalEquipmentCount").value(greaterThanOrEqualTo(9)));

        // when: 分页查询 D99 部门的风险设备清单
        // then: 可以正常查询出 E09
        mockMvc.perform(get("/governance/equipment-risks")
                        .header("token", adminToken)
                        .param("unitCode", "D99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.rows[0].equipId").value("E09"));
    }

    /**
     * 测试风险评分边界规则和缺陷设备降级处理 (AC-002, AC-005)
     */
    @Test
    public void testRiskComputationAndFallback() throws Exception {
        // given: 使用超管 Token 获取全部风险设备
        // when: 查询 E01 (高龄风险) 
        mockMvc.perform(get("/governance/equipment-risks")
                        .header("token", adminToken)
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                // 1. E01 判定为高风险且健康分40，原因包含"使用年限占比超标"
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E01')].riskLevel").value("高风险"))
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E01')].healthScore").value(40))
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E01')].riskReasons").value("使用年限占比超标"))
                
                // 2. E02 维保次数为 3，判定为高风险，原因包含"维保次数超标"
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E02')].riskLevel").value("高风险"))
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E02')].riskReasons").value("维保次数超标"))

                // 3. E03 维保金额比过大，判定为高风险，原因包含"维保费用占比过高"
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E03')].riskLevel").value("高风险"))
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E03')].riskReasons").value("维保费用占比过高"))

                // 4. E08 缺失分类（无法算出使用年限比率），但不导致 500，且正常展示在列表中，判定为低风险（作为数据质量降级）
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E08')].riskLevel").value("低风险"))
                .andExpect(jsonPath("$.data.rows[?(@.equipId=='E08')].healthScore").value(100));
    }
}
