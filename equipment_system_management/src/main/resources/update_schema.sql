-- ==========================================================
-- 设备管理系统 - 数据库表结构升级脚本
-- 用于支持四级角色 RBAC 权限控制及业务流程流转
-- ==========================================================

-- 1. 修改设备表 (equipment)：增加设备领用/保管人字段
ALTER TABLE `equipment` 
ADD COLUMN `custodian` varchar(50) DEFAULT NULL COMMENT '当前保管人/领用人用户名(关联 sys_user.username)';

-- 2. 修改维修记录表 (maintenance_record)：增加报修人、故障描述、工单状态字段
ALTER TABLE `maintenance_record` 
ADD COLUMN `reporter` varchar(50) DEFAULT NULL COMMENT '报修人用户名(关联 sys_user.username)',
ADD COLUMN `fault_description` text DEFAULT NULL COMMENT '故障描述(操作员报修填写)',
ADD COLUMN `maint_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '工单状态: 0-待指派, 1-维修中, 2-已完成';
