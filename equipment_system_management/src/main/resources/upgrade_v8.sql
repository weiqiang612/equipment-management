-- 升级脚本 v8 (TASK-015 事找人 MVP 与消息中心 - 新增系统消息表)
CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型: high_risk_equipment, pending_claim, overdue_maintenance',
  `target_user` VARCHAR(100) NOT NULL COMMENT '目标接收用户',
  `status` INT NOT NULL DEFAULT 0 COMMENT '读取状态: 0-未读, 1-已读',
  `is_valid` INT NOT NULL DEFAULT 1 COMMENT '是否有效: 1-有效, 0-已失效',
  `ref_type` VARCHAR(50) COMMENT '关联业务类型: equipment, claim, maintenance',
  `ref_id` VARCHAR(100) COMMENT '关联业务ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_target_user_status` (`target_user`, `status`),
  INDEX `idx_ref_type_ref_id` (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';
