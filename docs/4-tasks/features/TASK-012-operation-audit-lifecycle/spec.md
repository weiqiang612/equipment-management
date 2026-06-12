# TASK-012: 操作审计与设备生命周期详情

**Status**: Draft
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-012-operation-audit-lifecycle/`

## Objective
建立关键业务操作审计和设备生命周期详情页，让系统从分散业务记录升级为可追溯、可审计的全生命周期资产管理平台。

## Scope

### In scope
- 新增操作审计能力，记录设备新增、修改、删除、领用、退还、报修、指派、维修完成、调拨、报废、数据库备份和恢复等关键动作。
- 新增 `operation_log` 审计表，并提供对应迁移脚本。
- 新增设备详情页 `/equipment/detail/:equipId`，集中展示设备基础信息、折旧信息、当前保管人、领用历史、维修历史、调拨历史、报废信息和操作审计时间线。
- 新增后端设备详情与审计查询接口，返回 DTO/VO，不直接暴露数据库实体。
- Role 3 系统管理员可查看全局审计数据；Role 2 资产管理员可查看业务管理范围内的设备详情和审计记录；Role 0/1 继续遵守当前数据隔离边界。
- 更新 `docs/2-designs/db_schema.md`、`docs/2-designs/api_contract.md`、`docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md` 和 `docs/2-designs/role_positioning.md`，记录审计与生命周期详情的业务定位、数据结构、接口契约、UI 入口和角色权限边界。

### Out of scope
- 不实现 AI 审计分析或自然语言报告。
- 不引入工作流引擎。
- 不改变现有领用、维修、调拨、报废状态流转规则。
- 不允许审计日志编辑或删除。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "关键业务操作会生成不可编辑的审计日志。",
    "steps": [
      "执行设备新增、设备修改、领用审批、维修指派、维修完成、调拨、报废、备份或恢复中的任一操作。",
      "查询对应审计记录。",
      "Verify: 审计日志包含操作者、角色、操作类型、业务对象类型、业务对象ID、操作时间、摘要和结果状态。"
    ],
    "passes": false
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "设备详情页集中展示单台设备的完整生命周期信息。",
    "steps": [
      "访问 /equipment/detail/:equipId。",
      "Verify: 页面展示设备基础信息、折旧信息、当前保管人、领用历史、维修历史、调拨历史、报废信息和操作审计时间线。",
      "Verify: 各历史记录按时间倒序或时间线顺序清晰展示。"
    ],
    "passes": false
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "设备详情和审计查询必须遵守现有 RBAC 数据权限边界。",
    "steps": [
      "使用 Role 0 账号访问非本人保管且非本部门空闲设备详情。",
      "Verify: 请求被拒绝或不返回敏感详情。",
      "使用 Role 1 账号访问非本人相关维修工单审计详情。",
      "Verify: 响应不包含无关工单明细。",
      "使用 Role 3 账号查询审计数据。",
      "Verify: 只读可见，不提供业务修改入口。"
    ],
    "passes": false
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "数据库结构和迁移脚本必须与设计文档一致。",
    "steps": [
      "检查 docs/2-designs/db_schema.md 和迁移脚本。",
      "Verify: 文档和脚本包含 operation_log 表结构、索引、字段含义和约束说明。",
      "执行后端测试。",
      "Verify: 审计日志写入和查询使用迁移后的表结构。"
    ],
    "passes": false
  },
  {
    "id": "AC-005",
    "category": "integration",
    "description": "新增 API 必须写入接口契约并使用统一 Result 响应。",
    "steps": [
      "检查 docs/2-designs/api_contract.md。",
      "Verify: 文档包含设备详情接口和审计日志查询接口的请求头、参数、响应结构和权限说明。",
      "调用新增接口。",
      "Verify: 响应使用 Result 统一结构，data 为 DTO/VO。"
    ],
    "passes": false
  },
  {
    "id": "AC-006",
    "category": "edge-case",
    "description": "审计写入失败不能静默吞掉，也不能破坏原业务状态一致性。",
    "steps": [
      "模拟审计写入异常。",
      "执行关键业务操作。",
      "Verify: 系统记录错误并返回可诊断失败信息，不能出现业务已变更但无任何审计痕迹且无错误提示的情况。",
      "Verify: 事务边界明确，关键业务和审计记录保持一致。"
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
| db | true | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |
| project-specific design | true | `docs/2-designs/role_positioning.md` |

### Approval-sensitive changes
- Add database migration for `operation_log`.
- Introduce cross-cutting audit writes inside existing Service flows.

### Explicit non-maintenance
- `AGENTS.md`, `.codex/session-start.js`, `init.sh`, and `init.ps1` do not need maintenance because this task does not change ports, startup commands, health checks, test/lint commands, or runnable submodules.
- No centralized configuration document exists; this task does not introduce new environment variables or config keys.
- `docs/3-constraints/adr/` does not need maintenance unless implementation introduces a new dependency, irreversible audit retention policy, or architecture strategy beyond the scoped append-only audit layer already captured in `architecture.md`.

- 本任务是 TASK-011 数据看板之后的二期管理闭环增强任务，应在 TASK-011 完成后实施。
- 审计日志是后续 AI 报告、异常解释和风险建议的重要上下文来源。
- 审计表只追加，不提供编辑和删除能力；如需清理历史数据，应另行设计归档策略。
