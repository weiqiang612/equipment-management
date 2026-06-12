package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentClaimDao;
import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.EquipmentClaim;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.service.EquipmentClaimService;
import com.weiqiang.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import com.weiqiang.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备领用与审批服务实现类
 */
@Service
@RequiredArgsConstructor
public class EquipmentClaimServiceImpl implements EquipmentClaimService {

    private final EquipmentClaimDao equipmentClaimDao;
    private final EquipmentDao equipmentDao;
    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyClaim(String equipId, String remark) {
        // 1. 校验设备是否存在
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }

        // 2. 校验设备状态是在用
        if (!"在用".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：该设备当前不可领用，只有“在用”状态的设备才能发起领用申请！");
        }

        // 3. 校验保管人为空
        if (equipment.getCustodian() != null && !equipment.getCustodian().trim().isEmpty()) {
            throw new BusinessException("操作失败：该设备已有保管人，无法申请！");
        }

        // 4. 防越权跨部门领用
        String currentUnitCode = BaseContext.getCurrentUnitCode();
        if (currentUnitCode == null || !currentUnitCode.equals(equipment.getUnitCode())) {
            throw new BusinessException("部门不匹配，无法申请本部门之外的设备");
        }

        // 5. 校验是否有处于待审批状态的申请，防重复申请
        EquipmentClaim pendingClaim = equipmentClaimDao.getPendingClaimByEquipId(equipId);
        if (pendingClaim != null) {
            throw new BusinessException("操作失败：该设备已有处于待审批状态的领用申请，请勿重复申请！");
        }

        // 6. 插入申请记录
        String currentUsername = BaseContext.getCurrentName();
        EquipmentClaim claim = new EquipmentClaim();
        claim.setEquipId(equipId);
        claim.setApplicant(currentUsername);
        claim.setStatus(EquipmentClaim.STATUS_PENDING); // 待审批
        claim.setRemark(remark);

