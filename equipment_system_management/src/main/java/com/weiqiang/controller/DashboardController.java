package com.weiqiang.controller;

import com.weiqiang.vo.DashboardSummaryVO;
import com.weiqiang.common.Result;
import com.weiqiang.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取看板聚合数据
     */
    @GetMapping("/summary")
    public Result getDashboardSummary() {
        log.info("接收到获取看板聚合数据请求");
        final DashboardSummaryVO summary = dashboardService.getDashboardSummary();
        return Result.success(summary);
    }
}
