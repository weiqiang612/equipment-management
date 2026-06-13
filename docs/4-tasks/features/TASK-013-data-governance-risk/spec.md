# TASK-013: 数据治理与运营风险分析

**Status**: Complete
**Created**: 2026-06-12
**Feature dir**: `docs/4-tasks/features/TASK-013-data-governance-risk/`

## Objective
基于现有业务数据和 TASK-012 的审计/生命周期上下文，提供数据质量检查、风险设备识别、维修成本异常分析和资产健康评分能力。

## Scope

### In scope
- 新增 Role 2/3 可见的数据治理页面 `/governance`，展示质量总览、风险分布、异常设备和长期空闲设备概览。
- 新增 `GET /governance/summary`，返回数据质量总览、风险等级分布、成本异常概览、长期空闲设备概览。
- 新增 `GET /governance/equipment-risks`，分页返回风险设备清单，支持 `riskLevel`、`unitCode`、`categoryId` 筛选。
- 新增后端治理聚合能力：`GovernanceController`、`GovernanceService`、`GovernanceDao`、治理 DTO/VO。
- 风险评分规则使用后端命名常量，不做数据库配置化。
- Role 2 仅查看本单位或业务管理范围内治理数据；Role 3 全局只读；Role 0/1 禁止访问治理接口。
- 更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md` 和 `docs/2-designs/api_contract.md`。

### Out of scope
- 不引入 AI、自然语言报告或智能问答。
- 不新增依赖。
- 不新增或修改数据库表结构、索引或迁移脚本。
- 不实现规则后台配置化。
- 不替代 TASK-012 操作审计与设备生命周期详情。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "Role 2/3 能访问数据治理页面并看到质量总览、风险分布、异常设备和空闲设备概览。",
    "steps": [
      "使用 Role 2 或 Role 3 账号登录系统。",
      "访问 /governance。",
      "Verify: 页面展示数据质量总览、风险等级分布、维修成本异常概览和长期空闲设备概览。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "风险设备清单按固定规则计算风险等级，并支持按风险等级、单位、分类筛选。",
    "steps": [
      "调用 GET /governance/equipment-risks。",
      "分别使用 riskLevel、unitCode、categoryId 查询参数筛选。",
      "Verify: 返回分页结果包含风险等级、风险原因、健康评分、维修次数、维修费用占比和使用年限占比。",
      "Verify: 高中低风险等级符合 spec 中定义的阈值规则。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "治理接口必须遵守 RBAC 和单位级数据隔离。",
    "steps": [
      "使用 Role 0 或 Role 1 账号访问 /governance/summary 或 /governance/equipment-risks。",
      "Verify: 请求被拒绝并返回权限不足响应。",
      "使用 Role 2 账号查询其他单位治理明细。",
      "Verify: 响应不包含越权单位明细。",
      "使用 Role 3 账号访问治理页面。",
      "Verify: 可全局只读查看，不提供业务写操作入口。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "新增治理 API 必须写入接口契约并使用统一 Result 响应。",
    "steps": [
      "检查 docs/2-designs/api_contract.md。",
      "Verify: 文档包含 /governance/summary 和 /governance/equipment-risks 的请求头、参数、响应结构 and 权限说明。",
      "调用新增接口。",
      "Verify: 响应使用 Result 统一结构，data 为 DTO/VO，不直接暴露数据库实体。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "edge-case",
    "description": "缺少分类、单位、购入日期、原值、保管人匹配信息时，治理结果应降级为质量问题项，不导致接口 500。",
    "steps": [
      "准备缺少分类、单位、购入日期、原值或保管人单位不匹配的设备数据。",
      "调用 GET /governance/summary 和 GET /governance/equipment-risks。",
      "Verify: 接口正常返回，异常数据进入质量问题统计或风险原因说明。"
    ],
    "passes": true
  },
  {
    "id": "AC-006",
    "category": "performance",
    "description": "治理统计在后端使用 SQL 聚合和分页，不允许前端拉全量业务列表自行计算。",
    "steps": [
      "检查前端 Governance 页面数据加载逻辑。",
      "Verify: 页面通过 /governance/* 接口获取聚合和分页数据。",
      "检查后端 DAO 查询。",
      "Verify: 汇总统计使用 COUNT、SUM、GROUP BY 等数据库聚合能力，风险清单使用分页查询。"
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
| api | true | `docs/2-designs/api_contract.md` |
| db | false | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Risk rules
- `ageRatio = usedMonths / (usefulLife * 12)`.
- High risk: non-scrapped equipment matches any of `ageRatio >= 0.9`, maintenance count `>= 3`, maintenance cost / original value `>= 0.3`.
- Medium risk: non-scrapped equipment matches any of `ageRatio >= 0.75`, maintenance count `>= 2`, maintenance cost / original value `>= 0.15`, status equals `维修`.
- Low risk: registered equipment that does not match high or medium risk.
- Long-term idle in v1: `custodian IS NULL` and `status = '在用'`.

### Explicit non-maintenance
- `docs/2-designs/db_schema.md` is not updated because this task does not add tables, fields, indexes, constraints, or migrations.
- `AGENTS.md`, `.codex/session-start.js`, `init.sh`, and `init.ps1` do not need maintenance because this task does not change ports, startup commands, health checks, test/lint commands, or runnable submodules.
- AI reports, exception explanation, natural language Q&A, and AI event explanations are reserved for TASK-016 or later.
