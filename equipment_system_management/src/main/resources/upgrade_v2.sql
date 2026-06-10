-- ==========================================================
-- 设备管理系统 - 数据库表结构升级脚本 (V2)
-- 用于支持部门级隔离 (unit_code) 与精细化派工 (maint_person_id)
-- ==========================================================

-- 1. 修改系统用户表 (sys_user)：增加 unit_code 字段并建立外键关联 department(unit_code)
ALTER TABLE sys_user 
ADD COLUMN unit_code varchar(20) DEFAULT NULL COMMENT '所属单位代码',
ADD CONSTRAINT fk_user_dept FOREIGN KEY (unit_code) REFERENCES department(unit_code);

-- 2. 修改设备检修信息表 (maintenance_record)：增加 maint_person_id 并建立外键关联 sys_user(id)
ALTER TABLE maintenance_record
ADD COLUMN maint_person_id int(11) DEFAULT NULL COMMENT '指派维修工用户ID',
ADD CONSTRAINT fk_maint_user FOREIGN KEY (maint_person_id) REFERENCES sys_user(id);
