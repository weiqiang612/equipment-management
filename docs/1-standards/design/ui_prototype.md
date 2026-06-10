# 国家标准设备管理系统 (EMS) - 原型与 UI 设计规范 (ui_prototype.md)

## 1. 主视觉与色彩规范
本系统采用 **科技工业蓝灰** 视觉体系，界面要求清晰、现代且符合国家标准软件的使用习惯。
*   **主色 (Primary)**：`#409EFF` (Element UI 经典蓝)
*   **成功色 (Success)**：`#67C23A` (用于“在用”设备状态、备份成功标识)
*   **警告色 (Warning)**：`#E6A23C` (用于“维修”设备状态、恢复确认提示)
*   **危险色 (Danger)**：`#F56C6C` (用于“报废”设备状态、删除设备、清除 Token 退出)
*   **侧边栏背景**：`#304156` (科技暗蓝灰)

---

## 2. 界面布局结构
系统采用经典的后台管理左右分栏布局：
1.  **左侧导航栏 (Aside)**：
    *   折叠/展开宽度：200px。
    *   **RBAC 动态隐藏**：依据当前用户的 `role` 渲染菜单。操作员只可见“我的设备”、“报修申请”；其他角色可见“设备资产管理”、“分类管理”、“单位管理”等。
2.  **右侧主区域**：
    *   头部 (Header)：面包屑导航、用户真实姓名欢迎词及当前角色 Tag 标签、退出登录按钮。
    *   主视图 (Main)：背景色 `#f0f2f5`，留白 20px，内部使用卡片组件 (`el-card`) 承载表格和检索表单。

---

## 3. Figma / 交互原型指针
*   **在线 Figma 设计稿链接**：`https://figma.com/file/equipment-management-system-v1` (示例)
*   **原型快照归档**：
    *   原始登录与注册界面的像素级设计快照保存在：
        *   登录界面：[login_page_mockup](file:///d:/project/equipment-management/login_page_mockup_1781065853532.png)
        *   注册界面：[register_page_final](file:///d:/project/equipment-management/register_page_final_edit_1781074961797.png)

---

## 4. UI 组件像素级还原准则
*   **组件命名**：Vue 组件文件命名一律使用大驼峰且不使用单个单词（如 `UserManage.vue` 而非 `User.vue`）。
*   **自适应设计**：表格和表单必须支持在 1280px 至 1920px 宽度下的自适应缩放，超出部分使用分页器拦截，禁止页面底端出现横向滚动条。
*   **操作反馈**：
    *   删除、恢复、退出等敏感操作必须使用 `this.$confirm` 弹出危险提示确认框。
    *   接口交互成功后必须触发 `this.$message.success` 进行吐司提示。
