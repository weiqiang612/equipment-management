package com.weiqiang.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 设备领用与审批记录实体类
 * 对应数据库表 t_equipment_claim
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentClaim {
    // 状态常量定义
    public static final int STATUS_PENDING = 0;   // 待审批
    public static final int STATUS_APPROVED = 1;  // 已同意
    public static final int STATUS_REJECTED = 2;  // 已拒绝
    public static final int STATUS_CANCELLED = 3; // 已撤回
    public static final int STATUS_RETURNED = 4;  // 已退还
    public static final int STATUS_DIRECT = 5;    // 直接分配

    private Integer claimId; // 领用申请单号
    private String equipId; // 设备编号
    private String applicant; // 申请人/保管人用户名
    private String approver; // 审批人/指派人用户名
    private Integer status; // 领用状态: 0-待审批, 1-已同意, 2-已拒绝, 3-已撤回, 4-已退还, 5-直接分配
    private String remark; // 领用原因/审批意见/退还备注/直接分配备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间

    // 扩展字段，便于前端展示
    private String equipName; // 设备名称
    private String applicantRealName; // 申请人真实姓名
    private String approverRealName; // 审批人真实姓名
}
