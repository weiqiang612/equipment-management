# TASK-018: WEB 课程设计报告计划书

**Status**: Completed
**Created**: 2026-06-14
**Feature dir**: `docs/4-tasks/features/TASK-018-web-course-report/`

## Objective
先将 `docs/` 中滞后于前后端现有代码的需求、设计、API、数据库和 UI 文档对齐到真实实现，再为 `WEB开发技术课程设计报告` 生成一份可执行、可核验的详尽撰写计划，使正式报告能够参照毕业论文结构标准，同时只覆盖课程设计要求和本项目真实具备的内容。

## Scope

### In scope
- 基于当前前后端代码对齐 `docs/1-requirements/` 与 `docs/2-designs/`，修正从 RBAC、登录注册之后新增能力未同步或旧小项目口径残留的问题。
- 基于 `23WEB课程设计报告.doc` 提取课程设计报告硬性要求，包括需求分析、系统设计、实现说明、运行效果、课程设计总结和不少于 10 篇参考文献。
- 参考 `孙佳慧毕业设计说明书.docx` 的论文级章节组织、摘要、目录、技术介绍、系统分析、系统设计、系统实现、系统测试和参考文献表达方式。
- 基于 `docs/1-requirements/`、`docs/2-designs/`、任务包、代码目录和 git 提交记录，整理国家标准设备管理系统的真实功能、架构、数据库、接口、界面、测试和演示材料计划。
- 明确正式报告需要补齐的图表、截图、代码片段、测试用例、视频脚本和格式检查项。

### Out of scope
- 不修改后端、前端、数据库、API 契约或项目运行逻辑。
- 不在课程设计报告中加入课程设计模板未要求的原创性声明、版权授权、致谢、保密声明等毕业论文专属部分。
- 不编造项目未实现的功能、算法、外部系统或实验结果。
- 不生成最终 `.docx` 正文成稿；本任务只生成报告撰写计划和执行清单。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "计划书完整覆盖课程设计报告明确要求的全部组成部分。",
    "steps": [
      "读取原始课程设计报告模板中的撰写要求。",
      "在计划书中列出需求分析、系统设计、实现说明、运行效果、课程设计总结和参考文献等必需部分。",
      "验证计划书没有遗漏模板中明确要求的提交视频和程序结构说明。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "计划书能够参照毕业论文标准形成清晰章节结构，但不引入课程设计未要求的毕业论文专属内容。",
    "steps": [
      "读取毕业设计说明书的摘要、目录、技术介绍、分析、设计、实现、测试、结论和参考文献结构。",
      "将适合课程设计的章节映射为 WEB 课程设计报告大纲。",
      "验证原创性声明、版权授权、致谢等毕业论文专属内容被明确排除。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "integration",
    "description": "项目 docs 与当前代码实现保持一致，计划书的业务、架构、接口、数据库和 UI 内容均能追溯到 docs、代码目录或 git 提交记录。",
    "steps": [
      "从 `docs/1-requirements/` 提取项目定位、角色、业务流程和功能范围。",
      "从 `docs/2-designs/` 提取架构、API、数据库和 UI 设计依据。",
      "根据当前前后端代码修正旧口径，例如领用条件、消息中心、AI 助手、审计、治理、路由与接口索引。",
      "从代码目录和 git log 提取实现章节、截图清单和提交演进依据。",
      "验证计划书不出现无法追溯的功能承诺。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "edge-case",
    "description": "计划书明确处理文档资料不完整或格式转换异常的情况。",
    "steps": [
      "记录课程设计模板原始 `.doc` 已通过 Word COM 临时转换为可读 `.docx` 的事实。",
      "明确正式报告撰写应以原始 `.doc` 模板要求为准，而不是损坏的转换副本。",
      "验证临时转换产物只作为读取辅助，不作为最终提交文件。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "security",
    "description": "计划书不得要求泄露密钥、个人敏感信息或绕过项目安全边界。",
    "steps": [
      "检查计划书中的截图、接口、AI Provider、数据库和测试章节要求。",
      "验证计划书不要求暴露 API Key、密码哈希、Token、数据库账号或真实敏感数据。",
      "验证涉及 AI 辅助内容时只描述草案生成和人工确认，不描述自动审批、自动报废或自动恢复数据库。"
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
| db | true | `docs/2-designs/db_schema.md` |
| ui | true | `docs/2-designs/ui_prototype.md`, `docs/2-designs/role_positioning.md` |
| constraints | false | `docs/3-constraints/` |
| adr | false | `docs/3-constraints/adr/` |
| agent-runtime | false | `AGENTS.md`, `.codex/session-start.js`, `init.sh`, `init.ps1` |

### Approval-sensitive changes
- None. This task creates planning documents only and does not change runtime code, database schema, API contracts, dependencies, configuration, or startup behavior.

### Explicit non-maintenance
- `docs/3-constraints/` and `docs/3-constraints/adr/` do not need updates because no new long-lived engineering rule or architecture decision is introduced.
- Runtime scripts and AGENTS files do not need updates because ports, commands, startup protocol and module boundaries are unchanged.
