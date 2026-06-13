# TASK-014: Tasks

**Spec**: `spec.md`
**Status**: In Progress

## Key decisions
- 维修闭环继续复用现有检修、报废、审计和生命周期聚合能力，不引入新子系统。
- 完工登记与最终处置拆分为两个业务节点：维修工程师只负责完工登记，资产管理员负责最终复核与处置结论。
- “转报废”复用现有报废能力和设备生命周期聚合链路，避免维护两套终止处置逻辑。

## Progress

- [ ] T1 — 更新 `docs/1-requirements/project_overview.md` 和 `docs/1-requirements/requirements_analysis.md`，补充检修闭环、复核结论与转报废业务规则 · covers: doc-maintenance, AC-001, AC-002, AC-003, AC-005
- [ ] T2 — 更新 `docs/2-designs/architecture.md`，说明检修闭环状态机、复核分流与报废复用链路 · covers: doc-maintenance, AC-001, AC-002, AC-003
- [ ] T3 — 更新 `docs/2-designs/api_contract.md`，补充检修指派、完工登记、复核恢复可用和复核转报废接口契约 · covers: AC-001, AC-002, AC-003, AC-004, AC-005
- [ ] T4 — 更新 `docs/2-designs/db_schema.md` 并草拟升级 SQL，扩展 `maintenance_record` 状态与复核字段设计 · covers: AC-001, AC-002, AC-004, AC-005
- [ ] T5 — 更新 `docs/2-designs/ui_prototype.md`，补充检修记录页的待复核状态、复核动作与转报废交互 · covers: doc-maintenance, AC-001, AC-002, AC-003
- [ ] T6 — 调整检修与报废相关 POJO / DTO / VO，表达待复核、复核意见、最终处置结论和生命周期展示字段 · covers: AC-001, AC-002, AC-004
- [ ] T7 — 改造 `MaintenanceRecordService` / DAO 状态机，实现“待指派 -> 维修中 -> 待复核 -> 已复核可用/转报废”流转与非法跳转阻断 · covers: AC-001, AC-003, AC-005
- [ ] T8 — 打通复核转报废与审计留痕、生命周期聚合链路，确保设备状态、报废记录和审计日志一致 · covers: AC-002, AC-004, AC-005
- [ ] T9 — 调整检修控制器接口，明确指派、完工登记、复核恢复可用和复核转报废的请求入口与权限控制 · covers: AC-001, AC-002, AC-003, AC-004
- [ ] T10 — 更新前端 `MaintenanceRecord.vue`、相关 API 与交互弹窗，展示新状态并按角色开放指派、完工、复核和转报废动作 · covers: AC-001, AC-002, AC-003, AC-005
- [ ] T11 — 补充后端测试，覆盖正常闭环、转报废、跨单位越权、他人工单越权和重复复核阻断 · covers: AC-001, AC-002, AC-003, AC-004, AC-005
- [ ] T12 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [ ] T13 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [ ] T14 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [ ] T15 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete

## Dependencies
- T6 requires T1-T5 to define the target workflow, contract, schema and UI states.
- T7 and T8 require T4 and T6 to be complete.
- T9 requires T3, T7 and T8.
- T10 requires T3, T5 and T9.
- T11-T15 require implementation tasks to be complete.

## Blockers
<!-- Fill in if something is preventing progress -->
