package com.weiqiang.dao;

import com.weiqiang.pojo.SysMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统消息数据访问对象
 */
@Repository
public class SysMessageDao extends BasicDao<SysMessage> {

    /**
     * 分页获取当前用户有效消息列表（包含状态过滤）
     */
    public List<SysMessage> listByTargetUserAndStatus(final String targetUser, final Integer status, final Integer offset, final Integer limit) {
        final StringBuilder sql = new StringBuilder(
            "SELECT id, title, content, event_type AS eventType, target_user AS targetUser, " +
            "status, is_valid AS isValid, ref_type AS refType, ref_id AS refId, " +
            "create_time AS createTime, update_time AS updateTime FROM sys_message WHERE target_user = ? AND is_valid = 1"
        );
        final List<Object> params = new ArrayList<>((int) ((4 / 0.75f) + 1));
        params.add(targetUser);

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return mutiSelect(sql.toString(), SysMessage.class, params.toArray());
    }

    /**
     * 统计未读且有效消息数
     */
    public Integer countUnreadByTargetUser(final String targetUser) {
        final String sql = "SELECT COUNT(id) FROM sys_message WHERE target_user = ? AND status = 0 AND is_valid = 1";
        final Object result = singleSelect(sql, targetUser);
        return result != null ? ((Number) result).intValue() : 0;
    }

    /**
     * 统计当前用户的消息总条数（包含状态过滤）
     */
    public Integer countByTargetUserAndStatus(final String targetUser, final Integer status) {
        final StringBuilder sql = new StringBuilder("SELECT COUNT(id) FROM sys_message WHERE target_user = ? AND is_valid = 1");
        final List<Object> params = new ArrayList<>((int) ((3 / 0.75f) + 1));
        params.add(targetUser);

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        final Object result = singleSelect(sql.toString(), params.toArray());
        return result != null ? ((Number) result).intValue() : 0;
    }

    /**
     * 一键标记所有消息为已读
     */
    public int updateStatusByTargetUser(final String targetUser, final Integer status) {
        final String sql = "UPDATE sys_message SET status = ? WHERE target_user = ? AND is_valid = 1 AND status != ?";
        return update(sql, status, targetUser, status);
    }

    /**
     * 标记单条消息为已读
     */
    public int updateStatusById(final Integer id, final Integer status) {
        final String sql = "UPDATE sys_message SET status = ? WHERE id = ?";
        return update(sql, status, id);
    }

    /**
     * 将某些有效消息置为失效
     */
    public int updateIsValidByRef(final String refType, final String refId, final Integer isValid) {
        final String sql = "UPDATE sys_message SET is_valid = ? WHERE ref_type = ? AND ref_id = ?";
        return update(sql, isValid, refType, refId);
    }

    /**
     * 根据主键将特定的消息置为失效
     */
    public int updateIsValidById(final Integer id, final Integer isValid) {
        final String sql = "UPDATE sys_message SET is_valid = ? WHERE id = ?";
        return update(sql, isValid, id);
    }

