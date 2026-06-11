# 前后端接口契约规范 (API Contract)

本规范约定了设备管理系统在 **登录鉴权、账户注册与用户权限管理** 模块下的前后端接口细节。前后端在并行开发时，必须严格遵守本契约定义的请求路径、Header、入参及出参格式。

---

## 📌 统一响应格式 (Result)

所有后端 API 返回的 JSON 报文均采用项目已定义的 `Result` 进行封装：

| 属性名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `code` | `Integer` | 状态码：`1` 表示成功，`0` 表示业务失败（如账密错误、重复注册）。 |
| `msg` | `String` | 提示消息：当成功时通常为 `"success"`，失败时返回具体错误原因。 |
| `data` | `Object` | 承载的实际数据实体（可以为 Object、Array、String 或 null）。 |

---

## 🔐 接口列表

### 1. 用户登录 (User Login)

用于验证用户身份并下发 JWT Token。

*   **请求路径**：`POST /users/login`
*   **请求头**：`Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "username": "operator1",
      "password": "plain_text_password"
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：返回生成的加密 Token。
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJvcGVyYXRvcjEiLCJyb2xlIjowLCJleHAiOjE3ODEwNjY3ODJ9..." 
        }
        ```
    *   **失败 (Result.code = 0)**：
        ```json
        {
          "code": 0,
          "msg": "用户名或密码错误",
          "data": null
        }
        ```

---

### 2. 用户自助注册 (User Register)

供普通操作员自助注册账号。新注册用户角色强制默认为 `0`（设备操作员）。

*   **请求路径**：`POST /users/register`
*   **请求头**：`Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "username": "new_user_1",
      "realName": "张三",
      "password": "plain_text_password"
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```
    *   **失败 (用户名已存在)**：
        ```json
        {
          "code": 0,
          "msg": "用户名已存在",
          "data": null
        }
        ```

---

### 3. 获取用户列表 (Get User List)

仅限 **系统管理员 (role=3)** 调用，用于在用户管理后台展示所有系统账户（出参必须剔除密码字段，防安全泄露）。

*   **请求路径**：`GET /users`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>` （必须携带登录成功后获取的 Token）
*   **请求参数**：无
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": [
            {
              "id": 1,
              "username": "operator1",
              "realName": "设备操作员小张",
              "role": 0,
              "createTime": "2026-06-10 14:00:00",
              "updateTime": "2026-06-10 14:00:00"
            },
            {
              "id": 2,
              "username": "admin",
              "realName": "超级管理员系统",
              "role": 3,
              "createTime": "2026-06-01 10:00:00",
              "updateTime": "2026-06-01 10:00:00"
            }
          ]
        }
        ```
    *   **失败 (未带 Token / Token 过期 / 非管理员越权)**：返回 `401` 或 `403`：
        ```json
        {
          "code": 0,
          "msg": "权限不足，拒绝访问",
          "data": null
        }
        ```

---

### 4. 修改用户角色 (Update User Role)

仅限 **系统管理员 (role=3)** 调用，用于在管理后台变更某用户的岗位权限。

*   **请求路径**：`PUT /users/role`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "id": 1,       // 目标用户的自增主键 ID
      "role": 2      // 目标分配的角色编号 (0-操作员, 1-工程师, 2-资产管理员, 3-系统管理员)
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```
    *   **失败 (非法角色值 / 越权修改)**：
        ```json
        {
          "code": 0,
          "msg": "角色值无效或权限不足",
          "data": null
        }
        ```

---

### 5. 提交领用申请 (Apply Equipment Claim)

供操作员或管理员对本部门内无保管人的空闲设备发起领用申请。

*   **请求路径**：`POST /claims/apply`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "equipId": "E001",
      "remark": "日常研发使用"
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```

---

### 6. 撤回领用申请 (Cancel Equipment Claim)

供申请人自主撤回处于“待审批”状态的领用申请。

*   **请求路径**：`PUT /claims/{claimId}/cancel`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```

---

### 7. 审批领用申请 (Approve Equipment Claim)

供资产管理员/系统管理员审批本部门操作员发起的领用申请。

*   **请求路径**：`PUT /claims/{claimId}/approve`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "action": 1,    // 审批动作：1-同意, 2-拒绝
      "remark": "同意领用"  // 审批意见
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```

---

### 8. 主动退还设备 (Return Equipment)

供保管人本人主动归还已领用的设备，将其保管人重置为空。

*   **请求路径**：`POST /claims/return`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "equipId": "E001",
      "remark": "项目结束退还"
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```

---

### 9. 查询领用记录列表 (Get Claim List)

分页查询领用申请/历史审计列表。操作员仅能看到自己的记录，管理员能看到本部门的全部记录。

*   **请求路径**：`GET /claims`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `equipId` (可选，设备编号筛选)
    *   `status` (可选，状态筛选)
    *   `page` (默认 1)
    *   `pageSize` (默认 10)
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "total": 1,
            "rows": [
              {
                "claimId": 1,
                "equipId": "E001",
                "applicant": "operator1",
                "approver": "manager1",
                "status": 1,
                "remark": "同意领用",
                "createTime": "2026-06-11 10:00:00",
                "updateTime": "2026-06-11 10:05:00",
                "equipName": "研发笔记本",
                "applicantRealName": "张三",
                "approverRealName": "李四"
              }
            ]
          }
        }
        ```
