package com.weiqiang.dao;

import com.weiqiang.pojo.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 看板数据访问对象
 */
@Repository
public class DashboardDao extends BasicDao<Object> {

    // ==========================================
    // 共享/通用查询
    // ==========================================

    /**
     * 根据保管人获取其名下的所有设备（包含计算折旧所需的完整字段）
     */
    public List<Equipment> getEquipmentsByCustodian(String custodian) {
        String sql = "SELECT e.equip_id AS equipId, e.equip_name AS equipName, e.model, e.status, " +
                "e.purchase_date AS purchaseDate, e.original_value AS originalValue, " +
                "d.unit_code AS unitCode, d.unit_name AS unitName, " +
                "c.category_id AS categoryId, c.category_name AS categoryName, " +
                "c.useful_life AS usefulLife, c.residual_rate AS residualRate, e.custodian " +
                "FROM equipment e " +
                "JOIN department d ON e.unit_code = d.unit_code " +
                "JOIN category c ON e.category_id = c.category_id " +
                "WHERE e.custodian = ?";
        return mutiSelect(sql, Equipment.class, custodian);
    }

    // ==========================================
    // Role 0 (设备操作员) KPIs & listData
    // ==========================================

    public Long countEquipmentsByCustodian(String custodian) {
        String sql = "SELECT COUNT(*) FROM equipment WHERE custodian = ?";
        Object result = singleSelect(sql, custodian);
        return result != null ? (Long) result : 0L;
    }

    public Long countClaimsByApplicantAndStatus(String applicant, Integer status) {
        String sql = "SELECT COUNT(*) FROM t_equipment_claim WHERE applicant = ? AND status = ?";
        Object result = singleSelect(sql, applicant, status);
        return result != null ? (Long) result : 0L;
    }

    public Long countActiveMaintenancesByReporter(String reporter) {
        String sql = "SELECT COUNT(*) FROM maintenance_record WHERE reporter = ? AND maint_status IN (0, 1)";
        Object result = singleSelect(sql, reporter);
        return result != null ? (Long) result : 0L;
    }

    public List<EquipmentClaim> getClaimsByApplicant(String applicant) {
        String sql = "SELECT claim_id AS claimId, equip_id AS equipId, status, create_time AS createTime " +
                "FROM t_equipment_claim WHERE applicant = ? ORDER BY create_time DESC LIMIT 10";
        return mutiSelect(sql, EquipmentClaim.class, applicant);
    }

    public List<MaintenanceRecord> getMaintenancesByReporter(String reporter) {
        String sql = "SELECT maint_id AS maintId, equip_id AS equipId, maint_status AS maintStatus, fault_description AS faultDescription " +
                "FROM maintenance_record WHERE reporter = ? ORDER BY maint_id DESC LIMIT 10";
        return mutiSelect(sql, MaintenanceRecord.class, reporter);
    }

    // ==========================================
    // Role 1 (维修工) KPIs & charts & listData
    // ==========================================

    public Long countMaintenancesByPersonAndStatus(Integer personId, Integer status) {
        String sql = "SELECT COUNT(*) FROM maintenance_record WHERE maint_person_id = ? AND maint_status = ?";
        Object result = singleSelect(sql, personId, status);
        return result != null ? (Long) result : 0L;
    }

    public List<DashboardMaintTrend> getMaintCostTrendByPerson(Integer personId) {
        String sql = "SELECT DATE_FORMAT(maint_date, '%Y-%m') AS month, SUM(maint_cost) AS cost " +
                "FROM maintenance_record " +
                "WHERE maint_person_id = ? AND maint_status = 2 " +
                "GROUP BY DATE_FORMAT(maint_date, '%Y-%m') " +
                "ORDER BY month ASC";
        return mutiSelect(sql, DashboardMaintTrend.class, personId);
    }

    public List<MaintenanceRecord> getMaintenancesByPerson(Integer personId) {
        String sql = "SELECT maint_id AS maintId, equip_id AS equipId, fault_description AS faultDescription, maint_status AS maintStatus " +
                "FROM maintenance_record " +
                "WHERE maint_person_id = ? " +
                "ORDER BY maint_id DESC LIMIT 10";
        return mutiSelect(sql, MaintenanceRecord.class, personId);
    }

    // ==========================================
    // Role 2 (资产管理员) KPIs & charts & listData
    // ==========================================

    public Long countEquipmentsByUnit(String unitCode) {
        String sql = "SELECT COUNT(*) FROM equipment WHERE unit_code = ?";
        Object result = singleSelect(sql, unitCode);
        return result != null ? (Long) result : 0L;
    }

