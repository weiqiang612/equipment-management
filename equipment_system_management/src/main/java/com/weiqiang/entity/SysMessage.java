package com.weiqiang.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 系统消息实体类
 * 对应数据库表 sys_message
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SysMessage {
    private Integer id; // 主键ID
    private String title; // 消息标题
    private String content; // 消息内容
    private String eventType; // 事件类型: high_risk_equipment, pending_claim, overdue_maintenance
    private String targetUser; // 目标接收用户
    private Integer status; // 读取状态: 0-未读, 1-已读
    private Integer isValid; // 是否有效: 1-有效, 0-已失效
    private String refType; // 关联业务类型: equipment, claim, maintenance
    private String refId; // 关联业务ID
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
