package com.weiqiang.service;

import com.weiqiang.common.PageBean;

public interface OperationLogService {
    void record(String opType, String targetType, String targetId, String summary, Integer status, String errorMsg);
    PageBean listLogs(String operator, String opType, String targetType, Integer status, Integer page, Integer pageSize);
}
