package com.weiqiang.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.pojo.*;
import com.weiqiang.service.AiAssistantService;
import com.weiqiang.service.DashboardService;
import com.weiqiang.service.EquipmentService;
import com.weiqiang.service.GovernanceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 辅助服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService, InitializingBean {

    private final DashboardService dashboardService;
    private final GovernanceService governanceService;
    private final EquipmentService equipmentService;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    @Value("${ai.provider.api-key:}")
    private String apiKey;

    @Value("${ai.provider.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.provider.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.provider.timeout-ms:30000}")
    private int timeoutMs;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public AiReportDraftVO generateOperationReportDraft(final String period, final Integer role, final String currentUnitCode) {
        // 1. 检验配置与权限
        checkApiKeyConfigured();

        log.info("开始生成资产运营报告草案. period: {}, role: {}, unitCode: {}", period, role, currentUnitCode);

        // 2. 聚合上下文数据
        final DashboardSummaryVO dashboardSummary = dashboardService.getDashboardSummary();
        final GovernanceSummaryVO governanceSummary = governanceService.getGovernanceSummary(role, currentUnitCode);

        // 3. 构建 Prompt
        final String systemPrompt = "你是一个资产设备管理专家。请基于以下提供的系统运营和数据治理统计结果，生成一份资产运营分析报告草案（请输出排版规整的美观 Markdown 格式，不允许含有任何 HTML 标签，且避免泄露敏感的系统字段）。";
        
        final StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("数据周期：").append("weekly".equalsIgnoreCase(period) ? "本周 (Weekly)" : "本月 (Monthly)").append("\n\n");
        userPrompt.append("## 一、 看板核心 KPI 数据\n");
        if (dashboardSummary != null && dashboardSummary.getKpis() != null) {
            for (final Map.Entry<String, Object> entry : dashboardSummary.getKpis().entrySet()) {
                userPrompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else {
            userPrompt.append("（无看板统计数据）\n");
        }

        userPrompt.append("\n## 二、 数据治理与运营风险摘要\n");
        if (governanceSummary != null) {
            userPrompt.append("- 资产治理评分: ").append(governanceSummary.getQualityScore()).append("分\n");
            userPrompt.append("- 质量存疑资产总数: ").append(governanceSummary.getIssueCount()).append("\n");
            userPrompt.append("- 疑似重复录入资产数: ").append(governanceSummary.getDuplicateCount()).append("\n");
            userPrompt.append("- 高风险设备总数: ").append(governanceSummary.getHighRiskCount()).append("\n");
            userPrompt.append("- 中风险设备总数: ").append(governanceSummary.getMediumRiskCount()).append("\n");
            userPrompt.append("- 长期空闲状态设备数: ").append(governanceSummary.getIdleCount()).append("\n");
            userPrompt.append("- 维保成本偏高异常数: ").append(governanceSummary.getCostAnomalyCount()).append("\n");
        } else {
            userPrompt.append("（无数据治理统计）\n");
        }

        userPrompt.append("\n请分析上述数据并生成报告，要求草案清晰呈现：\n")
                  .append("1. 资产原值总额及状态分布的趋势点评\n")
                  .append("2. 本期数据治理评分和突出质量风险解读（包括疑似重复、信息缺失或长期闲置设备）\n")
                  .append("3. 设备维保高频与成本异常支出的原因分析\n")
                  .append("4. 本单位下阶段应该优先跟进的代办提醒与具体处置建议。");

        // 4. 发送外部 AI 接口请求
        final String aiResponseText = callChatCompletions(systemPrompt, userPrompt.toString());

        return AiReportDraftVO.builder()
                .title("weekly".equalsIgnoreCase(period) ? "资产运营周报 AI 草案" : "资产运营月报 AI 草案")
                .content(aiResponseText)
                .period(period)
                .generatedTime(LocalDateTime.now())
                .build();
    }

    @Override
    public AiEquipmentSummaryVO generateEquipmentSummary(final String equipId, final Integer role, final String currentUnitCode, final String currentUsername) {
        // 1. 加载设备详情并执行跨单位水平隔离越权校验
        final EquipmentDetailVO detail = equipmentService.getEquipmentDetail(equipId);
        if (detail == null) {
            throw new BusinessException("操作失败：目标设备不存在！");
        }

        // 跨单位水平越权校验：只要当前请求的不是全局系统管理员 (role != 3)，所有角色都必须归属于该设备的单位下
        if (role == null || role != 3) {
            if (detail.getUnitCode() == null || !detail.getUnitCode().equals(currentUnitCode)) {
                log.warn("用户 {} 越权试图获取其它单位 {} 设备的生命周期分析: {}", currentUsername, detail.getUnitCode(), equipId);
                throw new ForbiddenException("权限不足：无权跨单位访问设备生命周期数据！");
            }
        }

        // 2. 检验配置
        checkApiKeyConfigured();

        log.info("开始生成设备 {} 的生命周期分析摘要. 申请人: {}", equipId, currentUsername);

        // 3. 构建 Prompt
        final String systemPrompt = "你是一个固定资产技术诊断专家。请基于以下这台设备的生命周期数据，生成该设备的生命周期健康摘要及处置处置建议（请输出排版规整的 Markdown 格式）。";

        final StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("### 设备基础信息\n")
                  .append("- 设备编号: ").append(detail.getEquipId()).append("\n")
                  .append("- 设备名称: ").append(detail.getEquipName()).append("\n")
                  .append("- 型号规格: ").append(detail.getModel()).append("\n")
                  .append("- 当前状态: ").append(detail.getStatus()).append("\n")
                  .append("- 购入日期: ").append(detail.getPurchaseDate()).append("\n")
                  .append("- 原值: ").append(detail.getOriginalValue()).append(" 元\n")
                  .append("- 所属单位: ").append(detail.getUnitName()).append("\n")
                  .append("- 设备分类: ").append(detail.getCategoryName()).append("\n");

        userPrompt.append("\n### 折旧状况\n")
                  .append("- 预计折旧年限: ").append(detail.getUsefulLife()).append(" 年\n")
                  .append("- 残值率: ").append(detail.getResidualRate()).append("\n")
                  .append("- 累计折旧额: ").append(detail.getAccumulatedDepreciation()).append(" 元\n")
                  .append("- 净值: ").append(detail.getNetValue()).append(" 元\n");

        userPrompt.append("\n### 设备流转历史\n");
        userPrompt.append("- 领用申请次数: ").append(detail.getClaims() != null ? detail.getClaims().size() : 0).append("\n");
        userPrompt.append("- 调拨记录次数: ").append(detail.getTransfers() != null ? detail.getTransfers().size() : 0).append("\n");
        userPrompt.append("- 历史检修记录次数: ").append(detail.getMaintenances() != null ? detail.getMaintenances().size() : 0).append("\n");

        if (detail.getMaintenances() != null && !detail.getMaintenances().isEmpty()) {
            userPrompt.append("\n部分检修明细：\n");
            for (int i = 0; i < Math.min(detail.getMaintenances().size(), 5); i++) {
                final MaintenanceRecord rec = detail.getMaintenances().get(i);
                userPrompt.append("  * 维保日期: ").append(rec.getMaintDate())
                          .append(", 状态: ").append(rec.getMaintStatus() == 3 ? "已复核可用" : (rec.getMaintStatus() == 4 ? "复核转报废" : "维修中"))
                          .append(", 费用: ").append(rec.getMaintCost()).append(" 元")
                          .append(", 故障描述: ").append(rec.getFaultDescription())
                          .append(", 维保内容: ").append(rec.getMaintContent()).append("\n");
            }
        }

        userPrompt.append("\n请分析该设备的数据并给出分析摘要：\n")
                  .append("1. 该设备当前所处生命周期阶段（折旧占比与物理健康度评估）\n")
                  .append("2. 该设备的核心隐患说明（如频繁报修、折旧到期、高额累计维保费等）\n")
                  .append("3. 给出该设备明确的人工复核意见与最终处置策略（如：继续在用、加强维护、限期大修或建议直接报废处置）。");

        // 4. 发送外部请求
        final String aiResponseText = callChatCompletions(systemPrompt, userPrompt.toString());

        // 确定风险级别 (基于年限或费用)
        String riskLevel = "low";
        if (detail.getNetValue() != null && detail.getOriginalValue() != null && detail.getOriginalValue().doubleValue() > 0) {
            final double depreciationRatio = detail.getAccumulatedDepreciation().doubleValue() / detail.getOriginalValue().doubleValue();
            if (depreciationRatio >= 0.9 || (detail.getMaintenances() != null && detail.getMaintenances().size() >= 3)) {
                riskLevel = "high";
            } else if (depreciationRatio >= 0.75 || (detail.getMaintenances() != null && detail.getMaintenances().size() >= 2)) {
                riskLevel = "medium";
            }
        }

        return AiEquipmentSummaryVO.builder()
                .equipId(detail.getEquipId())
                .equipName(detail.getEquipName())
                .summary(aiResponseText)
                .riskLevel(riskLevel)
                .generatedTime(LocalDateTime.now())
                .build();
    }

    /**
     * 校验 API Key 是否已配置
     */
    private void checkApiKeyConfigured() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("检测到 AI Provider API Key 未配置，抛出业务异常执行优雅降级");
            throw new BusinessException("AI 辅助服务未启用：请联系管理员配置 AI 接口凭证");
        }
    }

    /**
     * 调用 OpenAI 接口的底层网络交互
     */
    private String callChatCompletions(final String systemPrompt, final String userPrompt) {
        try {
            // 构建消息请求体
            final List<OpenAiMessage> messages = new ArrayList<>((int) ((2 / 0.75f) + 1));
            messages.add(new OpenAiMessage("system", systemPrompt));
            messages.add(new OpenAiMessage("user", userPrompt));

            final OpenAiRequest openAiRequest = new OpenAiRequest(model, messages);
            final String requestBody = objectMapper.writeValueAsString(openAiRequest);

            final String completionsUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            log.info("向 AI 接口发起 POST 请求. URL: {}, Model: {}", completionsUrl, model);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(completionsUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();

            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseBody = response.body();

            if (statusCode != 200) {
                log.error("AI 接口返回错误响应. HTTP Status: {}, Body: {}", statusCode, responseBody);
                throw new BusinessException("AI 接口调用失败 (HTTP " + statusCode + "): 接口授权过期或外部服务暂时不可用！");
            }

            // 解析大模型返回的数据
            final ChatCompletionResponse completionResponse = objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            if (completionResponse != null 
                    && completionResponse.getChoices() != null 
                    && !completionResponse.getChoices().isEmpty()
                    && completionResponse.getChoices().get(0).getMessage() != null) {
                return completionResponse.getChoices().get(0).getMessage().getContent();
            }

            throw new BusinessException("AI 接口调用成功但返回了空数据或不兼容的响应格式！");

        } catch (final HttpTimeoutException e) {
            log.error("AI 接口调用响应超时", e);
            throw new BusinessException("AI 辅助服务响应超时：连接外部大模型服务超时，请稍后重试！");
        } catch (final IOException | InterruptedException e) {
            log.error("AI 接口通信异常", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("AI 辅助服务异常：大模型连接中断，具体原因: " + e.getMessage());
        }
    }

    // OpenAI 协议传输辅助类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OpenAiRequest {
        private String model;
        private List<OpenAiMessage> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class OpenAiMessage {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionResponse {
        private List<Choice> choices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private Message message;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            private String role;
            private String content;
        }
    }
}
