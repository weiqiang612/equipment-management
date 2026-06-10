-- ==========================================================
-- 设备管理系统 - 用户权限表结构 (sys_user)
-- ==========================================================

DROP TABLE IF EXISTS `sys_user`;
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

-- ----------------------------
-- 插入四级 RBAC 初始测试账号
-- 密码明文统一为: 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
-- ----------------------------
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`) VALUES 
('operator1', 'e10adc3949ba59abbe56e057f20f883e', '设备操作员小张', 0),
('engineer1', 'e10adc3949ba59abbe56e057f20f883e', '维修工程师李工', 1),
('manager1', 'e10adc3949ba59abbe56e057f20f883e', '资产管理员王经', 2),
('admin', 'e10adc3949ba59abbe56e057f20f883e', '超级管理员系统', 3);

