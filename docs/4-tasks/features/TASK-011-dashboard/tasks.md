# TASK-011: Tasks

**Spec**: `spec.md`
**Status**: In Progress

## Key decisions
- 第一版看板作为单个完整 feature 实现，不拆分为多个 task。
- 不修改数据库结构，只基于现有表做聚合查询。
- 后端提供单一聚合入口 `GET /dashboard/summary`，前端按 `role` 渲染不同模块。
- Role 3 保持全局只读审计视角，不提供业务审批或派单操作入口。
- 第一版引入 `echarts`，不引入 GSAP，也不引入不必要的 Vue 图表封装库。
- 视觉方向为企业级资产运营驾驶舱，继承现有深蓝侧栏、白色卡片和冷灰内容区。

## Progress

- [ ] T1 — 同步长期 Harness 文档：更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md`、`docs/2-designs/role_positioning.md` 和 `docs/2-designs/api_contract.md`，明确数据看板定位、角色入口、UI 结构和 `GET /dashboard/summary` 契约 · covers: doc-maintenance, AC-001, AC-005, AC-007
- [ ] T2 — 新增后端看板 VO/DTO，定义指标卡、图表数据、待办项和角色化摘要响应对象 · covers: AC-001, AC-002, AC-003, AC-004, AC-005
- [ ] T3 — 新增 `DashboardDao` 聚合查询能力，使用现有表统计资产、分类、部门、维保、领用、用户角色和备份状态相关数据 · covers: AC-002, AC-003, AC-004, AC-006
- [ ] T4 — 新增 `DashboardService` / `DashboardServiceImpl`，根据 `BaseContext` 中的角色、用户名和单位代码组装角色化看板数据 · covers: AC-001, AC-002, AC-003, AC-004, AC-006
- [ ] T5 — 新增 `DashboardController`，实现 `GET /dashboard/summary`，保持统一 `Result` 响应并避免 Controller 承载业务逻辑 · covers: AC-001, AC-004, AC-005
- [ ] T6 — 在 `equipment-web` 引入 `echarts` 依赖，并确认不引入 GSAP 或额外 Vue 图表封装库 · covers: AC-007, AC-008
- [ ] T7 — 新增前端 `equipment-web/src/api/dashboard.js` 和 `equipment-web/src/views/Dashboard.vue`，实现角色化数据看板页面、KPI 卡片区、待办区和明细区 · covers: AC-001, AC-002, AC-003, AC-006, AC-007
- [ ] T8 — 封装 Dashboard 图表面板或局部图表方法，使用 ECharts 实现环形图、柱状图、折线图或柱线组合图，并在 Vue 2 生命周期中正确初始化、resize 和销毁 · covers: AC-002, AC-003, AC-007, AC-008
- [ ] T9 — 更新前端路由和菜单，将 `/` 重定向到 `/dashboard`，并为所有登录角色提供数据看板入口 · covers: AC-001
- [ ] T10 — 做视觉与交互细化：继承现有后台深蓝/白卡/冷灰风格，补充状态色、loading 状态、hover/focus 状态和移动端堆叠布局 · covers: AC-001, AC-007, AC-008
- [ ] T11 — 为后端看板聚合和权限隔离补充测试，覆盖 Role 0/1/2/3 的关键响应边界 · covers: AC-002, AC-003, AC-004, AC-005, AC-006
- [ ] T12 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [ ] T13 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [ ] T14 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [ ] T15 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete when implementation is finished

## Dependencies
- T2 requires T1
- T3 requires T2
- T4 requires T3
- T5 requires T4
- T6 can run after frontend dependency strategy is confirmed
- T7 requires T1 and T5 contract shape
- T8 requires T6 and T7
- T9 requires T7
- T10 requires T7 and T8
- T11-T14 require implementation tasks to be complete
- T15 requires all implementation and verification gates to be complete

## Blockers
<!-- Fill in if something is preventing progress -->
