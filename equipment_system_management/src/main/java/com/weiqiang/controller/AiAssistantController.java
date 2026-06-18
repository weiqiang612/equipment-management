package com.weiqiang.controller;

import com.weiqiang.common.Result;
import com.weiqiang.dto.AiDraftReportRequest;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.service.AiAssistantService;
import com.weiqiang.utils.BaseContext;
import com.weiqiang.vo.AiEquipmentSummaryVO;
import com.weiqiang.vo.AiReportDraftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 辅助控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    /**
     * 生成资产运营报告草案 (周报/月报)
     * 仅限资产管理员 (role=2) 与系统管理员 (role=3) 调用
     */
    @PostMapping("/reports/operations/draft")
    public Result draftOperationReport(@RequestBody final AiDraftReportRequest request) {
        final Integer role = BaseContext.getCurrentRole();
        final String currentUnitCode = BaseContext.getCurrentUnitCode();
        final String currentUsername = BaseContext.getCurrentName();

        log.info("用户 {} (role: {}, unitCode: {}) 请求生成资产运营报告草案", currentUsername, role, currentUnitCode);

        // 垂直越权检查：仅限角色 >= 2
        if (role == null || role < 2) {
            log.warn("用户 {} 越权尝试访问 AI 运营报告接口被拦截", currentUsername);
            throw new ForbiddenException("权限不足：无权访问 AI 运营分析报告！");
        }

        final String period = request != null ? request.getPeriod() : "weekly";
        final AiReportDraftVO draftVO = aiAssistantService.generateOperationReportDraft(period, role, currentUnitCode);
        return Result.success(draftVO);
    }

    /**
     * 生成单台设备生命周期摘要与建议
     * 仅限资产管理员 (role=2) 与系统管理员 (role=3) 调用，且服务层会实施跨单位隔离校验
     */
    @PostMapping("/equipment/{equipId}/summary")
    public Result summarizeEquipment(@PathVariable("equipId") final String equipId) {
        final Integer role = BaseContext.getCurrentRole();
        final String currentUnitCode = BaseContext.getCurrentUnitCode();
        final String currentUsername = BaseContext.getCurrentName();

        log.info("用户 {} (role: {}, unitCode: {}) 请求生成设备 {} 的生命周期摘要", currentUsername, role, currentUnitCode, equipId);

        // 垂直越权检查：仅限角色 >= 2
        if (role == null || role < 2) {
            log.warn("用户 {} 越权尝试访问 AI 设备摘要接口被拦截", currentUsername);
            throw new ForbiddenException("权限不足：无权访问 AI 设备生命周期分析！");
        }

        final AiEquipmentSummaryVO summaryVO = aiAssistantService.generateEquipmentSummary(equipId, role, currentUnitCode, currentUsername);
        return Result.success(summaryVO);
    }
}
