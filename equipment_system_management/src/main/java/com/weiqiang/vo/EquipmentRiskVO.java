package com.weiqiang.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 风险设备分析 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentRiskVO {
    private String equipId;
    private String equipName;
    private String model;
    private String categoryId;
    private String categoryName;
    private String unitCode;
    private String unitName;
    private String custodian;
    private String status;
    private Integer healthScore;
    private String riskLevel;
    private String riskReasons;
    private Integer maintenanceCount;
    private Double costRatio;
    private Double ageRatio;
    private BigDecimal originalValue;
    private LocalDate purchaseDate;
}
