CREATE TABLE IF NOT EXISTS `t_equipment_claim` (
  `claim_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '领用申请单号',
  `equip_id` varchar(20) NOT NULL COMMENT '设备编号',
  `applicant` varchar(50) NOT NULL COMMENT '申请人/保管人用户名(关联 sys_user.username)',
  `approver` varchar(50) DEFAULT NULL COMMENT '审批人/指派人用户名(关联 sys_user.username)',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '领用状态: 0-待审批, 1-已同意, 2-已拒绝, 3-已撤回, 4-已退还, 5-直接分配',
  `remark` varchar(500) DEFAULT NULL COMMENT '领用原因/审批意见/退还备注/直接分配备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`claim_id`),
  KEY `idx_claim_equip` (`equip_id`),
  KEY `idx_claim_applicant` (`applicant`),
  KEY `idx_claim_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备领用与审批记录表';
