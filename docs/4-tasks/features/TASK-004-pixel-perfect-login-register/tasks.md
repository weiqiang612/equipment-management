# TASK-004: Tasks

**Spec**: `spec.md`
**Status**: In Progress

## Key decisions
- **切图与分栏方案**：登录卡片通过 `flex` 将 `923px` 容器分为左侧 `login_bg.png` (462px) 和右侧白色表单 (461px)；注册页采用全屏左右 Flex 两栏（50.6% : 49.4%），左侧完全由 `register_bg.png` 铺满，实现无缝像素对齐。
- **字体引入**：在前端公共 HTML 中引入 Google Fonts `Outfit`，并设定为两页面的首选字体。
- **非路由链接处理**：对登录页的忘记密码和帮助链接绑定 Element UI 的 `$message` 弹窗，提醒用户联系管理员。

## Progress

- [x] T1 — 引入 Outfit 字体 — `public/index.html` · covers: AC-001, AC-003
- [x] T2 — 重构登录页 HTML 与路由/事件逻辑 — `UserLogin.vue` · covers: AC-001, AC-002
- [x] T3 — 编写登录页 CSS 样式（居中卡片、圆角阴影、按钮和输入框高精还原）— `UserLogin.vue` · covers: AC-001
- [x] T4 — 重构注册页 HTML 与字段表单逻辑 — `UserRegister.vue` · covers: AC-003
- [x] T5 — 编写注册页 CSS 样式（全屏分栏比例、右上角链接定位、按钮和输入框高精还原）— `UserRegister.vue` · covers: AC-003
- [x] T6 — 联调校验与路由跳转逻辑（账号密码验证、成功跳转、提示信息）— `UserLogin.vue`, `UserRegister.vue` · covers: AC-004
- [x] T7 — 运行 `cd equipment-web && npm run build` — 确保前端打包成功无编译错误
- [x] T8 — 验证 ACs — 更新 `spec.md` 中各项 `passes` 为 `true`
- [x] T9 — 更新 `docs/3-tasks/CURRENT_PLAN.md` — 标记本任务完成

## Dependencies
- T2, T3, T4, T5, T6 requires T1
- T7, T8, T9 requires T1–T6
