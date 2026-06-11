# TASK-007: Frontend Maintenance Data Flow and Scrap Form Fix

**Status**: Draft
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-007-frontend-maint-scrap-fix/`

## Objective
修复前端维保流程和报废表单的交互与对接 Bug。解决指派时由于缺少维修工物理 ID 而导致后端拦截报错、设备报修申请时强填未来字段却缺少故障描述的业务倒置，以及报废表单 ref 引用名称不一致导致 JS 运行时崩溃等核心缺陷，实现设备维保流转链路与报废流程在前端的完备性与闭环一致性。

## Scope

### In scope
1. **维保指派物理 ID 传参绑定**：
   - 修改 `MaintenanceRecord.vue` 维保修改/指派表单，将“检修人”下拉框 `<el-select>` 的 `v-model` 绑定到新增的 `form.maintPersonId` 字段。
   - 下拉选项 `<el-option>` 的值（`:value`）从维修工姓名变为物理 ID `item.id`。
   - 新增 `handleMaintainerChange` 方法，在选择维修工程师时，自动根据被选 ID 从 `maintainers` 列表中匹配对应的真实姓名并同步更新回回填字段 `form.maintPerson`（确保后端接收到的字符型姓名不缺失）。
   - 修改 `handleEdit` 加载回显逻辑，将后端返回 of `row.maintPersonId` 载入 `form.maintPersonId`。
2. **设备报修申请表单重构**：
   - 修改 `Equipment.vue` 设备台账中的“设备检修登记”弹窗，更名为“设备报修申请”。
   - 在弹窗表单中，隐藏/移除“检修人”、“检修费用”及“检修内容”的输入框和对应的必填强校验（因为发起报修时无法确定这些信息）。
   - 增加“故障描述”多行文本域（`<el-input type="textarea">`），`v-model` 绑定到新加的 `maintForm.faultDescription` 字段，并设置为必填强校验。
   - 相应修改其提交方法 `submitMaint`，校验并提交 `faultDescription`。
3. **报废页面表单引用修正**：
   - 修改 `ScrapRecord.vue` 报废记录页面，将其确认提交方法 `submitForm` 中的 `this.$refs.postForm.validate` 修正为 `this.$refs.scrapForm.validate`。
   - 将 `resetForm` 中的 `this.$refs.postForm.resetFields` 修正为 `this.$refs.scrapForm.resetFields`。
   - 确保其与表单声明 `<el-form ref="scrapForm" ...>` 完全对应，彻底消除 TypeError 报错。

### Out of scope
- 不修改数据库物理表结构和后端的 Java 控制器/业务层/DAO 接口，完全在前端 Vue 页面和其与后端对接的传参上进行加固和规范化。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "【P0】资产管理员可在检修记录列表中对处于待指派状态的工单进行合法指派，无报错且 ID 能够成功传递",
    "steps": [
      "使用资产管理员登录系统进入设备检修记录管理",
      "针对某待指派（状态为待指派）的工单点击修改",
      "在检修人下拉框中选择某维修工（如李工）并保存",
      "Verify: 接口提交成功（前端不报错，后端校验通过），刷新列表后该工单状态变更为维修中，且检修人正确显示为李工"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "【P0】保管人在发起设备报修时，能录入故障描述且不需要提前填写检修人、费用和具体内容",
    "steps": [
      "使用操作员登录系统进入我的设备列表",
      "针对自己保管的在用设备点击维修",
      "观察弹窗：确认弹窗标题为设备报修申请，无检修人、检修费用和检修内容等输入项，只有故障描述多行文本框",
      "在故障描述中填写故障原因（如屏幕闪烁无法使用）并点击提交",
      "Verify: 报修申请成功提交，刷新列表后设备状态变更为维修，进入检修记录中能看到该笔状态为待指派的工单"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "【P0】资产管理员可正常录入、修改和撤销报废补录单，确认操作无前端 JS 报错",
    "steps": [
      "使用资产管理员登录系统进入设备报废记录管理",
      "点击新增报废补录，选择在用设备，填写原因及审批人并确定",
      "Verify: 补录表单验证通过并成功提交，无 Cannot read properties of undefined (reading 'validate') 运行时控制台报错"
    ],
    "passes": true
  }
]
```

## Notes
- 本修复将配合 TASK-005 的后端物理加固，完成维保闭环。
