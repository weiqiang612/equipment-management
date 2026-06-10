# 国家标准设备管理系统 (EMS) - 需求分析报告

## 1. 引言

### 1.1 编写目的
明确“国家标准设备管理系统”的业务、功能及数据需求，包含原有的设备、分类、单位、调拨、检修、报废模块，以及新引入的 **登录鉴权与 RBAC (基于角色的权限控制) 体系**，为系统开发及测试提供最新基准。

### 1.2 项目背景
随着企事业单位固定资产规模的扩大，传统的纸质或简单表格管理已无法满足设备全生命周期监管需求。本项目旨在开发一套符合 GB/T 14885 标准设备分类体系、支持多角色协作、具备高安全性的自动化管理系统。

---

## 2. 业务需求概述

系统主要服务于企事业单位的设备使用者、技术维护人员、资产管理员及运维人员，核心目标包括：
*   **规范化分类**：严格遵循国家标准设备分类编码（GB/T 14885）。
*   **全生命周期追踪**：记录设备从入库、在用、维修到报废的每一个状态变更。
*   **动态调拨管理**：实现设备在不同部门间的透明流转。
*   **数据安全备份**：提供一键 SQL 备份与恢复。
*   **四级角色鉴权 (新)**：系统分为设备操作员、维修工程师、资产管理员、系统管理员四级，实现菜单级与接口级权限拦截。

---

## 3. 功能需求分析

### 3.1 账户鉴权与权限管理 (新)
*   **用户登录**：支持输入用户名及密码登录，后端基于 MD5 加密比对，并下发有效期 12 小时的 JWT Token。
*   **自助注册**：供普通操作员自助注册，注册新用户角色一律强制默认为 `0` (设备操作员)，且用户名全局唯一。
*   **用户权限管理**：仅系统管理员（`role=3`）可见，支持查看所有用户账户列表（安全脱敏），并在管理后台下拉修改用户的角色。

### 3.2 基础数据管理
*   **分类管理**：维护国标分类编码、预计使用年限及残值率。
*   **单位管理**：维护设备使用部门及负责人信息。

### 3.3 设备业务管理
*   **设备入库**：登记设备编号、名称、原值、购入日期等基础信息。
*   **设备领用**：新增设备时，指派当前的保管/领用人（`custodian`）。
*   **设备调拨**：支持在不同单位之间调拨，记录调拨日期、变动类型、原单位及新单位信息。
*   **检修维护**：记录检修内容、费用及检修人，自动更新设备状态为“维修”。
*   **报废处理**：对报废设备执行报废登记，记录报废原因与审批人，状态变更为“报废”。

### 3.4 系统安全与维护
*   **数据备份**：调用 `mysqldump` 生成 SQL 脚本并按时间戳命名存储。
*   **数据恢复**：支持选择已有的备份文件进行一键还原。

---

## 4. 详细数据字典 (基线)

### 4.1 系统用户表 (sys_user)
*   用于存储系统所有的登录账户和角色权限。

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PRIMARY KEY, AUTO_INCREMENT | 用户唯一标识 |
| `username` | VARCHAR | 50 | UNIQUE, NOT NULL | 登录账号 |
| `password` | VARCHAR | 100 | NOT NULL | 登录密码 (MD5密文) |
| `real_name` | VARCHAR | 50 | - | 真实姓名 |
| `role` | TINYINT | - | DEFAULT 0 | 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员 |
| `create_time` | DATETIME | - | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | - | ON UPDATE CURRENT_TIMESTAMP | 资料更新时间 |

### 4.2 设备使用单位表 (department)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `unit_code` | VARCHAR | 20 | PRIMARY KEY | 单位代码，唯一标识 |
| `unit_name` | VARCHAR | 50 | NOT NULL | 单位名称 |
| `manager` | VARCHAR | 20 | NOT NULL | 部门负责人姓名 |

