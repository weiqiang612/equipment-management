package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.EquipmentDepreciationVO;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public int addEquipment(Equipment equipment) {
        // 校验唯一编号防重复
        Equipment oldEquip = equipmentDao.getEquipmentById(equipment.getEquipId());
        if (oldEquip != null) {
            throw new BusinessException("操作失败：唯一编号已存在，请勿重复添加！");
        }
        return equipmentDao.addEquipment(equipment);
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

        String oldCustodian = oldEquip.getCustodian();
        String newCustodian = equipment.getCustodian();

        int rows = equipmentDao.updateEquipment(equipment);

        if (rows > 0) {
            boolean oldIsEmpty = oldCustodian == null || oldCustodian.trim().isEmpty();
            boolean newIsEmpty = newCustodian == null || newCustodian.trim().isEmpty();

            if (!oldIsEmpty && newIsEmpty) {
                // 有主 -> NULL: 写入 status=4 (已退还) 审计日志
                com.weiqiang.pojo.EquipmentClaim audit = new com.weiqiang.pojo.EquipmentClaim();
                audit.setEquipId(equipment.getEquipId());
                audit.setApplicant(oldCustodian);
                audit.setApprover(com.weiqiang.utils.BaseContext.getCurrentName());
                audit.setStatus(com.weiqiang.pojo.EquipmentClaim.STATUS_RETURNED);
                audit.setRemark("管理员取消分配");
                equipmentClaimDao.addClaim(audit);
            } else if (newCustodian != null && !newCustodian.trim().isEmpty() && !newCustodian.equals(oldCustodian)) {
                // 无主 -> 有人，或者换人: 写入 status=5 (直接分配) 审计日志
                com.weiqiang.pojo.EquipmentClaim audit = new com.weiqiang.pojo.EquipmentClaim();
                audit.setEquipId(equipment.getEquipId());
                audit.setApplicant(newCustodian);
                audit.setApprover(com.weiqiang.utils.BaseContext.getCurrentName());
                audit.setStatus(com.weiqiang.pojo.EquipmentClaim.STATUS_DIRECT);
                audit.setRemark("管理员直接分配");
                equipmentClaimDao.addClaim(audit);
            }
        }
        return rows;
    }

    @Override
    public boolean deleteEquipment(String equipId) {
        return equipmentDao.deleteEquipment(equipId);
    }

    @Override
    public PageBean getEquipmentsDynamic(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end, Integer page, Integer pageSize) {
        Long total = equipmentDao.getEquipmentsNum(equipName, unitCode, categoryId, status, begin, end);
        List<Equipment> equipmentsDynamic = equipmentDao.getEquipmentsDynamic(equipName, unitCode, categoryId, status, begin, end, page, pageSize);

        if (equipmentsDynamic == null) {
            equipmentsDynamic = new ArrayList<>();
        }
        if (total == null) {
            total = 0L;
        }
        return new PageBean(total, equipmentsDynamic);
    }

    @Override
    public List<EquipmentDepreciationVO> getEquipmentsDynamicForExport(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end) {
        List<Equipment> list = equipmentDao.getEquipmentsDynamic(equipName, unitCode, categoryId, status, begin, end, null, null);
        return list.stream().map(this::calculateAccumulated).collect(Collectors.toList());
    }

    /**
     * 计算某台设备的累积折旧
     */
    public EquipmentDepreciationVO calculateAccumulated(Equipment equipment) {
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
}
