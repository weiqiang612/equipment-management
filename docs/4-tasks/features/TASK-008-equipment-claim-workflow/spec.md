# TASK-008: 设备领用与审批工作流

**Status**: Draft
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-008-equipment-claim-workflow/`

## Objective
引入设备领用申请与审批工作流，允许操作员对本部门内无保管人的空闲设备发起领用申请，经资产管理员审批同意后自动绑定保管关系。同时，支持资产管理员主动指派保管人，以及操作员自主退还设备、撤回申请，并提供完整的流转历史审计记录。

## Scope

### In scope
- **操作员领用申请**：设备操作员（role=0）可以对本部门内 custodian 为 NULL 且状态为“在用”的设备发起领用申请，填写领用原因。
- **操作员撤回申请**：在管理员审批前，操作员可以撤回自己处于“待审批”状态的领用申请。
- **操作员自主退还**：操作员对自己名下保管的设备，可直接点击“退还”，清空设备的 custodian，使设备回归部门公用池。
- **管理员审批领用**：资产管理员（role=2）可以审批本部门操作员发起的领用申请（同意或拒绝，并填写审批意见）。
- **管理员直接指派（直通车）**：资产管理员（role=2）在设备编辑中可以直接指派保管人，跳过审批流直接生效。
- **流转历史全审计**：
  - 新增申请记录表 `t_equipment_claim`。
  - 操作员发起申请、撤回申请、自主退还时自动记录审计信息。
  - 管理员审批同意或拒绝时记录审计信息。
  - 管理员在设备台账中直接指派保管人时，自动记录类型为“直接分配”的审计记录。
  - 设备发生调拨（Transfer）、报废（Scrap）、删除保管人用户（sys_user）等业务时，系统自动联动更新状态并写入保管关系终止/作废的审计记录。

### Out of scope
- **多级审批流**：仅需要设备所在部门的资产管理员一级审批即可。
- **自动审批**：操作员发起的申请均需要人工审批，不支持自动同意。

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "functional",
    "description": "操作员提交领用申请并可撤回",
    "steps": [
      "使用操作员 operator1（部门 D98）登录，找到部门内 custodian 为 NULL 且状态为'在用'的设备 E001。",
      "发起领用申请，填写原因'日常研发使用'。",
      "验证：数据库中创建一条 status=0（待审批）、applicant='operator1' 的申请记录。",
      "在管理员未审批前，operator1 点击'撤回申请'。",
      "验证：申请记录状态更新为 status=3（已撤回），设备 custodian 仍为 NULL。"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "functional",
    "description": "管理员审批通过，设备保管人自动更新",
    "steps": [
      "operator1 重新发起设备 E001 的领用申请。",
      "使用资产管理员 manager1（部门 D98）登录，查看待审批列表，找到该申请并点击'同意'，输入审批意见'同意领用'。",
      "验证：申请记录状态更新为 status=1（已同意），approver='manager1'，并且设备 E001 的 custodian 字段自动更新为 'operator1'。"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "管理员审批拒绝，设备保管人保持不变",
    "steps": [
      "operator1 针对设备 E002（custodian 为 NULL）发起领用申请。",
      "manager1 登录并选择'拒绝'该申请，输入审批意见'库存不足，暂缓分配'。",
      "验证：申请记录状态更新为 status=2（已拒绝），设备 E002 的 custodian 字段仍保持 NULL。"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "functional",
    "description": "操作员自主退还设备，系统自动解绑并留痕",
    "steps": [
      "operator1（部门 D98）登录，针对自己保管的设备 E001 点击'退还设备'，输入退还备注'项目结束归还'。",
      "验证：设备 E001 的 custodian 自动清空为 NULL；同时在 t_equipment_claim 表中自动插入一条 status=4（已退还）、applicant='operator1' 的历史审计记录。"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "functional",
    "description": "管理员直接指派保管人并自动生成直接分配审计记录",
    "steps": [
      "manager1 登录设备台账，编辑无主设备 E003，在保管人下拉框选择 operator1 并保存。",
      "验证：E003 的 custodian 变更为 'operator1'，并且系统自动在 t_equipment_claim 中插入一条 status=5（直接分配）、applicant='operator1'、approver='manager1'、remark='管理员直接分配' 的记录。"
    ],
    "passes": true
  },
  {
    "id": "AC-006",
    "category": "security",
    "description": "越权控制：禁止跨部门领用与非本人/非管理员审批",
    "steps": [
      "使用操作员 operator2（部门 D99）登录，尝试对 D98 部门的设备 E001 提交领用申请。",
      "验证：接口报错拒绝（提示“部门不匹配，无法申请”）。",
      "使用 operator1 尝试调用审批接口审批自己的申请，或者 D99 部门的管理员尝试审批 D98 部门的申请。",
      "验证：接口返回 403 权限不足拒绝访问。"
    ],
    "passes": true
  },
  {
    "id": "AC-007",
    "category": "integration",
    "description": "调拨与报废等外部业务联动审计",
    "steps": [
      "设备 E001 的保管人为 operator1，执行调拨操作将 E001 调拨至新单位 D99。",
      "验证：调拨成功后 E001 所在部门变为 D99，custodian 变更为 NULL。同时 t_equipment_claim 中自动生成一条 status=4（已退还）的记录，备注指明'设备调拨导致保管关系清退'。",
      "对设备 E003 执行报废操作。",
      "验证：报废成功后设备状态为'报废'，custodian 清空为 NULL。同时在 t_equipment_claim 中自动生成一条 status=4（已退还）的记录，备注指明'设备报废导致保管关系清退'。"
    ],
    "passes": true
  },
  {
    "id": "AC-CONTRACT",
    "category": "integration",
    "description": "API 契约与数据库设计更新",
    "steps": [
      "更新 api_contract.md 中的领用审批与退还接口设计。",
      "更新 db_schema.md 中的 t_equipment_claim 结构设计并编写 DDL 迁移脚本。",
      "验证：代码中的数据对象与迁移脚本及设计文档保持 100% 一致。"
    ],
    "passes": true
  }
]
```

## Notes
- 领用状态定义：`0-待审批`, `1-已同意`, `2-已拒绝`, `3-已撤回`, `4-已退还`, `5-直接分配`。
- 本次新增的表不需要设置强外键物理约束，遵循项目现有的逻辑约束设计，防止级联删除引起的性能问题。
