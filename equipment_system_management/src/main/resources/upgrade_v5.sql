-- 升级脚本 v5 (TASK-012 设备操作审计日志与设备全生命周期详情的聚合功能开发)

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `operator` varchar(50) NOT NULL COMMENT '操作人用户名',
  `operator_role` tinyint(4) NOT NULL COMMENT '操作人角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员',
  `op_type` varchar(50) NOT NULL COMMENT '操作类型',
  `target_type` varchar(50) NOT NULL COMMENT '业务对象类型',
  `target_id` varchar(50) NOT NULL COMMENT '业务对象ID',
  `op_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `summary` varchar(500) NOT NULL COMMENT '操作摘要',
  `status` tinyint(4) NOT NULL COMMENT '状态: 1-成功, 0-失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败错误信息',
  PRIMARY KEY (`id`),
  KEY `idx_op_log_operator` (`operator`),
  KEY `idx_op_log_op_time` (`op_time`),
  KEY `idx_op_log_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
