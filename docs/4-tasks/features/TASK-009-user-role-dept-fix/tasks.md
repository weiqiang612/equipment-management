# TASK-009: 任务清单

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- **角色与单位混合绑定规则**：系统管理员 (`role=3`) 作为全局角色，其 `unit_code` 必须为 `NULL`；其余角色（操作员 `0`、维修工 `1`、资产管理员 `2`）注册和更新时强制要求绑定有效部门（非空且在 `department` 表中存在）。
- **部门调动资产强阻断**：当管理员更改用户的 `unitCode` 时，后端在事务中校验该用户当前正保管的设备数。如果数量大于 0，则中断更新并抛出业务异常，在前端友好提示用户清退设备。
- **平滑扩展接口入参**：在原有的 `PUT /users/role` 接口请求体中增加可选字段 `unitCode`，兼容旧前端，同时支持全量属性更新。

## Progress

- [x] T1 — 更新 API 契约：修改 `docs/2-designs/api_contract.md` 中 `/users/register` 和 `/users/role` 的参数约定，加入 `unitCode` 属性说明 · covers: {AC-004}
- [x] T2 — 更新数据库文档与创建迁移脚本：在 `docs/2-designs/db_schema.md` 中补充 `sys_user.unit_code` 字段描述，并在项目的数据库初始化 SQL 中编写对应的字段创建及约束 SQL（如果物理数据库还没有该字段） · covers: {AC-004}
- [x] T3 — 更新 User 实体与数据访问层：检查 `User.java` 以确保 `unitCode` 属性与数据库映射正常，在 `UserDao.java` 中重构更新方法以支持同时修改 `role` 和 `unit_code` · covers: {AC-001, AC-002}
- [x] T4 — 编写后端核心业务逻辑：在 `UserServiceImpl.java` 的 `register` 和 `updateRole` 中实现业务校验，包括必填校验、单位有效性校验、系统管理员自动清空单位，以及部门变更时的保管设备清退阻断逻辑 · covers: {AC-001, AC-002, AC-003}
- [x] T5 — 编写控制器接口校验：在 `UserController.java` 中接收完整参数，执行必要的入参格式校验 · covers: {AC-001, AC-002, AC-003}
- [x] T6 — 编写后端单元测试：在 `UserControllerTests.java` 或新测试类中编写单元测试，覆盖正常分配、管理员清空单位、保管设备阻断修改等测试场景 · covers: {AC-001, AC-002, AC-003}
- [x] T7 — 修改前端 API 请求：修改 `src/api/user.js`，将注册与修改角色的 API入参扩展以支持 `unitCode` 传参 · covers: {AC-001, AC-002}
- [x] T8 — 改造前端注册页面：修改 `src/views/UserRegister.vue`，引入获取部门列表逻辑，并在表单中新增“所属单位”的必填下拉选择框，注册提交时上传该字段 · covers: {AC-001}
- [x] T9 — 改造前端用户管理页面：修改 `src/views/UserManage.vue`，展示用户的“所属单位”，在列表中支持管理员下拉分配/修改用户的所属单位，并联动系统管理员角色的置空与禁用逻辑 · covers: {AC-002, AC-003}
- [x] T10 — 运行后端测试门禁：在后端目录下执行 `cd equipment_system_management && mvn test`，确保所有测试用例通过 · covers: {AC-001, AC-002, AC-003}
- [x] T11 — 手动验证验收标准：运行前后端，手动验证 AC-001、AC-002、AC-003，并在通过后将 `spec.md` 中的 `passes` 状态置为 `true` · covers: {AC-001, AC-002, AC-003, AC-004}
- [x] T12 — 更新系统总计划：更新 `docs/4-tasks/CURRENT_PLAN.md` 以归档此任务 · covers: {AC-004}

## Dependencies
- T2 依赖于 T1
- T3 依赖于 T2
- T4 依赖于 T3
- T5 与 T6 依赖于 T4
- T7 依赖于 T1
- T8 与 T9 依赖于 T7
- T10 依赖于 T3–T6
- T11 依赖于 T8, T9, T10
- T12 依赖于 T11

## Blockers
<!-- 暂无阻塞项 -->
