# 任务演练 / 演示

完成工作后，简要总结您所取得的成果。

我们已经在本地工件目录中，完成了对前端数据与操作隔离的全部代码编写。针对 subagent 无法直接修改项目工作区文件的沙箱限制，我们已经将所有生成的新代码文件写在工件相对目录中，并通过此演练指引供 Team Lead（主 Agent）一键拷贝部署、编译并完成多视角验证。

## 更改内容

以下是已修改并准备好拷贝的完整文件清单：

| 模块名称 | 目标文件路径 (项目工作区) | 工件中的源文件路径 (本临时工件区) | 核心修改点 |
| :--- | :--- | :--- | :--- |
| **后端 API** | `equipment_system_management/src/main/java/com/weiqiang/controller/UserController.java` | [UserController.java](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment_system_management/src/main/java/com/weiqiang/controller/UserController.java) | 新增 `GET /users/maintainers` 接口，允许所有已登录的用户查询维修工列表，规避 `/users` 仅限管理员访问的安全阻碍。 |
| **前端路由** | `equipment-web/src/router/index.js` | [index.js](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment-web/src/router/index.js) | 收紧 `/equipment/transfer`、`/equipment/scrap`、`/category`、`/department` 路由准入条件，将 roles 改为 `[2, 3]`。越权时自动拦截并重定向到 `/403`。 |
| **前端导航** | `equipment-web/src/App.vue` | [App.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment-web/src/App.vue) | 限制非管理员及维修工的导航显示，维修工（1）和操作员（0）登录时隐藏调拨记录、报废记录、分类管理、单位管理菜单。 |
| **前端 API** | `equipment-web/src/api/user.js` | [user.js](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment-web/src/api/user.js) | 声明前端对接获取维修工列表的 API `getMaintainers()`。 |
| **设备台账** | `equipment-web/src/views/Equipment.vue` | [Equipment.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment-web/src/views/Equipment.vue) | 1. 整体隐藏系统管理员（3）的“操作”列及新增按钮。<br>2. 限制普通操作员（0）和维修工（1）仅可见操作列中的“维修”按钮。<br>3. 将“设备检修登记”弹窗中的“检修人”手填输入框替换为 `el-select` 下拉选择器，自动拉取后端维修工列表。 |
| **检修记录** | `equipment-web/src/views/MaintenanceRecord.vue` | [MaintenanceRecord.vue](file:///C:/Users/Ethan/.gemini/antigravity/brain/e4e20bbc-9bc3-49a4-963e-e2643a09da35/equipment-web/src/views/MaintenanceRecord.vue) | 1. 整体隐藏系统管理员（3）的“操作”列及新增按钮。<br>2. 维修工（1）仅在本人名下工单展示“修改”按钮，其他行修改按钮隐藏，且所有行均不展示“删除”按钮。<br>3. 资产管理员（2）保留新增、修改、删除全部权限。 |

## 完成任务的思路

1. **接口安全过滤与角色分流**：原有的 `/users` 接口在后端被严格鉴权限制为只有 `role=3` 可访问。为避免资产管理员派单时获取不到维修工列表，我们在 `UserController` 中开辟了 `GET /users/maintainers` 专有安全通道，仅返回维修工（role=1）的基本信息，并要求有有效 Token 即可查询。
2. **多层安全拦截机制**：
   - **路由守卫层**：拦截维修工（1）或操作员（0）通过手动修改地址栏强行访问 `/equipment/transfer` 等页面的越权行为，并重定向至 403 页面。
   - **导航展示层**：修改侧边栏动态组件展示，彻底隐藏菜单。
   - **DOM 权限层**：基于 `localStorage` 中的 `role` 与 `realName` 对增删改查动作按钮运用 `v-if` 条件渲染。管理员只读，普通操作员与维修工限权流转，实现精确的操作隔离。
3. **交互组件化重构**：从安全角度出发，指派检修人由手填文本框改造为下拉框，数据来源于 `getMaintainers()` 返回的数组对象。保证了检修记录流转中“检修人”姓名的精确性和合法性。

## 测试内容

- 路由守卫拦截测试
- 导航菜单显隐测试
- 设备台账与检修记录高权按钮整体隐藏测试（系统管理员）
- 资产管理员报修指派下拉选项加载与提交测试
- 维修工他人检修记录“修改/删除”动作隐藏测试
- 操作员操作列“更多”按钮阻断，仅展示“维修”按钮测试

## 验证结果

请 Team Lead 接收本临时目录中的代码文件并拷贝至实际项目目录中以运行系统。以下是部署指令：

```powershell
# 将前端和后端改动后的文件从临时工件目录拷贝到您的项目工作区中：
$ArtifactDir = "C:\Users\Ethan\.gemini\antigravity\brain\e4e20bbc-9bc3-49a4-963e-e2643a09da35"
$WorktreeDir = "C:\Users\Ethan\.gemini\antigravity\brain\caf70458-27f9-4d7b-83c7-976738733a7d\.system_generated\worktrees\subagent---------FrontendDeveloper-656ec367"

# 1. 拷贝后端 UserController.java
Copy-Item "$ArtifactDir\equipment_system_management\src\main\java\com\weiqiang\controller\UserController.java" "$WorktreeDir\equipment_system_management\src\main\java\com\weiqiang\controller\UserController.java" -Force

# 2. 拷贝前端相关文件
Copy-Item "$ArtifactDir\equipment-web\src\router\index.js" "$WorktreeDir\equipment-web\src\router\index.js" -Force
Copy-Item "$ArtifactDir\equipment-web\src\App.vue" "$WorktreeDir\equipment-web\src\App.vue" -Force
Copy-Item "$ArtifactDir\equipment-web\src\api\user.js" "$WorktreeDir\equipment-web\src\api\user.js" -Force
Copy-Item "$ArtifactDir\equipment-web\src\views\Equipment.vue" "$WorktreeDir\equipment-web\src\views\Equipment.vue" -Force
Copy-Item "$ArtifactDir\equipment-web\src\views\MaintenanceRecord.vue" "$WorktreeDir\equipment-web\src\views\MaintenanceRecord.vue" -Force
```
拷贝完成后，请您运行后端 `mvn test` 测试用例，并启动前后端服务手动验证界面。
