# TASK-019: WEB 课程设计正式报告撰写

**Status**: Ready
**Created**: 2026-06-14
**Feature dir**: `docs/4-tasks/features/TASK-019-write-web-course-report/`

## Objective
依据 `TASK-018` 已完成的报告计划书，按章节生成 WEB 课程设计正式报告正文，并完成格式、目录、截图、参考文献和敏感信息检查，使交付物达到可提交的正式报告或 Markdown 初稿标准。

## Scope

### In scope
- 明确引用并执行 `docs/4-tasks/features/TASK-018-web-course-report/report_plan.md` 中的章节结构、取材边界、截图清单、参考文献计划和最终验收清单。
- 按章节生成报告正文，覆盖摘要、绪论、相关技术、需求分析、系统设计、系统实现、系统测试、课程设计总结、参考文献和附录。
- 交付正式报告 `.docx` 或 Markdown 初稿；Markdown 初稿应具备可转换为 `.docx` 的章节层级、图表占位、截图说明和参考文献格式。
- 检查目录层级、标题编号、图表编号、截图说明、参考文献数量与格式、代码/接口引用真实性。
- 检查并清除或脱敏密码、Token、API Key、数据库账号、真实个人敏感信息等内容。

### Out of scope
- 不修改后端、前端、数据库、API 契约或项目运行逻辑。
- 不新增课程设计模板未要求的毕业论文专属章节，例如原创性声明、版权授权、致谢或保密声明。
- 不编造项目未实现的功能、测试结果、性能指标、论文文献或截图证据。
- 不把 AI 辅助功能描述为自动审批、自动报废、自动恢复数据库或任何无人确认的高风险决策。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "正式报告或 Markdown 初稿必须以 TASK-018 的报告计划书为直接依据。",
    "steps": [
      "读取 `docs/4-tasks/features/TASK-018-web-course-report/report_plan.md`。",
      "在报告任务执行说明或正文依据部分明确引用该计划书。",
      "验证章节结构、截图清单、参考文献计划和验收清单均与 TASK-018/report_plan.md 对齐。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "报告正文按章节完整生成，覆盖课程设计报告所需核心内容。",
    "steps": [
      "生成摘要、关键词、目录占位和第 1 至第 7 章正文。",
      "覆盖绪论、相关技术、需求分析、系统设计、系统实现、系统测试和课程设计总结。",
      "补齐参考文献和附录，验证不存在空章节或仅有标题无正文的章节。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "integration",
    "description": "报告内容必须能追溯到当前项目文档、代码、接口、数据库设计或 TASK-018 计划书。",
    "steps": [
      "使用 `docs/1-requirements/`、`docs/2-designs/` 和当前代码目录作为事实来源。",
      "对功能、角色、数据库表、接口、页面和安全边界进行真实性核对。",
      "验证报告不出现无法在项目资料中追溯的功能承诺、测试结论或外部系统描述。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "edge-case",
    "description": "报告完成前必须进行格式、目录、截图和参考文献检查。",
    "steps": [
      "检查标题层级、目录生成条件、图表编号、截图编号 and 截图说明。",
      "确认运行效果截图覆盖 TASK-018/report_plan.md 要求的核心模块，缺失截图以明确 TODO 标注。",
      "确认参考文献不少于 10 篇，且不存在明显虚构、重复或格式混乱的条目。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "security",
    "description": "报告与截图不得泄露敏感信息或突破项目安全边界。",
    "steps": [
      "检查正文、代码片段、接口示例、截图和附录中的密码、Token、API Key、数据库账号和真实敏感个人信息。",
      "对必须展示的账号或数据使用测试数据或脱敏表达。",
      "验证 AI、备份恢复、用户管理等高风险模块只描述人工确认和权限控制，不描述绕过鉴权或自动执行危险操作。"
    ],
    "passes": true
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
| ui | false | `docs/2-designs/ui_prototype.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Approval-sensitive changes
- None. This task produces report writing artifacts only and does not change dependencies, runtime configuration, API contracts, database schemas, application code, startup scripts, or agent runtime files.

### Explicit non-maintenance
- Long-lived requirements and design documents do not need maintenance because `TASK-018` already completed the docs/code alignment required for report writing.
- Runtime scripts and AGENTS files do not need updates because the report writing task does not change ports, commands, health checks, module boundaries, or session-start behavior.
- ADRs are not required because this task makes no new architecture decision and introduces no irreversible technical tradeoff.
