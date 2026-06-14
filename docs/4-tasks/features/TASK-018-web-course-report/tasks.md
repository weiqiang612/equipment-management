# TASK-018: Tasks

**Spec**: `spec.md`
**Status**: 100% Complete

## Key decisions
- 报告计划以课程设计模板要求为上限，借鉴毕业论文结构表达，但不照搬毕业论文专属声明类章节。
- 所有项目事实必须来自 `docs/`、代码目录、git 提交记录或已读取的本地报告文件，不编造未实现能力。
- 本任务只生成计划书和执行清单，不修改业务代码、API、数据库、UI 或运行配置。

## Progress

- [x] T1 — 盘点当前前后端代码中的真实页面、接口、服务、实体、数据库脚本和角色边界 · covers: AC-003
- [x] T2 — 对齐 `docs/1-requirements/`，修正旧小项目口径并补齐 RBAC 后新增业务能力 · covers: AC-003
- [x] T3 — 对齐 `docs/2-designs/architecture.md` 与 `role_positioning.md`，补齐真实分层、路由、权限和事件边界 · covers: AC-003, AC-005
- [x] T4 — 对齐 `docs/2-designs/api_contract.md`，补齐当前 Controller 与前端 API 模块实际使用的接口索引 · covers: AC-003, AC-005
- [x] T5 — 对齐 `docs/2-designs/db_schema.md`，补齐迁移后实际存在的数据表、状态字段和索引说明 · covers: AC-003
- [x] T6 — 对齐 `docs/2-designs/ui_prototype.md`，补齐当前路由、页面、菜单与交互状态说明 · covers: AC-003
- [x] T7 — 整理课程设计模板要求，形成正式报告必需内容清单 · covers: AC-001, AC-004
- [x] T8 — 映射毕业论文结构到 WEB 课程设计报告章节，明确保留与排除项 · covers: AC-002
- [x] T9 — 汇总代码目录和 git 提交记录，形成系统实现章节、演进依据和截图清单 · covers: AC-003
- [x] T10 — 设计报告图表清单，包括用例图、数据流图、功能模块图、架构图、E-R 图和核心时序图 · covers: AC-001, AC-002, AC-003
- [x] T11 — 设计系统测试章节计划，包括功能测试、权限测试、接口测试、边界测试和联调验证 · covers: AC-001, AC-003, AC-005
- [x] T12 — 设计 5 分钟项目演示和程序结构说明视频脚本 · covers: AC-001, AC-003
- [x] T13 — 设计参考文献方向和不少于 10 篇参考文献补齐规则 · covers: AC-001, AC-002
- [x] T14 — 编写完整报告计划书，包含章节大纲、每章资料来源、待补材料、写作注意事项和验收清单 · covers: AC-001, AC-002, AC-003, AC-004, AC-005
- [x] T15 — 安全与真实性检查，确认计划书不要求泄露密钥、敏感数据或描述未实现功能 · covers: AC-003, AC-005
- [x] T16 — Runtime verification note: no implementation code changed; Maven test was not required for this documentation-only task
- [x] T17 — Verify ACs: update `passes` to `true` in spec.md for each passing criterion
- [x] T18 — Update `docs/4-tasks/CURRENT_PLAN.md` — mark this task complete when the report plan is finished

## Dependencies
- T2 through T6 depend on T1 because docs 对齐必须先以代码事实为准。
- T8 depends on T7 because the course template defines the upper bound of report content.
- T9 through T14 depend on aligned docs from T2 through T6.
- T15, T16, T17, and T18 require the plan document to be complete.

## Blockers
- None currently.
