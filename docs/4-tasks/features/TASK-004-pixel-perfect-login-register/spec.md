# TASK-004: Pixel-perfect Login and Register Pages

**Status**: Draft
**Created**: 2026-06-10
**Feature dir**: `docs/3-tasks/features/TASK-004-pixel-perfect-login-register/`

## Objective
对前端的登录和注册界面做到像素级还原，完全一致，并确保原有的表单验证、接口认证及页面跳转逻辑正常运行。

## Scope

### In scope
- 引入 Google Fonts `Outfit` 字体并在登录和注册页面中全局使用，确保文本排版效果与设计稿一致。
- 登录页面重构为居中卡片布局，外层背景色为 `#f2f8f8`；卡片尺寸为 `923px * 658px`，圆角为 `12px` 并带微弱投影；卡片左侧为 `login_bg.png` (462px 宽)，右侧为表单 (461px 宽)。
- 登录页中的 "Forgot Password?" 与 "Need Help?" 链接在点击时，弹出提示 "请联系系统管理员获取帮助"。
- 注册页面重构为全屏左右 Flex 两栏（左栏 50.6% 使用 `register_bg.png`，右栏 49.4% 为白色背景表单）；右上角绝对定位放置 "Already have an account? Log In" 链接。
- 将输入框前缀图标绑定为 Element UI 内置图标，占位符文字完全贴合设计图，按钮圆角设为两端半圆，背景色统一采用亮绿色（符合设计稿 `#0fad5b`）。
- 保留现有的 API 请求及成功跳转逻辑。

### Out of scope
- 修改后端认证 API 或数据库 Schema。
- 新增非设计稿中的第三方认证或验证码功能。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "登录界面完美还原，包含外层 `#f2f8f8` 背景、`923px * 658px` 居中卡片（12px 圆角及细微阴影）、左半部分 `login_bg.png` 占 `462px` 宽度、右半部分白色背景表单占 `461px` 宽度。",
    "steps": [
      "浏览器访问前端 `/login` 路径",
      "检查卡片是否在屏幕中央水平垂直居中",
      "验证左侧是否正确加载 3D 齿轮插画背景图，右侧是否为带有 brand 标题和表单的白色容器",
      "Verify: 登录页面整体视觉与 login_page_mockup_1781065853532.png 图像完全一致"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "登录页上的忘记密码和帮助链接在点击时能够正常触发提示消息。",
    "steps": [
      "访问登录页，点击 Password 输入框上方的 'Forgot Password?' 链接",
      "验证是否弹出消息提示 '请联系系统管理员获取帮助'",
      "点击底部的 'Need Help?' 链接",
      "Verify: 页面上弹出消息提示 '请联系系统管理员获取帮助'"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "注册界面完美还原，包含全屏 Flex 两栏布局、左栏宽度占比 `50.6%` 并以 `register_bg.png` 作为背景、右栏宽度占比 `49.4%` 且为白底表单、右上角有绝对定位的 'Already have an account? Log In' 链接。",
    "steps": [
      "浏览器访问前端 `/register` 路径",
      "检查左半部分是否显示 3D 流水线插画背景图且宽度占 50.6%，右半部分是否为白底表单且宽度占 49.4%",
      "检查右上角是否包含 'Already have an account? Log In' 链接",
      "Verify: 注册页面整体视觉与 register_page_final_edit_1781074961797.png 图像完全一致"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "integration",
    "description": "登录与注册页面的表单验证、API 交互及路由跳转逻辑完备且正确。",
    "steps": [
      "访问登录页，输入错误账号密码点击 Log In，验证校验提示正常",
      "输入正确账号密码点击 Log In，验证是否成功调用 API 并在 LocalStorage 写入 token 和角色信息，随后成功跳转至后台首页",
      "访问注册页，点击右上角 'Log In' 验证是否跳转到登录页",
      "输入用户名、姓名、两次不一致密码，验证注册校验提示 '两次输入密码不一致'",
      "输入合法注册数据点击 Create Account，验证是否调用 API 并显示 '注册成功'，在 1.5s 后自动跳转到登录页",
      "Verify: 现有认证流运行无误，无逻辑缺失"
    ],
    "passes": true
  }
]
```

## Notes
- 引入的 Outfit 字体链接地址为 `https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap`
- 注册与登录界面的按钮背景色和悬浮色：`#0fad5b` / `#0d964e`
- 按钮圆角：`border-radius: 23px;` (完美半圆角)
- 输入框圆角：`border-radius: 8px;`，边框颜色：`#e2e8f0`，聚焦时边框色为 `#0fad5b`
