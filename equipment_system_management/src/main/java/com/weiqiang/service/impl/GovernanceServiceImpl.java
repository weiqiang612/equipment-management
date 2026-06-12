package com.weiqiang.service.impl;

import com.weiqiang.dao.GovernanceDao;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.pojo.EquipmentRiskVO;
import com.weiqiang.pojo.GovernanceSummaryVO;
import com.weiqiang.pojo.DepartmentRiskDistributionVO;
import com.weiqiang.pojo.CategoryRiskDistributionVO;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.service.GovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据治理服务实现类
 */
@Service
@RequiredArgsConstructor
public class GovernanceServiceImpl implements GovernanceService {

    private final GovernanceDao governanceDao;

    @Override
    public GovernanceSummaryVO getGovernanceSummary(final Integer role, final String currentUnitCode) {
        if (role == null || (role != 2 && role != 3)) {
            throw new ForbiddenException("权限不足");
        }

        final String unit = (role == 2) ? currentUnitCode : null;

        final Integer totalEquipmentCount = governanceDao.countTotalEquipments(unit);
        final Integer missingFieldsCount = governanceDao.countMissingFieldsEquipments(unit);
        final Integer mismatchCount = governanceDao.countMismatchEquipments(unit);
        final Integer duplicateCount = governanceDao.countDuplicateEquipments(unit);
        final Integer idleCount = governanceDao.countIdleEquipments(unit);
        final Integer costAnomalyCount = governanceDao.countCostAnomalies(unit);
        final Integer issueCount = governanceDao.countUniqueIssueEquipments(unit);

        final Map<String, Integer> riskCounts = governanceDao.getRiskDistribution(unit);
        final Integer highRiskCount = riskCounts.getOrDefault("high", 0);
        final Integer mediumRiskCount = riskCounts.getOrDefault("medium", 0);
        final Integer lowRiskCount = riskCounts.getOrDefault("low", 0);

        final Double qualityScore;
        if (totalEquipmentCount == 0) {
            qualityScore = 100.0;
        } else {
            final double score = ((totalEquipmentCount - issueCount) / (double) totalEquipmentCount) * 100.0;
            qualityScore = Math.max(0.0, Math.round(score * 100.0) / 100.0);
        }

        final List<DepartmentRiskDistributionVO> departmentDistribution = governanceDao.getDepartmentRiskDistribution(unit);
        final List<CategoryRiskDistributionVO> categoryDistribution = governanceDao.getCategoryRiskDistribution(unit);

        return GovernanceSummaryVO.builder()
                .qualityScore(qualityScore)
                .totalEquipmentCount(totalEquipmentCount)
                .issueCount(issueCount)
                .missingFieldsCount(missingFieldsCount)
                .mismatchCount(mismatchCount)
                .duplicateCount(duplicateCount)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .idleCount(idleCount)
                .costAnomalyCount(costAnomalyCount)
                .departmentDistribution(departmentDistribution)
                .categoryDistribution(categoryDistribution)
                .build();
    }

    @Override
    public PageBean<EquipmentRiskVO> listEquipmentRisks(
            final String riskLevel,
            final String unitCode,
            final String categoryId,
            final Integer page,
            final Integer pageSize,
            final Integer role,
            final String currentUnitCode
    ) {
        if (role == null || (role != 2 && role != 3)) {
            throw new ForbiddenException("权限不足");
        }

        final String actualUnitCode = (role == 2) ? currentUnitCode : unitCode;

        final Integer total = governanceDao.countEquipmentRisks(riskLevel, actualUnitCode, categoryId);
        final Integer offset = (page - 1) * pageSize;
        final List<EquipmentRiskVO> rows = governanceDao.listEquipmentRisks(riskLevel, actualUnitCode, categoryId, offset, pageSize);

        for (final EquipmentRiskVO row : rows) {
            final String level = row.getRiskLevel();
            if ("高风险".equals(level)) {
                row.setHealthScore(40);
            } else if ("中风险".equals(level)) {
                row.setHealthScore(70);
            } else {
                row.setHealthScore(100);
            }

            final List<String> reasons = new ArrayList<>(6); // (4 / 0.75) + 1 = 6
            final String status = row.getStatus();
            final double ageRatio = row.getAgeRatio() != null ? row.getAgeRatio() : 0.0;
            final int maintenanceCount = row.getMaintenanceCount() != null ? row.getMaintenanceCount() : 0;
            final double costRatio = row.getCostRatio() != null ? row.getCostRatio() : 0.0;

            if (!"报废".equals(status)) {
                if (ageRatio >= 0.9 || maintenanceCount >= 3 || costRatio >= 0.3) {
                    if (ageRatio >= 0.9) {
                        reasons.add("使用年限占比超标");
                    }
                    if (maintenanceCount >= 3) {
                        reasons.add("维保次数超标");
                    }
                    if (costRatio >= 0.3) {
                        reasons.add("维保费用占比过高");
                    }
                } else if (ageRatio >= 0.75 || maintenanceCount >= 2 || costRatio >= 0.15 || "维修".equals(status)) {
                    if (ageRatio >= 0.75) {
                        reasons.add("使用年限占比较高");
                    }
                    if (maintenanceCount >= 2) {
                        reasons.add("维保次数偏高");
                    }
                    if (costRatio >= 0.15) {
                        reasons.add("维保费用占比偏高");
                    }
                    if ("维修".equals(status)) {
                        reasons.add("设备处于维修状态");
                    }
                }
            }

            if (reasons.isEmpty()) {
                row.setRiskReasons("正常");
            } else {
                row.setRiskReasons(String.join(",", reasons));
            }
        }

        return new PageBean<>(total.longValue(), rows);
    }
}
