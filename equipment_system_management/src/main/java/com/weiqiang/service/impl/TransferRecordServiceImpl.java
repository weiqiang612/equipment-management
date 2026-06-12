package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.dao.TransferRecordDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.utils.BaseContext;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.TransferRecord;
import com.weiqiang.service.TransferRecordService;
import com.weiqiang.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 调拨记录服务实现类
 */
@Service
@RequiredArgsConstructor
public class TransferRecordServiceImpl implements TransferRecordService {

    private final TransferRecordDao transferRecordDao;
    private final EquipmentDao equipmentDao;
    private final OperationLogService operationLogService;

    @Override
    public List<TransferRecord> getTransferRecords() {
        return transferRecordDao.getTransferRecords();
    }

    @Override
    public TransferRecord getTransferRecordById(Integer transferId) {
        return transferRecordDao.getTransferRecordById(transferId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean transferEquip(String equipId, TransferRecord transferRecord) {
        // 1. 防报废穿透：已报废的设备禁止调拨
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }

        // 资产管理员只能调拨本单位设备，且调出单位必须是其所属单位
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 2) {
            String currentUnitCode = BaseContext.getCurrentUnitCode();
            if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：无权调拨其他单位的设备");
            }
            if (transferRecord.getOutUnitCode() == null || !transferRecord.getOutUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：调出单位与所属单位不一致");
            }
        }

        if ("报废".equals(equipment.getStatus())) {
            throw new BusinessException("该设备已报废，禁止进行此操作");
        }
        if ("维修".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：该设备正在维修中，无法进行调拨操作");
        }

        // 不可以从本部门调到本部门
        if (transferRecord.getOutUnitCode().equals(transferRecord.getInUnitCode())){
            throw new BusinessException("不可以从本部门调到本部门！");
        }

        String oldCustodian = equipment.getCustodian();

        boolean success = transferRecordDao.transferEquip(equipId, transferRecord, oldCustodian);
        if (success) {
            Number lastId = (Number) transferRecordDao.singleSelect("SELECT LAST_INSERT_ID()");
            Integer transferId = lastId != null ? lastId.intValue() : null;
            operationLogService.record("设备调拨", "transfer_record", String.valueOf(transferId), 
                "设备调拨: 设备 " + equipId + " 从单位 " + transferRecord.getOutUnitCode() + " 调拨至单位 " + transferRecord.getInUnitCode() + "，变动类型: " + transferRecord.getChangeType() + "，经办人: " + transferRecord.getOperator(), 1, null);
        }
        return success;
    }

    @Override
    public int updateTransferRecord(Integer transferId, TransferRecord transferRecord) {
        return transferRecordDao.updateTransferRecord(transferId, transferRecord);
    }

    @Override
    public boolean deleteTransferRecord(Integer transferId) {
        TransferRecord recordById = transferRecordDao.getTransferRecordById(transferId);
        if (recordById == null) {
            throw new BusinessException("该调拨记录不存在");
        }
        String equipId = recordById.getEquipId();
        String outUnitCode = recordById.getOutUnitCode();
        return transferRecordDao.deleteTransferRecord(transferId, equipId, outUnitCode);
    }
}
