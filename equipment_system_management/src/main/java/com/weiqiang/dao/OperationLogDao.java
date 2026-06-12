package com.weiqiang.dao;

import com.weiqiang.pojo.OperationLog;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OperationLogDao extends BasicDao<OperationLog> {

    public int insert(OperationLog log) {
        String sql = "INSERT INTO operation_log (operator, operator_role, op_type, target_type, target_id, summary, status, error_msg, op_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return update(sql, log.getOperator(), log.getOperatorRole(), log.getOpType(), 
                      log.getTargetType(), log.getTargetId(), log.getSummary(), 
                      log.getStatus(), log.getErrorMsg(), log.getOpTime());
    }

    public List<OperationLog> select(String operator, String opType, String targetType, Integer status, Integer page, Integer pageSize) {
        StringBuilder sql = new StringBuilder("SELECT id, operator, operator_role as operatorRole, op_type as opType, " +
                "target_type as targetType, target_id as targetId, op_time as opTime, summary, status, error_msg as errorMsg " +
                "FROM operation_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (operator != null && !operator.trim().isEmpty()) {
            sql.append(" AND operator = ?");
            params.add(operator);
        }
        if (opType != null && !opType.trim().isEmpty()) {
            sql.append(" AND op_type = ?");
            params.add(opType);
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            sql.append(" AND target_type = ?");
            params.add(targetType);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY op_time DESC");

        if (page != null && pageSize != null) {
            int start = (page - 1) * pageSize;
            sql.append(" LIMIT ?, ?");
            params.add(start);
            params.add(pageSize);
        }

        return mutiSelect(sql.toString(), OperationLog.class, params.toArray());
    }

    public long count(String operator, String opType, String targetType, Integer status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM operation_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (operator != null && !operator.trim().isEmpty()) {
            sql.append(" AND operator = ?");
            params.add(operator);
        }
        if (opType != null && !opType.trim().isEmpty()) {
            sql.append(" AND op_type = ?");
            params.add(opType);
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            sql.append(" AND target_type = ?");
            params.add(targetType);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        Object result = singleSelect(sql.toString(), params.toArray());
        return result == null ? 0L : ((Number) result).longValue();
    }
}
