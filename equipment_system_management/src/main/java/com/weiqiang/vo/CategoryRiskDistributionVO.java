package com.weiqiang.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRiskDistributionVO {
    private String categoryId;
    private String categoryName;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
}
