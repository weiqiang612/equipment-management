package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据治理总览 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernanceSummaryVO {
    private Double qualityScore;
    private Integer totalEquipmentCount;
    private Integer issueCount;
    private Integer missingFieldsCount;
    private Integer mismatchCount;
    private Integer duplicateCount;
    private Integer highRiskCount;
    private Integer mediumRiskCount;
    private Integer lowRiskCount;
    private Integer idleCount;
    private Integer costAnomalyCount;
    private java.util.List<DepartmentRiskDistributionVO> departmentDistribution;
    private java.util.List<CategoryRiskDistributionVO> categoryDistribution;
}
