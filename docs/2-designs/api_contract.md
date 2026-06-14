# 前后端接口契约规范 (API Contract)

本规范约定了设备管理系统在 **登录鉴权、账户注册与用户权限管理** 模块下的前后端接口细节。前后端在并行开发时，必须严格遵守本契约定义的请求路径、Header、入参及出参格式。

---

## 1. 统一响应格式

所有后端 API 返回的 JSON 报文均采用项目已定义的 `Result` 进行封装：

| 属性名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `code` | `Integer` | 状态码：`1` 表示成功，`0` 表示业务失败（如账密错误、重复注册）。 |
| `msg` | `String` | 提示消息：当成功时通常为 `"success"`，失败时返回具体错误原因。 |
| `data` | `Object` | 承载的实际数据实体（可以为 Object、Array、String 或 null）。 |

---

## 2. 接口明细

### 2.1 用户登录

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

### 2.2 用户注册

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

### 2.3 用户列表

限 **资产管理员 (role=2)** 与 **系统管理员 (role=3)** 调用。该接口既用于系统管理员的“用户权限管理”后台，也用于资产管理员在设备分配等业务场景中获取可选人员列表。出参必须剔除密码字段，防止敏感信息泄露。

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

### 2.4 修改用户角色

仅限 **系统管理员 (role=3)** 调用，用于兼容旧版前端按角色/单位分别提交的更新方式。新前端主流程改用统一资料更新接口。

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

### 2.5 更新用户资料

仅限 **系统管理员 (role=3)** 调用，用于在管理后台统一修改某用户的真实姓名、角色与所属单位。`username` 为稳定登录标识，不支持修改。

*   **请求路径**：`PUT /users/{id}`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `id`：目标用户主键 ID
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "realName": "张三",
      "role": 2,
      "unitCode": "D001"
    }
    ```
*   **规则说明**：
    *   当 `role = 3` 时，后端强制将 `unitCode` 置为 `null`。
    *   当 `role != 3` 时，`unitCode` 为必填且必须是有效单位编码。
    *   若单位发生变更且用户名下仍有保管设备，后端阻断本次更新。
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

### 2.6 重置用户密码

仅限 **系统管理员 (role=3)** 调用，用于为其他账号直接设置新密码。当前登录管理员本人不允许通过此接口修改自己的密码，应改走右上角“修改密码”入口。

*   **请求路径**：`PUT /users/{id}/password/reset`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `id`：目标用户主键 ID
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "newPassword": "ResetPass123"
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

### 2.7 修改本人密码

所有已登录用户均可调用，用于验证旧密码后修改本人密码。

*   **请求路径**：`PUT /users/password`
*   **请求头**：
    *   `Content-Type: application/json`
    *   `token: <JWT_TOKEN_STRING>`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "oldPassword": "ResetPass123",
      "newPassword": "SelfPass123"
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
    *   **失败 (旧密码错误)**：
        ```json
        {
          "code": 0,
          "msg": "旧密码错误",
          "data": null
        }
        ```

---

### 2.8 删除用户

仅限 **系统管理员 (role=3)** 调用，用于删除某一系统账号。删除时遵循以下后端规则：

*   若该用户名下仍有关联设备保管关系，系统会先自动清空对应设备的 `custodian`，并写入一条状态为 `4-已退还` 的领用流水，备注为“用户被删除导致保管关系自动清退”。
*   若该用户仍有关联的未完结检修工单（作为报修人或被指派维修人），后端阻断删除并返回业务错误。

*   **请求路径**：`DELETE /users/{id}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `id`：目标用户主键 ID
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": null
        }
        ```
    *   **失败 (仍有未完结检修工单)**：
        ```json
        {
          "code": 0,
          "msg": "操作失败：该用户尚有未完结的检修工单，无法删除！",
          "data": null
        }
        ```

---

### 2.9 提交领用申请

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

### 2.10 撤回领用申请

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

### 2.11 审批领用申请

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

### 2.12 主动退还设备

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

### 2.13 领用记录列表

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

### 2.14 看板数据

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

### 2.15 审计日志流水

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

### 2.16 设备生命周期详情

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

---

### 2.17 数据治理总览

获取当前登录部门的数据治理与运营风险总览。Role 2 资产管理员只能查看本单位数据，Role 3 系统管理员可查看全局数据。Role 0/1 拒绝访问。

*   **请求路径**：`GET /governance/summary`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：无
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "qualityScore": 95.5,
            "totalEquipmentCount": 100,
            "issueCount": 4,
            "missingFieldsCount": 1,
            "mismatchCount": 2,
            "duplicateCount": 2,
            "highRiskCount": 3,
            "mediumRiskCount": 5,
            "lowRiskCount": 92,
            "idleCount": 10,
            "costAnomalyCount": 2
          }
        }
        ```

