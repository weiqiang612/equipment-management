# TASK-003: Frontend Auth and Menu RBAC

**Status**: Completed
**Created**: 2026-06-10
**Feature dir**: `docs/3-tasks/features/TASK-003-frontend-auth-and-menu-rbac/`

## Objective
实现前端的登录与注册界面，编写客户端 Token 的拦截和响应流转逻辑，并根据用户的角色在前端路由和导航侧边栏上实现严谨的 RBAC 访问权限拦截。

## Scope

### In scope
- 在前端 `src/api` 下新建 `user.js` 网络请求文件，定义登录 (`/users/login`)、注册 (`/users/register`)、用户列表获取及角色修改的请求函数。
- 修改 [request.js](file:///d:/project/equipment-management/equipment-web/src/utils/request.js)：
  - **请求拦截**：读取本地 Token 自动放入 `Authorization` 或 `token` 请求头。
  - **响应拦截**：遇到 401 报错弹出气泡提示“登录已失效，请重新登录”，清除本地 Token 缓存并重定向至 `/login`。
- 新建 [Login.vue](file:///d:/project/equipment-management/equipment-web/src/views/Login.vue) 登录界面，UI 对齐 Element UI 朴素白灰科技蓝风格，支持表单前端校验。
- 新建 [Register.vue](file:///d:/project/equipment-management/equipment-web/src/views/Register.vue) 注册界面，支持用户名、真实姓名、密码、确认密码，注册成功弹出提示并延时 1.5 秒后跳转到 `/login`。
- 新建 [UserManage.vue](file:///d:/project/equipment-management/equipment-web/src/views/UserManage.vue) 用户管理页，供管理员（`role=3`）展示账号列表、下拉修改角色。
- 修改 `router/index.js` 添加全局路由守卫：
  - 未登录（无 Token）拦截除 `/login` 和 `/register` 外的所有访问。
  - 登录后，拦截普通用户越权访问 `/user-manage` 等高权页面（若不满足 `meta.roles` 校验，则重定向至 403 页面）。
- 修改 Sidebar 侧边栏，根据当前登录用户的 `role` 动态过滤隐藏无权限的菜单项。

### Out of scope
- 后端 JWT 令牌下发及用户表校验接口。
- 物理数据库表结构与初始数据的变更。
- 数据折旧等其他非鉴权业务的前端界面开发。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "登录页面前端校验、Token 存储与成功跳转",
    "steps": [
      "访问 /login，输入空账密提交",
      "Verify: 前端阻断提交，并显示输入项校验提示",
      "输入正确账号密码提交，验证后端返回成功",
      "Verify: 浏览器成功在 LocalStorage 中存入 Token 字段，并重定向至后台首页，显示用户真实姓名"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "注册页面表单验证与成功延时跳转",
    "steps": [
      "访问 /register 页面，输入不一致的确认密码",
      "Verify: 前端阻断提交，提示两次密码不一致",
      "输入正确的用户名、真实姓名、两次一致的密码提交",
      "Verify: 弹出“注册成功，即将为您跳转到登录页”提示，等待 1.5 秒后页面自动重定向到 /login 登录页"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "全局路由守卫拦截与放行白名单",
    "steps": [
      "清除本地 LocalStorage 中的 Token，手动在地址栏输入访问 /equipment (设备列表)",
      "Verify: 页面被路由守卫强制重定向回 /login",
      "未登录时在地址栏输入 /register",
      "Verify: 正常进入注册页，不受拦截影响"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "security",
    "description": "侧边栏菜单 RBAC 动态过滤与越权地址拦截",
    "steps": [
      "使用普通操作员 test_op (role=0) 的账户登录系统",
      "Verify: 侧边栏仅显示“我的设备、报修申请”，隐藏“设备台账”与“用户管理”等菜单",
      "在地址栏中手动输入访问 /user-manage",
      "Verify: 路由守卫检测其无管理员权限，自动拦截并重定向到 403 页面"
    ],
    "passes": true
  }
]
```

## Notes
- 前端界面的组件命名必须符合 Vue2 开发规范，不能使用单单词命名（例如使用 `UserManage.vue` 代替 `User.vue`），符合 `never-do.md` 约束。
- 动态菜单和角色拦截以本地存储中的 `role` 值为唯一校验依据。
