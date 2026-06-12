package com.weiqiang.controller;

import com.weiqiang.pojo.PageBean;
import com.weiqiang.pojo.Result;
import com.weiqiang.service.OperationLogService;
import com.weiqiang.anno.RequiresRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/list")
    @RequiresRoles({3})
    public Result listLogs(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String opType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageBean pageBean = operationLogService.listLogs(operator, opType, targetType, status, page, pageSize);
        return Result.success(pageBean);
    }
}
