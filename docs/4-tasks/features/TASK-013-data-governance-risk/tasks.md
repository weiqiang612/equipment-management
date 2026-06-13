# TASK-013: Tasks

**Spec**: `spec.md`
**Status**: Complete

## Key decisions
- 本任务是三期“数据治理与运营分析”，已在 TASK-012 之后完成并确认验收通过。
- 第一版不新增数据库结构，不新增依赖，不接 AI。
- 风险阈值使用后端命名常量，不做数据库配置化。
- Role 2 按单位边界查看治理数据，Role 3 全局只读，Role 0/1 禁止访问。

## Progress

- [x] T1 — 同步长期 Harness 文档：更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md` 和 `docs/2-designs/ui_prototype.md`，明确三期数据治理目标、规则边界、UI 入口和角色使用场景 · covers: doc-maintenance, AC-001, AC-003, AC-006
- [x] T2 — 更新 `docs/2-designs/api_contract.md`，补充 `GET /governance/summary` 和 `GET /governance/equipment-risks` 的请求头、参数、响应结构 and 权限说明 · covers: AC-002, AC-003, AC-004
- [x] T3 — 新增治理 VO/DTO 和风险等级常量，定义健康评分、风险原因、数据质量问题、异常成本和空闲设备摘要结构 · covers: AC-001, AC-002, AC-004, AC-005
- [x] T4 — 新增 `GovernanceDao` 聚合查询和分页查询，统计质量问题、风险分布、成本异常、空闲设备、部门/分类风险分布和风险设备清单 · covers: AC-001, AC-002, AC-005, AC-006
- [x] T5 — 新增 `GovernanceService` / `GovernanceServiceImpl`，实现风险评分、质量检查、异常降级和 Role 2/3 权限裁剪逻辑 · covers: AC-001, AC-002, AC-003, AC-005, AC-006
- [x] T6 — 新增 `GovernanceController`，实现 `/governance/summary` 和 `/governance/equipment-risks`，保持统一 `Result` 响应 · covers: AC-003, AC-004
- [x] T7 — 新增前端 `equipment-web/src/api/governance.js` 和 `Governance.vue`，展示质量总览、风险分布、异常设备、空闲设备概览和风险设备分页列表 · covers: AC-001, AC-002, AC-005, AC-006
- [x] T8 — 更新前端路由和菜单，仅 Role 2/3 可见 `/governance` 入口，Role 0/1 不显示入口且后端仍强制拦截 · covers: AC-001, AC-003
- [x] T9 — 补充后端测试，覆盖 Role 0/1 拒绝、Role 2 单位隔离、Role 3 全局只读、风险阈值边界和异常数据降级 · covers: AC-002, AC-003, AC-005
- [x] T10 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [x] T11 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [x] T12 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [x] T13 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark TASK-013 complete and move active feature to TASK-014

## Dependencies
- T3 requires T1 and T2
- T4 requires T3
- T5 requires T4
- T6 requires T5
- T7 requires T2 and T6
- T8 requires T7
- T9-T12 require implementation tasks to be complete
- T13 requires the task package to stay consistent with CURRENT_PLAN.md

## Blockers
<!-- Fill in if something is preventing progress -->
