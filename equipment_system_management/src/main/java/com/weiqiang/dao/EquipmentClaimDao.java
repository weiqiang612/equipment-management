package com.weiqiang.dao;

import com.weiqiang.entity.EquipmentClaim;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备领用与审批记录数据访问对象
 */
@Repository
public class EquipmentClaimDao extends BasicDao<EquipmentClaim> {

    // 新增领用记录
    public int addClaim(EquipmentClaim claim) {
        String sql = "INSERT INTO t_equipment_claim (equip_id, applicant, approver, status, remark) VALUES (?, ?, ?, ?, ?)";
        return update(sql, claim.getEquipId(), claim.getApplicant(), claim.getApprover(), claim.getStatus(), claim.getRemark());
    }

    // 根据ID查询
    public EquipmentClaim getClaimById(Integer claimId) {
        String sql = "SELECT c.claim_id AS claimId, c.equip_id AS equipId, c.applicant AS applicant, " +
                "c.approver AS approver, c.status AS status, c.remark AS remark, " +
                "c.create_time AS createTime, c.update_time AS updateTime, " +
                "e.equip_name AS equipName, u1.real_name AS applicantRealName, u2.real_name AS approverRealName " +
                "FROM t_equipment_claim c " +
                "LEFT JOIN equipment e ON c.equip_id = e.equip_id " +
                "LEFT JOIN sys_user u1 ON c.applicant = u1.username " +
                "LEFT JOIN sys_user u2 ON c.approver = u2.username " +
                "WHERE c.claim_id = ?";
        return selectOne(sql, EquipmentClaim.class, claimId);
    }

    // 根据设备ID和申请状态查询是否已有待审批
    public EquipmentClaim getPendingClaimByEquipId(String equipId) {
        String sql = "SELECT c.claim_id AS claimId, c.equip_id AS equipId, c.applicant AS applicant, " +
                "c.approver AS approver, c.status AS status, c.remark AS remark, " +
                "c.create_time AS createTime, c.update_time AS updateTime " +
                "FROM t_equipment_claim c " +
                "WHERE c.equip_id = ? AND c.status = 0 LIMIT 1";
        return selectOne(sql, EquipmentClaim.class, equipId);
    }

    // 更新状态和审批信息
    public int updateClaimStatus(Integer claimId, Integer status, String approver, String remark) {
        String sql = "UPDATE t_equipment_claim SET status = ?, approver = ?, remark = ? WHERE claim_id = ?";
        return update(sql, status, approver, remark, claimId);
    }

    // 动态列表查询
    public List<EquipmentClaim> getClaimsDynamic(String equipId, Integer status, String applicant, String unitCode, Integer page, Integer pageSize) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.claim_id AS claimId, c.equip_id AS equipId, c.applicant AS applicant, " +
                "c.approver AS approver, c.status AS status, c.remark AS remark, " +
                "c.create_time AS createTime, c.update_time AS updateTime, " +
                "e.equip_name AS equipName, u1.real_name AS applicantRealName, u2.real_name AS approverRealName " +
                "FROM t_equipment_claim c " +
                "LEFT JOIN equipment e ON c.equip_id = e.equip_id " +
                "LEFT JOIN sys_user u1 ON c.applicant = u1.username " +
                "LEFT JOIN sys_user u2 ON c.approver = u2.username " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (equipId != null && !equipId.trim().isEmpty()) {
            sql.append("AND c.equip_id = ? ");
            params.add(equipId);
        }
        if (status != null) {
            sql.append("AND c.status = ? ");
            params.add(status);
        }
        if (applicant != null && !applicant.trim().isEmpty()) {
            sql.append("AND c.applicant = ? ");
            params.add(applicant);
        }
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append("AND e.unit_code = ? ");
            params.add(unitCode);
        }

        sql.append("ORDER BY c.create_time DESC ");

        if (page != null && pageSize != null) {
            int offset = (page - 1) * pageSize;
            sql.append("LIMIT ?, ? ");
            params.add(offset);
            params.add(pageSize);
        }

        return mutiSelect(sql.toString(), EquipmentClaim.class, params.toArray());
    }

    // 获取总数量
    public Long getClaimsNum(String equipId, Integer status, String applicant, String unitCode) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                "FROM t_equipment_claim c " +
                "LEFT JOIN equipment e ON c.equip_id = e.equip_id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (equipId != null && !equipId.trim().isEmpty()) {
            sql.append("AND c.equip_id = ? ");
            params.add(equipId);
        }
        if (status != null) {
            sql.append("AND c.status = ? ");
            params.add(status);
        }
        if (applicant != null && !applicant.trim().isEmpty()) {
            sql.append("AND c.applicant = ? ");
            params.add(applicant);
        }
        if (unitCode != null && !unitCode.trim().isEmpty()) {
            sql.append("AND e.unit_code = ? ");
            params.add(unitCode);
        }

        Object result = singleSelect(sql.toString(), params.toArray());
        return result != null ? (Long) result : 0L;
    }
}
