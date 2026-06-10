package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.dao.TransferRecordDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.TransferRecord;
import com.weiqiang.service.TransferRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 调拨记录服务实现类
 */
@Service
public class TransferRecordServiceImpl implements TransferRecordService {

    @Autowired
    private TransferRecordDao transferRecordDao;

    @Autowired
    private EquipmentDao equipmentDao;

    @Override
    public List<TransferRecord> getTransferRecords() {
        return transferRecordDao.getTransferRecords();
    }

    @Override
    public TransferRecord getTransferRecordById(Integer transferId) {
        return transferRecordDao.getTransferRecordById(transferId);
    }

    @Override
    public boolean transferEquip(String equipId, TransferRecord transferRecord) {
        // 1. 防报废穿透：已报废的设备禁止调拨
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }
        if ("报废".equals(equipment.getStatus())) {
            throw new BusinessException("该设备已报废，禁止进行此操作");
        }

        // 不可以从本部门调到本部门
        if (transferRecord.getOutUnitCode().equals(transferRecord.getInUnitCode())){
            throw new BusinessException("不可以从本部门调到本部门！");
        }

        return transferRecordDao.transferEquip(equipId, transferRecord);
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
