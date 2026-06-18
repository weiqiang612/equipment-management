package com.weiqiang.service;

import com.weiqiang.vo.EquipmentRiskVO;
import com.weiqiang.vo.GovernanceSummaryVO;
import com.weiqiang.common.PageBean;

/**
 * 数据治理服务接口
 */
public interface GovernanceService {

    /**
     * 获取数据治理总览
     */
    GovernanceSummaryVO getGovernanceSummary(final Integer role, final String currentUnitCode);

    /**
     * 分页查询风险设备列表
     */
    PageBean<EquipmentRiskVO> listEquipmentRisks(
            final String riskLevel,
            final String unitCode,
            final String categoryId,
            final Integer page,
            final Integer pageSize,
            final Integer role,
            final String currentUnitCode
    );
}
