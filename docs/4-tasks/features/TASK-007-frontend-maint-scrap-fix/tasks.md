# TASK-007: Tasks

**Spec**: `spec.md`
**Status**: In Progress

## Key decisions
- 维修工下拉列表采用 `maintPersonId` 作为绑定值进行传递，满足后端对 ID 的物理强校验；同时利用 `@change` 事件的 `handleMaintainerChange` 方法自动同步匹配姓名并写入 `maintPerson` 中，防止姓名参数在指派后丢失。
- 重构设备台账页面中的“报修申请登记”弹窗逻辑，将其正位为“故障申报”行为：操作员仅需录入“故障描述”与“报修日期”；将尚未发生的“检修人”、“检修费用”与“检修内容”等写回操作剥离。
- 修正 `ScrapRecord.vue` 中在执行 `validate` 和 `resetFields` 时存在的表单组件 ref 引用错误，统一命名为 `scrapForm`。

## Progress

- [ ] T1 — 修改 `MaintenanceRecord.vue` 以在修改/指派时绑定 `maintPersonId` 并联动同步 `maintPerson` 姓名 · covers: AC-001
- [ ] T2 — 重构 `Equipment.vue` 中的报修登记弹窗，提供 `faultDescription` 文本域并移除/隐藏检修费用、内容等字段 · covers: AC-002
- [ ] T3 — 修正 `ScrapRecord.vue` 表单提交与重置方法中对 ref 的引用，从 `postForm` 改为 `scrapForm` · covers: AC-003
- [ ] T4 — 运行后端 Maven 测试套件：`cd equipment_system_management && mvn test` · covers: AC-001, AC-002, AC-003
- [ ] T5 — 运行前端 Linter 静态检查：`cd equipment-web && npm run lint` · covers: AC-001, AC-002, AC-003
- [ ] T6 — 验证所有的验收标准，更新 `spec.md` 中的 `passes` 状态为 `true` · covers: AC-001, AC-002, AC-003
- [ ] T7 — 更新 `docs/4-tasks/CURRENT_PLAN.md` 并标记此 Feature 为完成 · covers: AC-001, AC-002, AC-003

## Dependencies
- T3, T4, T5, T6, T7 依赖于 T1 与 T2
- T6, T7 依赖于 T3, T4, T5
