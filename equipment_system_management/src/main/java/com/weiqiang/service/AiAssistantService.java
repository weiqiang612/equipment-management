package com.weiqiang.service;

import com.weiqiang.pojo.AiReportDraftVO;
import com.weiqiang.pojo.AiEquipmentSummaryVO;

/**
 * AI 辅助服务接口
 */
public interface AiAssistantService {
    /**
     * 生成资产运营报告草案 (周报/月报)
     */
    AiReportDraftVO generateOperationReportDraft(final String period, final Integer role, final String currentUnitCode);

    /**
     * 生成单台设备的生命周期分析与建议
     */
    AiEquipmentSummaryVO generateEquipmentSummary(final String equipId, final Integer role, final String currentUnitCode, final String currentUsername);
}
