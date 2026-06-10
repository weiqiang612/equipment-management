# TASK-005: 后端权限控制与状态流转校验 — 任务列表

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- 在数据库表结构修改时，优先为 `sys_user` 引入物理部门外键，并为 `maintenance_record` 引入维修工 ID 外键，建立严密的数据参照关系。
- 采用自定义注解 `@RequiresRoles` 配合 AOP 切面 `SecurityAspect` 拦截越权访问，将鉴权与业务解耦。
- 在操作员查询数据时，利用 BaseContext 提取当前用户的用户名和部门，在 SQL 动态条件中完成数据隔离过滤。
- 对报废状态做生命周期强拦截，防止已销户设备继续参与流转。

## Progress

- [x] T1 — 更新 API 契约设计：更新 `docs/1-standards/design/api_contract.md` 补录 403 越权响应及相关参数规范 · covers: {AC-006}
- [x] T2 — 升级 DB Schema 并编写物理表结构变更脚本：
  - 更新 `docs/1-standards/design/db_schema.md`。
  - 编写 DDL 升级脚本：为 `sys_user` 添加 `unit_code` 关联外键；为 `maintenance_record` 添加 `maint_person_id` 关联外键。
  - 在 MySQL 中执行表结构升级 · covers: {AC-004, AC-006}
- [x] T3 — 新增自定义注解与异常类：创建 `RequiresRoles` 注解以及 `ForbiddenException` 异常类 · covers: {AC-001, AC-002}
- [x] T4 — 实现 BaseContext 线程上下文及 AOP 拦截：
  - 实现 `BaseContext` 工具类（基于 ThreadLocal）。
  - 在 `LoginCheckInterceptor` 校验成功后解析 Claims 并写入 `BaseContext`，并在请求结束时释放。
  - 实现 `SecurityAspect` 切面，对带有 `@RequiresRoles` 注解的方法拦截，角色不匹配时抛出 `ForbiddenException` · covers: {AC-001, AC-002}
- [x] T5 — 配置全局异常处理：在 `GlobalExceptionHandler` 中拦截 `ForbiddenException`，设置 HTTP 状态码为 403，并返回统一 Result 包体 · covers: {AC-001, AC-002}
- [x] T6 — 控制器方法鉴权接入：在设备、维保、调拨、报废、主数据及备份还原 Controller 上加入 `@RequiresRoles` 注解进行写限制 · covers: {AC-001, AC-002}
- [x] T7 — 设备与检修列表后端数据隔离实现：
  - 在 `EquipmentServiceImpl` 动态查询中获取 `BaseContext` 中的用户。若为操作员（0），SQL 拼接强行限定：`custodian = 当前用户名 OR (custodian IS NULL AND unit_code = 当前用户部门)`。
  - 在 `MaintenanceRecordServiceImpl` 中，若为操作员（0），查询限定为 `reporter = 当前用户名` · covers: {AC-004}
- [x] T8 — 设备流转与约束控制实现：
  - 在 `TransferRecordServiceImpl` 设备调拨业务逻辑中，成功保存调拨记录后，自动将设备的 `custodian` 变更为 `NULL`。
  - 在 `MaintenanceRecordServiceImpl` 登记报修时，若为操作员操作，强校验设备保管人是自己；强制设置 `reporter` 为当前登录人，状态设为 `0`；设备状态变更为 `维修`。
  - 在登记维修结果时，校验当前用户角色为 1 或 2，且工单当前状态为 1 且设备在维修状态。
  - 在调拨、报修、设备信息更新接口入口处，强校验该设备当前状态若为 `'报废'`，则直接抛出业务异常 · covers: {AC-003, AC-005, AC-006}
- [x] T9 — 主数据及用户级联删除安全拦截：
  - 在 `CategoryServiceImpl` 和 `DepartmentServiceImpl` 删除逻辑中，前置校验是否存在关联设备，存在则拦截。
  - 在 `UserServiceImpl` 删除用户逻辑中，前置校验用户是否有关联的保管资产或未完结工单，若有则拦截 · covers: {AC-007, AC-008}
- [x] T10 — 编写集成与流转测试用例：编写 Service 层与 Controller 层的测试类，模拟不同角色鉴权，验证 P0/P1/P2 的全部越权和隔离场景 · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008}
- [x] T11 — 运行后端测试：执行 `cd equipment_system_management && mvn test` 保证所有测试通过 · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008}
- [x] T12 — 验证 ACs 状态：手动验证各 AC 并在 `spec.md` 中更新 passes 状态为 true · covers: {AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008}
- [x] T13 — 更新进度规划：修改 `docs/3-tasks/CURRENT_PLAN.md` 标记本任务完成 · covers: {AC-006}

## Dependencies
- T2 requires T1 (必须先定好 DDL 及外键契约才能修改数据库)
- T3 requires T2
- T4 requires T3
- T5 requires T4
- T6 requires T5 (AOP 拦截逻辑与全局异常拦截就绪后，控制器才可应用注解)
- T7 requires T4 (数据隔离需要依赖 BaseContext 提供的当前线程用户属性)
- T8 requires T4 (状态流转需要 BaseContext 获取操作者，且依赖 T2 升级的外键字段)
- T9 requires T4
- T10 requires T6, T7, T8, T9
- T11, T12, T13 require T10

## Blockers
