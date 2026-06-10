# TASK-001: DB Schema for RBAC Roles

**Status**: Draft
**Created**: 2026-06-10
**Feature dir**: `docs/3-tasks/features/TASK-001-db-schema-for-rbac/`

## Objective
扩展数据库表结构并同步更新 Java POJO 实体类，为即将到来的四级角色鉴权及业务闭环提供底层数据支撑。

## Scope

### In scope
- 修改 `sys_user` 表结构，变更 `role` 字段定义，支持 0-设备操作员、1-维修工程师、2-资产管理员、3-系统管理员；插入 4 个岗位的初始测试账号并使用 MD5 密码加密。
- 修改 `equipment` 表，新增 `custodian` (保管人) 字段，不设物理外键。
- 修改 `maintenance_record` 表，新增 `reporter` (报修人)、`fault_description` (故障描述) 和 `maint_status` (工单状态) 字段，不设物理外键。
- 新增 `User.java` 实体类，并同步更新 `Equipment.java` 与 `MaintenanceRecord.java` 实体类。

### Out of scope
- 具体的登录、注册、修改角色、报修、维修等业务 Controller 接口编写。
- 引入多对多的权限角色关联配置表，保持简单的单角色设计。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "sys_user 结构升级与数据初始化，密码经过 MD5 加密",
    "steps": [
      "执行用户表变更和初始化脚本，确认 sys_user 表 role 字段能表达 0-3 角色的定义",
      "Verify: 数据库中成功插入 test_op(role=0), test_eng(role=1), test_mgr(role=2), test_admin(role=3) 四个账号，且密码均为 123456 的 MD5 密文 e10adc3949ba59abbe56e057f20f883e"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "equipment 表变更与 POJO 属性同步",
    "steps": [
      "执行设备表变更 SQL 脚本，在 equipment 表中成功新增 custodian 字段",
      "Verify: Equipment.java 实体类中成功增加 custodian 属性，且与数据库字段映射正确，项目无编译报错"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "maintenance_record 表变更与 POJO 属性同步",
    "steps": [
      "执行维修表变更 SQL 脚本，在 maintenance_record 表中成功新增 reporter, fault_description, maint_status 字段",
      "Verify: MaintenanceRecord.java 实体类中成功增加 reporter, faultDescription, maintStatus 属性，项目无编译报错"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "创建 Java User 实体类且项目顺利编译通过",
    "steps": [
      "在 pojo 包下创建 User.java 实体类，字段包括 id, username, password, realName, role, createTime, updateTime",
      "Verify: 运行 Maven 编译命令，项目能够顺利编译通过，且没有任何编译异常"
    ],
    "passes": true
  }
]
```

## Notes
- 采用单角色映射的极简 RBAC 设计（role=0,1,2,3），降低手写 SQL 和 BasicDao 的开发门槛，同时维持逻辑的严谨。
- 数据库关联仅在逻辑层维系，不设置物理外键，避免复杂的级联删除操作。
