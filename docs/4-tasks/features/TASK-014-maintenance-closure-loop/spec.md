# TASK-014: 维修流转闭环与复核处置

**Status**: Draft
**Created**: 2026-06-13
**Feature dir**: `docs/4-tasks/features/TASK-014-maintenance-closure-loop/`

## Objective
在现有报修与派单基础上补齐“完工复核”和“恢复可用/转报废”分流，使检修业务形成可审计、可复盘的完整闭环。

## Scope

### In scope
- 将检修工单状态从“待指派 / 维修中 / 已完成”扩展为覆盖完工待复核和最终处置结论的闭环状态。
- 调整检修流转为：操作员报修 -> 资产管理员指派 -> 维修工程师完工登记 -> 资产管理员复核 -> 恢复可用或转报废。
- 新增资产管理员复核动作与意见记录，复核通过后再决定设备恢复在用或进入报废处置。
- 完工登记不再直接把设备状态恢复为“在用”，而是先进入“待复核”。
- 若复核结论为“转报废”，复用现有报废业务能力生成报废记录并保持生命周期与审计链路完整。
- 更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/api_contract.md`、`docs/2-designs/db_schema.md` 和 `docs/2-designs/ui_prototype.md`。

### Out of scope
- 不引入 AI 建议、消息推送、WebSocket 或事件中心。
- 不引入优先级、SLA、催办升级或多级审批。
- 不改造系统管理员为可写角色；系统管理员仍保持审计只读。
- 不引入独立工作流引擎或新子系统。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "检修工单能按“报修 -> 指派 -> 完工待复核 -> 复核恢复可用”闭环流转。",
    "steps": [
      "使用 Role 0 账号对本人保管设备发起报修，使用 Role 2 账号指派给本单位维修工程师，使用 Role 1 账号登记完工。",
      "Verify: 工单状态依次流转为待指派、维修中、待复核，设备在完工登记后仍保持维修态。",
      "使用 Role 2 账号对该工单执行“复核通过恢复可用”，Verify: 工单进入最终已复核可用状态，设备状态恢复为“在用”，审计日志记录完整。 "
    ],
    "passes": false
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "复核结论可将不可修复设备转入报废处置闭环。",
    "steps": [
      "使用 Role 1 账号完成一张维修工单并使其进入待复核。",
      "使用 Role 2 账号在复核时选择“转报废”并填写处置原因。",
      "Verify: 工单进入最终转报废状态，设备状态变为“报废”，报废记录与生命周期详情中能看到本次处置结果。 "
    ],
    "passes": false
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "复核与完工动作必须遵守角色和单位边界。",
    "steps": [
      "使用 Role 0 或 Role 3 账号尝试执行工单指派、完工复核或转报废动作。",
      "使用非本人 Role 1 账号尝试登记其他工程师的维修工单，或使用跨单位 Role 2 账号尝试复核他单位工单。",
      "Verify: 所有越权请求均被拒绝，且不会修改工单状态、设备状态或报废记录。 "
    ],
    "passes": false
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "检修闭环的接口契约、数据库设计和迁移脚本必须一致。",
    "steps": [
      "检查 docs/2-designs/api_contract.md、docs/2-designs/db_schema.md 与对应升级 SQL。",
      "Verify: 文档和迁移包含新增工单状态、复核字段、复核接口/请求体/响应结构及转报废处置说明。",
      "调用闭环相关接口，Verify: 返回统一 Result 结构，持久化字段与设计文档一致。 "
    ],
    "passes": false
  },
  {
    "id": "AC-005",
    "category": "edge-case",
    "description": "系统必须阻断非法状态跳转和重复处置。",
    "steps": [
      "尝试对待指派工单直接复核、对待复核前的工单执行转报废、或对已复核完成工单再次完工/再次复核。",
      "尝试在设备已报废或工单已完结后重复发起相关闭环动作。",
      "Verify: 系统返回明确业务错误，不产生重复报废记录，不会把设备状态错误回滚为在用。 "
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

### Approval-sensitive changes
- 需要扩展 `maintenance_record` 状态枚举与复核字段，并新增对应升级 SQL。
- 需要新增或拆分检修闭环接口契约，避免继续用单一更新接口承载全部状态跳转。
- 如选择复用现有报废 Service 直接触发转报废，需要确认请求体复用方式与报废审计字段映射。

### Explicit non-maintenance
- `docs/3-constraints/` 不需要维护，因为本任务不新增长期安全禁令或 Agent 运行规则，只是在现有 RBAC 与事务边界内扩展业务流转。
- `AGENTS.md`、`.codex/session-start.js`、`init.sh` 和 `init.ps1` 不需要维护，因为本任务不改变端口、启动命令、健康检查或子模块结构。