        boolean success = equipmentClaimDao.addClaim(claim) > 0;
        if (success) {
            Number lastId = (Number) equipmentClaimDao.singleSelect("SELECT LAST_INSERT_ID()");
            Integer claimId = lastId != null ? lastId.intValue() : null;
            operationLogService.record("领用申请", "t_equipment_claim", String.valueOf(claimId), 
                "申请人 " + currentUsername + " 申请领用设备 " + equipId, 1, null);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelClaim(Integer claimId) {
        EquipmentClaim claim = equipmentClaimDao.getClaimById(claimId);
        if (claim == null) {
            throw new BusinessException("该领用申请不存在");
        }

        // 限本人在状态0撤回
        String currentUsername = BaseContext.getCurrentName();
        if (!currentUsername.equals(claim.getApplicant())) {
            throw new BusinessException("越权操作：只能撤回自己的领用申请");
        }

        if (claim.getStatus() != EquipmentClaim.STATUS_PENDING) {
            throw new BusinessException("操作失败：只能撤回“待审批”状态的申请");
        }

        boolean success = equipmentClaimDao.updateClaimStatus(claimId, EquipmentClaim.STATUS_CANCELLED, null, "申请人撤回") > 0;
        if (success) {
            operationLogService.record("领用撤回", "t_equipment_claim", claimId.toString(), 
                "申请人 " + currentUsername + " 撤回了领用申请 " + claimId, 1, null);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveClaim(Integer claimId, Integer action, String remark) {
        EquipmentClaim claim = equipmentClaimDao.getClaimById(claimId);
        if (claim == null) {
            throw new BusinessException("该领用申请不存在");
        }

        if (claim.getStatus() != EquipmentClaim.STATUS_PENDING) {
            throw new BusinessException("操作失败：只能审批“待审批”状态的申请");
        }

        // 限制只有同部门的管理员才能审批
        String currentUnitCode = BaseContext.getCurrentUnitCode();
        Equipment equipment = equipmentDao.getEquipmentById(claim.getEquipId());
        if (equipment == null) {
            throw new BusinessException("申请对应的设备不存在");
        }

        if (currentUnitCode == null || !currentUnitCode.equals(equipment.getUnitCode())) {
            throw new ForbiddenException("权限不足：不能审批跨部门的设备领用申请");
        }

        String currentUsername = BaseContext.getCurrentName();

        if (action == 1) { // 同意
            // 校验当时设备保管人是否仍为空
            if (equipment.getCustodian() != null && !equipment.getCustodian().trim().isEmpty()) {
                throw new BusinessException("操作失败：该设备已被分配保管人，无法通过审批！");
            }
            equipment.setCustodian(claim.getApplicant());
            int row1 = equipmentDao.updateEquipment(equipment);
            int row2 = equipmentClaimDao.updateClaimStatus(claimId, EquipmentClaim.STATUS_APPROVED, currentUsername, remark);

            if (row1 <= 0 || row2 <= 0) {
                throw new RuntimeException("审批处理失败，事务回滚");
            }
            operationLogService.record("领用同意", "t_equipment_claim", claimId.toString(), 
                "审批人 " + currentUsername + " 同意了领用申请 " + claimId + "，设备保管人变更为 " + claim.getApplicant(), 1, null);
            return true;
        } else if (action == 2) { // 拒绝
            boolean success = equipmentClaimDao.updateClaimStatus(claimId, EquipmentClaim.STATUS_REJECTED, currentUsername, remark) > 0;
            if (success) {
                operationLogService.record("领用拒绝", "t_equipment_claim", claimId.toString(), 
                    "审批人 " + currentUsername + " 拒绝了领用申请 " + claimId + "，审批意见: " + remark, 1, null);
            }
            return success;
        } else {
            throw new BusinessException("无效的审批动作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnEquipment(String equipId, String remark) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }

        String currentUsername = BaseContext.getCurrentName();
        if (equipment.getCustodian() == null || !equipment.getCustodian().equals(currentUsername)) {
            throw new BusinessException("越权操作：只有保管人本人才能退还该设备");
        }

        // 清空保管人，并写入退还审计日志
        equipment.setCustodian(null);
        int row1 = equipmentDao.updateEquipment(equipment);

        EquipmentClaim claim = new EquipmentClaim();
        claim.setEquipId(equipId);
        claim.setApplicant(currentUsername);
        claim.setApprover(null);
        claim.setStatus(EquipmentClaim.STATUS_RETURNED); // 已退还
        claim.setRemark(remark);
        int row2 = equipmentClaimDao.addClaim(claim);

        if (row1 <= 0 || row2 <= 0) {
            throw new RuntimeException("退还处理失败，事务回滚");
        }
        operationLogService.record("设备退还", "equipment", equipId, 
            "保管人 " + currentUsername + " 退还设备 " + equipId + "，退还备注: " + remark, 1, null);
        return true;
    }

    @Override
    public PageBean<EquipmentClaim> getClaims(String equipId, Integer status, Integer page, Integer pageSize) {
        Integer currentRole = BaseContext.getCurrentRole();
        String currentUsername = BaseContext.getCurrentName();
        String currentUnitCode = BaseContext.getCurrentUnitCode();

        String applicant = null;
        String unitCode = null;

        if (currentRole != null && currentRole == 0) {
            // 操作员仅能查到自己的记录
            applicant = currentUsername;
        } else if (currentRole != null && (currentRole == 2 || currentRole == 3)) {
            // 管理员查自己部门的
            unitCode = currentUnitCode;
        }

        List<EquipmentClaim> list = equipmentClaimDao.getClaimsDynamic(equipId, status, applicant, unitCode, page, pageSize);
        Long total = equipmentClaimDao.getClaimsNum(equipId, status, applicant, unitCode);

        return new PageBean<>(total, list);
    }
}
