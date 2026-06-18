package com.weiqiang.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 看板通用图表数据结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartData {
    private String name;
    private Object value;
}
