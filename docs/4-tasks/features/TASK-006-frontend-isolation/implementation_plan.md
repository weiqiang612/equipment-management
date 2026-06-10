# 实施计划 - 前端数据隔离与操作隔离改造

简要描述问题、背景上下文以及此更改所完成的工作。

## 需要用户评审的内容
> [!IMPORTANT]
> 1. 为了实现“资产管理员指派维修工下拉选择”，我们需要在后端 `UserController` 额外新增获取所有维修工列表的接口（`GET /users/maintainers`），因为原有的 `GET /users` 接口只有系统管理员（`role=3`）可以访问，资产管理员（`role=2`）请求会返回 403 权限不足。
> 2. 原检修记录中，指派维修工是直接输入文本名字，改造后将在检修人指派弹窗时从下拉框中选择维修工 realName，并在指派时将 realName 回传提交。

## 待解决问题 (Open Questions)
> [!WARNING]
> 目前没有待解决的阻碍性问题。

## 建议的更改

### 1. 后端接口支持
#### [MODIFY] [UserController.java](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment_system_management/src/main/java/com/weiqiang/controller/UserController.java)
- 新增 `GET /users/maintainers` 路由，允许所有已登录的用户（尤其是资产管理员 `role=2`）获取角色为 1 的维修工用户列表。

### 2. 前端路由与菜单权限收紧
#### [MODIFY] [router/index.js](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment-web/src/router/index.js)
- 将设备调拨记录（`/equipment/transfer`）、设备报废记录（`/equipment/scrap`）、分类管理（`/category`）和单位管理（`/department`）的准入角色 `roles` 收紧为 `[2, 3]`。

#### [MODIFY] [App.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment-web/src/App.vue)
- 调整侧边栏结构，通过 `v-if="role === 2 || role === 3"` 限制调拨记录、报废记录、分类管理和单位管理菜单的显示范围，为维修工（1）和操作员（0）隐藏这些菜单。

### 3. 前端表格操作隔离与指派下拉选择器改造
#### [MODIFY] [user.js](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment-web/src/api/user.js)
- 新增 `getMaintainers()` API 请求方法。

#### [MODIFY] [Equipment.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment-web/src/views/Equipment.vue)
- 在组件 `created()` 生命周期中获取当前用户的角色 `role` 和 `realName`。
- 在“新增设备”按钮和操作列上增加 `v-if` 判断：系统管理员（3）登录时，隐藏“新增设备”按钮和整列“操作”，确保其只读属性。
- 调整操作列中的按钮显示：资产管理员（2）可见“编辑”按钮和“更多”下拉菜单（包含维修、调拨、报废、删除）；操作员（0）和维修工（1）不可见“编辑”与下拉菜单，仅可见“维修”按钮。
- 将“设备检修登记”弹窗中的检修人手动输入框，改造成 `<el-select>` 下拉选择器，在弹窗加载时调用 `getMaintainers()` 接口加载数据。

#### [MODIFY] [MaintenanceRecord.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/caf70458-27f9-4d7b-83c7-976738733a7d/.system_generated/worktrees/subagent---------FrontendDeveloper-656ec367/equipment-web/src/views/MaintenanceRecord.vue)
- 在组件 `created()` 生命周期中获取当前用户的角色 `role` 和 `realName`。
- 针对系统管理员（3）通过 `v-if="role !== 3"` 整体隐藏“操作”列及新增按钮（如果有的话）。
- 针对维修工（1）进行“修改”按钮的操作隔离：仅当其为工单负责人（`scope.row.maintPerson === realName`）时可见“修改”按钮，否则隐藏。其他工程师的记录隐藏“修改”与“删除”；仅资产管理员（2）可见“删除”按钮。

## 验证计划
### 自动化测试
1. 在后端编译并执行单元测试，确认新增接口不破坏现有功能：
   `cd equipment_system_management && mvn test`

### 手动验证
1. 启动后端和前端服务。
2. 登录不同的角色账号并进行以下校验：
   - **操作员 (0)**：进入页面，确认侧边栏只显示“我的设备”和“报修申请”。在设备台账表格的操作列中仅能看到“维修”按钮。确认强行访问调拨、报废、分类和单位路由时被重定向到 `/403`。
   - **维修工 (1)**：确认侧边栏隐藏调拨记录、报废记录、分类管理和单位管理。在检修记录页面，定位到非本人负责的工单，确认“修改”和“删除”按钮不可见；定位到本人负责的工单，确认“修改”按钮可见。强行访问限权路由时被拦截重定向到 `/403`。
   - **资产管理员 (2)**：在设备台账中点击某设备进行报修登记，确认检修人输入框已经变成下拉选择器，并正确加载了系统内所有维修工程师。操作列中能够看到“编辑”以及“更多”（包含维修、调拨、报废、删除设备）。
   - **系统管理员 (3)**：进入设备台账和检修记录页面，确认“操作”列已被完全物理隐藏，页面上的“新增设备”等写操作按钮也已隐藏。
