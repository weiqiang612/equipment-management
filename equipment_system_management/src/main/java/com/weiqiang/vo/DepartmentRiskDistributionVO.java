package com.weiqiang.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRiskDistributionVO {
    private String unitCode;
    private String unitName;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
}
