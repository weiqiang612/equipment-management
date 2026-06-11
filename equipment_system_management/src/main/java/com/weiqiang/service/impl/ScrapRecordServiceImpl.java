package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.dao.ScrapRecordDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.ScrapRecord;
import com.weiqiang.service.ScrapRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报废记录服务实现类
 */
@Service
@RequiredArgsConstructor
public class ScrapRecordServiceImpl implements ScrapRecordService {

    private final ScrapRecordDao scrapRecordDao;
    private final EquipmentDao equipmentDao;

    @Override
    public List<ScrapRecord> getScrapRecords() {
        return scrapRecordDao.getScrapRecords();
    }

    @Override
    public boolean scrapEquip(String equipId, ScrapRecord scrapRecord) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }

        // 报废状态的设备无法再报废
        if ("报废".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：该设备已报废，无法再报废！");
        }

        // 生成报废单号
        String generatedNo = "BF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-MM-dd-HH-ss"));
        scrapRecord.setScrapNo(generatedNo);
        scrapRecord.setEquipId(equipId);

        String oldCustodian = equipment.getCustodian();

        return scrapRecordDao.scrapEquip(equipId, scrapRecord, oldCustodian);
    }

    @Override
    public boolean deleteScrapRecord(String equipId, String scrapNo) {
        return scrapRecordDao.deleteScrapRecord(equipId, scrapNo);
    }

    @Override
    public int putScrapRecord(String scrapNo, ScrapRecord scrapRecord) {
        return scrapRecordDao.putScrapRecord(scrapNo, scrapRecord);
    }
}
