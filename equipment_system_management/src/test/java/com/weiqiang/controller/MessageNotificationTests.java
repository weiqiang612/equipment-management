package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.SysMessageDao;
import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.SysMessage;
import com.weiqiang.pojo.User;
import com.weiqiang.service.SysMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 消息通知集成测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
public class MessageNotificationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    @Autowired
    private SysMessageDao sysMessageDao;

    @Autowired
    private SysMessageService sysMessageService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String op1Token;
    private String mgr1Token;
    private String eng1Token;

    private String op2Token;
    private String mgr2Token;
    private String eng2Token;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    public void setup() throws Exception {
        // 1. 清理数据
        cleanupData();

        // 2. 初始化部门与分类数据
        userDao.update("INSERT INTO department (unit_code, unit_name, manager) VALUES ('D88', '测试部门', '王经理')");
        userDao.update("INSERT INTO category (category_id, category_name, useful_life, residual_rate) VALUES ('C88', '测试分类', 5, 0.05)");

        // 3. 注册并设置用户，获取 Token
        mgr1Token = setupUser("test_msg_mgr_1", 2);
        eng1Token = setupUser("test_msg_eng_1", 1);
        op1Token = setupUser("test_msg_op_1", 0);

        mgr2Token = setupUser("test_msg_mgr_2", 2);
        eng2Token = setupUser("test_msg_eng_2", 1);
        op2Token = setupUser("test_msg_op_2", 0);

        userAToken = setupUser("test_msg_user_a", 0);
        userBToken = setupUser("test_msg_user_b", 0);
    }

    @AfterEach
    public void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        userDao.update("DELETE FROM sys_message WHERE target_user LIKE 'test_msg_%'");
        userDao.update("DELETE FROM maintenance_record WHERE equip_id LIKE 'TE_MSG_%' OR maint_id IN (8888, 9999) OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code = 'D88' OR category_id = 'C88')");
        userDao.update("DELETE FROM t_equipment_claim WHERE equip_id LIKE 'TE_MSG_%' OR equip_id IN (SELECT equip_id FROM equipment WHERE unit_code = 'D88' OR category_id = 'C88')");
        userDao.update("DELETE FROM equipment WHERE equip_id LIKE 'TE_MSG_%' OR unit_code = 'D88' OR category_id = 'C88'");
        userDao.update("DELETE FROM sys_user WHERE username LIKE 'test_msg_%'");
        userDao.update("DELETE FROM department WHERE unit_code = 'D88'");
        userDao.update("DELETE FROM category WHERE category_id = 'C88'");
    }

    private String setupUser(final String username, final Integer role) throws Exception {
        final User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setRealName(username + "_real");
        user.setUnitCode("D88");
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        final User dbUser = userDao.getByUsername(username);
        userDao.update("UPDATE sys_user SET role = ?, unit_code = 'D88' WHERE id = ?", role, dbUser.getId());

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

    private void resetCoolDownCache() {
        try {
            final Object target = AopTestUtils.getUltimateTargetObject(sysMessageService);
            final java.lang.reflect.Field field = target.getClass().getDeclaredField("lastSyncTimeMap");
            field.setAccessible(true);
            final Map<?, ?> map = (Map<?, ?>) field.get(target);
            if (map != null) {
                map.clear();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试场景 1：事件生成与路由
     */
    @Test
    public void testEventGenerationAndRouting() throws Exception {
        // Given
        // 1. 高风险设备
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id) " +
                       "VALUES ('TE_MSG_001', '高风险设备测试', 'Model-Y', '在用', DATE_SUB(NOW(), INTERVAL 72 MONTH), 10000.00, 'D88', 'C88')");

        // 2. 积压审批：25小时前创建
        userDao.update("INSERT INTO t_equipment_claim (equip_id, applicant, approver, status, remark, create_time, update_time) " +
                       "VALUES ('TE_MSG_001', 'test_msg_op_1', 'test_msg_mgr_1', 0, '领用备注', DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR))");

        // 3. 超时维保：49小时前指派
        final User eng1 = userDao.getByUsername("test_msg_eng_1");
        userDao.update("INSERT INTO maintenance_record (maint_id, equip_id, maint_date, fault_description, maint_status, maint_person_id, maint_person, assign_time) " +
                       "VALUES (9999, 'TE_MSG_001', CURRENT_DATE(), '测试故障', 1, ?, '测试维修工', DATE_SUB(NOW(), INTERVAL 49 HOUR))", eng1.getId());

        // When & Then
        // 资产管理员 test_msg_mgr_1 获取消息
        final MvcResult mgrRes = mockMvc.perform(get("/messages")
                        .header("token", mgr1Token))
                .andExpect(status().isOk())
                .andReturn();

        final String mgrContent = mgrRes.getResponse().getContentAsString();
        final Result mgrResultObj = objectMapper.readValue(mgrContent, Result.class);
        assertEquals(1, mgrResultObj.getCode(), "管理员拉取消息接口调用失败，错误: " + mgrResultObj.getMsg());
        assertNotNull(mgrResultObj.getData());

        final PageBean<?> mgrPage = objectMapper.readValue(objectMapper.writeValueAsString(mgrResultObj.getData()), PageBean.class);
        assertEquals(3, mgrPage.getTotal());

        // 维修工程师 test_msg_eng_1 获取消息
        final MvcResult engRes = mockMvc.perform(get("/messages")
                        .header("token", eng1Token))
                .andExpect(status().isOk())
                .andReturn();

        final Result engResultObj = objectMapper.readValue(engRes.getResponse().getContentAsString(), Result.class);
        assertEquals(1, engResultObj.getCode(), "维修工拉取消息接口调用失败，错误: " + engResultObj.getMsg());
        final PageBean<?> engPage = objectMapper.readValue(objectMapper.writeValueAsString(engResultObj.getData()), PageBean.class);
        assertEquals(1, engPage.getTotal());
    }

    /**
     * 测试场景 2：去重与失效
     */
    @Test
    public void testDeduplicationAndInvalidation() throws Exception {
        // Given
        final User eng2 = userDao.getByUsername("test_msg_eng_2");
        userDao.update("INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id) " +
                       "VALUES ('TE_MSG_002', '去重设备测试', 'Model-Y', '在用', NOW(), 10000.00, 'D88', 'C88')");
        userDao.update("INSERT INTO maintenance_record (maint_id, equip_id, maint_date, fault_description, maint_status, maint_person_id, maint_person, assign_time) " +
                       "VALUES (8888, 'TE_MSG_002', CURRENT_DATE(), '去重测试故障', 1, ?, '测试维修工2', DATE_SUB(NOW(), INTERVAL 49 HOUR))", eng2.getId());

        // When & Then
        // 首次请求同步并查询未读数，预期为 1
        mockMvc.perform(get("/messages/unread-count")
                        .header("token", eng2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(1));

        // 验证数据库里仅有一条有效未读消息，无重复
        final List<SysMessage> msgs = sysMessageDao.listValidMessagesByTargetUser("test_msg_eng_2");
        assertEquals(1, msgs.size());

        // 消除超时原因，更新数据库里的工单为已完成 (maint_status = 3)
        userDao.update("UPDATE maintenance_record SET maint_status = 3, complete_time = NOW() WHERE maint_id = 8888");

        // 绕过 30秒 同步冷却机制以触发消息失效更新
        resetCoolDownCache();

        // 再次获取未读消息，触发同步
        mockMvc.perform(get("/messages/unread-count")
                        .header("token", eng2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        // 检查数据库中原来的消息有效性变为 0
        final SysMessage invalidatedMsg = sysMessageDao.getById(msgs.get(0).getId());
        assertEquals(0, invalidatedMsg.getIsValid());
    }

    /**
     * 测试场景 3：安全隔离性
     */
    @Test
    public void testSecurityIsolation() throws Exception {
        // Given
        final SysMessage msg = new SysMessage();
        msg.setTitle("越权测试消息");
        msg.setContent("内容");
        msg.setEventType("high_risk_equipment");
        msg.setTargetUser("test_msg_user_b");
        msg.setStatus(0);
        msg.setIsValid(1);
        msg.setRefType("equipment");
        msg.setRefId("TE_MSG_003");
        sysMessageDao.insert(msg);

        final SysMessage inserted = sysMessageDao.getValidMessageByRef("test_msg_user_b", "high_risk_equipment", "equipment", "TE_MSG_003");
        assertNotNull(inserted);

        // When & Then
        // test_msg_user_a 越权标记已读，预期返回 403
        mockMvc.perform(put("/messages/" + inserted.getId() + "/read")
                        .header("token", userAToken))
                .andExpect(status().isForbidden());
    }
}
