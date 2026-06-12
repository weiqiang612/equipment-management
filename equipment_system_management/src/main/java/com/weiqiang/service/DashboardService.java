package com.weiqiang.service;

import com.weiqiang.pojo.DashboardSummaryVO;

/**
 * 看板业务接口
 */
public interface DashboardService {
    /**
     * 获取看板聚合数据
     */
    DashboardSummaryVO getDashboardSummary();
}
