# TASK-005: 后端权限控制与状态流转校验

**Status**: Draft
**Created**: 2026-06-10
**Feature dir**: `docs/3-tasks/features/TASK-005-backend-rbac-workflow/`

## Objective
实现基于角色的权限控制（RBAC）与设备状态流转的后端安全校验，对核心写接口进行准入阻断，完成数据库物理层（部门归属、指派外键）的加固，并实现业务链条中报废拦截、调拨保管人自动清空和删除关联检查，确保系统强一致性与高安全性。

## Scope

### In scope

#### 【P0 级核心安全与隔离】
1. **统一接口鉴权**：引入 `@RequiresRoles` 及 AOP 切面 `SecurityAspect`。凡越权请求均抛出 `ForbiddenException`，由全局异常处理器拦截，返回 HTTP 403 状态码及 `Result.error("权限不足")`。
2. **设备及主数据写保护**：设备增删改、调拨、报废，以及单位/分类的增删改，仅限角色 `2` (资产管理员) 可写。数据库备份/恢复仅限角色 `3` (系统管理员) 操作。
3. **数据隔离过滤**：
   - 设备查询：操作员（`role=0`）仅能查询到保管人 `custodian` 为自己，**或保管人为空（NULL）且设备当前所在单位与当前用户单位一致**（部门公共资产）的设备。
   - 检修查询：操作员（`role=0`）仅能看到自己为报修人 `reporter` 报修的工单；维修工（`role=1`）可全局只读。
4. **生命周期拦截（防状态穿透）**：禁止对任何已处于 `报废` 状态的设备执行编辑、调拨、报修等二次变更。

#### 【P1 级流转一致性加固】
5. **用户所属部门关联 (sys_user.unit_code)**：数据库层面在 `sys_user` 新增 `unit_code` 字段关联 `department(unit_code)`，明确员工所属部门，为部门级数据隔离奠定物理基础。
6. **调拨保管人联动清退**：在设备调拨成功变更 `unit_code` 后，后端自动更新 `custodian = NULL`，完成部门级清退，防跨单位保管人越权。
7. **精准指派工单校验 (maint_person_id)**：
   - 数据库层面在 `maintenance_record` 表中新增 `maint_person_id` 指向 `sys_user.id`。
   - 指派维修工时，后端强校验该 ID 用户必须在系统中存在，且其角色 `role` 必须为 `1` (维修工)。
8. **主数据删除级联安全拦截**：删除单位或分类前，强校验该单位/分类下是否挂载了设备，若有则友好拦截，拒绝删除。

#### 【P2 级运维防死锁】
9. **删除用户关联检查**：删除用户时，校验其名下是否挂载了保管设备或未完结工单，若有则拦截，拒绝删除以防出现僵尸资产。
10. **灾备还原安全性处理**：在数据库一键还原操作时，进行会话安全防御，防止管理员还原数据库后导致自身登录凭证直接失效。

### Out of scope
- 前端界面的菜单隐藏、操作隔离以及路由拦截修改（这将在后续的前端独立任务 TASK-006 中完成）。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "security",
    "description": "【P0】非管理员用户尝试备份或还原数据库时应当被拦截并返回 403 权限不足",
    "steps": [
      "使用角色为 0, 1 或 2 的用户登录系统获取 Token",
      "使用该 Token 请求 POST /system/db/backup 接口",
      "Verify: 接口返回 HTTP 状态码 403，返回 JSON 内容为 {\"code\":0,\"msg\":\"权限不足\",\"data\":null}"
    ],
    "passes": false
  },
  {
    "id": "AC-002",
    "category": "security",
    "description": "【P0】非资产管理员角色尝试添加设备、修改设备或删除设备时应当被拦截并返回 403 权限不足",
    "steps": [
      "使用角色为 0, 1 或 3 的用户登录系统获取 Token",
      "使用该 Token 请求 POST /equipments 接口（添加设备）或 DELETE /equipments/E001（删除设备）",
      "Verify: 接口返回 HTTP 状态码 403，且内容为权限不足"
    ],
    "passes": false
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "【P0】禁止对已报废状态的设备执行编辑、调拨、报修等后续流转操作",
    "steps": [
      "查询到设备编号 E003 的状态已经是 '报废'",
      "使用资产管理员账号（role=2）登录系统，发送请求调拨该设备 POST /equipments/transfer/E003",
      "Verify: 接口请求被拦截，返回 200 或业务错误 400，msg 提示 '该设备已报废，禁止进行此操作'"
    ],
    "passes": false
  },
  {
    "id": "AC-004",
    "category": "functional",
    "description": "【P0】操作员查询设备台账时仅能看到自己保管的或同单位未分配的设备，实现数据隔离",
    "steps": [
      "操作员 zhangsan（所属单位 D001）登录系统，发送请求 GET /equipments 检索列表",
      "Verify: 返回的设备数据中，所有设备保管人 custodian 均为 zhangsan，或保管人为 NULL 且 unit_code 均为 D001，绝无 custodian=lisi 且部门非 D001 的设备"
    ],
    "passes": false
  },
  {
    "id": "AC-005",
    "category": "functional",
    "description": "【P1】设备被调拨到新部门后，原保管人字段应当被自动置为空",
    "steps": [
      "查询到设备 E001 当前属于部门 D001，保管人是 zhangsan",
      "资产管理员（role=2）将 E001 调拨到新部门 D002",
      "Verify: 调拨成功后，查询 E001 的详情，发现 unit_code 变更为 D002，但 custodian 字段自动变成了 NULL"
    ],
    "passes": false
  },
  {
    "id": "AC-006",
    "category": "functional",
    "description": "【P1】检修单指派的检修人必须为合法的维修工角色，且存储物理 ID 关联",
    "steps": [
      "资产管理员登录系统，对待指派工单 1001 发起指派",
      "传入指派参数 maint_person_id 为 10 (对应用户 role=0 操作员)",
      "Verify: 接口报错，提示 '所指派的员工并非维修工，指派失败'",
      "传入指派参数 maint_person_id 为 2 (对应用户 role=1 维修工)",
      "Verify: 指派成功，数据库表中 1001 记录的 maint_person_id 正确存为 2，且 maint_status 流转为 1 (维修中)"
    ],
    "passes": false
  },
  {
    "id": "AC-007",
    "category": "functional",
    "description": "【P1】删除有挂载设备的分类或单位时，应当友好拦截防 SQL 数据库崩盘",
    "steps": [
      "确认单位 D001 下当前拥有 5 台设备",
      "资产管理员请求删除 D001 接口：DELETE /departments/D001",
      "Verify: 接口成功拦截，返回业务友好提示 '该部门下仍有设备，无法删除'"
    ],
    "passes": false
  },
  {
    "id": "AC-008",
    "category": "functional",
    "description": "【P2】删除用户时，若用户名下依然挂有保管设备，应当友好拦截以防产生僵尸资产",
    "steps": [
      "确认操作员 zhangsan 当前是设备 E002 的保管人",
      "系统管理员请求删除 zhangsan 接口：DELETE /users/zhangsan",
      "Verify: 接口拦截，返回业务提示 '该员工名下仍有保管资产，无法删除该账户'"
    ],
    "passes": false
  }
]
```

## Notes
- 所有的越权与校验问题均由拦截切面和 Service 层的业务代码保护，返回状态码 403 或友好业务 Result。
