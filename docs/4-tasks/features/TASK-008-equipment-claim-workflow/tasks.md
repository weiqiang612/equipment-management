# TASK-008: 设备领用与审批工作流任务清单

**Spec**: `spec.md`
**Status**: 100% Complete

## Key decisions
- 新增 `t_equipment_claim` 表来完整记录“领用申请、审批、撤销、退还、直接分配”的全生命周期保管人变更轨迹。
- 管理员直接分配设备、调拨设备清退、报废设备清退等业务均通过底层的逻辑切入点（Service 事务中）级联生成 `t_equipment_claim` 的审计流记录，保障流转历史 100% 完整。
- 操作员自助退还设备和撤销待审批申请无须管理员审核，以减轻管理负担，但退库历史必须自动留痕。

## Progress

- [x] T1 — 更新 API 契约：`docs/2-designs/api_contract.md` · covers: AC-CONTRACT
- [x] T2 — 更新数据库 Schema 设计并编写迁移脚本：更新 `docs/2-designs/db_schema.md` 并编写 `equipment_system_management/src/main/resources/update_claim_schema.sql` · covers: AC-CONTRACT
- [x] T3 — 创建后端领用审批实体及传输对象：`EquipmentClaim.java` 实体类 · covers: AC-CONTRACT
- [x] T4 — 创建数据持久层：`EquipmentClaimDao.java`，使用 `JdbcTemplate` 实现基本 CRUD SQL 语句 · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-007
- [x] T5 — 实现核心业务逻辑：`EquipmentClaimService.java` 及实现类（包含申请、审批、撤回、退还方法，并在调拨和报废模块中加入审计留痕级联调用） · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-007
- [x] T6 — 改造设备与用户管理 Service 逻辑：在修改设备保管人、调拨设备、报废设备、删除用户时，级联处理申请记录与流痕 · covers: AC-004, AC-005, AC-007
- [x] T7 — 创建 API 暴露层：`EquipmentClaimController.java`，添加安全校验、输入校验与异常拦截 · covers: AC-001, AC-002, AC-003, AC-004, AC-006
- [x] T8 — 编写后端业务流集成单元测试：`EquipmentClaimServiceTest.java` · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007
- [x] T9 — 编写前端 API 接口：`equipment-web/src/api/claim.js` · covers: AC-CONTRACT
- [x] T10 — 改造前端台账页面：修改 `Equipment.vue`，增加设备领用和退还功能按钮，以及管理员编辑中直接修改保管人字段 · covers: AC-001, AC-004, AC-005
- [x] T11 — 创建前端审批与申请历史页面：新建 `EquipmentClaim.vue` 并在 `router/index.js` 中添加菜单路由 · covers: AC-001, AC-002, AC-003
- [x] T12 — 运行后端 Maven 测试套件：`cd equipment_system_management && mvn test` — 确保 100% 编译并通过所有测试 · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007
- [x] T13 — 验证所有验收条件：在 spec.md 中将所有验证通过 of AC passes 字段标为 true · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007
- [x] T14 — 更新任务全局规划：更新 `docs/4-tasks/CURRENT_PLAN.md` 标记本任务完成

## Dependencies
- T2 依赖 T1
- T3, T4 依赖 T2
- T5 依赖 T4
- T6 依赖 T5
- T7 依赖 T5, T6
- T8 依赖 T7
- T9 依赖 T7
- T10, T11 依赖 T9
- T12, T13, T14 依赖 T1–T11
