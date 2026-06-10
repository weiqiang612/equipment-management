# TASK-002: Tasks

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- **失效时长（12小时）**：Token 有效期配置为 12 小时（对应决策 Q1: B），在保证安全的前提下提供良好的开发与调试周期。
- **用户名查重校验**：在注册用户时先执行 SELECT 查询（对应决策 Q2: A），若存在该用户名则返回友好提示信息，不依赖数据库硬性拦截。
- **密码密文存储**：使用规范的 MD5 进行密码哈希，确保账户防窃取安全性。

## Progress

- [x] T1 — 创建后端用户数据接口类 `UserDao.java` 继承 `BasicDao<User>` · covers: AC-001, AC-002, AC-004
- [x] T2 — 引入并编写 JWT 加解密工具类 `JwtUtils.java`（失效期12小时） · covers: AC-001, AC-003
- [x] T3 — 编写安全拦截器 `LoginCheckInterceptor.java` 并配置 Spring MVC 放行白名单 · covers: AC-003
- [x] T4 — 编写账号登录注册核心业务逻辑类 `UserService.java`（含查重与密码 MD5 转换） · covers: AC-001, AC-002, AC-004
- [x] T5 — 编写前端交互路由控制类 `UserController.java` (暴露 login、register、用户修改及查询 API) · covers: AC-001, AC-002, AC-004
- [x] T6 — 运行测试套件 `cd equipment_system_management && mvn test` 进行完整性回归验证 · covers: AC-001, AC-002, AC-003, AC-004
- [x] T7 — 验证 ACs：更新 `spec.md` 中所有验证通过的 criteria 的 `passes` 为 `true`
- [x] T8 — 更新 `docs/3-tasks/CURRENT_PLAN.md` 标记本任务完成

## Dependencies
- T2, T3, T4 依赖 T1
- T5 依赖 T2, T3, T4
- T6, T7, T8 依赖 T1-T5 开发完毕

## Blockers
<!-- 无阻碍物 -->
