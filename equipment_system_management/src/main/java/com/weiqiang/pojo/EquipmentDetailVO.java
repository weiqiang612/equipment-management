package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDetailVO {
    // 基础属性
    private String equipId;
    private String equipName;
    private String model;
    private String status;
    private LocalDate purchaseDate;
    private BigDecimal originalValue;
    private String unitCode;
    private String unitName;
    private String categoryId;
    private String categoryName;

    // 保管属性
    private String custodian;
    private String custodianRealName;

    // 折旧属性
    private Integer usefulLife;
    private BigDecimal residualRate;
    private BigDecimal monthlyDepreciation;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netValue;

    // 历史及审计
    private List<EquipmentClaim> claims;
    private List<MaintenanceRecord> maintenances;
    private List<TransferRecord> transfers;
    private ScrapRecord scrap;
    private List<OperationLogVO> auditTimeline;
}
