# TASK-002: Backend Auth and API

**Status**: Completed
**Created**: 2026-06-10
**Feature dir**: `docs/3-tasks/features/TASK-002-backend-auth-and-api/`

## Objective
实现后端的 JWT 令牌生成与验证工具、登录鉴权 Web 拦截器，以及登录、注册、修改角色、用户查询的 API 接口，构筑系统安全的网络边界。

## Scope

### In scope
- 编写 [UserDao.java](file:///d:/project/equipment-management/equipment_system_management/src/main/java/com/weiqiang/dao/UserDao.java) 继承 `BasicDao<User>`。
- 引入并编写 `JwtUtils.java` 实体工具，令牌有效期 12 小时，Payload 存放 id, username, role 属性。
- 编写 `LoginCheckInterceptor.java` 拦截非白名单请求，从 Header 提取 JWT 验证并拦截非法调用。
- 注册拦截器并配置白名单，放行 `POST /users/login` 与 `POST /users/register` 接口。
- 编写 `UserController` 和 `UserService` 提供登录校验（MD5密码比对）、注册查重（已存在用户名则返回 Result.error 且状态为 0）、获取用户列表与修改用户角色 API（仅限系统管理员 role=3）。

### Out of scope
- 前端登录与注册页面 UI 样式及交互（在前端任务中处理）。
- 前端路由拦截与动态侧边栏的实现（在前端任务中处理）。
- 其他业务 Controller（如设备、报修）的具体角色防越权控制。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "登录接口安全校验与 JWT 下发",
    "steps": [
      "使用正确账号密码向 POST /users/login 发送请求",
      "Verify: 接口返回 Result.code = 1 且 data 中携带有效的 JWT token",
      "使用错误密码或不存在的账号向 POST /users/login 发送请求",
      "Verify: 接口拦截并返回 Result.code = 0 且带有多余的账号密码错误提示"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "注册接口防重校验与默认操作员角色",
    "steps": [
      "向 POST /users/register 发起新用户 test_new 注册请求",
      "Verify: 注册成功，且通过数据库确认 test_new 密码为密文，role 强制默认为 0 (操作员)",
      "再次使用 test_new 向 POST /users/register 发起注册",
      "Verify: 注册被业务拦截，返回 Result.code = 0，且提示“用户名已存在”"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "security",
    "description": "安全拦截器拦截与白名单放行",
    "steps": [
      "不带 JWT Token 时，向任意受保护接口 (如 GET /equipments) 发起请求",
      "Verify: 接口被拦截，返回 HTTP 401 状态码或带有未登录的 Result.error 提示",
      "不带 JWT Token 时，向 POST /users/login 或 POST /users/register 发起请求",
      "Verify: 接口正常处理，无拦截拦截现象"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "functional",
    "description": "系统管理员专用的用户列表获取与角色变更 API",
    "steps": [
      "以 admin (role=3) 的 Token 发起 PUT /users/role 接口，修改某用户角色",
      "Verify: 角色修改成功，返回 code=1 且数据库中 role 字段同步更新",
      "以 operator (role=0) 的 Token 发起上述角色修改接口或获取用户列表接口",
      "Verify: 接口拦截并返回 403 权限不足"
    ],
    "passes": true
  }
]
```

## Notes
- 引入的 JWT 依赖包优先使用项目原有的依赖（如 `jjwt`），并在 Maven 编译时自动加载。
- 密码加解密及验证强制使用 MD5 工具进行处理，密码禁止以明文形式出现在控制台日志中。
