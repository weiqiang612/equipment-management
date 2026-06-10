# TASK-003: Tasks

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- **未登录拦截提示**：检测到 Token 过期时清除本地缓存并弹出气泡提示（对应决策 Q1: A），避免给用户带来卡死或莫名跳转的疑惑。
- **注册成功延时跳转**：用户注册成功后弹出气泡并延时 1.5 秒跳转登录页（对应决策 Q2: A），保证良好的视觉过渡和确认感。
- **动态菜单控制**：在 Sidebar 统一根据 LocalStorage 中的 `role` 值进行 `v-if` 条件隐藏。

## Progress

- [x] T1 — 在前端 `src/api` 下创建用户请求模块 `user.js`（实现登录、注册、修改角色 API） · covers: AC-001, AC-002, AC-004
- [x] T2 — 修改 Axios 封装类 `request.js`：请求拦截器附加 Token，响应拦截器截获 401 抛错清除 Token 并重定向 · covers: AC-001, AC-003
- [x] T3 — 编写 Vue2 登录页面组件 `Login.vue`（适配 Element UI 经典风格） · covers: AC-001
- [x] T4 — 编写 Vue2 注册页面组件 `Register.vue`（包含确认密码校验与延时重定向逻辑） · covers: AC-002
- [x] T5 — 编写 Vue2 用户角色分发管理页面 `UserManage.vue`（管理员专属） · covers: AC-004
- [x] T6 — 修改 `router/index.js`，添加路由守卫 `router.beforeEach`、白名单放行及页面角色 meta 拦截 · covers: AC-003, AC-004
- [x] T7 — 修改侧边栏导航组件（通常在 `Home.vue` 中），实现不同 `role` 下菜单项的动态隐藏 · covers: AC-004
- [x] T8 — 运行前端构建命令验证代码无打包/编译报错 · covers: AC-001, AC-002, AC-003, AC-004
- [x] T9 — 验证 ACs：更新 `spec.md` 中所有验证通过的 criteria 的 `passes` 为 `true`
- [x] T10 — 更新 `docs/3-tasks/CURRENT_PLAN.md` 标记本任务完成

## Dependencies
- T2, T3, T4, T5 依赖 T1
- T6, T7 依赖 T2-T5
- T8, T9, T10 依赖 T1-T7 开发完毕

## Blockers
<!-- 无阻碍物 -->
