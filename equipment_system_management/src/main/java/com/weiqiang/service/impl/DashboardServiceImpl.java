package com.weiqiang.service.impl;

import com.weiqiang.config.DBBackupProperties;
import com.weiqiang.dao.DashboardDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.*;
import com.weiqiang.service.DashboardService;
import com.weiqiang.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 看板业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardDao dashboardDao;
    private final DBBackupProperties backupProperties;

    @Override
    public DashboardSummaryVO getDashboardSummary() {
        // 1. 获取当前登录用户的上下文参数
        final Integer role = BaseContext.getCurrentRole();
        final String username = BaseContext.getCurrentName();
        final String unitCode = BaseContext.getCurrentUnitCode();
        final Integer userId = BaseContext.getCurrentId();

        if (role == null) {
            log.error("Dashboard JWT Context missing role attribute.");
            throw new BusinessException("未获取到当前用户的权限角色信息，请重新登录");
        }

        log.info("用户看板查询 | 用户名: {}, 角色: {}, 部门编码: {}, 用户ID: {}", username, role, unitCode, userId);

        // 2. 根据角色构造各自的角色数据
        final Map<String, Object> kpis = new LinkedHashMap<>();
        final Map<String, Object> charts = new LinkedHashMap<>();
        final Map<String, Object> listData = new LinkedHashMap<>();

        switch (role) {
            case 0:
                // 设备操作员
                buildOperatorSummary(username, unitCode, kpis, charts, listData);
                break;
            case 1:
                // 维修工程师
                buildMaintainerSummary(userId, kpis, charts, listData);
                break;
            case 2:
                // 资产管理员
                buildAssetManagerSummary(unitCode, kpis, charts, listData);
                break;
            case 3:
                // 系统管理员
                buildSystemAdminSummary(kpis, charts, listData);
                break;
            default:
                log.error("Invalid dashboard role request: {}", role);
                throw new BusinessException("非法的用户角色");
        }

        return DashboardSummaryVO.builder()
                .role(role)
                .kpis(kpis)
                .charts(charts)
                .listData(listData)
                .build();
    }

    /**
     * Role 0: 设备操作员数据看板拼装
     */
    private void buildOperatorSummary(String username, String unitCode, Map<String, Object> kpis, Map<String, Object> charts, Map<String, Object> listData) {
        // KPIs
        kpis.put("myEquipCount", dashboardDao.countEquipmentsByCustodian(username));
        kpis.put("myActiveClaims", dashboardDao.countClaimsByApplicantAndStatus(username, 0)); // 0-待审批
        kpis.put("myActiveMaintenances", dashboardDao.countActiveMaintenancesByReporter(username));

        // 计算个人保管设备累计折旧总额
        final List<Equipment> equips = dashboardDao.getEquipmentsByCustodian(username);
        BigDecimal totalDepValue = BigDecimal.ZERO;
        if (equips != null) {
            for (final Equipment e : equips) {
                try {
                    // 本地计算累积折旧
                    final BigDecimal accumulated = calculateAccumulated(e);
                    totalDepValue = totalDepValue.add(accumulated);
                } catch (Exception ex) {
                    log.warn("设备 {} 折旧计算失败，跳过统计。异常: {}", e.getEquipId(), ex.getMessage());
                }
            }
        }
        kpis.put("myDepreciationValue", totalDepValue.setScale(2, RoundingMode.HALF_UP));

        // listData
        listData.put("myEquipments", equips);
        listData.put("myClaims", dashboardDao.getClaimsByApplicant(username));
        listData.put("myMaintenances", dashboardDao.getMaintenancesByReporter(username));
    }

    /**
     * 计算单台设备的累积折旧金额
     */
    private BigDecimal calculateAccumulated(Equipment equipment) {
        if (equipment.getUsefulLife() == null || equipment.getUsefulLife() <= 0) {
            return BigDecimal.ZERO;
        }
        if (equipment.getPurchaseDate() == null || equipment.getOriginalValue() == null || equipment.getResidualRate() == null) {
            return BigDecimal.ZERO;
        }
        
        final LocalDate startDate = equipment.getPurchaseDate().plusMonths(1).withDayOfMonth(1);
        final LocalDate now = LocalDate.now();
        long monthsUsed = ChronoUnit.MONTHS.between(startDate, now);
        if (monthsUsed < 0) monthsUsed = 0;
        
        final int totalLifeMonths = equipment.getUsefulLife() * 12;
        if (monthsUsed > totalLifeMonths) monthsUsed = totalLifeMonths;
        
        final BigDecimal totalDepreciable = equipment.getOriginalValue().multiply(BigDecimal.ONE.subtract(equipment.getResidualRate()));
        final BigDecimal monthlyDepreciation = totalDepreciable.divide(BigDecimal.valueOf(totalLifeMonths), 10, RoundingMode.HALF_UP);
        BigDecimal accumulatedDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsUsed)).setScale(2, RoundingMode.HALF_UP);

        if (monthsUsed >= totalLifeMonths) {
            accumulatedDepreciation = totalDepreciable;
        }
        return accumulatedDepreciation;
    }

    /**
     * Role 1: 维修工程师数据看板拼装
     */
    private void buildMaintainerSummary(Integer userId, Map<String, Object> kpis, Map<String, Object> charts, Map<String, Object> listData) {
        // KPIs: 工单状态 1-维修中 (对工程师而言即指派给我的未完结工单数)，2-已完成
        final Long pendingMaintCount = dashboardDao.countMaintenancesByPersonAndStatus(userId, 1);
        kpis.put("myPendingMaint", pendingMaintCount); // 分配我的待处理数
        kpis.put("myInMaint", pendingMaintCount);      // 维修中数 (系统中工单流程指派后直接为状态1，故这两项等同)
        kpis.put("myCompletedMaint", dashboardDao.countMaintenancesByPersonAndStatus(userId, 2));

        // charts
        charts.put("maintCostTrend", dashboardDao.getMaintCostTrendByPerson(userId));

        // listData
        listData.put("myWorkOrders", dashboardDao.getMaintenancesByPerson(userId));
    }

    /**
     * Role 2: 资产管理员数据看板拼装
     */
    private void buildAssetManagerSummary(String unitCode, Map<String, Object> kpis, Map<String, Object> charts, Map<String, Object> listData) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            throw new BusinessException("该资产管理员未分配部门/单位，无法获取看板数据");
        }

        // KPIs (按单位物理隔离)
        kpis.put("totalEquipment", dashboardDao.countEquipmentsByUnit(unitCode));
        kpis.put("totalValue", dashboardDao.sumEquipmentValueByUnit(unitCode).setScale(2, RoundingMode.HALF_UP));
        kpis.put("inUseCount", dashboardDao.countEquipmentsByUnitAndStatus(unitCode, "在用"));
        kpis.put("inMaintenanceCount", dashboardDao.countEquipmentsByUnitAndStatus(unitCode, "维修"));
        kpis.put("scrappedCount", dashboardDao.countEquipmentsByUnitAndStatus(unitCode, "报废"));

        // charts (按单位物理隔离)
        charts.put("categoryDistribution", dashboardDao.getCategoryDistributionByUnit(unitCode));
        charts.put("departmentDistribution", dashboardDao.getDepartmentDistributionByUnit(unitCode));
        charts.put("maintenanceTrend", dashboardDao.getMaintenanceTrendByUnit(unitCode));

        // listData
        listData.put("pendingClaims", dashboardDao.getPendingClaimsByUnit(unitCode));
        listData.put("pendingMaintenances", dashboardDao.getPendingMaintenancesByUnit(unitCode));
    }

    /**
     * Role 3: 系统管理员数据看板拼装 (超级审计视角，无操作，能看全局，可看数据库备份状态)
     */
    private void buildSystemAdminSummary(Map<String, Object> kpis, Map<String, Object> charts, Map<String, Object> listData) {
        // KPIs (全局透视)
        kpis.put("totalEquipment", dashboardDao.countAllEquipments());
        kpis.put("totalValue", dashboardDao.sumAllEquipmentValue().setScale(2, RoundingMode.HALF_UP));
        kpis.put("totalUsers", dashboardDao.countAllUsers());

        // 读取数据库备份文件夹中的文件
        String backupPath = backupProperties != null ? backupProperties.getPath() : null;
        final File dir = (backupPath != null && !backupPath.trim().isEmpty()) ? new File(backupPath) : null;
        final File[] files = (dir != null && dir.exists() && dir.isDirectory()) ? dir.listFiles((d, name) -> name.endsWith(".sql")) : null;
        final long backupCount = files != null ? files.length : 0L;
        kpis.put("backupCount", backupCount);

        // charts
        charts.put("userRoleDistribution", dashboardDao.getUserRoleDistribution());

        // listData
        final List<Map<String, Object>> fileInfoList = new ArrayList<>();
        if (files != null) {
            for (final File file : files) {
                final Map<String, Object> map = new HashMap<>();
                map.put("name", file.getName());
                map.put("size", file.length());
                map.put("lastModified", file.lastModified());
                fileInfoList.add(map);
            }
        }
        // 按最后修改时间降序排序并截取前10条
        fileInfoList.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        final List<Map<String, Object>> backupFiles = fileInfoList.size() > 10 ? fileInfoList.subList(0, 10) : fileInfoList;
        listData.put("backupFiles", backupFiles);
    }
}
