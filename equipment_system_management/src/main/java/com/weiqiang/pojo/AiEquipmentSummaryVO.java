package com.weiqiang.pojo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 设备生命周期分析摘要响应对象
 */
@Data
@Builder
public class AiEquipmentSummaryVO {
    private String equipId;
    private String equipName;
    private String summary;
    private String riskLevel;
    private LocalDateTime generatedTime;
}
