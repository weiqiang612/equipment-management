package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.utils.BaseContext;
import com.weiqiang.entity.Equipment;
import com.weiqiang.vo.EquipmentDepreciationVO;
import com.weiqiang.common.PageBean;
import com.weiqiang.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.weiqiang.service.OperationLogService;
import com.weiqiang.vo.EquipmentDetailVO;
import com.weiqiang.entity.EquipmentClaim;
import com.weiqiang.entity.MaintenanceRecord;
import com.weiqiang.entity.TransferRecord;
import com.weiqiang.entity.ScrapRecord;
import com.weiqiang.vo.OperationLogVO;
import com.weiqiang.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentDao equipmentDao;
    private final com.weiqiang.dao.EquipmentClaimDao equipmentClaimDao;
    private final OperationLogService operationLogService;
    private final com.weiqiang.dao.MaintenanceRecordDao maintenanceRecordDao;
    private final com.weiqiang.dao.TransferRecordDao transferRecordDao;
    private final com.weiqiang.dao.ScrapRecordDao scrapRecordDao;
    private final com.weiqiang.dao.OperationLogDao operationLogDao;
    private final com.weiqiang.dao.UserDao userDao;

    @Override
    public List<Equipment> getEquipments() {
        return equipmentDao.getEquipments();
    }

    @Override
    public Equipment getEquipmentById(String equipId) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment != null) {
            Integer currentRole = com.weiqiang.utils.BaseContext.getCurrentRole();
            if (currentRole != null && currentRole == 0) {
                String currentUsername = com.weiqiang.utils.BaseContext.getCurrentName();
                String currentUnitCode = com.weiqiang.utils.BaseContext.getCurrentUnitCode();
                boolean isCustodian = currentUsername != null && currentUsername.equals(equipment.getCustodian());
                boolean isDeptEmptyCustodian = equipment.getCustodian() == null && currentUnitCode != null && currentUnitCode.equals(equipment.getUnitCode());
                if (!isCustodian && !isDeptEmptyCustodian) {
                    return null;
                }
            }
        }
        return equipment;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int addEquipment(Equipment equipment) {
        // 校验唯一编号防重复
        Equipment oldEquip = equipmentDao.getEquipmentById(equipment.getEquipId());
        if (oldEquip != null) {
            throw new BusinessException("操作失败：唯一编号已存在，请勿重复添加！");
        }
        // 资产管理员只能录入本单位资产
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 2) {
            equipment.setUnitCode(BaseContext.getCurrentUnitCode());
        }
        int rows = equipmentDao.addEquipment(equipment);
        if (rows > 0) {
            operationLogService.record("设备新增", "equipment", equipment.getEquipId(), 
                "新增设备: " + equipment.getEquipName() + " (" + equipment.getEquipId() + ")", 1, null);
        }
        return rows;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int updateEquipment(Equipment equipment) {
        // 防报废穿透：已报废的设备禁止编辑
        Equipment oldEquip = equipmentDao.getEquipmentById(equipment.getEquipId());
        if (oldEquip == null) {
            throw new BusinessException("该设备不存在");
        }
        if ("报废".equals(oldEquip.getStatus())) {
            throw new BusinessException("该设备已报废，禁止进行此操作");
        }

        // 资产管理员只能更新本单位资产，且强制绑定本单位 unitCode
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 2) {
            String currentUnitCode = BaseContext.getCurrentUnitCode();
            if (oldEquip.getUnitCode() == null || !oldEquip.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：无权修改其他单位 of 设备");
            }
            equipment.setUnitCode(currentUnitCode);
        }

        String oldCustodian = oldEquip.getCustodian();
        String newCustodian = equipment.getCustodian();

        int rows = equipmentDao.updateEquipment(equipment);

        if (rows > 0) {
            operationLogService.record("设备修改", "equipment", equipment.getEquipId(), 
                "修改设备: " + equipment.getEquipName() + " (" + equipment.getEquipId() + ")", 1, null);
            boolean oldIsEmpty = oldCustodian == null || oldCustodian.trim().isEmpty();
            boolean newIsEmpty = newCustodian == null || newCustodian.trim().isEmpty();

            if (!oldIsEmpty && newIsEmpty) {
                // 有主 -> NULL: 写入 status=4 (已退还) 审计日志
                EquipmentClaim audit = new EquipmentClaim();
                audit.setEquipId(equipment.getEquipId());
                audit.setApplicant(oldCustodian);
                audit.setApprover(com.weiqiang.utils.BaseContext.getCurrentName());
                audit.setStatus(EquipmentClaim.STATUS_RETURNED);
                audit.setRemark("管理员取消分配");
                equipmentClaimDao.addClaim(audit);
                operationLogService.record("设备退还", "equipment", equipment.getEquipId(), 
                    "管理员取消用户 " + oldCustodian + " 对设备 " + equipment.getEquipId() + " 的保管分配", 1, null);
            } else if (newCustodian != null && !newCustodian.trim().isEmpty() && !newCustodian.equals(oldCustodian)) {
                // 无主 -> 有人，或者换人: 写入 status=5 (直接分配) 审计日志
                EquipmentClaim audit = new EquipmentClaim();
                audit.setEquipId(equipment.getEquipId());
                audit.setApplicant(newCustodian);
                audit.setApprover(com.weiqiang.utils.BaseContext.getCurrentName());
                audit.setStatus(EquipmentClaim.STATUS_DIRECT);
                audit.setRemark("管理员直接分配");
                equipmentClaimDao.addClaim(audit);
                operationLogService.record("直接分配", "equipment", equipment.getEquipId(), 
                    "管理员直接分配设备 " + equipment.getEquipId() + " 给用户 " + newCustodian, 1, null);
            }
        }
        return rows;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean deleteEquipment(String equipId) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }
        
        // 资产管理员只能删除本单位设备
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 2) {
            String currentUnitCode = BaseContext.getCurrentUnitCode();
            if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：无权删除其他单位 of 设备");
            }
        }

        String name = equipment.getEquipName();
        boolean success = equipmentDao.deleteEquipment(equipId);
        if (success) {
            operationLogService.record("设备删除", "equipment", equipId, 
                "删除设备: " + name + " (" + equipId + ")", 1, null);
        }
        return success;
    }

    @Override
    public PageBean getEquipmentsDynamic(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end, String custodian, Integer page, Integer pageSize) {
        // 资产管理员只能检索本单位的设备列表
        Integer currentRole = BaseContext.getCurrentRole();
        String finalUnitCode = unitCode;
        if (currentRole != null && currentRole == 2) {
            finalUnitCode = BaseContext.getCurrentUnitCode();
        }

        Long total = equipmentDao.getEquipmentsNum(equipName, finalUnitCode, categoryId, status, begin, end, custodian);
        List<Equipment> equipmentsDynamic = equipmentDao.getEquipmentsDynamic(equipName, finalUnitCode, categoryId, status, begin, end, custodian, page, pageSize);

        if (equipmentsDynamic == null) {
            equipmentsDynamic = new ArrayList<>();
        }
        if (total == null) {
            total = 0L;
        }
        return new PageBean(total, equipmentsDynamic);
    }

    @Override
    public List<EquipmentDepreciationVO> getEquipmentsDynamicForExport(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end, String custodian) {
        // 资产管理员只能导出本单位的设备列表
        Integer currentRole = BaseContext.getCurrentRole();
        String finalUnitCode = unitCode;
        if (currentRole != null && currentRole == 2) {
            finalUnitCode = BaseContext.getCurrentUnitCode();
        }

        List<Equipment> list = equipmentDao.getEquipmentsDynamic(equipName, finalUnitCode, categoryId, status, begin, end, custodian, null, null);
        return list.stream().map(this::calculateAccumulated).collect(Collectors.toList());
    }

    /**
     * 计算某台设备的累积折旧
     */
    public EquipmentDepreciationVO calculateAccumulated(Equipment equipment) {
        if (equipment.getUsefulLife() == null || equipment.getUsefulLife() <= 0) {
            throw new BusinessException("该设备所属分类的预计使用年限未配置，无法计算折旧");
        }
        EquipmentDepreciationVO vo = new EquipmentDepreciationVO();
        vo.setEquipId(equipment.getEquipId());
        vo.setResidualRate(equipment.getResidualRate());
        vo.setUsefulLife(equipment.getUsefulLife());
        vo.setOriginalValue(equipment.getOriginalValue());
        vo.setEquipName(equipment.getEquipName());
        vo.setCategoryName(equipment.getCategoryName());
        vo.setUnitName(equipment.getUnitName());
        vo.setPurchaseDate(equipment.getPurchaseDate());
        vo.setStatus(equipment.getStatus());
        
        LocalDate startDate = vo.getPurchaseDate().plusMonths(1).withDayOfMonth(1);
        LocalDate now = LocalDate.now();
        long monthsUsed = ChronoUnit.MONTHS.between(startDate, now);
        if (monthsUsed < 0) monthsUsed = 0;
        
        int totalLifeMonths = vo.getUsefulLife() * 12;
        if (monthsUsed > totalLifeMonths) monthsUsed = totalLifeMonths;
        
        BigDecimal totalDepreciable = equipment.getOriginalValue().multiply(BigDecimal.ONE.subtract(vo.getResidualRate()));
        BigDecimal mouthlyDepreciation = totalDepreciable.divide(BigDecimal.valueOf(totalLifeMonths), 10, RoundingMode.HALF_UP);
        BigDecimal accumulatedDepreciation = mouthlyDepreciation.multiply(BigDecimal.valueOf(monthsUsed)).setScale(2, RoundingMode.HALF_UP);

        if (monthsUsed >= totalLifeMonths) {
            vo.setIsFullyDepreciated(true);
            accumulatedDepreciation = totalDepreciable;
        } else {
            vo.setIsFullyDepreciated(false);
        }

        vo.setMonthlyDepreciation(mouthlyDepreciation.setScale(2, RoundingMode.HALF_UP));
        vo.setAccumulated(accumulatedDepreciation);
        vo.setNetValue(vo.getOriginalValue().subtract(accumulatedDepreciation));
        return vo;
    }

    @Override
    public EquipmentDetailVO getEquipmentDetail(String equipId) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new com.weiqiang.exception.BusinessException("该设备不存在");
        }

        EquipmentDetailVO detail = new EquipmentDetailVO();
        detail.setEquipId(equipment.getEquipId());
        detail.setEquipName(equipment.getEquipName());
        detail.setModel(equipment.getModel());
        detail.setStatus(equipment.getStatus());
        detail.setPurchaseDate(equipment.getPurchaseDate());
        detail.setOriginalValue(equipment.getOriginalValue());
        detail.setUnitCode(equipment.getUnitCode());
        detail.setUnitName(equipment.getUnitName());
        detail.setCategoryId(equipment.getCategoryId());
        detail.setCategoryName(equipment.getCategoryName());
        detail.setCustodian(equipment.getCustodian());

        if (equipment.getCustodian() != null && !equipment.getCustodian().trim().isEmpty()) {
            User user = userDao.getByUsername(equipment.getCustodian());
            if (user != null) {
                detail.setCustodianRealName(user.getRealName());
            }
        }

        EquipmentDepreciationVO dep = calculateAccumulated(equipment);
        detail.setUsefulLife(dep.getUsefulLife());
        detail.setResidualRate(dep.getResidualRate());
        detail.setMonthlyDepreciation(dep.getMonthlyDepreciation());
        detail.setAccumulatedDepreciation(dep.getAccumulated());
        detail.setNetValue(dep.getNetValue());

        List<EquipmentClaim> claims = equipmentClaimDao.mutiSelect(
            "SELECT c.claim_id AS claimId, c.equip_id AS equipId, c.applicant AS applicant, " +
            "c.approver AS approver, c.status AS status, c.remark AS remark, " +
            "c.create_time AS createTime, c.update_time AS updateTime, " +
            "e.equip_name AS equipName, u1.real_name AS applicantRealName, u2.real_name AS approverRealName " +
            "FROM t_equipment_claim c " +
            "LEFT JOIN equipment e ON c.equip_id = e.equip_id " +
            "LEFT JOIN sys_user u1 ON c.applicant = u1.username " +
            "LEFT JOIN sys_user u2 ON c.approver = u2.username " +
            "WHERE c.equip_id = ? ORDER BY c.create_time DESC", EquipmentClaim.class, equipId);
        detail.setClaims(claims);

        List<MaintenanceRecord> maintenances = maintenanceRecordDao.mutiSelect(
            "SELECT maint_id maintId, equip_id equipId, maint_date maintDate, maint_content maintContent, " +
            "maint_cost maintCost, maint_person maintPerson, reporter, fault_description faultDescription, " +
            "maint_status maintStatus, maint_person_id maintPersonId " +
            "FROM maintenance_record WHERE equip_id = ? ORDER BY maint_date DESC, maint_id DESC", MaintenanceRecord.class, equipId);
        detail.setMaintenances(maintenances);

        List<TransferRecord> transfers = transferRecordDao.mutiSelect(
            "SELECT t.transfer_id AS transferId, t.equip_id AS equipId, t.out_unit_code AS outUnitCode, " +
            "t.in_unit_code AS inUnitCode, t.transfer_date AS transferDate, t.change_type AS changeType, " +
            "t.operator, t.reason, d1.unit_name AS outUnitName, d2.unit_name AS inUnitName " +
            "FROM transfer_record t " +
            "LEFT JOIN department d1 ON t.out_unit_code = d1.unit_code " +
            "LEFT JOIN department d2 ON t.in_unit_code = d2.unit_code " +
            "WHERE t.equip_id = ? ORDER BY t.transfer_date DESC, t.transfer_id DESC", TransferRecord.class, equipId);
        detail.setTransfers(transfers);

        ScrapRecord scrap = scrapRecordDao.selectOne(
            "SELECT equip_id equipId, scrap_no scrapNo, scrap_date scrapDate, approver, reason " +
            "FROM scrap_record WHERE equip_id = ?", ScrapRecord.class, equipId);
        detail.setScrap(scrap);

        List<OperationLogVO> auditTimeline = operationLogDao.mutiSelect(
            "SELECT id, operator, operator_role AS operatorRole, op_type AS opType, " +
            "target_type AS targetType, target_id AS targetId, op_time AS opTime, " +
            "summary, status, error_msg AS errorMsg " +
            "FROM operation_log WHERE (target_type = 'equipment' AND target_id = ?) " +
            "OR (target_type IN ('t_equipment_claim', 'maintenance_record', 'transfer_record', 'scrap_record') AND target_id IN (" +
            "  SELECT CAST(claim_id AS CHAR) FROM t_equipment_claim WHERE equip_id = ? UNION " +
            "  SELECT CAST(maint_id AS CHAR) FROM maintenance_record WHERE equip_id = ? UNION " +
            "  SELECT CAST(transfer_id AS CHAR) FROM transfer_record WHERE equip_id = ? UNION " +
            "  SELECT equip_id FROM scrap_record WHERE equip_id = ?" +
            ")) ORDER BY op_time DESC, id DESC", OperationLogVO.class, equipId, equipId, equipId, equipId, equipId);
        
        if (auditTimeline != null) {
            for (OperationLogVO vo : auditTimeline) {
                if (vo.getOperator() != null) {
                    User user = userDao.getByUsername(vo.getOperator());
                    if (user != null) {
                        vo.setOperatorRealName(user.getRealName());
                    }
                }
            }
        }
        detail.setAuditTimeline(auditTimeline);

        return detail;
    }
}
