# TASK-012: Tasks

**Spec**: `spec.md`
**Status**: Backlog

## Key decisions
- 本任务作为 TASK-011 之后的待办任务，不覆盖当前 Active feature。
- 操作审计采用追加式 `operation_log` 表，不支持编辑或删除审计记录。
- 设备生命周期详情页以设备为主线聚合现有业务记录和新增审计记录。
- Role 3 保持审计只读，Role 2 保持业务管理视角，Role 0/1 严格遵守个人数据边界。

## Progress

- [ ] T1 — 更新 `docs/2-designs/db_schema.md` 并新增迁移脚本，定义 `operation_log` 表结构、索引和字段含义 · covers: AC-001, AC-004, AC-006
- [ ] T2 — 更新 `docs/2-designs/api_contract.md`，补充设备详情接口和审计日志查询接口契约 · covers: AC-002, AC-003, AC-005
- [ ] T3 — 同步长期 Harness 文档：更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md` 和 `docs/2-designs/role_positioning.md`，描述审计与生命周期详情在业务流程、架构、UI 和角色权限中的定位 · covers: doc-maintenance, AC-001, AC-002, AC-003, AC-004, AC-005
- [ ] T4 — 新增审计日志 POJO/DTO/VO，定义操作类型、对象类型、操作者信息、摘要、结果状态和时间字段 · covers: AC-001, AC-004, AC-005
- [ ] T5 — 新增 `OperationLogDao` 和审计写入能力，确保关键操作写入参数化 SQL 且不直接暴露数据库实体 · covers: AC-001, AC-004, AC-006
- [ ] T6 — 在设备、领用、维修、调拨、报废、备份恢复等关键 Service 流程接入审计记录 · covers: AC-001, AC-006
- [ ] T7 — 新增设备详情聚合查询能力，汇总设备基础信息、折旧、领用、维修、调拨、报废和审计时间线 · covers: AC-002, AC-003, AC-005
- [ ] T8 — 新增 Controller 接口，提供设备详情和审计日志查询能力，并保持统一 `Result` 响应 · covers: AC-002, AC-003, AC-005
- [ ] T9 — 新增前端设备详情页和详情入口，展示生命周期时间线、历史记录分区和审计记录 · covers: AC-002, AC-003
- [ ] T10 — 新增系统管理员审计查询入口，只读展示关键操作流水和筛选条件 · covers: AC-001, AC-003, AC-005
- [ ] T11 — 补充后端测试，覆盖审计写入、权限隔离、设备详情聚合和审计异常路径 · covers: AC-001, AC-002, AC-003, AC-004, AC-005, AC-006
- [ ] T12 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [ ] T13 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [ ] T14 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [ ] T15 — Update `docs/4-tasks/CURRENT_PLAN.md` — move this task from Backlog to Done when implementation is finished

## Dependencies
- T4 requires T1 and T2
- T5 requires T4
- T6 requires T5
- T7 requires T1, T2, and T4
- T8 requires T7
- T9 requires T2 and T8
- T10 requires T2, T5, and T8
- T11-T14 require implementation tasks to be complete
- T15 requires all implementation and verification gates to be complete

## Blockers
<!-- Fill in if something is preventing progress -->
