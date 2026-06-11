# [任务演示]

修复了系统“用户角色”与“所属单位（部门）”之间的关联管理遗漏，并基于最新的前后端代码通过了完整的单元测试和 Chrome MCP 端到端验证。

## 更改内容
- **API 契约与数据库模型**：
  - 更新 [api_contract.md](file:///D:/project/equipment-management/docs/2-designs/api_contract.md) 与 [db_schema.md](file:///D:/project/equipment-management/docs/2-designs/db_schema.md)，同步规范 `unitCode` 属性与 `sys_user` 字段。
  - 创建并编写升级注释脚本 [upgrade_v4.sql](file:///D:/project/equipment-management/equipment_system_management/src/main/resources/upgrade_v4.sql)。
- **后端核心业务逻辑**：
  - 重构 `UserDao.java` 与 `UserServiceImpl.java`，使用 MyBatis 扩展用户角色/所属单位联合更新操作，并在 `UserServiceImpl` 中引入常量代替魔法数。
  - 新建 `UserVO.java` 对用户敏感密码哈希进行过滤脱敏，修复了安全漏洞。
  - 在 `register` 与 `updateRole` 中引入部门非空校验（除系统管理员外强制绑定有效单位），并基于事务实现了部门变更时的“名下保管设备强阻断校验”，抛出 `BusinessException`。
- **单元集成测试**：
  - 修正了 3 个集成测试类中因数据库结构变动引起的用户单位初始依赖，并验证通过。
- **前端页面及交互优化**：
  - 重构 `UserRegister.vue`：引入部门列表，增设所属单位必填选择项，并支持格式校验阻断。
  - 重构 `UserManage.vue`：列表新增单位展示，允许管理员在后台直接分配/修改用户的所属单位，并在选择“系统管理员”角色时，联动清空单位、置为全局角色且置灰禁用单位选择器。

## 完成任务的思路
1. **职责分离与数据完整**：系统管理员（全局角色）不应从属于任何具体的设备使用单位，因此必须强制将其 unit_code 置为 `NULL` 并锁定输入；而其余非全局业务角色必须拥有明确的责任归属部门，确保设备领用审批与流转中的数据隔离和归口管理正常进行。
2. **资产安全与业务完整**：如果用户名下仍有未交接或未退还的保管设备，则其不能直接调离当前单位。使用强阻断校验防范了“人走机留、坏账空饷”的安全资产风险。
3. **安全脱敏与合规**：用户敏感的密码哈希字段在原有的 `/users` 列表中被直接传给前端，导致严重的敏感信息泄露。本次重构中通过引入 `UserVO` 进行了严格脱敏，并使用常量重构魔法数，提高可维护性。

## 测试内容
- **集成测试**：执行 `mvn test`，5 个核心集成测试（包括设备领用、用户管理和流程安全性测试）全部通过。
- **端到端（E2E）测试场景**：
  1. 用户自助注册未选择单位时的校验阻断；
  2. 填写正确部门完成注册；
  3. 后台管理员成功为该用户变更所属单位；
  4. 当该用户正保管设备时，尝试变更其部门，确认后端执行阻断报错；
  5. 修改其系统角色为系统管理员，验证所属单位联动清空为 NULL 并置灰禁用。

## 验证结果

以下是使用 Chrome MCP 工具进行端到端验证的页面截图：

### 1. 用户注册强校验阻断
若用户在自助注册时未选择所属部门，表单拦截提交并弹出红色校验提示：
![用户注册未选单位阻断](C:/Users/Ethan/.gemini/antigravity/brain/c057941d-e60c-47c8-af35-5e91d2c34029/e2e_register_block.png)

### 2. 正常注册并跳转
填入完整信息并选择“测试单位A”后，成功注册并跳转回登录页：
![用户注册成功跳转](C:/Users/Ethan/.gemini/antigravity/brain/c057941d-e60c-47c8-af35-5e91d2c34029/e2e_register_success.png)

### 3. 后台管理员修改所属单位
管理员登录后，将新注册的 `test_claim_e2e_1` 单位变更为“测试单位B”，修改成功：
![后台修改用户单位成功](C:/Users/Ethan/.gemini/antigravity/brain/c057941d-e60c-47c8-af35-5e91d2c34029/e2e_dept_change_success.png)

### 4. 设备保管人部门变更强阻断
通过 SQL 构造物理设备 `EQ2024001` 由 `test_claim_e2e_1` 保管。尝试在后台将其调拨回“测试单位A”，后端强阻断校验并友好抛出异常信息：“操作失败：该用户尚有未清退的保管设备，请先去设备管理处退还或交接设备！”：
![保管设备部门变更强阻断](C:/Users/Ethan/.gemini/antigravity/brain/c057941d-e60c-47c8-af35-5e91d2c34029/e2e_dept_block.png)

### 5. 系统管理员角色置空与置灰联动
清退设备后，管理员将该用户角色修改为“系统管理员”。确认其单位展示联动重置为“全局角色”，且“单位分配操作”下拉框置灰禁用不可点：
![管理员角色置空置灰联动](C:/Users/Ethan/.gemini/antigravity/brain/c057941d-e60c-47c8-af35-5e91d2c34029/e2e_role_admin_clear.png)
