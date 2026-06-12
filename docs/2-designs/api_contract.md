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
      "password": "plain_text_password",
      "unitCode": "D001"    // 所属单位代码 (必填)
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
      "role": 2,      // 目标分配的角色编号 (0-操作员, 1-工程师, 2-资产管理员, 3-系统管理员)
      "unitCode": "D001" // 所属单位代码 (当 role=3 时可传 null/不传，其他角色为必填)
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

---

### 10. 获取看板数据 (Get Dashboard Summary)

获取当前登录角色的聚合数据看板。后端根据 Token 解析出的角色、用户名及所属单位动态裁剪数据。

*   **请求路径**：`GET /dashboard/summary`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：无
*   **响应结构 (Result.data)**：
    *   **通用结构**：
        ```json
        {
          "role": 2, // 0-操作员, 1-维修工, 2-资产管理员, 3-系统管理员
          "kpis": {}, // KPI指标项（不同角色键值对不同）
          "charts": {}, // 图表数据集（不同角色键值对不同，内部常为 { name, value } 或 { month, cost, count }）
          "listData": {} // 列表/待办数据集
        }
        ```
    *   **Role 2 (资产管理员) 响应示例**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "role": 2,
            "kpis": {
              "totalEquipment": 105,
              "totalValue": 854000.00,
              "inUseCount": 85,
              "inMaintenanceCount": 12,
              "scrappedCount": 8
            },
            "charts": {
              "categoryDistribution": [
                { "name": "网络设备", "value": 25 },
                { "name": "计算机设备", "value": 80 }
              ],
              "departmentDistribution": [
                { "name": "研发部", "value": 45 },
                { "name": "行政部", "value": 60 }
              ],
              "maintenanceTrend": [
                { "month": "2026-05", "cost": 5400.00, "count": 6 },
                { "month": "2026-06", "cost": 3200.00, "count": 4 }
              ]
            },
            "listData": {
              "pendingClaims": [
                { "claimId": 1, "equipId": "E001", "equipName": "研发笔记本", "applicantRealName": "张三", "createTime": "2026-06-11 10:00:00" }
              ],
              "pendingMaintenances": [
                { "maintId": 5, "equipId": "E002", "faultDescription": "开机无显示", "maintStatus": 0 }
              ]
            }
          }
        }
        ```
    *   **Role 3 (系统管理员) 响应示例**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "role": 3,
            "kpis": {
              "totalEquipment": 105,
              "totalValue": 854000.00,
              "totalUsers": 12,
              "backupCount": 3
            },
            "charts": {
              "userRoleDistribution": [
                { "name": "操作员", "value": 6 },
                { "name": "维修工", "value": 3 },
                { "name": "资产管理员", "value": 2 },
                { "name": "系统管理员", "value": 1 }
              ]
            },
            "listData": {
              "backupFiles": [
                { "name": "backup_1781065853532.sql", "size": 15420, "lastModified": 1781065853532 }
              ]
            }
          }
        }
        ```
    *   **Role 1 (维修工程师) 响应示例**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "role": 1,
            "kpis": {
              "myPendingMaint": 3, // 分配我的待处理工单数
              "myInMaint": 2, // 维修中工单数
              "myCompletedMaint": 15 // 历史完工数
            },
            "charts": {
              "maintCostTrend": [
                { "month": "2026-05", "cost": 1500.00 },
                { "month": "2026-06", "cost": 800.00 }
              ]
            },
            "listData": {
              "myWorkOrders": [
                { "maintId": 6, "equipId": "E003", "faultDescription": "网口松动", "maintStatus": 1 }
              ]
            }
          }
        }
        ```
    *   **Role 0 (设备操作员) 响应示例**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "role": 0,
            "kpis": {
              "myEquipCount": 4, // 个人保管设备数
              "myActiveClaims": 1, // 个人在途领用申请数
              "myActiveMaintenances": 1, // 个人在途报修数
              "myDepreciationValue": 1540.00 // 个人保管设备累计折旧总额
            },
            "listData": {
              "myEquipments": [
                { "equipId": "E004", "equipName": "办公电脑", "status": "在用", "originalValue": 5000.00 }
              ],
              "myClaims": [
                { "claimId": 2, "equipId": "E005", "status": 0, "createTime": "2026-06-11 12:00:00" }
              ],
              "myMaintenances": [
                { "maintId": 7, "equipId": "E004", "maintStatus": 1 }
              ]
            }
          }
        }
        ```

---

### 11. 查询审计日志流水 (Get Operation Log List)

仅限 **系统管理员 (role=3)** 调用，用于在操作审计后台展示系统关键操作。

*   **请求路径**：`GET /system/log/list`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `operator` (可选，操作人筛选)
    *   `opType` (可选，操作类型筛选)
    *   `targetType` (可选，对象类型筛选)
    *   `status` (可选，状态筛选，1-成功，0-失败)
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
                "id": 1,
                "operator": "admin",
                "operatorRole": 3,
                "opType": "设备新增",
                "targetType": "equipment",
                "targetId": "E001",
                "opTime": "2026-06-12 10:00:00",
                "summary": "新增设备: 研发笔记本 (E001)",
                "status": 1,
                "errorMsg": null
              }
            ]
          }
        }
        ```

---

### 12. 查询设备生命周期聚合详情 (Get Equipment Lifecycle Detail)

查询单台设备的全生命周期流转历史。各角色访问严格受到 RBAC 越权校验：普通用户（Role 0）仅能访问自己保管的设备；维修工和管理员（Role 1/2）仅能访问本单位的设备；系统管理员（Role 3）无限制。

*   **请求路径**：`GET /equipments/detail/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "equipId": "E001",
            "equipName": "研发笔记本",
            "model": "ThinkPad T14",
            "status": "在用",
            "purchaseDate": "2026-06-01",
            "originalValue": 8000.00,
            "unitCode": "D001",
            "unitName": "研发部",
            "categoryId": "C001",
            "categoryName": "计算机设备",
            "custodian": "operator1",
            "custodianRealName": "张三",
            "usefulLife": 5,
            "residualRate": 0.05,
            "monthlyDepreciation": 126.67,
            "accumulatedDepreciation": 126.67,
            "netValue": 7873.33,
            "claims": [
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
            ],
            "maintenances": [
              {
                "maintId": 1,
                "equipId": "E001",
                "maintDate": "2026-06-05",
                "maintContent": "更换风扇",
                "maintCost": 150.00,
                "maintPerson": "工程师小王",
                "reporter": "operator1",
                "faultDescription": "风扇噪音大",
                "maintStatus": 2,
                "maintPersonId": 3
              }
            ],
            "transfers": [],
            "scrap": null,
            "auditTimeline": [
              {
                "id": 2,
                "operator": "manager1",
                "operatorRole": 2,
                "opType": "领用同意",
                "targetType": "t_equipment_claim",
                "targetId": "1",
                "opTime": "2026-06-11 10:05:00",
                "summary": "审批人 manager1 同意了领用申请 1，设备保管人变更为 operator1",
                "status": 1,
                "errorMsg": null
              },
              {
                "id": 1,
                "operator": "operator1",
                "operatorRole": 0,
                "opType": "领用申请",
                "targetType": "t_equipment_claim",
                "targetId": "1",
                "opTime": "2026-06-11 10:00:00",
                "summary": "申请人 operator1 申请领用设备 E001",
                "status": 1,
                "errorMsg": null
              }
            ]
          }
        }
        ```
