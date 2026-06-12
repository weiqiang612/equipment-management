package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.pojo.EquipmentRiskVO;
import com.weiqiang.pojo.GovernanceSummaryVO;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.pojo.Result;
import com.weiqiang.service.GovernanceService;
import com.weiqiang.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据治理与风险分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/governance")
@RequiresRoles({2, 3})
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    /**
     * 获取数据治理总览
     */
    @GetMapping("/summary")
    public Result getGovernanceSummary() {
        final Integer role = BaseContext.getCurrentRole();
        final String currentUnitCode = BaseContext.getCurrentUnitCode();
        log.info("获取数据治理总览 - 角色: {}, 单位: {}", role, currentUnitCode);

        final GovernanceSummaryVO summary = governanceService.getGovernanceSummary(role, currentUnitCode);
        return Result.success(summary);
    }

    /**
     * 查询风险设备清单
     */
    @GetMapping("/equipment-risks")
    public Result getEquipmentRisks(
            @RequestParam(value = "riskLevel", required = false) final String riskLevel,
            @RequestParam(value = "unitCode", required = false) final String unitCode,
            @RequestParam(value = "categoryId", required = false) final String categoryId,
            @RequestParam(value = "page", defaultValue = "1") final Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") final Integer pageSize
    ) {
        final Integer role = BaseContext.getCurrentRole();
        final String currentUnitCode = BaseContext.getCurrentUnitCode();
        log.info("查询风险设备清单 - 角色: {}, 隔离单位: {}, 筛选条件 - 级别: {}, 单位: {}, 分类: {}, 页码: {}, 每页大小: {}",
                role, currentUnitCode, riskLevel, unitCode, categoryId, page, pageSize);

        final PageBean<EquipmentRiskVO> risks = governanceService.listEquipmentRisks(
                riskLevel, unitCode, categoryId, page, pageSize, role, currentUnitCode
        );
        return Result.success(risks);
    }
}
