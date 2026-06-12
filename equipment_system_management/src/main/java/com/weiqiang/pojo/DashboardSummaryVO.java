package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据看板聚合数据VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryVO {
    private Integer role; // 0-操作员, 1-维修工, 2-资产管理员, 3-系统管理员
    private Map<String, Object> kpis; // KPI 指标映射
    private Map<String, Object> charts; // 图表数据映射
    private Map<String, Object> listData; // 待办/列表数据集映射
}
