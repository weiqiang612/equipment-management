# TASK-017: Tasks

**Spec**: `spec.md`
**Status**: Complete

## Key decisions
- 本任务是前端定稿前的 UI/UX 收尾，不改变后端接口、数据库结构或权限模型。
- 优先统一信息层级和交互可达性，不重做主题视觉或导航骨架。
- 关键表格页的优化以“首屏可操作性”为核心：自动定位、摘要筛选、固定操作列。

## Progress

- [x] T1 — 更新 `docs/2-designs/ui_prototype.md`，补充统一页头结构、导航分组、状态标签语义、空状态和关键表格页首屏可操作性规范 · covers: doc-maintenance, AC-001, AC-002, AC-003, AC-004, AC-005
- [x] T2 — 收口全局框架页 `equipment-web/src/App.vue`，统一左侧导航分组、命名、消息入口和顶栏信息层级 · covers: AC-001, AC-005
- [x] T3 — 收口消息中心 `equipment-web/src/views/MessageCenter.vue`，统一页头、未读筛选、空状态和消息跳转后的可理解性 · covers: AC-001, AC-004, AC-005
- [x] T4 — 优化检修页 `equipment-web/src/views/MaintenanceRecord.vue`，实现待办摘要条、目标工单自动定位、高亮和固定右侧操作列 · covers: AC-002, AC-003, AC-004
- [x] T5 — 检查并统一至少一个其他关键表格页的首屏可操作性与状态标签表达，优先处理与消息/待办联动最紧密的页面 · covers: AC-001, AC-002, AC-005
- [x] T6 — 统一关键页面的空状态、提示文案和状态标签颜色映射，消除同义不同色或同色不同义 · covers: AC-004, AC-005
- [x] T7 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [x] T8 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [x] T9 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete

## Dependencies
- T2-T6 require T1 design规范先补齐。
- T4 requires全局导航和消息入口基础交互已稳定。
- T7-T9 require implementation tasks to be complete.

## Blockers
<!-- Fill in if something is preventing progress -->
