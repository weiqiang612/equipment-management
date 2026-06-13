-- 升级脚本 v6 (TASK-014 维修流转闭环与复核处置)
ALTER TABLE `maintenance_record` 
ADD COLUMN `reviewer` varchar(50) DEFAULT NULL COMMENT '复核人用户名',
ADD COLUMN `review_comments` varchar(500) DEFAULT NULL COMMENT '复核意见',
ADD COLUMN `review_date` datetime DEFAULT NULL COMMENT '复核时间';
