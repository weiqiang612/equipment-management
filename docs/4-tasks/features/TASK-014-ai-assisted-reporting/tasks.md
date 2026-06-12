# TASK-014: Tasks

**Spec**: `spec.md`
**Status**: Backlog

## Key decisions
- 本任务是四期 AI 辅助能力，作为 TASK-012 和 TASK-013 之后的待办任务，不覆盖当前 Active feature。
- 第一版 AI 只生成草案、摘要和建议，不执行任何业务写操作。
- 第一版不新增数据库表；AI 生成内容不持久化。
- AI Provider、模型、API Key 配置和是否新增依赖属于实施前确认项。

## Progress

- [ ] T1 — 同步长期 Harness 文档：更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md` 和 `docs/2-designs/ui_prototype.md`，明确 AI 辅助的业务定位、安全边界、UI 入口和角色使用场景 · covers: doc-maintenance, AC-001, AC-002, AC-003, AC-005, AC-006
- [ ] T2 — 更新 `docs/2-designs/api_contract.md`，补充 `POST /ai/reports/operations/draft` 和 `POST /ai/equipment/{equipId}/summary` 的请求体、响应结构、权限说明和未配置错误说明 · covers: AC-001, AC-002, AC-004, AC-005
- [ ] T3 — 如实现选择具体 AI Provider、模型或长期安全策略，新增 ADR 记录供应商选择、调用边界、失败降级和不可自动执行业务动作的决策 · covers: doc-maintenance, AC-003, AC-005, AC-006
- [ ] T4 — 新增 AI 请求/响应 DTO/VO，定义报告草案、设备摘要、风险证据、建议动作、Provider 状态和错误信息结构 · covers: AC-001, AC-002, AC-004, AC-005
- [ ] T5 — 新增 AI 上下文组装服务，从 Dashboard、Governance、Operation Log、Equipment Detail 等已授权 DTO/VO 汇总最小必要上下文 · covers: AC-001, AC-002, AC-003, AC-006
- [ ] T6 — 新增 `AiAssistantService`，实现 Provider 未配置降级、超时/失败处理、提示词模板、输出草案标记和敏感字段过滤 · covers: AC-001, AC-002, AC-005, AC-006
- [ ] T7 — 新增 `AiAssistantController`，实现 `/ai/reports/operations/draft` 和 `/ai/equipment/{equipId}/summary`，保持统一 `Result` 响应并限制 Role 2/3 访问 · covers: AC-001, AC-002, AC-003, AC-004
- [ ] T8 — 新增前端 `equipment-web/src/api/aiAssistant.js` 和 `AiAssistant.vue`，支持生成运营报告草案、设备生命周期摘要、错误降级展示和复制草案 · covers: AC-001, AC-002, AC-005
- [ ] T9 — 更新前端路由和菜单，仅 Role 2/3 可见 `/ai-assistant` 入口，Role 0/1 不显示入口且后端仍强制拦截 · covers: AC-001, AC-003
- [ ] T10 — 补充后端测试，覆盖 Role 0/1 拒绝、Role 2/3 可用、Provider 未配置、Provider 失败、敏感字段过滤和 AI 不调用业务写接口 · covers: AC-003, AC-005, AC-006
- [ ] T11 — Run `cd equipment_system_management && mvn test` — all tests must pass
- [ ] T12 — Run `cd equipment-web && npm run lint` — frontend lint must pass
- [ ] T13 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [ ] T14 — Update `docs/4-tasks/CURRENT_PLAN.md` — keep TASK-012 active and leave this task in Backlog until implementation starts

## Dependencies
- T4 requires T1 and T2
- T5 requires TASK-011, TASK-012, and TASK-013 data contracts to be available
- T6 requires T4 and T5
- T7 requires T6
- T8 requires T2 and T7
- T9 requires T8
- T10-T13 require implementation tasks to be complete
- T14 requires the task package to stay consistent with CURRENT_PLAN.md

## Blockers
<!-- Fill in if something is preventing progress -->
