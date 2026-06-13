package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author 袁志刚
 * @version 1.0
 * 设备检修信息表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {
    private Integer maintId; // 维修单号
    private String equipId; // 设备编号
    private String equipName; // 设备名称
    private LocalDate maintDate; // 检修日期
    private String maintContent; // 检修内容描述
    private BigDecimal maintCost; // 检修费用
    private String maintPerson; // 检修人
    private String reporter; // 报修人用户名
    private String faultDescription; // 故障描述
    private Integer maintStatus; // 维修工单状态: 0-待指派, 1-维修中, 2-已完成(待复核), 3-已复核可用, 4-已复核转报废
    private Integer maintPersonId; // 指派维修工用户ID
    private String reviewer; // 复核人用户名
    private String reviewComments; // 复核意见
    private java.time.LocalDateTime reviewDate; // 复核日期
    private String scrapNo; // 仅在复核转报废时接收报废单号
}
