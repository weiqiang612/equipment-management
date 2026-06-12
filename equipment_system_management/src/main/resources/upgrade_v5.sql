-- 升级脚本 v5 (TASK-012 操作审计与设备生命周期详情)
-- 创建操作审计日志表

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator` varchar(50) NOT NULL COMMENT '操作人用户名(关联 sys_user.username)',
  `operator_role` tinyint(4) NOT NULL COMMENT '操作人角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员',
  `op_type` varchar(50) NOT NULL COMMENT '操作类型',
  `target_type` varchar(50) NOT NULL COMMENT '业务对象类型',
  `target_id` varchar(50) NOT NULL COMMENT '业务对象ID',
  `op_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作执行时间',
  `summary` varchar(500) DEFAULT NULL COMMENT '操作摘要',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '结果状态: 0-失败, 1-成功',
  `error_msg` text DEFAULT NULL COMMENT '错误异常信息',
  PRIMARY KEY (`id`),
  KEY `idx_op_log_operator` (`operator`),
  KEY `idx_op_log_target` (`target_type`, `target_id`),
  KEY `idx_op_log_time` (`op_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
