# TASK-017: 全局 UI/UX 收尾优化

**Status**: Draft
**Created**: 2026-06-14
**Feature dir**: `docs/4-tasks/features/TASK-017-ui-ux-polish/`

## Objective
在不修改后端接口、数据库结构和权限模型的前提下，对设备管理系统前端进行一次定稿前的 UI/UX 收尾优化，提升导航一致性、页面头部统一性和关键表格页的首屏可操作性。

## Scope

### In scope
- 统一关键页面的页头结构，形成“标题 + 副标题 + 右侧主操作 + 可选摘要条”的后台信息层级。
- 收口左侧导航的信息架构、命名和消息入口展示，保持当前蓝灰企业后台风格不变。
- 优化关键表格页的“首屏可操作性”，包括消息/待办跳转后的自动定位、操作列固定和待办摘要筛选。
- 统一状态标签的文案语义和颜色映射，避免不同页面同义不同色或同色不同义。
- 统一空状态、待办筛选、跳转高亮和行内操作的交互基线。
- 更新 `docs/2-designs/ui_prototype.md`，将本轮 UI/UX 收尾规范纳入长期设计文档。

### Out of scope
- 不新增或修改任何后端 API、数据库表、索引、迁移脚本或权限模型。
- 不引入新的前端依赖、全局插件、第三方 UI 库或构建配置变更。
- 不重做主题色、整体视觉风格或导航骨架，只做结构和交互收口。
- 不新增前端自动化测试框架；本任务以 lint 和人工验收为主。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "关键页面页头风格统一，用户能在首屏快速识别页面目标、当前状态和主操作入口。",
    "steps": [
      "分别进入数据看板、消息中心、检修页及至少一个典型表格页。",
      "检查页头是否采用统一的信息层级：标题、副标题、右侧主操作、可选摘要条。",
      "Verify: 页头结构一致，主操作位置稳定，不再出现有的页面只有标题、有的页面堆满按钮的割裂感。"
    ],
    "passes": false
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "从消息中心或待办入口跳转到检修等关键表格页后，目标记录能立即被看到且操作项首屏可见。",
    "steps": [
      "从消息中心或带 query 参数的待办入口跳转到检修页。",
      "观察目标工单是否自动定位、高亮，并检查操作列是否固定在右侧。",
      "Verify: 用户无需二次滚动或横向查找即可看到目标工单及对应操作按钮。"
    ],
    "passes": false
  },
  {
    "id": "AC-003",
    "category": "integration",
    "description": "关键表格页支持轻量待办摘要筛选，并保持与现有角色权限和路由参数联动一致。",
    "steps": [
      "使用 Role 1、Role 2 和 Role 3 账号分别进入检修页或其他待办型表格页。",
      "点击摘要条切换待办视图，并结合带 maintId 等参数的跳转场景进行检查。",
      "Verify: 摘要条数量、筛选结果、角色可见范围和目标记录联动均与现有权限规则一致。"
    ],
    "passes": false
  },
  {
    "id": "AC-004",
    "category": "edge-case",
    "description": "在无待办、无未读或目标记录不存在的情况下，页面仍能给出清晰稳定的空状态与提示。",
    "steps": [
      "分别制造无未读消息、无待处理检修工单、跳转目标记录不存在三种场景。",
      "进入相关页面并观察空状态、提示语和布局稳定性。",
      "Verify: 页面不出现空白、错位或死链跳转，而是展示统一的空状态和轻量提示。"
    ],
    "passes": false
  },
  {
    "id": "AC-005",
    "category": "functional",
    "description": "左侧导航、状态标签和页面命名在视觉与语义上保持统一，形成可交付的定稿体验。",
    "steps": [
      "检查左侧导航的分组顺序、命名、消息入口和关键页面的状态标签颜色。",
      "对比消息中心、检修页、数据治理页等含状态标签页面的语义一致性。",
      "Verify: 导航名称易懂，管理项分层清晰，待处理/处理中/待复核/已完成/高风险等状态颜色和文案一致。"
    ],
    "passes": false
  }
]
```

## Notes
### Documentation impact
| Area | Impacted | Maintenance target |
|---|---:|---|
| requirements | false | `docs/1-requirements/project_overview.md`, `docs/1-requirements/requirements_analysis.md` |
| architecture | false | `docs/2-designs/architecture.md` |
| api | false | `docs/2-designs/api_contract.md` |
| db | false | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Approval-sensitive changes
- None

### Explicit non-maintenance
- `docs/1-requirements/project_overview.md` 和 `docs/1-requirements/requirements_analysis.md` 不维护，因为本任务不改变业务角色、流程边界或功能范围，只做前端表现层收尾。
- `docs/2-designs/api_contract.md` 和 `docs/2-designs/db_schema.md` 不维护，因为本任务不新增接口、不修改请求响应结构，也不触碰数据库结构。
