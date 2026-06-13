# TASK-015: 事找人 MVP 与消息中心

**Status**: Draft
**Created**: 2026-06-13
**Feature dir**: `docs/4-tasks/features/TASK-015-event-notification-mvp/`

## Objective
基于现有数据治理、领用审批和检修闭环结果，建立规则驱动的事件提醒与消息中心 MVP，让系统先具备“发现问题并找到责任人”的能力。

## Scope

### In scope
- 建立规则驱动的事件模型，第一版覆盖高风险设备提醒、领用待审批积压提醒、维修单超时提醒三类事件。
- 为事件建立责任人路由规则：高风险设备通知本单位资产管理员，领用积压通知对应单位资产管理员，维修超时通知当前维修工程师并可按规则抄送本单位资产管理员。
- 新增事件/消息存储能力，支持未读、已读、事件类型、目标用户、关联业务对象和创建时间。
- 新增 Role 1/2/3 可见的消息中心页面，支持事件列表、未读角标、事件详情和跳转到治理页、设备详情页、领用审批页或检修页。
- 事件来源复用既有治理规则、领用审批数据和检修工单状态，不引入 AI 作为事件判定条件。
- 更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md` 和 `docs/2-designs/ui_prototype.md`。

### Out of scope
- 不实现 WebSocket、SSE 或实时推送；第一版允许页面拉取和手动/定时刷新。
- 不实现 AI 自动生成事件，也不在本任务中实现 AI 解释与建议。
- 不实现复杂通知策略、SLA 升级、短信/邮件/企业微信/钉钉对接。
- 不引入开放式问答或通用聊天助手。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "系统能为高风险设备、审批积压和维修超时生成规则事件并路由给正确责任人。",
    "steps": [
      "准备一台满足高风险规则的设备、一批超过阈值的待审批领用记录和一张超过超时阈值的维修工单。",
      "触发事件生成逻辑或进入消息中心刷新事件列表。",
      "Verify: 对应责任人能看到三类事件，且每条事件包含标题、摘要、事件类型、关联对象和目标处理入口。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "消息中心支持未读管理和跳转处理闭环。",
    "steps": [
      "使用收到事件的 Role 1 或 Role 2 账号进入消息中心。",
      "查看未读事件、打开事件详情并执行“标记已读”或点击跳转到治理页、审批页、检修页、设备详情页。",
      "Verify: 未读数正确变化，跳转页面与事件类型匹配，用户可继续沿现有业务流程处理。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "事件读取与路由必须遵守角色和单位隔离。",
    "steps": [
      "使用无关单位的资产管理员、无关维修工程师或普通操作员访问消息中心和事件详情接口。",
      "尝试读取不属于自己的事件或通过参数伪造读取其他人的事件。",
      "Verify: 无关用户不可见或被拒绝访问，系统不会泄露跨单位事件内容。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "事件模型与现有治理、审批、检修页面保持联动一致。",
    "steps": [
      "从消息中心分别打开高风险设备事件、审批积压事件和维修超时事件。",
      "Verify: 高风险事件能落到治理页或设备详情，审批积压事件能落到领用审批页，维修超时事件能落到检修页。",
      "Verify: 事件摘要与源业务数据一致，不出现与治理规则或工单状态冲突的展示。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "edge-case",
    "description": "系统必须避免重复刷屏和失效事件污染消息中心。",
    "steps": [
      "对同一风险设备、同一批积压审批或同一张超时工单多次触发事件生成逻辑。",
      "对已消除风险、已处理审批或已完工复核的对象再次刷新消息中心。",
      "Verify: 系统不会无限重复生成未读事件，已失效事件会被关闭、合并或不再展示为待处理。"
    ],
    "passes": true
  }
]
```

## Notes
### Documentation impact
| Area | Impacted | Maintenance target |
|---|---:|---|
| requirements | true | `docs/1-requirements/project_overview.md`, `docs/1-requirements/requirements_analysis.md` |
| architecture | true | `docs/2-designs/architecture.md` |
| api | false | `docs/2-designs/api_contract.md` |
| db | false | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Approval-sensitive changes
- 若实现阶段新增事件表、索引或迁移脚本，需要单独确认数据库变更方案。
- 若实现阶段选择定时任务而不是纯页面触发生成事件，需要确认是否引入新的调度入口或配置。

### Explicit non-maintenance
- `docs/2-designs/api_contract.md` 暂不维护，因为本次任务创建阶段只确定 MVP 方向，待实现时再结合最终接口形态补契约更稳妥。
- `docs/2-designs/db_schema.md` 暂不维护，因为事件表结构、去重策略和失效策略仍应在实现前结合最终方案确认。
