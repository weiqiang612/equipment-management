-- 升级脚本 v7 (TASK-015 事找人 MVP 与消息中心 - 维修超时字段扩充)
ALTER TABLE `maintenance_record` 
ADD COLUMN `assign_time` datetime DEFAULT NULL COMMENT '指派时间',
ADD COLUMN `complete_time` datetime DEFAULT NULL COMMENT '完工登记时间';
