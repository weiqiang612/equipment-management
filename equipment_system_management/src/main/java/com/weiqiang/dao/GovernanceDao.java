package com.weiqiang.dao;

import com.weiqiang.pojo.EquipmentRiskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据治理 DAO
 */
@Repository
@RequiredArgsConstructor
public class GovernanceDao {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 统计指定单位的设备总数
     */
    public Integer countTotalEquipments(final String unitCode) {
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            final String sql = "SELECT COUNT(*) FROM equipment WHERE unit_code = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, unitCode);
        } else {
            final String sql = "SELECT COUNT(*) FROM equipment";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        }
    }

    /**
     * 统计缺失关键字段或状态非法的设备数
     */
    public Integer countMissingFieldsEquipments(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM equipment WHERE " +
                "status IS NULL OR status NOT IN ('在用', '维修', '报废') " +
                "OR category_id IS NULL OR unit_code IS NULL " +
                "OR purchase_date IS NULL OR original_value IS NULL " +
                "OR original_value <= 0"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND unit_code = ?");
            params.add(unitCode);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计保管人部门与设备所属部门不一致的设备数
     */
    public Integer countMismatchEquipments(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM equipment e " +
                "INNER JOIN sys_user u ON e.custodian = u.username " +
                "WHERE e.custodian IS NOT NULL AND e.unit_code != u.unit_code"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND e.unit_code = ?");
            params.add(unitCode);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计疑似重复的设备数
     */
    public Integer countDuplicateEquipments(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
                "SELECT IFNULL(SUM(cnt), 0) FROM (" +
                "  SELECT COUNT(*) AS cnt FROM equipment"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append("  WHERE unit_code = ?");
            params.add(unitCode);
        }
        sql.append("  GROUP BY equip_name, model, unit_code, purchase_date, original_value" +
                   "  HAVING COUNT(*) > 1" +
                   ") t");
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计长期空闲设备数（保管人为 NULL 且状态为 '在用'）
     */
    public Integer countIdleEquipments(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM equipment WHERE custodian IS NULL AND status = '在用'"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND unit_code = ?");
            params.add(unitCode);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计维修费用占比异常的设备数 (维修费用/原值 >= 0.15且未报废)
     */
    public Integer countCostAnomalies(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM (" +
                "  SELECT e.original_value, SUM(IFNULL(mr.maint_cost, 0)) AS totalMaintCost " +
                "  FROM equipment e " +
                "  LEFT JOIN maintenance_record mr ON e.equip_id = mr.equip_id " +
                "  WHERE e.status != '报废'"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append("  AND e.unit_code = ?");
            params.add(unitCode);
        }
        sql.append("  GROUP BY e.equip_id, e.original_value" +
                   ") t WHERE t.original_value > 0 AND (t.totalMaintCost / t.original_value) >= 0.15");
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计存在任何数据质量问题的设备总数 (去重)
     */
    public Integer countUniqueIssueEquipments(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT e.equip_id) FROM equipment e " +
            "LEFT JOIN sys_user u ON e.custodian = u.username " +
            "LEFT JOIN ( " +
            "  SELECT equip_name, model, unit_code, purchase_date, original_value " +
            "  FROM equipment " +
            "  GROUP BY equip_name, model, unit_code, purchase_date, original_value " +
            "  HAVING COUNT(*) > 1 " +
            ") dup ON e.equip_name = dup.equip_name " +
            "     AND IFNULL(e.model, '') = IFNULL(dup.model, '') " +
            "     AND e.unit_code = dup.unit_code " +
            "     AND e.purchase_date = dup.purchase_date " +
            "     AND e.original_value = dup.original_value " +
            "WHERE (e.status IS NULL OR e.status NOT IN ('在用', '维修', '报废') " +
            "       OR e.category_id IS NULL OR e.unit_code IS NULL " +
            "       OR e.purchase_date IS NULL OR e.original_value IS NULL " +
            "       OR e.original_value <= 0) " +
            "   OR (e.custodian IS NOT NULL AND e.unit_code != u.unit_code) " +
            "   OR (dup.equip_name IS NOT NULL)"
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND e.unit_code = ?");
            params.add(unitCode);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 统计高中低风险分布数量
     */
    public Map<String, Integer> getRiskDistribution(final String unitCode) {
        final StringBuilder sql = new StringBuilder(
            "SELECT " +
            "  SUM(CASE WHEN riskLevel = '高风险' THEN 1 ELSE 0 END) AS highRiskCount, " +
            "  SUM(CASE WHEN riskLevel = '中风险' THEN 1 ELSE 0 END) AS mediumRiskCount, " +
            "  SUM(CASE WHEN riskLevel = '低风险' THEN 1 ELSE 0 END) AS lowRiskCount " +
            "FROM (" +
            "  SELECT " +
            "    e.status, " +
            "    CASE " +
            "      WHEN e.status != '报废' AND ( " +
            "        (IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) >= 0.9) OR " +
            "        (COUNT(mr.maint_id) >= 3) OR " +
            "        (IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) >= 0.3) " +
            "      ) THEN '高风险' " +
            "      WHEN e.status != '报废' AND ( " +
            "        (IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) >= 0.75) OR " +
            "        (COUNT(mr.maint_id) >= 2) OR " +
            "        (IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) >= 0.15) OR " +
            "        (e.status = '维修') " +
            "      ) THEN '中风险' " +
            "      ELSE '低风险' " +
            "    END AS riskLevel " +
            "  FROM equipment e " +
            "  LEFT JOIN category c ON e.category_id = c.category_id " +
            "  LEFT JOIN maintenance_record mr ON e.equip_id = mr.equip_id "
        );
        final List<Object> params = new ArrayList<>();
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append("  WHERE e.unit_code = ? ");
            params.add(unitCode);
        }
        sql.append(
            "  GROUP BY e.equip_id, e.status, e.purchase_date, e.original_value, c.useful_life" +
            ") t"
        );

        final Map<String, Object> result = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
        final Map<String, Integer> counts = new HashMap<String, Integer>((int) ((3 / 0.75f) + 1));
        counts.put("high", result.get("highRiskCount") != null ? ((Number) result.get("highRiskCount")).intValue() : 0);
        counts.put("medium", result.get("mediumRiskCount") != null ? ((Number) result.get("mediumRiskCount")).intValue() : 0);
        counts.put("low", result.get("lowRiskCount") != null ? ((Number) result.get("lowRiskCount")).intValue() : 0);
        return counts;
    }

    /**
     * 构造通用的风险设备清单查询 SQL 的一部分（子查询）
     */
    private String buildRiskBaseSql() {
        return "SELECT " +
               "  e.equip_id AS equipId, " +
               "  e.equip_name AS equipName, " +
               "  e.model AS model, " +
               "  e.status AS status, " +
               "  e.purchase_date AS purchaseDate, " +
               "  e.original_value AS originalValue, " +
               "  e.unit_code AS unitCode, " +
               "  e.category_id AS categoryId, " +
               "  e.custodian AS custodian, " +
               "  d.unit_name AS unitName, " +
               "  c.category_name AS categoryName, " +
               "  COUNT(mr.maint_id) AS maintenanceCount, " +
               "  IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) AS ageRatio, " +
               "  IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) AS costRatio, " +
               "  CASE " +
               "    WHEN e.status != '报废' AND ( " +
               "      (IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) >= 0.9) OR " +
               "      (COUNT(mr.maint_id) >= 3) OR " +
               "      (IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) >= 0.3) " +
               "    ) THEN '高风险' " +
               "    WHEN e.status != '报废' AND ( " +
               "      (IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) >= 0.75) OR " +
               "      (COUNT(mr.maint_id) >= 2) OR " +
               "      (IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) >= 0.15) OR " +
               "      (e.status = '维修') " +
               "    ) THEN '中风险' " +
               "    ELSE '低风险' " +
               "  END AS riskLevel " +
               "FROM equipment e " +
               "LEFT JOIN department d ON e.unit_code = d.unit_code " +
               "LEFT JOIN category c ON e.category_id = c.category_id " +
               "LEFT JOIN maintenance_record mr ON e.equip_id = mr.equip_id " +
               "GROUP BY " +
               "  e.equip_id, " +
               "  e.equip_name, " +
               "  e.model, " +
               "  e.status, " +
               "  e.purchase_date, " +
               "  e.original_value, " +
               "  e.unit_code, " +
               "  e.category_id, " +
               "  e.custodian, " +
               "  d.unit_name, " +
               "  c.category_name, " +
               "  c.useful_life";
    }

    /**
     * 分页查询风险设备列表对应的总数
     */
    public Integer countEquipmentRisks(final String riskLevel, final String unitCode, final String categoryId) {
        final StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ( ");
        sql.append(buildRiskBaseSql());
        sql.append(" ) t WHERE 1 = 1");
        final List<Object> params = new ArrayList<>();

        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            sql.append(" AND t.riskLevel = ?");
            params.add(riskLevel);
        }
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND t.unitCode = ?");
            params.add(unitCode);
        }
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            sql.append(" AND t.categoryId = ?");
            params.add(categoryId);
        }

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    /**
     * 分页查询风险设备列表
     */
    public List<EquipmentRiskVO> listEquipmentRisks(final String riskLevel, final String unitCode, final String categoryId, final Integer offset, final Integer limit) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM ( ");
        sql.append(buildRiskBaseSql());
        sql.append(" ) t WHERE 1 = 1");
        final List<Object> params = new ArrayList<>();

        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            sql.append(" AND t.riskLevel = ?");
            params.add(riskLevel);
        }
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append(" AND t.unitCode = ?");
            params.add(unitCode);
        }
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            sql.append(" AND t.categoryId = ?");
            params.add(categoryId);
        }

        sql.append(" ORDER BY t.equipId ASC");
        if (offset != null && limit != null) {
            sql.append(" LIMIT ?, ?");
            params.add(offset);
            params.add(limit);
        }

        return jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(EquipmentRiskVO.class), params.toArray());
    }
}
