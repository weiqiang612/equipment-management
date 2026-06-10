# 国家标准设备管理系统 (EMS) - 数据库设计规范 (db_schema.md)

## 1. 概述
本规范整合了系统的数据实体关系转换规则、第三范式 (3NF) 评价、主外键逻辑关联以及 MySQL 物理存储和全量 DDL 脚本，作为设备管理系统的唯一数据库基线设计。

---

## 2. 关系模式转换 (逻辑结构设计)
关系模式中，**<u>下划线</u>** 表示主键，*斜体* 表示外键。

### 2.1 基础数据模块
*   **国家标准设备分类表 (category)**
    *   关系模式：分类(<u>category_id</u>, category_name, useful_life, residual_rate)
*   **设备使用单位表 (department)**
    *   关系模式：单位(<u>unit_code</u>, unit_name, manager)

### 2.2 用户与权限模块
*   **系统用户表 (sys_user)**
    *   关系模式：用户(<u>id</u>, username, password, real_name, role, create_time, update_time)
    *   *注：`username` 字段设为唯一索引。*

### 2.3 设备核心与业务记录模块
*   **设备信息表 (equipment)**
    *   关系模式：设备(<u>equip_id</u>, equip_name, model, status, purchase_date, original_value, *unit_code*, *category_id*, *custodian*)
    *   *外键说明*：
        *   `unit_code` 参照单位表 `department.unit_code`
        *   `category_id` 参照分类表 `category.category_id`
        *   `custodian` 逻辑参照用户表 `sys_user.username` (当前保管人)
*   **设备调拨信息表 (transfer_record)**
    *   关系模式：调拨(<u>transfer_id</u>, *equip_id*, *out_unit_code*, *in_unit_code*, transfer_date, change_type, operator, reason)
    *   *外键说明*：
        *   `equip_id` 参照设备表 `equipment.equip_id`
        *   `out_unit_code` 和 `in_unit_code` 参照单位表 `department.unit_code`
*   **设备检修信息表 (maintenance_record)**
    *   关系模式：检修(<u>maint_id</u>, *equip_id*, maint_date, maint_content, maint_cost, maint_person, *reporter*, fault_description, maint_status)
    *   *外键说明*：
        *   `equip_id` 参照设备表 `equipment.equip_id`
        *   `reporter` 逻辑参照用户表 `sys_user.username` (故障申报人)
*   **设备报废信息表 (scrap_record)**
    *   关系模式：报废(<u>*equip_id*</u>, scrap_no, scrap_date, approver, reason)
    *   *外键说明*：`equip_id` 参照设备表 `equipment.equip_id`，此处既是主码也是外码，体现 `1:1` 关系。

---

## 3. 数据完整性与范式评价

### 3.1 完整性设计
*   **参照完整性**：业务记录中的 `equip_id` 必须在设备表中存在；设备表中的 `unit_code` 和 `category_id` 分别在单位和分类表中存在；`custodian` 和 `reporter` 必须存在于 `sys_user.username` 中。
*   **用户定义完整性**：
    *   状态约束：设备状态限于：在用、维修、报废。工单状态限于：0-待指派, 1-维修中, 2-已完成。
    *   数值约束：原值、预计使用年限必须大于 0。
    *   唯一约束：报废单号 (`scrap_no`) 全局唯一。

### 3.2 范式评价 (3NF)
本关系模式符合第三范式：所有属性都是原子的（1NF）；非主属性完全依赖于主键，消除部分函数依赖（2NF）；不存在非主属性对主键的传递函数依赖（3NF），有效防止了数据冗余与更新异常。

---

## 4. 存取路径与索引设计

*   **聚集索引**：各表主键字段自动建立主键索引。
*   **辅助索引 (非聚集索引)**：
    *   `idx_equip_unit` (on `equipment.unit_code`)：加速按单位筛选设备及多表联查。
    *   `idx_equip_cate` (on `equipment.category_id`)：加速按分类进行统计。
    *   `idx_purchase_date` (on `equipment.purchase_date`)：优化购入时间段筛选。
    *   `idx_equip_status` (on `equipment.status`)：优化管理员状态筛选。
    *   `idx_transfer_equip` / `idx_transfer_date`：加速调拨流水的检索与排序。
    *   `idx_maint_equip` / `idx_scrap_equip`：优化维保记录和报废单的读取性能。

---

## 5. 全量建表 SQL 脚本 (DDL)

```sql
CREATE DATABASE IF NOT EXISTS `equipment_management_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `equipment_management_system`;