---

### 2.18 风险设备清单

分页查询受风险影响的设备列表，支持根据风险等级、使用单位、分类编码筛选。Role 2 资产管理员仅能过滤本单位的数据，Role 3 系统管理员可获取全局数据。Role 0/1 拒绝访问。

*   **请求路径**：`GET /governance/equipment-risks`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `riskLevel` (可选，风险等级："高风险", "中风险", "低风险")
    *   `unitCode` (可选，单位代码。Role 2 只能传本单位代码，传其他或不传时均由后端强制过滤为本单位代码)
    *   `categoryId` (可选，分类编码)
    *   `page` (默认 1)
    *   `pageSize` (默认 10)
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "total": 3,
            "rows": [
              {
                "equipId": "E001",
                "equipName": "研发笔记本",
                "model": "ThinkPad T14",
                "categoryId": "C001",
                "categoryName": "计算机设备",
                "unitCode": "D001",
                "unitName": "研发部",
                "custodian": "operator1",
                "status": "在用",
                "healthScore": 40,
                "riskLevel": "高风险",
                "riskReasons": "维保次数超标,使用年限占比超标",
                "maintenanceCount": 3,
                "costRatio": 0.12,
                "ageRatio": 0.95,
                "originalValue": 8000.00,
                "purchaseDate": "2021-06-01"
              }
            ]
          }
        }
        ```

---

### 2.19 指派维保工单

限 **资产管理员 (role=2)** 调用，用于指派待处理工单给本单位维修工程师。

*   **请求路径**：`PUT /maintenanceRecords/assign/{maintId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "maintPersonId": 3
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

### 2.20 登记维保完工

限 **被指派的维修工程师 (role=1)** 或 **资产管理员 (role=2)** 调用，登记实际维修数据，更新工单为“待复核”状态，设备依然保持“维修”态。

*   **请求路径**：`PUT /maintenanceRecords/complete/{maintId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "maintDate": "2026-06-13",
      "maintContent": "更换电容，加固电路板",
      "maintCost": 150.00,
      "maintPerson": "维修工小张"
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

### 2.21 完工复核处置

限 **资产管理员 (role=2)** 调用，对本单位待复核的工单进行结案处置，分流为“恢复可用”或“转报废”。

*   **请求路径**：`PUT /maintenanceRecords/review/{maintId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    *   **恢复可用分支**：
        ```json
        {
          "maintStatus": 3,
          "reviewComments": "复核合格，设备运行正常"
        }
        ```
    *   **转报废分支**：
        ```json
        {
          "maintStatus": 4,
          "reviewComments": "无法修复，老化严重",
          "scrapNo": "BF2606130000001" // 可选，为空时后端将自动生成
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

### 2.22 资产运营报告草案

限 **资产管理员 (role=2)** 或 **系统管理员 (role=3)** 调用，基于当前系统的看板统计数据、操作审计与治理风险等信息生成一份周度或月度运营报告草案。

*   **请求路径**：`POST /ai/reports/operations/draft`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **请求体 (RequestBody - JSON)**：
    ```json
    {
      "period": "weekly"
    }
    ```
*   **响应示例**：
    *   **成功 (Result.code = 1)**：返回生成的 markdown 格式文本草案。
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "title": "资产运营周报草案",
            "content": "# 资产运营周报\n...\n",
            "period": "weekly",
            "generatedTime": "2026-06-13T16:30:00"
          }
        }
        ```
    *   **接口未启用 (Result.code = 0)**（API Key 未配置时优雅降级）：
        ```json
        {
          "code": 0,
          "msg": "AI 辅助服务未启用：请联系管理员配置 AI 接口凭证",
          "data": null
        }
        ```
    *   **权限不足 (Result.code = 0, HTTP 403)**：
        ```json
        {
          "code": 0,
          "msg": "权限不足，拒绝访问",
          "data": null
        }
        ```

---

### 2.23 设备生命周期摘要

限 **资产管理员 (role=2)** 或 **系统管理员 (role=3)** 调用，针对指定设备拉取该设备的资产信息、保管领用历史、检修记录和审计日志等，组装上下文以生成生命周期健康摘要及处置草案建议。

*   **请求路径**：`POST /ai/equipment/{equipId}/summary`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **响应示例**：
    *   **成功 (Result.code = 1)**：
        ```json
        {
          "code": 1,
          "msg": "success",
          "data": {
            "equipId": "TE001",
            "equipName": "高频振动筛",
            "summary": "### 设备健康分析摘要\n...\n",
            "riskLevel": "high",
            "generatedTime": "2026-06-13T16:30:00"
          }
        }
        ```
    *   **接口未启用 (Result.code = 0)**：
        ```json
        {
          "code": 0,
          "msg": "AI 辅助服务未启用：请联系管理员配置 AI 接口凭证",
          "data": null
        }
        ```



---

## 3. 接口索引

> 本节按当前 Controller 与前端 `api/` 模块补齐接口索引，用于报告写作和截图验收。详细请求/响应示例优先参考前文已展开的关键接口；本节主要保证文档覆盖现有代码入口。

| 模块 | 方法 | 路径 | 角色边界 | 前端调用 |
| :--- | :--- | :--- | :--- | :--- |
| 用户 | POST | `/users/login` | 白名单 | `api/user.js::login` |
| 用户 | POST | `/users/register` | 白名单，注册默认为 Role 0 | `api/user.js::register` |
| 用户 | GET | `/users` | Role 2/3 | `api/user.js::getUsers` |
| 用户 | PUT | `/users/role` | Role 3 | `api/user.js::updateUserRole` |
| 用户 | PUT | `/users/{id}` | Role 3 | `api/user.js::updateUserProfile` |
| 用户 | DELETE | `/users/{id}` | Role 3 | `api/user.js::deleteUser` |
| 用户 | PUT | `/users/{id}/password/reset` | Role 3 | `api/user.js::resetUserPassword` |
| 用户 | PUT | `/users/password` | 已登录用户 | `api/user.js::changeCurrentPassword` |
| 用户 | GET | `/users/maintainers` | 已登录用户 | `api/user.js::getMaintainers` |
| 设备 | GET | `/equipments` | 已登录用户，服务层裁剪 | `api/equipment.js::getEquipments` |
| 设备 | GET | `/equipments/export` | 已登录用户，服务层裁剪 | 设备导出功能 |
| 设备 | GET | `/equipments/{equipId}` | 已登录用户 | 设备编辑/详情前置读取 |
| 设备 | POST | `/equipments` | Role 2 | `api/equipment.js::addEquipment` |
| 设备 | PUT | `/equipments/{equipId}` | Role 2 | `api/equipment.js::updateEquipment` |
| 设备 | DELETE | `/equipments/{equipId}` | Role 2 | `api/equipment.js::deleteEquipment` |
| 设备 | POST | `/equipments/maint/{equipId}` | 已登录用户，服务层校验保管/单位 | `api/equipment.js::maintenanceEquip` |
| 设备 | POST | `/equipments/scrap/{equipId}` | Role 2 | `api/equipment.js::scrapEquipment` |
| 设备 | POST | `/equipments/transfer/{equipId}` | Role 2 | `api/equipment.js::transferEquipment` |
| 设备 | GET | `/equipments/calculateAccumulated/{equipId}` | 已登录用户 | `api/equipment.js::getCalculateAccumulated` |
| 设备 | GET | `/equipments/detail/{equipId}` | 已登录用户，服务层裁剪 | `api/equipment.js::getEquipmentDetail` |
| 领用 | POST | `/claims/apply` | Role 0/2/3 | `api/claim.js::applyClaim` |
| 领用 | PUT | `/claims/{claimId}/cancel` | Role 0/2/3 | `api/claim.js::cancelClaim` |
| 领用 | PUT | `/claims/{claimId}/approve` | Role 2/3 | `api/claim.js::approveClaim` |
| 领用 | POST | `/claims/return` | Role 0/2/3 | `api/claim.js::returnEquipment` |
| 领用 | GET | `/claims` | Role 0/2/3 | `api/claim.js::getClaims` |
| 检修 | GET | `/maintenanceRecords` | 已登录用户 | `api/MaintenanceRecord.js` |
| 检修 | POST | `/maintenanceRecords/{equipId}` | 已登录用户，服务层校验 | `api/MaintenanceRecord.js` |
| 检修 | DELETE | `/maintenanceRecords/{maintId}` | Role 2 | `api/MaintenanceRecord.js` |
| 检修 | PUT | `/maintenanceRecords/{maintId}` | Role 1/2 | `api/MaintenanceRecord.js` |
| 检修 | PUT | `/maintenanceRecords/assign/{maintId}` | Role 2 | `api/MaintenanceRecord.js` |
| 检修 | PUT | `/maintenanceRecords/complete/{maintId}` | Role 1/2 | `api/MaintenanceRecord.js` |
| 检修 | PUT | `/maintenanceRecords/review/{maintId}` | Role 2 | `api/MaintenanceRecord.js` |
| 调拨 | GET | `/transferRecords` | Role 2/3 | `api/transfer.js::getTransferRecords` |
| 调拨 | GET | `/transferRecords/{transferId}` | Role 2/3 | `api/transfer.js::getTransferById` |
| 调拨 | POST | `/transferRecords/{equipId}` | Role 2/3 | `api/transfer.js::addTransfer` |
| 调拨 | PUT | `/transferRecords/{transferId}` | Role 2/3 | `api/transfer.js::updateTransfer` |
| 调拨 | DELETE | `/transferRecords/{transferId}` | Role 2/3 | `api/transfer.js::deleteTransfer` |
| 报废 | GET | `/scrapRecords` | Role 2/3 | `api/ScrapRecord.js::getScrapList` |
| 报废 | POST | `/scrapRecords/{equipId}` | Role 2/3 | 报废页面 |
| 报废 | PUT | `/scrapRecords/{scrapNo}` | Role 2/3 | `api/ScrapRecord.js::updateScrapRecord` |
| 报废 | DELETE | `/scrapRecords/{scrapNo}` | Role 2/3 | `api/ScrapRecord.js::deleteScrapRecord` |
| 分类 | GET | `/categories` | 已登录用户 | `api/category.js::getCategories` |
| 分类 | GET | `/categories/{categoryId}` | 已登录用户 | `api/category.js::getCategoryById` |
| 分类 | POST | `/categories` | Role 2 | `api/category.js::addCategory` |
| 分类 | PUT | `/categories/{categoryId}` | Role 2 | `api/category.js::updateCategory` |
| 分类 | DELETE | `/categories/{categoryId}` | Role 2 | `api/category.js::deleteCategory` |
| 单位 | GET | `/departments` | 登录拦截器放行 GET，用于注册页加载单位 | `api/department.js::getDepts` |
| 单位 | GET | `/departments/{unitCode}` | 已登录用户 | 单位详情读取 |
| 单位 | POST | `/departments` | Role 2 | `api/department.js::addDept` |
| 单位 | PUT | `/departments/{unitCode}` | Role 2 | `api/department.js::updateDept` |
| 单位 | DELETE | `/departments/{unitCode}` | Role 2 | `api/department.js::deleteDept` |
| 看板 | GET | `/dashboard/summary` | 已登录用户，按角色返回不同视角 | `api/dashboard.js::getDashboardSummary` |
| 治理 | GET | `/governance/summary` | Role 2/3 | `api/governance.js::getGovernanceSummary` |
| 治理 | GET | `/governance/equipment-risks` | Role 2/3 | `api/governance.js::getEquipmentRisks` |
| 消息 | GET | `/messages` | 已登录用户，按当前用户过滤 | `api/message.js::getMessages` |
| 消息 | GET | `/messages/unread-count` | 已登录用户 | `api/message.js::getUnreadCount` |
| 消息 | PUT | `/messages/{id}/read` | 已登录用户 | `api/message.js::readMessage` |
| 消息 | PUT | `/messages/read-all` | 已登录用户 | `api/message.js::readAllMessages` |
| 审计 | GET | `/system/log/list` | Role 3 | `api/audit.js::getAuditLogs` |
| 备份 | POST | `/system/db/backup` | Role 3 | `api/database.js::backupDB` |
| 备份 | POST | `/system/db/restore` | Role 3 | `api/database.js::restoreDB` |
| 备份 | GET | `/system/db/files` | Role 3 | `api/database.js::getBackupFiles` |
| 备份 | GET | `/system/db/config` | Role 3 | `api/database.js::getBackupConfig` |
| AI | POST | `/ai/reports/operations/draft` | Role 2/3，服务层裁剪上下文 | `api/aiAssistant.js::draftOperationReport` |
| AI | POST | `/ai/equipment/{equipId}/summary` | Role 2/3，服务层裁剪上下文 | `api/aiAssistant.js::getEquipmentAiSummary` |

### 3.1 实现口径
- 所有非白名单业务接口都应携带 Header `token`，响应使用统一 `Result` 包装。
- Role 3 在系统管理和审计上拥有最高权限，但业务写操作仍应以当前 Controller 和 Service 规则为准，不应在报告中泛化为“所有业务都可写”。
- `GET /departments` 被后端登录拦截器放行，是为了注册页加载单位下拉；其他单位写接口仍需 Role 2。

---

## 4. 基础 CRUD

> 本节补齐原小项目已有、但未完整进入 git 提交说明和接口文档的基础 CRUD 契约。当前实现以 `EquipmentController`、`CategoryController`、`DepartmentController` 与前端 `api/equipment.js`、`api/category.js`、`api/department.js` 为准。

### 4.1 设备列表

*   **请求路径**：`GET /equipments`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `equipName`：设备名称，模糊匹配，可选
    *   `unitCode`：单位代码，可选；Role 2 会按当前单位裁剪
    *   `categoryId`：分类编码，可选
    *   `status`：设备状态，可选，当前取值为“在用 / 维修 / 报废”
    *   `begin` / `end`：购入日期范围，可选
    *   `custodian`：当前保管人用户名，可选
    *   `page` / `pageSize`：分页参数，可选
*   **权限说明**：已登录用户可访问，后端服务层按角色、单位和保管关系裁剪。
*   **响应示例**：
    ```json
    {
      "code": 1,
      "msg": "success",
      "data": {
        "total": 1,
        "rows": [
          {
            "equipId": "E001",
            "equipName": "笔记本电脑",
            "model": "ThinkPad T14",
            "status": "在用",
            "purchaseDate": "2026-06-01",
            "originalValue": 6999.00,
            "unitCode": "D001",
            "categoryId": "C001",
            "custodian": null
          }
        ]
      }
    }
    ```

### 4.2 设备详情

*   **请求路径**：`GET /equipments/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `equipId`：设备编号
*   **权限说明**：已登录用户可访问；业务展示和操作仍按角色裁剪。

### 4.3 新增设备

*   **请求路径**：`POST /equipments`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **请求体示例**：
    ```json
    {
      "equipId": "E001",
      "equipName": "笔记本电脑",
      "model": "ThinkPad T14",
      "status": "在用",
      "purchaseDate": "2026-06-01",
      "originalValue": 6999.00,
      "unitCode": "D001",
      "categoryId": "C001",
      "custodian": null
    }
    ```
*   **业务规则**：
    *   设备编号不可重复。
    *   Role 2 新增设备时应绑定当前业务单位或可管理单位。
    *   当前代码不使用独立“闲置”状态；可领用设备用 `status = "在用"` 且 `custodian = null` 表示。

### 4.4 修改设备

*   **请求路径**：`PUT /equipments/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：
    *   已报废设备禁止继续执行普通修改。
    *   Role 2 不能越权修改其他单位设备。
    *   保管人从有到无会记录退还流水；保管人从无到有或变更保管人会记录直接分配流水。

### 4.5 删除设备

*   **请求路径**：`DELETE /equipments/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：
    *   目标设备必须存在。
    *   Role 2 不能越权删除其他单位设备。
    *   删除前应确保没有破坏性关联或业务状态冲突。

### 4.6 导出折旧列表

*   **请求路径**：`GET /equipments/export`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：与 `GET /equipments` 的筛选条件一致。
*   **响应说明**：返回带折旧数据的设备 VO 列表，供前端生成导出文件。

### 4.7 累计折旧

*   **请求路径**：`GET /equipments/calculateAccumulated/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **响应说明**：返回设备累计折旧、净值等计算结果。

### 4.8 分类列表

*   **请求路径**：`GET /categories`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：已登录用户可访问，用于设备表单和筛选下拉。
*   **响应示例**：
    ```json
    {
      "code": 1,
      "msg": "success",
      "data": [
        {
          "categoryId": "C001",
          "categoryName": "计算机设备",
          "usefulLife": 5,
          "residualRate": 0.05
        }
      ]
    }
    ```

### 4.9 分类详情

*   **请求路径**：`GET /categories/{categoryId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `categoryId`：分类编码

### 4.10 新增分类

*   **请求路径**：`POST /categories`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **请求体示例**：
    ```json
    {
      "categoryId": "C001",
      "categoryName": "计算机设备",
      "usefulLife": 5,
      "residualRate": 0.05
    }
    ```

### 4.11 修改分类

*   **请求路径**：`PUT /categories/{categoryId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：分类编码来自路径参数，修改内容来自请求体。

### 4.12 删除分类

*   **请求路径**：`DELETE /categories/{categoryId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：若该分类下仍有关联设备，后端返回业务错误并阻断删除。

### 4.13 单位列表

*   **请求路径**：`GET /departments`
*   **请求头**：
    *   登录后业务页面携带 `token`
    *   注册页加载单位下拉时，后端拦截器允许该 GET 请求不携带 token
*   **响应示例**：
    ```json
    {
      "code": 1,
      "msg": "success",
      "data": [
        {
          "unitCode": "D001",
          "unitName": "人工智能学院",
          "manager": "王老师"
        }
      ]
    }
    ```

### 4.14 单位详情

*   **请求路径**：`GET /departments/{unitCode}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **路径参数**：
    *   `unitCode`：单位代码

### 4.15 新增单位

*   **请求路径**：`POST /departments`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **请求体示例**：
    ```json
    {
      "unitCode": "D001",
      "unitName": "人工智能学院",
      "manager": "王老师"
    }
    ```

### 4.16 修改单位

*   **请求路径**：`PUT /departments/{unitCode}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。

### 4.17 删除单位

*   **请求路径**：`DELETE /departments/{unitCode}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：若该单位下仍有关联设备，后端返回业务错误并阻断删除。

---

## 5. 其他接口

> 本节补齐接口索引中尚未展开的维保、调拨、报废、消息、备份恢复及辅助接口。接口路径以当前 Controller 为准，前端调用以 `equipment-web/src/api/` 为准。

### 5.1 维修工程师列表

*   **请求路径**：`GET /users/maintainers`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：已登录用户可访问，主要用于资产管理员指派检修工单时加载维修工程师候选项。
*   **响应说明**：返回 Role 1 维修工程师用户列表，字段以 `UserVO` 或脱敏用户对象为准，不返回密码哈希。

### 5.2 台账快捷报修

*   **请求路径**：`POST /equipments/maint/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **路径参数**：
    *   `equipId`：设备编号
*   **请求体示例**：
    ```json
    {
      "faultDescription": "设备无法正常启动",
      "maintDate": "2026-06-14"
    }
    ```
*   **权限说明**：已登录用户可调用，服务层校验设备状态、保管关系和单位边界。
*   **业务规则**：
    *   已报废设备禁止报修。
    *   维修中设备禁止重复报修。
    *   Role 0 只能报修本人保管设备；Role 2 只能报修本单位设备。

### 5.3 台账快捷报废

*   **请求路径**：`POST /equipments/scrap/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **请求体示例**：
    ```json
    {
      "scrapNo": "S20260614001",
      "scrapDate": "2026-06-14",
      "approver": "manager1",
      "reason": "维修成本过高，建议报废"
    }
    ```
*   **业务规则**：报废成功后设备状态变更为“报废”，并记录报废流水。

### 5.4 台账快捷调拨

*   **请求路径**：`POST /equipments/transfer/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2 资产管理员。
*   **请求体示例**：
    ```json
    {
      "outUnitCode": "D001",
      "inUnitCode": "D002",
      "transferDate": "2026-06-14",
      "changeType": "部门调拨",
      "operator": "manager1",
      "reason": "项目组设备调整"
    }
    ```
*   **业务规则**：调拨成功后同步更新设备当前单位，并写入调拨流水。

### 5.5 新增检修记录

*   **请求路径**：`POST /maintenanceRecords/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：已登录用户可调用，服务层校验保管关系、单位边界和设备状态。
*   **说明**：该接口与 `/equipments/maint/{equipId}` 都可创建检修报修记录，页面可按入口选择调用。

### 5.6 删除检修记录

*   **请求路径**：`DELETE /maintenanceRecords/{maintId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2 资产管理员。
*   **业务规则**：仅允许删除符合当前业务状态约束的检修记录；若工单已进入不可删除状态，后端应返回业务错误。

### 5.7 修改检修记录

*   **请求路径**：`PUT /maintenanceRecords/{maintId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 1/2。
*   **说明**：用于兼容旧检修编辑入口；新闭环流程中的指派、完工和复核优先使用专用接口。

### 5.8 调拨记录列表

*   **请求路径**：`GET /transferRecords`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2/3。
*   **响应说明**：返回设备调拨流水列表，包括调拨单号、设备编号、原单位、新单位、调拨日期、变动类型、经办人和原因。

### 5.9 调拨记录详情

*   **请求路径**：`GET /transferRecords/{transferId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2/3。

### 5.10 新增调拨记录

*   **请求路径**：`POST /transferRecords/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2/3。
*   **说明**：记录设备从原单位到新单位的流转，并同步设备当前单位。

### 5.11 修改调拨记录

*   **请求路径**：`PUT /transferRecords/{transferId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2/3。

### 5.12 删除调拨记录

*   **请求路径**：`DELETE /transferRecords/{transferId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2/3。

### 5.13 报废记录列表

*   **请求路径**：`GET /scrapRecords`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2/3。
*   **响应说明**：返回报废设备编号、报废单号、报废日期、审批人和报废原因。

### 5.14 新增报废记录

*   **请求路径**：`POST /scrapRecords/{equipId}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2/3。
*   **说明**：记录设备报废信息，并同步设备状态为“报废”。

### 5.15 修改报废记录

*   **请求路径**：`PUT /scrapRecords/{scrapNo}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
    *   `Content-Type: application/json`
*   **权限说明**：Role 2/3。

### 5.16 删除报废记录

*   **请求路径**：`DELETE /scrapRecords/{scrapNo}`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 2/3。
*   **请求参数**：
    *   `equipId`：前端删除时会携带关联设备编号，用于同步回滚或定位业务对象。

### 5.17 消息列表

*   **请求路径**：`GET /messages`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `status`：读取状态，可选，0-未读，1-已读
    *   `page` / `pageSize`：分页参数
*   **权限说明**：已登录用户；后端按当前用户名过滤 `target_user`。
*   **响应说明**：返回消息标题、内容、事件类型、读取状态、有效状态、关联业务类型和关联业务 ID。

### 5.18 未读消息数

*   **请求路径**：`GET /messages/unread-count`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：已登录用户；只统计当前用户未读消息。

### 5.19 单条消息已读

*   **请求路径**：`PUT /messages/{id}/read`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：已登录用户；只能操作当前用户自己的消息。

### 5.20 全部消息已读

*   **请求路径**：`PUT /messages/read-all`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：已登录用户；只处理当前用户消息。

### 5.21 数据库备份

*   **请求路径**：`POST /system/db/backup`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 3 系统管理员。
*   **说明**：后端调用数据库备份工具生成 SQL 备份文件，文件名通常包含时间戳。

### 5.22 数据库恢复

*   **请求路径**：`POST /system/db/restore`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **请求参数**：
    *   `fileName`：备份文件名
*   **权限说明**：Role 3 系统管理员。
*   **风险说明**：恢复数据库属于高危操作，前端必须进行确认提示，后端执行失败时返回明确错误。

### 5.23 备份文件列表

*   **请求路径**：`GET /system/db/files`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 3 系统管理员。
*   **响应说明**：返回备份文件名、文件大小、创建时间等展示信息。

### 5.24 备份配置

*   **请求路径**：`GET /system/db/config`
*   **请求头**：
    *   `token: <JWT_TOKEN_STRING>`
*   **权限说明**：Role 3 系统管理员。
*   **响应说明**：返回当前备份目录等脱敏配置，不返回数据库密码。
