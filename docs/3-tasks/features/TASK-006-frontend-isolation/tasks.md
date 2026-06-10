# TASK-006: 前端数据隔离与操作隔离 — 任务列表

**Spec**: `spec.md`
**Status**: In Progress

## Key decisions
- 在路由配置中收紧调拨、报废、分类和单位管理的 meta.roles 为仅管理员角色，建立前端首道防御。
- 采用 Element UI Table 的 `v-if` 条件渲染动态控制“操作”列在审计管理员（3）登录时的整体隐藏。
- 维修工仅对其名下的检修记录展示“修改”按钮，实现最小操作授权原则。

## Progress

- [ ] T1 — 检查并确认前端调用 API 接口契约：检查 `api/equipment.js` 等接口定义与后端一致性 · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006}
- [ ] T2 — 修正路由拦截限制：修改 `router/index.js`，收紧调拨、报废、分类及单位页面的 roles 为 `[2, 3]` · covers: {AC-001}
- [ ] T3 — 升级侧边栏动态菜单控制：修改 `App.vue` 侧边栏结构，限制维修工（1）和操作员（0）的菜单显示范围 · covers: {AC-002}
- [ ] T4 — 改造设备台账页面操作栏显隐：
  - 修改 `views/Equipment.vue`。
  - 针对系统管理员（3）通过 `v-if="role !== 3"` 整体隐藏“操作”列及新增按钮。
  - 针对操作员（0）操作列只显示“维修”选项，隐藏其余所有高权写操作按钮。
  - 针对资产管理员（2）保留完整的增删改查及调拨、报废、删除按钮 · covers: {AC-003, AC-004}
- [ ] T5 — 改造检修记录页面操作栏及动作控制：
  - 修改 `views/MaintenanceRecord.vue`。
  - 针对系统管理员（3）通过 `v-if="role !== 3"` 整体隐藏“操作”列及新增按钮。
  - 针对维修工（1）的“修改”按钮增加控制 `v-if="role === 2 || (role === 1 && scope.row.maintPerson === realName)"`（即仅能修改自己负责的工单），其余行按钮隐藏 · covers: {AC-004, AC-005}
- [ ] T6 — 改造指派维修工下拉选择器组件：
  - 新增或调整获取维修工列表接口。
  - 在 `views/Equipment.vue`（或指派派单组件）中，将手动填名字文本的 Input 框替换为 `<el-select>` 下拉选择框。
  - 在弹窗加载时请求后端获取所有角色为 `1` 的用户，将 `maint_person_id` 和 `realName` 绑定至下拉框中，指派时回传 ID 及姓名 · covers: {AC-006}
- [ ] T7 — 多账户局部部署与手动联合功能校验：登录各个角色账号（0, 1, 2, 3）在浏览器中多视角验证路由拦截、侧边栏菜单隐藏、表格操作列显隐、指派下拉框及他人检修记录修改限制 · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006}
- [ ] T8 — 验证 ACs 状态：手动验证各 AC 并在 `spec.md` 中更新 passes 状态为 true · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006}
- [ ] T9 — 更新任务规划进度：修改 `docs/3-tasks/CURRENT_PLAN.md` 标记本任务完成

## Dependencies
- T2 requires T1
- T3 requires T2
- T4 requires T3
- T5 requires T3
- T6 requires T4, T5
- T7 requires T6
- T8, T9 require T7

## Blockers