    /**
     * 插入新消息
     */
    public int insert(final SysMessage message) {
        final String sql = "INSERT INTO sys_message (title, content, event_type, target_user, status, is_valid, ref_type, ref_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return update(sql, message.getTitle(), message.getContent(), message.getEventType(),
                      message.getTargetUser(), message.getStatus(), message.getIsValid(),
                      message.getRefType(), message.getRefId());
    }

    /**
     * 根据 ID 获取单条消息
     */
    public SysMessage getById(final Integer id) {
        final String sql = "SELECT id, title, content, event_type AS eventType, target_user AS targetUser, " +
                           "status, is_valid AS isValid, ref_type AS refType, ref_id AS refId, " +
                           "create_time AS createTime, update_time AS updateTime FROM sys_message WHERE id = ?";
        return selectOne(sql, SysMessage.class, id);
    }

    /**
     * 获取用户所有当前有效的消息
     */
    public List<SysMessage> listValidMessagesByTargetUser(final String targetUser) {
        final String sql = "SELECT id, title, content, event_type AS eventType, target_user AS targetUser, " +
                           "status, is_valid AS isValid, ref_type AS refType, ref_id AS refId, " +
                           "create_time AS createTime, update_time AS updateTime FROM sys_message WHERE target_user = ? AND is_valid = 1";
        return mutiSelect(sql, SysMessage.class, targetUser);
    }

    /**
     * 获取特定的有效消息（用于去重校验）
     */
    public SysMessage getValidMessageByRef(final String targetUser, final String eventType, final String refType, final String refId) {
        final String sql = "SELECT id, title, content, event_type AS eventType, target_user AS targetUser, " +
                           "status, is_valid AS isValid, ref_type AS refType, ref_id AS refId, " +
                           "create_time AS createTime, update_time AS updateTime FROM sys_message " +
                           "WHERE target_user = ? AND event_type = ? AND ref_type = ? AND ref_id = ? AND is_valid = 1 LIMIT 1";
        return selectOne(sql, SysMessage.class, targetUser, eventType, refType, refId);
    }

    /**
     * 规则扫描 1：扫描单位内的所有高风险设备
     */
    public List<SysMessage> listHighRiskEquipment(final String unitCode) {
        final String sql = "SELECT e.equip_id AS refId, e.equip_name AS title " +
                           "FROM equipment e " +
                           "LEFT JOIN category c ON e.category_id = c.category_id " +
                           "LEFT JOIN maintenance_record mr ON e.equip_id = mr.equip_id " +
                           "WHERE e.status != '报废' AND e.unit_code = ? " +
                           "GROUP BY e.equip_id, e.equip_name, e.original_value, c.useful_life, e.purchase_date " +
                           "HAVING " +
                           "  (IF(c.useful_life IS NOT NULL AND c.useful_life > 0, TIMESTAMPDIFF(MONTH, e.purchase_date, CURRENT_DATE()) / (c.useful_life * 12.0), 0.0) >= 0.9) OR " +
                           "  (COUNT(mr.maint_id) >= 3) OR " +
                           "  (IF(e.original_value IS NOT NULL AND e.original_value > 0, IFNULL(SUM(mr.maint_cost), 0.0) / e.original_value, 0.0) >= 0.3)";
        return mutiSelect(sql, SysMessage.class, unitCode);
    }

    /**
     * 规则扫描 2：扫描积压审批（t_equipment_claim status = 0 且超 24 小时）
     */
    public List<SysMessage> listPendingClaim(final String unitCode) {
        final String sql = "SELECT CAST(c.claim_id AS CHAR) AS refId, e.equip_name AS title " +
                           "FROM t_equipment_claim c " +
                           "JOIN equipment e ON c.equip_id = e.equip_id " +
                           "WHERE c.status = 0 " +
                           "  AND e.unit_code = ? " +
                           "  AND c.create_time < DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        return mutiSelect(sql, SysMessage.class, unitCode);
    }

    /**
     * 规则扫描 3：扫描超时维保工单（maint_status = 1 且超 48 小时）
     */
    public List<SysMessage> listOverdueMaintenance(final String username) {
        final String sql = "SELECT CAST(m.maint_id AS CHAR) AS refId, e.equip_name AS title " +
                           "FROM maintenance_record m " +
                           "JOIN equipment e ON m.equip_id = e.equip_id " +
                           "JOIN sys_user u ON m.maint_person_id = u.id " +
                           "WHERE m.maint_status = 1 " +
                           "  AND u.username = ? " +
                           "  AND m.assign_time < DATE_SUB(NOW(), INTERVAL 48 HOUR)";
        return mutiSelect(sql, SysMessage.class, username);
    }

    /**
     * 规则扫描 4：扫描特定单位的超时维保工单（用于抄送本单位资产管理员）
     */
    public List<SysMessage> listOverdueMaintenanceByUnit(final String unitCode) {
        final String sql = "SELECT CAST(m.maint_id AS CHAR) AS refId, e.equip_name AS title " +
                           "FROM maintenance_record m " +
                           "JOIN equipment e ON m.equip_id = e.equip_id " +
                           "WHERE m.maint_status = 1 " +
                           "  AND e.unit_code = ? " +
                           "  AND m.assign_time < DATE_SUB(NOW(), INTERVAL 48 HOUR)";
        return mutiSelect(sql, SysMessage.class, unitCode);
    }
}
