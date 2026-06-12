# TASK-014: AI 辅助报告与建议草案

**Status**: Draft
**Created**: 2026-06-12
**Feature dir**: `docs/4-tasks/features/TASK-014-ai-assisted-reporting/`

## Objective
在数据看板、操作审计、生命周期详情和数据治理能力稳定后，提供 AI 辅助的资产运营报告、异常解释、设备生命周期总结和处置建议草案。

## Scope

### In scope
- 新增 Role 2/3 可见的 AI 辅助页面 `/ai-assistant`，用于生成资产运营报告、风险解释和设备生命周期摘要。
- 新增 `POST /ai/reports/operations/draft`，基于看板、治理、审计和生命周期摘要生成周报/月报草案。
- 新增 `POST /ai/equipment/{equipId}/summary`，基于单台设备生命周期详情生成设备摘要和风险说明。
- 新增后端 AI 编排能力：`AiAssistantController`、`AiAssistantService`、AI 请求/响应 DTO/VO。
- AI 只读取系统聚合后的 DTO/VO，不直接查询数据库表，不直接调用审批、报废、恢复数据库等业务写接口。
- AI 输出必须标记为“草案/建议”，由人工确认后才能复制到业务流程中使用。
- 未配置 AI Provider/API Key 时，接口返回明确的未配置提示，不影响现有业务功能。
- 更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md` 和 `docs/2-designs/api_contract.md`。

### Out of scope
- 不实现 AI 自动审批、自动报废、自动调拨、自动恢复数据库。
- 不实现自由 SQL 问答或让 AI 直接访问数据库。
- 不保存 AI 生成内容到新表；第一版只返回草案结果。
- 不新增向量库、RAG 文档库或知识库问答。
- 不承诺特定 AI 厂商；实现前需确认供应商、模型、API Key 配置方式和网络可用性。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "Role 2/3 能生成资产运营周报或月报草案。",
    "steps": [
      "使用 Role 2 或 Role 3 账号访问 /ai-assistant。",
      "选择 period 为 weekly 或 monthly 并提交生成报告。",
      "Verify: 页面展示包含资产概况、风险摘要、维修成本异常、待办积压和建议动作的 AI 草案。"
    ],
    "passes": false
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "Role 2/3 能针对单台设备生成生命周期摘要和风险说明。",
    "steps": [
      "使用 Role 2 或 Role 3 账号选择一台有生命周期记录的设备。",
      "调用 POST /ai/equipment/{equipId}/summary。",
      "Verify: 响应包含设备生命周期摘要、主要风险证据和人工复核建议。"
    ],
    "passes": false
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "AI 能力必须遵守 RBAC 和高风险动作安全边界。",
    "steps": [
      "使用 Role 0 或 Role 1 账号访问 AI 接口。",
      "Verify: 请求被拒绝并返回权限不足响应。",
      "检查 AI 生成结果。",
      "Verify: 结果只包含草案和建议，不包含自动审批、自动报废、自动调拨或自动恢复数据库执行结果。"
    ],
    "passes": false
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "新增 AI API 必须写入接口契约并使用统一 Result 响应。",
    "steps": [
      "检查 docs/2-designs/api_contract.md。",
      "Verify: 文档包含 /ai/reports/operations/draft 和 /ai/equipment/{equipId}/summary 的请求头、请求体、响应结构、权限说明和未配置错误说明。",
      "调用新增接口。",
      "Verify: 响应使用 Result 统一结构，data 为 DTO/VO。"
    ],
    "passes": false
  },
  {
    "id": "AC-005",
    "category": "edge-case",
    "description": "AI Provider 未配置或调用失败时，系统应可诊断降级，不影响原业务功能。",
    "steps": [
      "在未配置 AI Provider/API Key 的环境调用 AI 接口。",
      "Verify: 返回明确的未配置提示，不抛出未处理异常。",
      "模拟 AI Provider 超时或失败。",
      "Verify: 返回可诊断失败信息，Dashboard、Governance、设备详情等非 AI 页面仍可正常使用。"
    ],
    "passes": false
  },
  {
    "id": "AC-006",
    "category": "performance",
    "description": "AI 输入必须使用后端聚合后的上下文，避免把全量业务表或敏感字段发送给 AI Provider。",
    "steps": [
      "检查 AiAssistantService 的上下文组装逻辑。",
      "Verify: 输入只包含看板、治理、审计和生命周期接口返回的必要 DTO 摘要。",
      "Verify: 不发送密码、Token、完整用户敏感信息或无关全量业务数据。"
    ],
    "passes": false
  }
]
```

## Notes
### Documentation impact
| Area | Impacted | Maintenance target |
|---|---:|---|
| requirements | true | `docs/1-requirements/project_overview.md`, `docs/1-requirements/requirements_analysis.md` |
| architecture | true | `docs/2-designs/architecture.md` |
| api | true | `docs/2-designs/api_contract.md` |
| db | false | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | true | `docs/3-constraints/adr/` if implementation chooses a concrete provider/model or adds long-lived AI safety policy |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Approval-sensitive changes
- Confirm AI provider, model, base URL, API key storage, timeout and retry policy before implementation.
- Confirm whether to add a dependency or use Java 11 HTTP client directly.
- Confirm any `application*.yml` changes before editing Spring configuration.

### Safety defaults
- AI output is advisory only.
- AI does not call business write APIs.
- AI context is assembled by backend services from existing DTO/VO summaries.
- If AI is unavailable, the feature fails closed with a clear error and does not affect non-AI flows.
