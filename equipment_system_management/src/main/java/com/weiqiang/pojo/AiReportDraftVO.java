package com.weiqiang.pojo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 报告草案响应对象
 */
@Data
@Builder
public class AiReportDraftVO {
    private String title;
    private String content;
    private String period;
    private LocalDateTime generatedTime;
}
