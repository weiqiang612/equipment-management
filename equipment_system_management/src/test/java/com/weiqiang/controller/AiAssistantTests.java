package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.AiDraftReportRequest;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 辅助决策集成测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AiAssistantTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String opToken;     // Role 0 (单位 D90)
    private String engToken;    // Role 1 (单位 D90)
    private String mgr1Token;   // Role 2 (单位 D90)
    private String mgr2Token;   // Role 2 (单位 D91)
    private String adminToken;  // Role 3 (单位 NULL)

    @BeforeEach
    public void setup() throws Exception {
        cleanupData();

        // 创建测试部门
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D90', '测试单位90', '主管90')");
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D91', '测试单位91', '主管91')");

        // 创建测试分类
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C90', '测试分类90', 5, 0.05)");

        // 初始化各个角色的用户
        opToken = setupUser("test_ai_op", 0, "D90");
        engToken = setupUser("test_ai_eng", 1, "D90");
        mgr1Token = setupUser("test_ai_mgr1", 2, "D90");
        mgr2Token = setupUser("test_ai_mgr2", 2, "D91");
        adminToken = setupUser("test_ai_admin", 3, null);

        // 注册一台本单位的设备 (单位 D90)
        final Equipment equipOwn = new Equipment();
        equipOwn.setEquipId("TE_AI_001");
        equipOwn.setEquipName("本单位测试设备");
        equipOwn.setModel("Model-A");
        equipOwn.setStatus("在用");
        equipOwn.setPurchaseDate(java.time.LocalDate.now());
        equipOwn.setOriginalValue(new java.math.BigDecimal("5000.00"));
        equipOwn.setUnitCode("D90");
        equipOwn.setCategoryId("C90");
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                equipOwn.getEquipId(), equipOwn.getEquipName(), equipOwn.getModel(), equipOwn.getStatus(), equipOwn.getPurchaseDate(), equipOwn.getOriginalValue(), equipOwn.getUnitCode(), equipOwn.getCategoryId());

        // 注册一台外单位 of 设备 (单位 D91)
        final Equipment equipOther = new Equipment();
        equipOther.setEquipId("TE_AI_002");
        equipOther.setEquipName("跨单位测试设备");
        equipOther.setModel("Model-B");
        equipOther.setStatus("在用");
        equipOther.setPurchaseDate(java.time.LocalDate.now());
        equipOther.setOriginalValue(new java.math.BigDecimal("6000.00"));
        equipOther.setUnitCode("D91");
        equipOther.setCategoryId("C90");
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                equipOther.getEquipId(), equipOther.getEquipName(), equipOther.getModel(), equipOther.getStatus(), equipOther.getPurchaseDate(), equipOther.getOriginalValue(), equipOther.getUnitCode(), equipOther.getCategoryId());
    }

    @AfterEach
    public void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE_AI_%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D90', 'D91'))");
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TE_AI_%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code IN ('D90', 'D91'))");
        userDao.update("DELETE FROM equipment WHERE equip_id LIKE 'TE_AI_%' OR unit_code IN ('D90', 'D91')");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'test_ai_%'");
        userDao.update("DELETE FROM department WHERE unit_code IN ('D90', 'D91')");
        userDao.update("DELETE FROM category WHERE category_id = 'C90'");
    }

    private String setupUser(final String username, final Integer role, final String unitCode) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setRealName(username + "_real");
        user.setUnitCode(unitCode != null ? unitCode : "D90"); // 注册时先用 D90 占位

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        final User dbUser = userDao.getByUsername(username);
        userDao.update("UPDATE sys_user SET role = ?, unit_code = ? WHERE id = ?", role, unitCode, dbUser.getId());

        final User loginUser = new User();
        loginUser.setUsername(username);
        loginUser.setPassword("password123");
        final MvcResult res = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        return (String) objectMapper.readValue(res.getResponse().getContentAsString(), Result.class).getData();
    }

    /**
     * 测试 1：Role 0/1 垂直越权被强拦截
     */
    @Test
    public void testRoleAuthorizationBlock() throws Exception {
        // A. 报告生成测试
        final AiDraftReportRequest req = new AiDraftReportRequest();
        req.setPeriod("weekly");

        mockMvc.perform(post("/ai/reports/operations/draft")
                        .header("token", opToken) // Role 0
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden()); // 预期 403

        mockMvc.perform(post("/ai/reports/operations/draft")
                        .header("token", engToken) // Role 1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // B. 设备生命周期摘要测试
        mockMvc.perform(post("/ai/equipment/TE_AI_001/summary")
                        .header("token", opToken)) // Role 0
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/ai/equipment/TE_AI_001/summary")
                        .header("token", engToken)) // Role 1
                .andExpect(status().isForbidden());
    }

    /**
     * 测试 2：Role 2 跨单位越权获取设备摘要被拦截
     */
    @Test
    public void testCrossUnitIsolationBlock() throws Exception {
        // mgr1 单位是 D90，设备 TE_AI_002 单位是 D91，预期拦截
        mockMvc.perform(post("/ai/equipment/TE_AI_002/summary")
                        .header("token", mgr1Token)) // Role 2 (D90)
                .andExpect(status().isForbidden()); // 预期 403 Forbidden
    }

    /**
     * 测试 3：未配置 API Key 时接口的优雅失败降级 (返回 code=0)
     */
    @Test
    public void testGracefulDegradationWhenNoApiKey() throws Exception {
        // 由于测试环境中默认未配置 AI_API_KEY，大模型调用逻辑中必然抛出 BusinessException 并捕获为 code=0

        // A. 资产管理员 mgr1 (D90) 请求报告生成
        final AiDraftReportRequest req = new AiDraftReportRequest();
        req.setPeriod("weekly");

        MvcResult resultReport = mockMvc.perform(post("/ai/reports/operations/draft")
                        .header("token", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Result resReport = objectMapper.readValue(resultReport.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), Result.class);
        assertEquals(0, resReport.getCode());
        assertEquals("AI 辅助服务未启用：请联系管理员配置 AI 接口凭证", resReport.getMsg());

        // B. 系统管理员 admin (全局) 请求本单位设备生命周期摘要
        MvcResult resultSummary = mockMvc.perform(post("/ai/equipment/TE_AI_001/summary")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        Result resSummary = objectMapper.readValue(resultSummary.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), Result.class);
        assertEquals(0, resSummary.getCode());
        assertEquals("AI 辅助服务未启用：请联系管理员配置 AI 接口凭证", resSummary.getMsg());
    }
}
