# TASK-009: 用户角色与所属单位管理修复

**Status**: Ready
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-009-user-role-dept-fix/`

## Objective
修复用户角色与所属单位的管理漏洞，支持在自助注册和后台修改时强制绑定和变更用户的所属单位，并对单位调动时的保管资产进行安全校验，保障系统内设备数据隔离与领用审批流程的正常运转。

## Scope

### In Scope (包含范围)
- 修改 API 契约中的 `POST /users/register` 接口，支持并在前端注册页面强制收集 `unitCode`。
- 扩展 API 契约中的 `PUT /users/role` 接口，使其支持同时修改用户的系统角色 (`role`) 和所属单位 (`unitCode`)。
- 完善后端的业务校验逻辑：
  - 非全局角色（0-设备操作员、1-维修工程师、2-资产管理员）在修改或注册时必须绑定有效的 `unitCode`（所属单位），而系统管理员（3-系统管理员）可以为 NULL（代表全局角色）。
  - 用户所属单位发生变更时，若名下仍有未清退归库的保管设备，后端执行强阻断校验，拒绝修改并抛出业务异常。
- 前端 `UserRegister.vue` 注册表单新增“所属部门”必填下拉项。
- 前端 `UserManage.vue` 后台用户列表展示用户的“所属单位”，并支持管理员下拉调整用户的所属单位（根据角色规则启用或禁用）。

### Out of Scope (不包含范围)
- 重构多对多的复杂 RBAC 权限设计（维持单角色极简设计不变）。
- 部门（单位）本身的新增、修改和删除基础管理功能。

## Acceptance criteria (验收标准)

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "自助注册用户时强制要求选择所属单位",
    "steps": [
      "1. 访问系统注册页面 /register",
      "2. 填写有效的用户名和密码，但不选择所属部门，点击注册",
      "3. Verify: 页面提示 '请选择所属部门' 并阻止表单提交",
      "4. 选择部门为 '研发部 (D001)' 并提交注册",
      "5. Verify: 注册成功，通过数据库确认该新建用户的 role 默认为 0 且 unit_code 字段正确存储为 'D001'"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "系统管理员在后台可以修改用户的所属单位和角色",
    "steps": [
      "1. 以系统管理员身份登录，进入 '用户权限管理' 页面",
      "2. 在列表中选择某一操作员，将其所属单位修改为 '测试部 (D002)' 并确认",
      "3. Verify: 页面提示 '修改成功' 且该用户的单位展示更新为测试部，数据库中该用户的 unit_code 变更为 'D002'",
      "4. 将该用户的系统角色修改为 '系统管理员'",
      "5. Verify: 该用户的所属单位选择器自动置空且变为禁用状态，数据库中用户的 role 更新为 3 且 unit_code 变更为 NULL"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "用户部门变更时名下有保管设备则执行强阻断",
    "steps": [
      "1. 准备测试数据：操作员 user_a 所属部门为 D001，名下正保管有至少一台设备 (如 E001，设备所属部门为 D001)",
      "2. 以系统管理员身份登录，进入用户权限管理页",
      "3. 尝试将 user_a 的所属部门修改为 D002 并点击确定",
      "4. Verify: 页面弹出报错提示 '该用户尚有未清退的保管设备，请先去设备管理处退还或交接设备！' 且部门修改请求被拒绝，数据库中用户的 unit_code 仍为 D001"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "API 契约和数据库设计规范文档同步更新",
    "steps": [
      "1. 核对 docs/2-designs/api_contract.md 中 /users/register 和 /users/role 的最新契约定义",
      "2. 核对 docs/2-designs/db_schema.md 中 sys_user 表结构，确保包含了 unit_code 字段",
      "3. Verify: 文档中定义的请求与响应结构与前后端实际代码及 SQL 形式一致"
    ],
    "passes": true
  }
]
```

## Notes
- 修改用户角色与单位接口在后端复用 `PUT /users/role` 路由，请求体接收 `User` 实体，字段包含 `id`、`role`、`unitCode`。
- 校验用户保管设备时，使用 `SELECT COUNT(*) FROM equipment WHERE custodian = ?` 语句，其中参数为用户的 `username`。
