# TASK-019: Tasks

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- 报告正文以 `docs/4-tasks/features/TASK-018-web-course-report/report_plan.md` 为直接执行依据。
- 交付物优先形成 Markdown 初稿；如格式和工具条件满足，再导出或整理为正式 `.docx`。
- 所有正文、截图说明、测试结论和参考文献必须可追溯、可核验、可脱敏。

## Progress

- [x] T1 — 建立报告初稿文件，记录 `TASK-018/report_plan.md`、`docs/1-requirements/`、`docs/2-designs/` 和代码目录作为写作依据 · covers: AC-001, AC-003
- [x] T2 — 生成封面字段、摘要、关键词和目录占位，保留课程设计口径并排除毕业论文专属声明章节 · covers: AC-001, AC-002
- [x] T3 — 编写第 1 章绪论正文，覆盖项目背景、设计意义和全文结构 · covers: AC-002, AC-003
- [x] T4 — 编写第 2 章相关技术正文，覆盖 Java 11、Spring Boot、Vue 2、Element UI、MySQL、JdbcTemplate、JWT、RBAC、ECharts 和 AI 辅助边界 · covers: AC-002, AC-003, AC-005
- [x] T5 — 编写第 3 章系统需求分析正文，覆盖四级角色、功能需求、非功能需求、权限矩阵和用例说明 · covers: AC-002, AC-003, AC-005
- [x] T6 — 编写第 4 章系统设计正文，覆盖总体架构、功能模块、数据库设计、接口设计和核心业务流程图占位 · covers: AC-002, AC-003
- [x] T7 — 编写第 5 章系统实现正文，按登录注册、设备台账、领用审批、检修闭环、看板治理消息、AI 辅助、备份恢复与审计分节说明 · covers: AC-002, AC-003, AC-005
- [x] T8 — 编写第 6 章系统测试正文，覆盖功能测试、权限测试、接口测试、边界测试、验证命令和实际结果记录位置 · covers: AC-002, AC-003, AC-004
- [x] T9 — 编写第 7 章课程设计总结和附录，归纳完成内容、不足、改进方向、项目结构、接口清单和数据库核心表 · covers: AC-002, AC-003
- [x] T10 — 补齐运行效果截图清单、图表编号、截图说明和缺失截图 TODO，不展示真实密码、Token、API Key 或敏感个人数据 · covers: AC-004, AC-005
- [x] T11 — 补齐不少于 10 篇参考文献，统一格式并剔除无法确认来源的虚构文献 · covers: AC-004
- [x] T12 — 执行格式与目录检查，确认标题层级、图表编号、目录生成条件、Markdown 到 `.docx` 转换条件或 `.docx` 排版要求 · covers: AC-004
- [x] T13 — 执行真实性与敏感信息检查，确认报告不编造未实现功能、不泄露密钥账号、不突破 AI 和备份恢复安全边界 · covers: AC-003, AC-005
- [x] T14 — Run `cd equipment_system_management && mvn test` — all backend tests must pass before final report completion
- [x] T15 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [x] T16 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete after the final report or Markdown draft is finished

## Dependencies
- T1 must finish before正文 generation because every section needs traceable sources.
- T2 through T9 depend on `TASK-018/report_plan.md` and the aligned project docs.
- T10 through T13 require the正文 draft to exist.
- T14, T15, and T16 require the report draft and checks to be complete.

## Blockers
- None currently.
