package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerTest {

    private static final String TEST_UNIT_CODE = "DDASH98";
    private static final String TEST_CATEGORY_ID = "CDASH99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        cleanup();
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('DDASH98', '测试部门A', '经理A')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('CDASH99', '测试分类', 5, 0.05)");

        userDao.update("INSERT INTO sys_user (username, password, real_name, role, create_time, update_time, unit_code) VALUES " +
                "('test_operator', 'e10adc3949ba59abbe56e057f20f883e', '操作员小张', 0, NOW(), NOW(), 'DDASH98')," +
                "('test_maintainer', 'e10adc3949ba59abbe56e057f20f883e', '维修工小李', 1, NOW(), NOW(), 'DDASH98')," +
                "('test_manager', 'e10adc3949ba59abbe56e057f20f883e', '管理员小王', 2, NOW(), NOW(), 'DDASH98')," +
                "('test_admin', 'e10adc3949ba59abbe56e057f20f883e', '超级管理员', 3, NOW(), NOW(), NULL)");
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // 清理设备关联
        userDao.update("UPDATE equipment SET custodian = NULL WHERE custodian LIKE 'test_%'");
        // 清理维保单关联用户外键 (防止外键约束报错)
        userDao.update("UPDATE maintenance_record SET maint_person_id = NULL WHERE maint_person_id IN (SELECT id FROM sys_user WHERE username LIKE 'test_%')");
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TD%'");
        userDao.update("DELETE FROM transfer_record WHERE equip_id LIKE 'TD%'");
        userDao.update("DELETE FROM scrap_record WHERE equip_id LIKE 'TD%'");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TD%'");
        userDao.update("DELETE FROM equipment WHERE equip_id LIKE 'TD%'");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'test_%'");
        userDao.update("DELETE FROM department WHERE unit_code = 'DDASH98'");
        userDao.update("DELETE FROM category WHERE category_id = 'CDASH99'");
    }

    private String getLoginToken(String username, String password) throws Exception {
        User loginUser = new User();
        loginUser.setUsername(username);
        loginUser.setPassword(password);

        MvcResult result = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Result resObj = objectMapper.readValue(content, Result.class);
        return (String) resObj.getData();
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        // 未带 Token，预期拦截返回 401
        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("NOT_LOGIN"));
    }

    @Test
    public void testOperatorDashboard() throws Exception {
        String token = getLoginToken("test_operator", "123456");
        assertNotNull(token);

        MvcResult result = mockMvc.perform(get("/dashboard/summary").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.role").value(0))
                .andExpect(jsonPath("$.data.kpis.myEquipCount").exists())
                .andExpect(jsonPath("$.data.kpis.myActiveClaims").exists())
                .andExpect(jsonPath("$.data.kpis.myActiveMaintenances").exists())
                .andExpect(jsonPath("$.data.kpis.myDepreciationValue").exists())
                .andExpect(jsonPath("$.data.listData.myEquipments").isArray())
                .andExpect(jsonPath("$.data.listData.myClaims").isArray())
                .andExpect(jsonPath("$.data.listData.myMaintenances").isArray())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("Operator Dashboard Response: " + content);
    }

    @Test
    public void testMaintainerDashboard() throws Exception {
        String token = getLoginToken("test_maintainer", "123456");
        assertNotNull(token);

        MvcResult result = mockMvc.perform(get("/dashboard/summary").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.role").value(1))
                .andExpect(jsonPath("$.data.kpis.myPendingMaint").exists())
                .andExpect(jsonPath("$.data.kpis.myInMaint").exists())
                .andExpect(jsonPath("$.data.kpis.myCompletedMaint").exists())
                .andExpect(jsonPath("$.data.charts.maintCostTrend").isArray())
                .andExpect(jsonPath("$.data.listData.myWorkOrders").isArray())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("Maintainer Dashboard Response: " + content);
    }

    @Test
    public void testAssetManagerDashboard() throws Exception {
        String token = getLoginToken("test_manager", "123456");
        assertNotNull(token);

        MvcResult result = mockMvc.perform(get("/dashboard/summary").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.role").value(2))
                .andExpect(jsonPath("$.data.kpis.totalEquipment").exists())
                .andExpect(jsonPath("$.data.kpis.totalValue").exists())
                .andExpect(jsonPath("$.data.kpis.inUseCount").exists())
                .andExpect(jsonPath("$.data.kpis.inMaintenanceCount").exists())
                .andExpect(jsonPath("$.data.kpis.scrappedCount").exists())
                .andExpect(jsonPath("$.data.charts.categoryDistribution").isArray())
                .andExpect(jsonPath("$.data.charts.departmentDistribution").isArray())
                .andExpect(jsonPath("$.data.charts.maintenanceTrend").isArray())
                .andExpect(jsonPath("$.data.listData.pendingClaims").isArray())
                .andExpect(jsonPath("$.data.listData.pendingMaintenances").isArray())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("Asset Manager Dashboard Response: " + content);
    }

    @Test
    public void testAssetManagerDepartmentDistributionUsesOriginalValueSum() throws Exception {
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES " +
                "('TD01', '测试设备1', 'Model-1', '在用', '2026-01-01', 1000.00, 'DDASH98', 'CDASH99', NULL)," +
                "('TD02', '测试设备2', 'Model-2', '在用', '2026-02-01', 2500.00, 'DDASH98', 'CDASH99', NULL)");

        String token = getLoginToken("test_manager", "123456");
        assertNotNull(token);

        mockMvc.perform(get("/dashboard/summary").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.charts.departmentDistribution[?(@.name=='测试部门A')].value").value(hasItem(3500.0)));
    }

    @Test
    public void testSystemAdminDashboard() throws Exception {
        String token = getLoginToken("test_admin", "123456");
        assertNotNull(token);

        MvcResult result = mockMvc.perform(get("/dashboard/summary").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.role").value(3))
                .andExpect(jsonPath("$.data.kpis.totalEquipment").exists())
                .andExpect(jsonPath("$.data.kpis.totalValue").exists())
                .andExpect(jsonPath("$.data.kpis.totalUsers").exists())
                .andExpect(jsonPath("$.data.kpis.backupCount").exists())
                .andExpect(jsonPath("$.data.charts.userRoleDistribution").isArray())
                .andExpect(jsonPath("$.data.listData.backupFiles").isArray())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("System Admin Dashboard Response: " + content);
    }
}
