package com.weiqiang.pojo;

import lombok.Data;

/**
 * AI 报告生成请求对象
 */
@Data
public class AiDraftReportRequest {
    private String period; // weekly / monthly
}
