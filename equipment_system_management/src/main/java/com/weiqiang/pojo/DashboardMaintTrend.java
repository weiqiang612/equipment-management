package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 维保费用与工单数量趋势统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMaintTrend {
    private String month;
    private BigDecimal cost;
    private Long count;
}