### 4.3 设备信息表 (equipment)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `equip_id` | VARCHAR | 20 | PRIMARY KEY | 设备唯一编号 |
| `equip_name` | VARCHAR | 100 | NOT NULL | 设备名称 |
| `model` | VARCHAR | 50 | - | 规格型号描述 |
| `status` | VARCHAR | 10 | DEFAULT '在用' | 状态：在用、维修、报废 |
| `purchase_date`| DATE | - | NOT NULL | 购入日期 |
| `original_value`| DECIMAL | 12,2 | NOT NULL, CHECK > 0 | 设备原值 |
| `unit_code` | VARCHAR | 20 | FOREIGN KEY | 当前所属单位代码 |
| `category_id` | VARCHAR | 20 | FOREIGN KEY | 所属分类编码 |
| `custodian` | VARCHAR | 50 | - | 当前保管人/领用人用户名 |

### 4.4 设备检修信息表 (maintenance_record)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `maint_id` | INT | - | PRIMARY KEY, AUTO_INCREMENT | 检修单号 |
| `equip_id` | VARCHAR | 20 | FOREIGN KEY | 关联的设备编号 |
| `maint_date` | DATE | - | NOT NULL | 执行检修的具体日期 |
| `maint_content`| VARCHAR | 500 | - | 检修内容详细描述 |
| `maint_cost` | DECIMAL | 10,2 | DEFAULT 0 | 检修费用 |
| `maint_person` | VARCHAR | 20 | - | 负责执行检修的人员姓名 |
| `reporter` | VARCHAR | 50 | - | 故障报修人用户名 |
| `fault_description` | TEXT | - | - | 报修故障详细描述 |
| `maint_status` | TINYINT | - | DEFAULT 0 | 0-待指派, 1-维修中, 2-已完成 |

### 4.5 国家标准设备分类表 (category)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `category_id` | VARCHAR | 20 | PRIMARY KEY | 国家标准的分类编码 |
| `category_name`| VARCHAR | 50 | NOT NULL | 分类名称 |
| `useful_life` | INT | - | NOT NULL, CHECK > 0 | 预计使用年限，必须大于零 |
| `residual_rate`| DECIMAL | 5,2 | NOT NULL | 残值率 |

### 4.6 设备调拨信息表 (transfer_record)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `transfer_id` | INT | - | PRIMARY KEY, AUTO_INCREMENT | 调拨单号 |
| `equip_id` | VARCHAR | 20 | FOREIGN KEY | 调拨设备编号 |
| `out_unit_code`| VARCHAR | 20 | FOREIGN KEY | 原使用单位代码 |
| `in_unit_code` | VARCHAR | 20 | FOREIGN KEY | 新调入单位代码 |
| `transfer_date`| DATE | - | NOT NULL | 调拨执行日期 |
| `change_type` | VARCHAR | 10 | - | 变动类型 |
| `operator` | VARCHAR | 20 | - | 经办人姓名 |
| `reason` | VARCHAR | 200 | - | 调拨原因说明 |

### 4.7 设备报废信息表 (scrap_record)

| 字段名 | 数据类型 | 长度 | 约束 | 备注/说明 |
| :--- | :--- | :--- | :--- | :--- |
| `equip_id` | VARCHAR | 20 | PRIMARY KEY | 报废设备编号 |
| `scrap_no` | VARCHAR | 20 | UNIQUE, NOT NULL | 报废单号 |
| `scrap_date` | DATE | - | NOT NULL | 报废日期 |
| `approver` | VARCHAR | 20 | - | 审批人员姓名 |
| `reason` | VARCHAR | 200 | - | 报废原因详细描述 |

---

## 5. 非功能需求
*   **安全性**：对数据恢复等高危操作设置强提醒；接口强制进行 JWT 签名和有效性鉴权拦截，阻止未登录与越权调用。
*   **健壮性**：后端数据库事务应保证调拨、检修及报废的多表写一致性，数据备份还原过程具备良好的异常阻断。
*   **易用性**：前端依据登录用户的角色展示动态侧边栏，页面元素根据所属权限进行动态显隐。