-- 1. 国家标准设备分类表
CREATE TABLE `category` (
  `category_id` varchar(20) NOT NULL COMMENT '国家标准的分类编码',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `useful_life` int(11) NOT NULL COMMENT '预计使用年限',
  `residual_rate` decimal(5,2) NOT NULL COMMENT '残值率',
  PRIMARY KEY (`category_id`),
  CONSTRAINT `chk_useful_life` CHECK (`useful_life` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国家标准设备分类表';

-- 2. 设备使用单位代码表
CREATE TABLE `department` (
  `unit_code` varchar(20) NOT NULL COMMENT '单位代码',
  `unit_name` varchar(50) NOT NULL COMMENT '单位名称',
  `manager` varchar(20) NOT NULL COMMENT '负责人',
  PRIMARY KEY (`unit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备使用单位代码表';

-- 3. 系统用户表
CREATE TABLE `sys_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '登录密码(MD5)',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role` tinyint(4) NOT NULL DEFAULT '0' COMMENT '角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 4. 设备信息表 (含保管人)
CREATE TABLE `equipment` (
  `equip_id` varchar(20) NOT NULL COMMENT '设备编号',
  `equip_name` varchar(100) NOT NULL COMMENT '设备名称',
  `model` varchar(50) DEFAULT NULL COMMENT '规格型号',
  `status` varchar(10) DEFAULT '在用' COMMENT '设备状态',
  `purchase_date` date NOT NULL COMMENT '购入日期',
  `original_value` decimal(12,2) NOT NULL COMMENT '原值',
  `unit_code` varchar(20) DEFAULT NULL COMMENT '当前单位代码',
  `category_id` varchar(20) DEFAULT NULL COMMENT '分类编码',
  `custodian` varchar(50) DEFAULT NULL COMMENT '当前保管人/领用人用户名(关联 sys_user.username)',
  PRIMARY KEY (`equip_id`),
  KEY `idx_equip_unit` (`unit_code`),
  KEY `idx_equip_cate` (`category_id`),
  KEY `idx_purchase_date` (`purchase_date`),
  KEY `idx_equip_status` (`status`),
  CONSTRAINT `equipment_ibfk_1` FOREIGN KEY (`unit_code`) REFERENCES `department` (`unit_code`),
  CONSTRAINT `equipment_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`),
  CONSTRAINT `chk_original_value` CHECK (`original_value` > 0),
  CONSTRAINT `chk_status` CHECK (`status` in ('在用','维修','报废'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 5. 设备调拨信息表
CREATE TABLE `transfer_record` (
  `transfer_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '调拨单号',
  `equip_id` varchar(20) DEFAULT NULL COMMENT '设备编号',
  `out_unit_code` varchar(20) DEFAULT NULL COMMENT '原单位代码',
  `in_unit_code` varchar(20) DEFAULT NULL COMMENT '新单位代码',
  `transfer_date` date NOT NULL COMMENT '调拨日期',
  `change_type` varchar(10) DEFAULT NULL COMMENT '变动类型',
  `operator` varchar(20) DEFAULT NULL COMMENT '经办人',
  `reason` varchar(200) DEFAULT NULL COMMENT '调拨原因',
  PRIMARY KEY (`transfer_id`),
  KEY `idx_transfer_equip` (`equip_id`),
  KEY `idx_transfer_date` (`transfer_date`),
  CONSTRAINT `transfer_record_ibfk_1` FOREIGN KEY (`equip_id`) REFERENCES `equipment` (`equip_id`),
  CONSTRAINT `transfer_record_ibfk_2` FOREIGN KEY (`out_unit_code`) REFERENCES `department` (`unit_code`),
  CONSTRAINT `transfer_record_ibfk_3` FOREIGN KEY (`in_unit_code`) REFERENCES `department` (`unit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备调拨信息表';

-- 6. 设备检修信息表
CREATE TABLE `maintenance_record` (
  `maint_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '检修单号',
  `equip_id` varchar(20) DEFAULT NULL COMMENT '设备编号',
  `maint_date` date NOT NULL COMMENT '检修日期',
  `maint_content` varchar(500) DEFAULT NULL COMMENT '检修内容描述',
  `maint_cost` decimal(10,2) DEFAULT '0.00' COMMENT '检修费用',
  `maint_person` varchar(20) DEFAULT NULL COMMENT '检修人',
  `reporter` varchar(50) DEFAULT NULL COMMENT '报修人用户名(关联 sys_user.username)',
  `fault_description` text COMMENT '故障描述(操作员报修填写)',
  `maint_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '工单状态: 0-待指派, 1-维修中, 2-已完成',
  PRIMARY KEY (`maint_id`),
  KEY `idx_maint_equip` (`equip_id`),
  CONSTRAINT `maintenance_record_ibfk_1` FOREIGN KEY (`equip_id`) REFERENCES `equipment` (`equip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备检修信息表';

-- 7. 设备报废信息表
CREATE TABLE `scrap_record` (
  `equip_id` varchar(20) NOT NULL COMMENT '设备编号',
  `scrap_no` varchar(20) NOT NULL COMMENT '报废单号',
  `scrap_date` date NOT NULL COMMENT '报废日期',
  `approver` varchar(20) DEFAULT NULL COMMENT '审批人',
  `reason` varchar(200) DEFAULT NULL COMMENT '报废原因',
  PRIMARY KEY (`equip_id`),
  UNIQUE KEY `scrap_no` (`scrap_no`),
  CONSTRAINT `scrap_record_ibfk_1` FOREIGN KEY (`equip_id`) REFERENCES `equipment` (`equip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备报废信息表';
```
