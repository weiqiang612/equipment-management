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

        // 5. 注册新用户
        final User registerUser = new User();
        registerUser.setUsername(TEST_USERNAME);
        registerUser.setPassword(TEST_PASSWORD);
        registerUser.setRealName("测试新用户");

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

        // 10. 以 admin 的 Token 修改上述新用户的角色为 1 (维修工程师)，预期操作成功
        final User roleUpdateUser = new User();
        roleUpdateUser.setId(dbUser.getId());
        roleUpdateUser.setRole(1);

        mockMvc.perform(put("/users/role")
                        .header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 验证数据库角色确实变为了 1
        final User dbUserUpdated = userDao.getByUsername(TEST_USERNAME);
        assertEquals(1, dbUserUpdated.getRole());

        // 11. 以普通操作员/工程师角色 Token 修改自己或其他用户的角色，预期被拦截返回 403
        mockMvc.perform(put("/users/role")
                        .header("token", operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("权限不足"));
    }
}
