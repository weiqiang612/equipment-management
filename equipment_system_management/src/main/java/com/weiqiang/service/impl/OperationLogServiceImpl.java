package com.weiqiang.service.impl;

import com.weiqiang.dao.OperationLogDao;
import com.weiqiang.pojo.OperationLog;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.service.OperationLogService;
import com.weiqiang.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogDao operationLogDao;
    private final com.weiqiang.dao.UserDao userDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String opType, String targetType, String targetId, String summary, Integer status, String errorMsg) {
        String currentName = BaseContext.getCurrentName();
        Integer currentRole = BaseContext.getCurrentRole();

        if (currentName == null) {
            currentName = "system";
        }
        if (currentRole == null) {
            currentRole = 3;
        }

        OperationLog logEntity = new OperationLog();
        logEntity.setOperator(currentName);
        logEntity.setOperatorRole(currentRole);
        logEntity.setOpType(opType);
        logEntity.setTargetType(targetType);
        logEntity.setTargetId(targetId);
        logEntity.setSummary(summary);
        logEntity.setStatus(status);
        logEntity.setErrorMsg(errorMsg);
        logEntity.setOpTime(LocalDateTime.now());

        int count = operationLogDao.insert(logEntity);
        if (count <= 0) {
            throw new RuntimeException("写入审计日志失败");
        }
    }

    @Override
    public PageBean listLogs(String operator, String opType, String targetType, Integer status, Integer page, Integer pageSize) {
        long total = operationLogDao.count(operator, opType, targetType, status);
        List<OperationLog> rows = operationLogDao.select(operator, opType, targetType, status, page, pageSize);
        
        List<com.weiqiang.pojo.OperationLogVO> voRows = new java.util.ArrayList<>();
        if (rows != null) {
            for (OperationLog logEntity : rows) {
                com.weiqiang.pojo.OperationLogVO vo = new com.weiqiang.pojo.OperationLogVO();
                vo.setId(logEntity.getId());
                vo.setOperator(logEntity.getOperator());
                vo.setOperatorRole(logEntity.getOperatorRole());
                vo.setOpType(logEntity.getOpType());
                vo.setTargetType(logEntity.getTargetType());
                vo.setTargetId(logEntity.getTargetId());
                vo.setOpTime(logEntity.getOpTime());
                vo.setSummary(logEntity.getSummary());
                vo.setStatus(logEntity.getStatus());
                vo.setErrorMsg(logEntity.getErrorMsg());
                
                if (logEntity.getOperator() != null) {
                    com.weiqiang.pojo.User user = userDao.getByUsername(logEntity.getOperator());
                    if (user != null) {
                        vo.setOperatorRealName(user.getRealName());
                    }
                }
                voRows.add(vo);
            }
        }
        return new PageBean(total, voRows);
    }
}
