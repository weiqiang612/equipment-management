# TASK-011: 数据看板

**Status**: Draft
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-011-dashboard/`

## Objective
实现一个基于 RBAC 的数据看板入口，让不同角色登录后看到与自身职责和数据权限匹配的资产、维保、领用和审计统计。

## Scope

### In scope
- 新增 `/dashboard` 前端页面，并将登录后的默认首页调整为数据看板。
- 新增 `GET /dashboard/summary` 后端聚合接口，使用统一 `Result` 响应格式。
- 基于当前登录用户的 `role`、`username`、`unitCode` 返回角色化看板数据。
- Role 2 资产管理员展示业务运营视角：全局资产统计、资产分布、维保趋势、待审批领用、待指派维修。
- Role 3 系统管理员展示审计视角：全局资产只读统计、用户角色分布、备份文件状态，不提供业务处理入口。
- Role 1 维修工程师展示个人维保视角：分配给我的未完成工单、维修中工单、完成统计、维保费用趋势。
- Role 0 设备操作员展示个人资产视角：我保管的设备、我的领用申请、我的报修进度和折旧概览。
- 引入 Apache ECharts 作为第一版图表库，直接在 Vue 2 Options API 组件生命周期中初始化和销毁图表实例。
- 使用企业级资产运营驾驶舱视觉方向：继承现有深蓝侧栏、白色卡片、冷灰背景，并用状态色区分资产、维修、报废、完成和审计信息。
- 第一版图表限定为环形图、柱状图、折线图或柱线组合图；核心操作和待办仍使用 Element UI 卡片、列表、标签和表格承载。
- 更新 `docs/1-requirements/project_overview.md`、`docs/1-requirements/requirements_analysis.md`、`docs/2-designs/architecture.md`、`docs/2-designs/ui_prototype.md`、`docs/2-designs/role_positioning.md` 和 `docs/2-designs/api_contract.md`，记录看板业务定位、角色入口、UI 结构和新增接口契约。

### Out of scope
- 不新增或修改数据库表结构。
- 不引入维修工时、紧急程度、优先级等当前数据模型不存在的指标。
- 不引入 GSAP 或复杂大屏转场动画。
- 不实现 3D 图表、桑基图、南丁格尔玫瑰图等第一版维护收益低的复杂可视化。
- 不改变现有业务审批、派单、领用、报修状态流转规则。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "所有已登录角色都能访问数据看板，并看到与角色匹配的首页内容。",
    "steps": [
      "使用任一有效角色账号登录系统。",
      "访问 /dashboard 或访问根路径 /。",
      "Verify: 页面展示数据看板，根路径重定向到 /dashboard，菜单中存在数据看板入口。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "资产管理员和系统管理员能看到全局资产透视数据，但系统管理员不出现业务处理操作入口。",
    "steps": [
      "分别使用 Role 2 和 Role 3 账号访问 /dashboard。",
      "检查设备总数、资产总原值、在用数、维修中数、报废数、分类分布、部门分布和维保趋势。",
      "Verify: Role 2 可看到待审批/待指派待办及跳转入口，Role 3 仅看到全局审计与系统状态信息，不展示业务审批或派单操作按钮。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "维修工程师和设备操作员只能看到个人工作台数据。",
    "steps": [
      "使用 Role 1 账号访问 /dashboard。",
      "Verify: 仅展示分配给当前维修工程师的未完成工单、完成统计和维保费用趋势。",
      "使用 Role 0 账号访问 /dashboard。",
      "Verify: 仅展示当前操作员保管设备、当前操作员领用申请和当前操作员报修进度。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "security",
    "description": "看板聚合接口必须遵守现有 RBAC 和数据隔离规则。",
    "steps": [
      "使用 Role 0 账号调用 GET /dashboard/summary。",
      "Verify: 响应中不包含其他用户保管的设备、其他用户领用申请或其他用户报修记录。",
      "使用 Role 1 账号调用 GET /dashboard/summary。",
      "Verify: 响应中不包含未分配给当前维修工程师的工单详情。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "integration",
    "description": "新增看板 API 必须和接口契约保持一致。",
    "steps": [
      "检查 docs/2-designs/api_contract.md。",
      "Verify: 文档包含 GET /dashboard/summary 的请求头、响应结构和角色化数据说明。",
      "调用 GET /dashboard/summary。",
      "Verify: 响应使用 Result 统一结构，字段不直接暴露数据库实体。"
    ],
    "passes": true
  },
  {
    "id": "AC-006",
    "category": "performance",
    "description": "看板聚合查询在常规数据量下应避免前端拉全量列表后自行统计。",
    "steps": [
      "检查 Dashboard 页面数据加载逻辑。",
      "Verify: 页面通过 GET /dashboard/summary 获取聚合数据，而不是调用多个列表接口拉全量业务数据后在前端统计。",
      "检查后端 SQL。",
      "Verify: 聚合查询使用 COUNT、SUM、GROUP BY 等数据库聚合能力，并复用现有索引字段。"
    ],
    "passes": true
  },
  {
    "id": "AC-007",
    "category": "functional",
    "description": "数据看板视觉设计应形成清晰的企业级资产运营驾驶舱，而不是普通列表页。",
    "steps": [
      "访问 /dashboard。",
      "Verify: 页面包含角色化标题区、KPI 卡片区、图表区、待办或明细区。",
      "Verify: 视觉风格继承现有后台深蓝、白卡、冷灰背景，并通过状态色区分在用、维修、报废、完成和审计信息。",
      "Verify: 页面不使用全屏炫酷大屏、霓虹渐变或复杂转场动画。"
    ],
    "passes": true
  },
  {
    "id": "AC-008",
    "category": "performance",
    "description": "ECharts 使用应保持轻量、可维护且兼容 Vue 2 生命周期。",
    "steps": [
      "检查 equipment-web/package.json。",
      "Verify: 仅新增 echarts 图表依赖，不新增 GSAP 或不必要的图表封装库。",
      "检查 Dashboard 图表组件实现。",
      "Verify: 图表实例在 mounted 后初始化，在 beforeDestroy 中销毁，并监听容器尺寸变化触发 resize。",
      "Verify: 页面 loading 时使用 Element UI loading 或骨架状态，不出现空白等待。"
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
| project-specific design | true | `docs/2-designs/role_positioning.md` |

### Approval-sensitive changes
- Add frontend dependency `echarts`.

### Explicit non-maintenance
- `docs/2-designs/db_schema.md` does not need maintenance because this task uses existing tables and does not add schema, entity constraints, indexes, or migrations.
- `docs/3-constraints/adr/` does not need maintenance because ECharts is a local UI library choice, not a long-lived architecture strategy or irreversible decision.
- No centralized configuration document exists; this task does not introduce new environment variables or config keys.

- 本任务只做数据看板第一版 MVP，优先保证指标准确、权限隔离正确、接口契约清晰。
- 现有数据库已经包含本任务需要的主要字段：`equipment.status`、`equipment.original_value`、`equipment.purchase_date`、`equipment.custodian`、`category.useful_life`、`category.residual_rate`、`maintenance_record.maint_status`、`maintenance_record.maint_cost`、`t_equipment_claim.status`。
- 如果实现过程中发现现有字段无法支撑某个指标，应降低为可解释的近似指标，不能未经迁移直接修改数据库结构。
- 图表库选择：ECharts 是本任务的一版依赖，用于设备状态分布、分类占比、部门资产分布、维保趋势等核心图表；GSAP 不纳入本任务。
- 前端兼容性：当前项目为 Vue 2.6 + Element UI，应使用 Options API，不使用 `<script setup>` 或 Vue 3 专属写法。