    public BigDecimal sumEquipmentValueByUnit(String unitCode) {
        String sql = "SELECT SUM(original_value) FROM equipment WHERE unit_code = ?";
        Object result = singleSelect(sql, unitCode);
        if (result == null) {
            return BigDecimal.ZERO;
        }
        return result instanceof BigDecimal ? (BigDecimal) result : new BigDecimal(result.toString());
    }

    public Long countEquipmentsByUnitAndStatus(String unitCode, String status) {
        String sql = "SELECT COUNT(*) FROM equipment WHERE unit_code = ? AND status = ?";
        Object result = singleSelect(sql, unitCode, status);
        return result != null ? (Long) result : 0L;
    }

    public List<DashboardChartData> getCategoryDistributionByUnit(String unitCode) {
        String sql = "SELECT c.category_name AS name, COUNT(*) AS value " +
                "FROM equipment e " +
                "JOIN category c ON e.category_id = c.category_id " +
                "WHERE e.unit_code = ? " +
                "GROUP BY c.category_name";
        return mutiSelect(sql, DashboardChartData.class, unitCode);
    }

    public List<DashboardChartData> getDepartmentDistributionByUnit(String unitCode) {
        String sql = "SELECT d.unit_name AS name, SUM(e.original_value) AS value " +
                "FROM equipment e " +
                "JOIN department d ON e.unit_code = d.unit_code " +
                "WHERE e.unit_code = ? " +
                "GROUP BY d.unit_name";
        return mutiSelect(sql, DashboardChartData.class, unitCode);
    }

    public List<DashboardMaintTrend> getMaintenanceTrendByUnit(String unitCode) {
        String sql = "SELECT DATE_FORMAT(mr.maint_date, '%Y-%m') AS month, SUM(mr.maint_cost) AS cost, COUNT(*) AS count " +
                "FROM maintenance_record mr " +
                "JOIN equipment e ON mr.equip_id = e.equip_id " +
                "WHERE e.unit_code = ? AND mr.maint_status = 2 " +
                "GROUP BY DATE_FORMAT(mr.maint_date, '%Y-%m') " +
                "ORDER BY month ASC";
        return mutiSelect(sql, DashboardMaintTrend.class, unitCode);
    }

    public List<EquipmentClaim> getPendingClaimsByUnit(String unitCode) {
        String sql = "SELECT c.claim_id AS claimId, c.equip_id AS equipId, e.equip_name AS equipName, " +
                "u.real_name AS applicantRealName, c.create_time AS createTime " +
                "FROM t_equipment_claim c " +
                "JOIN equipment e ON c.equip_id = e.equip_id " +
                "LEFT JOIN sys_user u ON c.applicant = u.username " +
                "WHERE e.unit_code = ? AND c.status = 0 " +
                "ORDER BY c.create_time DESC LIMIT 10";
        return mutiSelect(sql, EquipmentClaim.class, unitCode);
    }

    public List<MaintenanceRecord> getPendingMaintenancesByUnit(String unitCode) {
        String sql = "SELECT mr.maint_id AS maintId, mr.equip_id AS equipId, mr.fault_description AS faultDescription, mr.maint_status AS maintStatus " +
                "FROM maintenance_record mr " +
                "JOIN equipment e ON mr.equip_id = e.equip_id " +
                "WHERE e.unit_code = ? AND mr.maint_status = 0 " +
                "ORDER BY mr.maint_id DESC LIMIT 10";
        return mutiSelect(sql, MaintenanceRecord.class, unitCode);
    }

    // ==========================================
    // Role 3 (系统管理员) KPIs & charts
    // ==========================================

    public Long countAllEquipments() {
        String sql = "SELECT COUNT(*) FROM equipment";
        Object result = singleSelect(sql);
        return result != null ? (Long) result : 0L;
    }

    public BigDecimal sumAllEquipmentValue() {
        String sql = "SELECT SUM(original_value) FROM equipment";
        Object result = singleSelect(sql);
        if (result == null) {
            return BigDecimal.ZERO;
        }
        return result instanceof BigDecimal ? (BigDecimal) result : new BigDecimal(result.toString());
    }

    public Long countAllUsers() {
        String sql = "SELECT COUNT(*) FROM sys_user";
        Object result = singleSelect(sql);
        return result != null ? (Long) result : 0L;
    }

    public List<DashboardChartData> getUserRoleDistribution() {
        String sql = "SELECT " +
                "(CASE role " +
                "  WHEN 0 THEN '操作员' " +
                "  WHEN 1 THEN '维修工' " +
                "  WHEN 2 THEN '资产管理员' " +
                "  WHEN 3 THEN '系统管理员' " +
                " END) AS name, " +
                "COUNT(*) AS value " +
                "FROM sys_user " +
                "GROUP BY role";
        return mutiSelect(sql, DashboardChartData.class);
    }
}
