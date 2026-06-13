package com.weiqiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.dao.UserDao;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户管理控制器接口集成测试，验证权限及完整业务流程
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    private static final String TEST_USERNAME = "test_new_user";
    private static final String TEST_PASSWORD = "myPassword123";
    private static final String TEST_REAL_NAME = "测试新用户";
    private static final String TEST_UNIT_CODE = "DEPT001";

    @BeforeEach
    public void setup() {
        // 清理测试用的历史垃圾数据，保证测试的可重复性
        userDao.update("DELETE FROM sys_user WHERE username = ?", TEST_USERNAME);
    }

    @Test
    public void testAuthenticationAndAuthorizationFlow() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();

        // 1. 未带 Token 访问受保护接口，预期拦截并返回 401 状态码与 NOT_LOGIN
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("NOT_LOGIN"));

        // 2. 使用错误的账号或密码登录，预期返回 0 且提示错误
        final User loginErrorUser = new User();
        loginErrorUser.setUsername("admin");
        loginErrorUser.setPassword("wrongpassword");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginErrorUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        // 3. 使用正确密码登录（admin / 123456），预期返回 Token 且 code = 1
        final User adminLoginUser = new User();
        adminLoginUser.setUsername("admin");
        adminLoginUser.setPassword("123456");

        final MvcResult loginResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isString())
                .andReturn();

        final String loginResponseContent = loginResult.getResponse().getContentAsString();
        final Result adminResultObj = objectMapper.readValue(loginResponseContent, Result.class);
        final String adminToken = (String) adminResultObj.getData();
        assertNotNull(adminToken);

        // 4. 以 admin 的 Token 发起获取用户列表，预期正常放行返回列表
        mockMvc.perform(get("/users").header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray());

        // 5. 注册新用户 (添加单位 DEPT001)
        final User registerUser = new User();
        registerUser.setUsername(TEST_USERNAME);
        registerUser.setPassword(TEST_PASSWORD);
        registerUser.setRealName(TEST_REAL_NAME);
        registerUser.setUnitCode(TEST_UNIT_CODE);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 6. 验证数据库中该新用户密码已经过 MD5 转换，且 role 被强制覆盖为 0
        final User dbUser = userDao.getByUsername(TEST_USERNAME);
        assertNotNull(dbUser);
        assertEquals(0, dbUser.getRole());
        assertNotEquals(TEST_PASSWORD, dbUser.getPassword());
        assertEquals("487753b954871b5b05f854060de151d8", dbUser.getPassword()); // MD5(myPassword123)
        assertEquals(TEST_REAL_NAME, dbUser.getRealName());

        // 7. 再次使用相同用户名注册，预期被拦截返回 0，提示用户名已存在
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));

        // 8. 使用新注册的用户登录获取 Token
        final User operatorLoginUser = new User();
        operatorLoginUser.setUsername(TEST_USERNAME);
        operatorLoginUser.setPassword(TEST_PASSWORD);

        final MvcResult operatorLoginResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operatorLoginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        final String operatorResponseContent = operatorLoginResult.getResponse().getContentAsString();
        final Result operatorResultObj = objectMapper.readValue(operatorResponseContent, Result.class);
        final String operatorToken = (String) operatorResultObj.getData();
        assertNotNull(operatorToken);

        // 9. 以 operator 账号 Token 获取用户列表，预期拦截返回 403 权限不足
        mockMvc.perform(get("/users").header("token", operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        // 10. 以 admin 的 Token 修改上述新用户的角色为 1 (维修工程师) 并绑定单位，预期操作成功
        final User roleUpdateUser = new User();
        roleUpdateUser.setId(dbUser.getId());
        roleUpdateUser.setRole(1);
        roleUpdateUser.setUnitCode(TEST_UNIT_CODE);

        mockMvc.perform(put("/users/role")
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证数据库角色确实变为了 1 且单位变为了 DEPT001
        final User dbUserUpdated = userDao.getByUsername(TEST_USERNAME);
        assertEquals(1, dbUserUpdated.getRole());
        assertEquals(TEST_UNIT_CODE, dbUserUpdated.getUnitCode());

        // 11. 以普通操作员/工程师角色 Token 修改自己或其他用户的角色，预期被拦截返回 403
        mockMvc.perform(put("/users/role")
                        .header("token", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));

        // 12. [新增AC测试] 自助注册不填单位，预期失败并提示 '所属单位不能为空'
        final User noDeptRegisterUser = new User();
        noDeptRegisterUser.setUsername("test_no_dept");
        noDeptRegisterUser.setPassword("myPassword123");
        noDeptRegisterUser.setRealName("无单位用户");
        // 不设置 unitCode
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDeptRegisterUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("所属单位不能为空"));

        // 13. [新增AC测试] 修改角色为系统管理员 (role=3)，预期其所属单位自动重置为 NULL
        final User toAdminUser = new User();
        toAdminUser.setId(dbUser.getId());
        toAdminUser.setRole(3);
        toAdminUser.setUnitCode("DEPT002"); // 即使传了值，后端也应该自动清空

        mockMvc.perform(put("/users/role")
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAdminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final User dbUserAdmin = userDao.getById(dbUser.getId());
        assertEquals(3, dbUserAdmin.getRole());
        assertNull(dbUserAdmin.getUnitCode());

        // 14. [新增AC测试] 部门变更且名下有设备时的强阻断拦截
        // 先还原该用户为操作员 (role=0)，分配到 DEPT001 部门
        final User backToOpUser = new User();
        backToOpUser.setId(dbUser.getId());
        backToOpUser.setRole(0);
        backToOpUser.setUnitCode(TEST_UNIT_CODE);
        mockMvc.perform(put("/users/role")
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backToOpUser)))
                .andExpect(status().isOk());

        // 将设备 'EQ2024001' 临时指派给该新账号作为保管人
        userDao.update("UPDATE equipment SET custodian = ? WHERE equip_id = 'EQ2024001'", TEST_USERNAME);

        try {
            // 尝试变更部门为 DEPT002，期待强阻断
            final User blockUpdateUser = new User();
            blockUpdateUser.setId(dbUser.getId());
            blockUpdateUser.setRole(0);
            blockUpdateUser.setUnitCode("DEPT002");

            mockMvc.perform(put("/users/role")
                            .header("token", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blockUpdateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.msg").value("操作失败：该用户尚有未清退的保管设备，请先去设备管理处退还或交接设备！"));
        } finally {
            // 恢复设备 EQ2024001 保管关系，避免污染
            userDao.update("UPDATE equipment SET custodian = NULL WHERE equip_id = 'EQ2024001'");
        }
    }

    @Test
    public void testUserProfileAndPasswordManagementFlow() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String adminToken = loginAndGetToken(objectMapper, "admin", "123456");

        final User registerUser = new User();
        registerUser.setUsername(TEST_USERNAME);
        registerUser.setPassword(TEST_PASSWORD);
        registerUser.setRealName(TEST_REAL_NAME);
        registerUser.setUnitCode(TEST_UNIT_CODE);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final User dbUser = userDao.getByUsername(TEST_USERNAME);
        assertNotNull(dbUser);

        final User updateRequest = new User();
        updateRequest.setId(dbUser.getId());
        updateRequest.setUsername("should_not_change");
        updateRequest.setRealName("测试新用户-已修改");
        updateRequest.setRole(1);
        updateRequest.setUnitCode(TEST_UNIT_CODE);

        mockMvc.perform(put("/users/" + dbUser.getId())
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        final User updatedUser = userDao.getById(dbUser.getId());
        assertEquals(TEST_USERNAME, updatedUser.getUsername());
        assertEquals("测试新用户-已修改", updatedUser.getRealName());
        assertEquals(1, updatedUser.getRole());
        assertEquals(TEST_UNIT_CODE, updatedUser.getUnitCode());

        final Map<String, String> resetPasswordRequest = new HashMap<>();
        resetPasswordRequest.put("newPassword", "ResetPass123");

        mockMvc.perform(put("/users/" + dbUser.getId() + "/password/reset")
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildLoginUser(TEST_USERNAME, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        final String resetPasswordToken = loginAndGetToken(objectMapper, TEST_USERNAME, "ResetPass123");
        assertNotNull(resetPasswordToken);

        final Map<String, String> selfPasswordRequest = new HashMap<>();
        selfPasswordRequest.put("oldPassword", "ResetPass123");
        selfPasswordRequest.put("newPassword", "SelfPass123");

        mockMvc.perform(put("/users/password")
                        .header("token", resetPasswordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildLoginUser(TEST_USERNAME, "ResetPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        final String newPasswordToken = loginAndGetToken(objectMapper, TEST_USERNAME, "SelfPass123");
        assertNotNull(newPasswordToken);

        mockMvc.perform(put("/users/" + dbUser.getId() + "/password/reset")
                        .header("token", newPasswordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));
    }

    private String loginAndGetToken(final ObjectMapper objectMapper, final String username, final String password) throws Exception {
        final MvcResult loginResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildLoginUser(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        final Result resultObj = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Result.class);
        return (String) resultObj.getData();
    }

    private User buildLoginUser(final String username, final String password) {
        final User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
