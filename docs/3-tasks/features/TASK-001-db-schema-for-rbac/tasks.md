# TASK-001: Tasks

**Spec**: `spec.md`
**Status**: Complete

## Key decisions
- **极简单角色设计**：通过 `sys_user` 表中的 `role` 字段（`0-操作员`, `1-工程师`, `2-资产管理员`, `3-系统管理员`）管理权限，不引入多对多权限配置表，保证 `BasicDao` 交互的敏捷性。
- **逻辑层关联**：设备领用人 `custodian` 和维修报修人 `reporter` 在数据库层面仅作为逻辑关联字段，不设物理外键，避免复杂的级联删除操作。
- **密码加密规范**：初始测试账号的密码在写入数据库时均采用 `123456` 的 MD5 加密值。

## Progress

- [x] T1 — 修改并执行 SQL 变更脚本（修改 `user_schema.sql` 并在本地导入） · covers: AC-001, AC-002, AC-003
- [x] T2 — 创建 Java 用户实体类 `User.java` · covers: AC-001, AC-004
- [x] T3 — 修改 Java 设备实体类 `Equipment.java`（新增 `custodian` 属性） · covers: AC-002
- [x] T4 — 修改 Java 维修记录实体类 `MaintenanceRecord.java`（新增 `reporter`, `faultDescription`, `maintStatus` 属性） · covers: AC-003
- [x] T5 — 运行编译命令 `cd equipment_system_management && mvn clean compile` 验证代码无语法及依赖报错 · covers: AC-004
- [x] T6 — 运行测试套件 `cd equipment_system_management && mvn test` · covers: AC-004
- [x] T7 — 验证 ACs：更新 `spec.md` 中所有通过的 criteria 的 `passes` 为 `true`
- [x] T8 — 更新 `docs/3-tasks/CURRENT_PLAN.md` 标记本任务完成

## Dependencies
- T2, T3, T4 依赖 T1（数据库结构需先变更）
- T5, T6, T7, T8 依赖 T1-T4 的修改完成

## Blockers
<!-- 无阻碍物 -->
