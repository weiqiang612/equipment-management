# TASK-015: Tasks

**Spec**: `spec.md`
**Status**: Complete

## Key decisions
- 事件判定完全基于确定性规则，不依赖 AI 输出。
- 第一版消息中心采用拉取式刷新，不做 WebSocket 或第三方消息通道。
- 事件中心只负责“发现问题并找责任人”，解释和建议增强留给后续 AI 任务。

## Progress

- [x] T1 — 更新 `docs/1-requirements/project_overview.md` 和 `docs/1-requirements/requirements_analysis.md`，补充事找人 MVP、事件类型与责任人路由规则 · covers: doc-maintenance, AC-001, AC-002, AC-003, AC-005
- [x] T2 — 更新 `docs/2-designs/architecture.md`，说明事件来源、责任人路由、消息中心拉取式刷新与闭环边界 · covers: doc-maintenance, AC-001, AC-003, AC-004, AC-005
- [x] T3 — 更新 `docs/2-designs/ui_prototype.md`，补充消息中心入口、未读角标、事件列表与跳转交互 · covers: doc-maintenance, AC-002, AC-003, AC-004
- [x] T4 — 设计事件领域对象与事件去重/失效规则，明确高风险设备、审批积压、维修超时三类事件模型 · covers: AC-001, AC-004, AC-005
- [x] T5 — 实现事件生成与责任人路由核心逻辑，复用治理结果、领用审批数据与检修超时规则生成目标事件 · covers: AC-001, AC-003, AC-005
- [x] T6 — 实现消息中心查询、未读统计、已读处理与事件关闭/失效更新逻辑 · covers: AC-002, AC-003, AC-005
- [x] T7 — 集成消息中心前端页面、角标和目标页面跳转，复用治理页、审批页、检修页和设备详情页入口 · covers: AC-002, AC-004
- [x] T8 — 补充后端与前端测试，覆盖事件路由、跨单位隔离、未读状态变化和失效事件去重 · covers: AC-001, AC-002, AC-003, AC-004, AC-005
- [x] T9 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [x] T10 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [x] T11 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [x] T12 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete

## Dependencies
- T4 requires T1-T3 to align business, architecture and UI boundaries.
- T5 requires T4.
- T6 requires T5.
- T7 requires T5 and T6.
- T8-T12 require implementation tasks to be complete.

## Blockers
<!-- Fill in if something is preventing progress -->
