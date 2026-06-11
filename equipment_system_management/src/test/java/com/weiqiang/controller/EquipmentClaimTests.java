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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备领用与审批流程专属集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
public class EquipmentClaimTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String opTokenD98;
    private String opTokenD99;
    private String mgrTokenD98;
    private String adminToken;

    private Integer opD98Id;
    private Integer opD99Id;
    private Integer mgrD98Id;

    @BeforeEach
    public void setup() throws Exception {
        // 1. 清理数据
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TE%'");
        userDao.update("DELETE FROM transfer_record WHERE equip_id LIKE 'TE%'");
        userDao.update("DELETE FROM scrap_record WHERE equip_id LIKE 'TE%'");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE%'");
        userDao.update("DELETE FROM equipment WHERE equip_id LIKE 'TE%'");
        userDao.update("UPDATE sys_user SET unit_code = NULL WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'claim_%' OR username LIKE 'test_%'");
        userDao.update("DELETE FROM department WHERE unit_code IN ('D98', 'D99')");
        userDao.update("DELETE FROM category WHERE category_id = 'C99'");

        // 2. 初始化部门与分类
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D98', '测试部门A', '经理A'), ('D99', '测试部门B', '经理B')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C99', '测试分类', 5, 0.05)");

        // 3. 注册并准备 Token
        registerUser("claim_op_d98", "操作员A");
        registerUser("claim_op_d99", "操作员B");
        registerUser("claim_mgr_d98", "管理员A");

        opD98Id = userDao.getByUsername("claim_op_d98").getId();
        opD99Id = userDao.getByUsername("claim_op_d99").getId();
        mgrD98Id = userDao.getByUsername("claim_mgr_d98").getId();

        // 设置部门及角色
        userDao.update("UPDATE sys_user SET unit_code = 'D98', role = 0 WHERE id = ?", opD98Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D99', role = 0 WHERE id = ?", opD99Id);
        userDao.update("UPDATE sys_user SET unit_code = 'D98', role = 2 WHERE id = ?", mgrD98Id); // 资产管理员

        opTokenD98 = loginUser("claim_op_d98");
        opTokenD99 = loginUser("claim_op_d99");
        mgrTokenD98 = loginUser("claim_mgr_d98");

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
        user.setPassword("password123");
        user.setRealName(realName);
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    private String loginUser(String username) throws Exception {
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

    @Test
    public void testEquipmentClaimWorkflow() throws Exception {
        // A. 在部门 D98 新建一台空闲设备 TE101
        final Equipment equip = new Equipment();
        equip.setEquipId("TE101");
        equip.setEquipName("测试研发电脑");
        equip.setModel("Model-E");
        equip.setStatus("在用");
        equip.setPurchaseDate(LocalDate.now());
        equip.setOriginalValue(new BigDecimal("8000.00"));
        equip.setUnitCode("D98");
        equip.setCategoryId("C99");
        equip.setCustodian(null);

        mockMvc.perform(post("/equipments")
                        .header("token", mgrTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equip)))
                .andExpect(status().isOk());

        // B. 跨部门领用拦截测试 (操作员 B 属于 D99，尝试领用 D98 的 TE101)
        Map<String, String> applyBody = new HashMap<>();
        applyBody.put("equipId", "TE101");
        applyBody.put("remark", "我想跨部门申请");

        mockMvc.perform(post("/claims/apply")
                        .header("token", opTokenD99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("部门不匹配，无法申请本部门之外的设备"));

        // C. 同部门正常申请领用 (操作员 A 属于 D98)
        applyBody.put("remark", "需要用于开发测试工作");
        mockMvc.perform(post("/claims/apply")
                        .header("token", opTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 校验申请是否记录到了数据库，且状态为 0 (待审批)
        final String getClaimIdSql = "SELECT claim_id FROM t_equipment_claim WHERE equip_id = 'TE101' AND status = 0 ORDER BY claim_id DESC LIMIT 1";
        final Integer claimId = (Integer) userDao.singleSelect(getClaimIdSql);
        assertNotNull(claimId);

        // D. 重复申请拦截测试 (设备已有待审批的申请，再次申请)
        mockMvc.perform(post("/claims/apply")
                        .header("token", opTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：该设备已有处于待审批状态的领用申请，请勿重复申请！"));

        // E. 越权撤回拦截测试 (操作员 B 尝试撤回操作员 A 的申请)
        mockMvc.perform(put("/claims/" + claimId + "/cancel")
                        .header("token", opTokenD99))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("越权操作：只能撤回自己的领用申请"));

        // F. 正常撤回申请 (操作员 A 自行撤回)
        mockMvc.perform(put("/claims/" + claimId + "/cancel")
                        .header("token", opTokenD98))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证撤回后状态流转为 3
        final String checkCancelSql = "SELECT status FROM t_equipment_claim WHERE claim_id = ?";
        final Integer statusAfterCancel = (Integer) userDao.singleSelect(checkCancelSql, claimId);
        assertEquals(3, statusAfterCancel);

        // G. 对已撤回的申请进行审批拦截 (应该只能审批待审批状态 0)
        Map<String, Object> approveBody = new HashMap<>();
        approveBody.put("action", 1); // 同意
        approveBody.put("remark", "已阅");

        mockMvc.perform(put("/claims/" + claimId + "/approve")
                        .header("token", mgrTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("操作失败：只能审批“待审批”状态的申请"));

        // H. 重新发起领用申请
        mockMvc.perform(post("/claims/apply")
                        .header("token", opTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final Integer newClaimId = (Integer) userDao.singleSelect(getClaimIdSql);
        assertNotNull(newClaimId);
        assertNotEquals(claimId, newClaimId);

        // I. 跨部门审批拦截 (操作员 B 所属的管理员或不存在越权管理员，这里用 D99 token 调 D98 申请的审批 -> 预期 403 权限不足)
        // 既然 opTokenD99 是 role=0 没资格；如果是 D99 部门的管理员（我们可以把操作员 B 临时提权为 role=2 模拟跨部门管理员）
        userDao.update("UPDATE sys_user SET role = 2 WHERE id = ?", opD99Id);
        String mgrTokenD99 = loginUser("claim_op_d99"); // 变身为 D99 的管理员

        mockMvc.perform(put("/claims/" + newClaimId + "/approve")
                        .header("token", mgrTokenD99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isForbidden()); // 预期 403

        // J. 正常审批同意 (由 D98 管理员审批)
        mockMvc.perform(put("/claims/" + newClaimId + "/approve")
                        .header("token", mgrTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证设备保管人变更为 claim_op_d98
        final String getCustodianSql = "SELECT custodian FROM equipment WHERE equip_id = 'TE101'";
        final String custodian = (String) userDao.singleSelect(getCustodianSql);
        assertEquals("claim_op_d98", custodian);

        // K. 保管人主动退还设备 (操作员 A 退还)
        Map<String, String> returnBody = new HashMap<>();
        returnBody.put("equipId", "TE101");
        returnBody.put("remark", "用完了，谢谢");

        mockMvc.perform(post("/claims/return")
                        .header("token", opTokenD98)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证保管人已被置为 NULL
        final String custodianAfterReturn = (String) userDao.singleSelect(getCustodianSql);
        assertTrue(custodianAfterReturn == null || custodianAfterReturn.trim().isEmpty());

        // 验证退还审计日志 status=4 已经被写入
        final String getReturnClaimSql = "SELECT count(*) FROM t_equipment_claim WHERE equip_id = 'TE101' AND status = 4 AND applicant = 'claim_op_d98'";
        final Long returnCount = (Long) userDao.singleSelect(getReturnClaimSql);
        assertEquals(1L, returnCount);
    }
}
