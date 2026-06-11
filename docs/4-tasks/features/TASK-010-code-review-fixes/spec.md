# TASK-010: 代码审计问题修复 (Code Review Fixes)

**Status**: Ready
**Created**: 2026-06-11
**Feature dir**: `docs/4-tasks/features/TASK-010-code-review-fixes/`

## Objective
修复代码审查中发现的 8 个业务逻辑与安全问题，涵盖权限控制遗漏、健壮性缺陷、并发安全、前端一致性和安全漏洞。

## Scope

### In scope
- `ScrapRecordController` 和 `TransferRecordController` 的所有写操作接口添加 `@RequiresRoles` 权限注解
- `EquipmentServiceImpl.calculateAccumulated()` 添加 `usefulLife` 空值/零值前置校验，防止除零崩溃
- `TransferRecordServiceImpl.transferEquip()` 添加"维修"状态阻断校验，防止维修中设备被调拨
- `ScrapRecordServiceImpl.scrapEquip()` 报废单号生成增加毫秒 + 随机后缀，消除并发冲突
- 前端 `Equipment.vue` 的 `handleExport()` 改为调用后端 `/equipments/export` 接口
- `TransferRecordController` 和 `ScrapRecordController` 的 GET 查询接口添加 `@RequiresRoles({2, 3})` 限制
- `DatabaseController.restore()` 添加 `fileName` 白名单正则校验，防止路径穿越
- 删除 `Equipment.vue` 中未使用的 `formatStatus()` 死代码

### Out of scope
- 其他 Controller 的权限审查（仅修复已发现的问题）
- 数据库 schema 或 API 契约变更（本次修复不涉及）
- 新增自动化测试（现有测试已覆盖主要流程；不单独新增测试用例）

## Acceptance criteria

```json
[
  {
    "id": "AC-001",
    "category": "security",
    "description": "ScrapRecordController 和 TransferRecordController 缺失的权限注解已补齐，操作员无法绕过权限执行报废/调拨操作",
    "steps": [
      "以操作员(role=0) Token 调用 POST /scrapRecords/{equipId}",
      "以操作员(role=0) Token 调用 POST /transferRecords/{equipId}",
      "以操作员(role=0) Token 调用 DELETE /scrapRecords/{scrapNo}",
      "Verify: 所有请求返回 403 状态码，msg 为 '权限不足'"
    ],
    "passes": true
  },
  {
    "id": "AC-002",
    "category": "edge-case",
    "description": "折旧计算在分类 useFullLife 未配置时抛出明确业务异常而非除零崩溃",
    "steps": [
      "新建一个残值率正常但 useful_life 为 NULL 的分类",
      "将某台设备关联到该分类",
      "调用 GET /equipments/calculateAccumulated/{equipId}",
      "Verify: HTTP 200，code=0，msg 包含'预计使用年限未配置'"
    ],
    "passes": true
  },
  {
    "id": "AC-003",
    "category": "functional",
    "description": "维修中的设备被调拨时被明确阻断",
    "steps": [
      "将设备状态设为'维修'，尝试以资产管理员 Token 调拨该设备",
      "Verify: 返回 BusinessException，提示'正在维修中，无法进行调拨操作'"
    ],
    "passes": true
  },
  {
    "id": "AC-004",
    "category": "edge-case",
    "description": "报废单号生成包含毫秒和随机后缀，彻底消除同一秒内的并发冲突",
    "steps": [
      "连续两次在同一毫秒内调用报废接口（可通过测试代码模拟）",
      "Verify: 每次生成的 scrapNo 均不相同，无数据库唯一键冲突"
    ],
    "passes": true
  },
  {
    "id": "AC-005",
    "category": "integration",
    "description": "前端导出功能调用后端 /equipments/export 接口，折旧数据由后端统一计算",
    "steps": [
      "在前端点击'导出明细表'按钮",
      "Verify: 发出的 HTTP 请求路径为 /equipments/export 而非在前端使用 XLSX 遍历计算",
      "Verify: 导出的 Excel 中已提足折旧设备的'当前净值'等于残值"
    ],
    "passes": true
  },
  {
    "id": "AC-006",
    "category": "security",
    "description": "数据库恢复接口 fileName 参数经过白名单校验，无法进行路径穿越",
    "steps": [
      "以系统管理员 Token 调用 POST /system/db/restore?fileName=../../etc/passwd",
      "Verify: 返回 code=0，msg 包含'非法的文件名格式'",
      "以合法文件名 backup_2026.sql 调用",
      "Verify: 正常执行恢复逻辑"
    ],
    "passes": true
  },
  {
    "id": "AC-007",
    "category": "edge-case",
    "description": "Equipment.vue 中的 formatStatus 死代码已被删除",
    "steps": [
      "搜索 Equipment.vue 文件中 'formatStatus' 函数 definition",
      "Verify: 该函数已不存在"
    ],
    "passes": true
  }
]
```

## Notes
- 本次修复不涉及数据库变更，无需 DDL 迁移脚本
- 不涉及 API 请求/响应结构变更，无需更新 api_contract.md
- 各项修复均为独立修改，互无依赖，可并行实施
