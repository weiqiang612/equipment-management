# TASK-010: Tasks

**Spec**: `spec.md`
**Status**: Completed

## Key decisions
- ScrapRecordController/TransferRecordController 写操作权限：统一使用 `@RequiresRoles(2)`，查询接口使用 `@RequiresRoles({2, 3})`（Q1/Q2 确认方案 A）
- 前端导出改为调用后端 `/equipments/export` 接口，消除前端重复折旧计算逻辑
- 报废单号使用 `yyMMddHHmmssSSS` + 4位大写字母随机后缀
- 数据库恢复文件名采用正则 `^[\w\-]+\.sql$` 白名单校验
- 各项修复独立无依赖，可并行修改

## Progress

- [x] T1 — Controller 权限注解补全：`ScrapRecordController` + `TransferRecordController` 类级别添加 `@RequiresRoles` · covers: AC-001
- [x] T2 — Service 层逻辑修复三部曲：
  - [x] T2a `EquipmentServiceImpl.calculateAccumulated()` 添加 usefulLife 空值/零值校验
  - [x] T2b `TransferRecordServiceImpl.transferEquip()` 添加"维修"状态阻断
  - [x] T2c `ScrapRecordServiceImpl.scrapEquip()` 报废单号生成增加毫秒+随机后缀 · covers: AC-002, AC-003, AC-004
- [x] T3 — `DatabaseController.restore()` 添加 fileName 白名单正则校验 · covers: AC-006
- [x] T4 — 前端 `Equipment.vue` 修复：
  - [x] T4a `handleExport()` 改为调用后端 `/equipments/export` 接口
  - [x] T4b 删除未使用的 `formatStatus()` 方法 · covers: AC-005, AC-007
- [x] T5 — 运行 `cd equipment_system_management && mvn test` — 所有测试必须通过
- [x] T6 — 验证 ACs: 更新 spec.md 中每个 AC 的 `passes` 为 `true`
- [x] T7 — 更新 `docs/4-tasks/CURRENT_PLAN.md` — 标记此任务完成

## Dependencies
- T1, T2a-c, T3, T4a-b 互无依赖，可并行
- T5 需要 T1–T4 完成
- T6, T7 需要 T5 通过

## Blockers
- 无
