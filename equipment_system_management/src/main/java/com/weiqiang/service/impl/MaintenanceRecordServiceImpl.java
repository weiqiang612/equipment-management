package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.dao.MaintenanceRecordDao;
import com.weiqiang.dao.UserDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.MaintenanceRecord;
import com.weiqiang.pojo.User;
import com.weiqiang.service.MaintenanceRecordService;
import com.weiqiang.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.weiqiang.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备检修服务实现类
 */
@Service
public class MaintenanceRecordServiceImpl implements MaintenanceRecordService {

    @Autowired
    private MaintenanceRecordDao maintenanceRecordDao;

    @Autowired
    private EquipmentDao equipmentDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private OperationLogService operationLogService;

    @Override
    public List<MaintenanceRecord> getMaintenanceRecords() {
        return maintenanceRecordDao.getMaintenanceRecords();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean maintenanceEquip(String equipId, MaintenanceRecord maintenanceRecord) {
        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("该设备不存在");
        }
        
        // 防报废穿透：已报废的设备禁止报修
        if ("报废".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：该设备已报废，无法报修！");
        }
        
        // 重复报修校验：已经在维修中的设备禁止再次报修
        if ("维修".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：该设备已在维修中！");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        String currentUsername = BaseContext.getCurrentName();

        // 资产管理员只能报修本单位的设备
        if (currentRole != null && currentRole == 2) {
            String currentUnitCode = BaseContext.getCurrentUnitCode();
            if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：无权报修其他单位的设备");
            }
        }

        // 报修人约束：普通操作员发起报修时，校验设备保管人是否是自己
        if (currentRole != null && currentRole == 0) {
            if (equipment.getCustodian() == null || !equipment.getCustodian().equals(currentUsername)) {
                throw new BusinessException("您不是该设备的保管人，无权发起报修！");
            }
        }

        // 默认初始化值
        maintenanceRecord.setEquipId(equipId);
        if (currentUsername != null) {
            maintenanceRecord.setReporter(currentUsername);
        }
        maintenanceRecord.setMaintStatus(0); // 0-待指派
        if (maintenanceRecord.getMaintDate() == null) {
            maintenanceRecord.setMaintDate(java.time.LocalDate.now());
        }

        boolean success = maintenanceRecordDao.maintenanceEquip(equipId, maintenanceRecord);
        if (success) {
            Number lastId = (Number) maintenanceRecordDao.singleSelect("SELECT LAST_INSERT_ID()");
            Integer maintId = lastId != null ? lastId.intValue() : null;
            operationLogService.record("设备报修", "maintenance_record", String.valueOf(maintId), 
                "报修人 " + currentUsername + " 报修设备 " + equipId + "，故障描述: " + maintenanceRecord.getFaultDescription(), 1, null);
        }
        return success;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean deleteMaintenanceRecords(String equipId, Integer maintId) {
        MaintenanceRecord record = maintenanceRecordDao.getById(maintId);
        if (record == null) {
            throw new BusinessException("该维保工单不存在");
        }
        if (!record.getEquipId().equals(equipId)) {
            throw new BusinessException("操作失败：工单与设备不匹配，无法撤销！");
        }
        if (record.getMaintStatus() == null) {
            throw new BusinessException("操作失败：工单状态缺失，无法撤销！");
        }
        if (record.getMaintStatus() == 1) {
            throw new BusinessException("操作失败：维修中的工单不允许删除，请先完工登记。");
        }
        if (record.getMaintStatus() == 2) {
            throw new BusinessException("操作失败：已完成的工单不允许删除。");
        }

        Equipment equipment = equipmentDao.getEquipmentById(equipId);
        if (equipment == null) {
            throw new BusinessException("操作失败：关联设备不存在，无法撤销工单！");
        }
        if (!"维修".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：关联设备当前未处于维修状态，无法撤销工单！");
        }

        boolean success = maintenanceRecordDao.deleteMaintenanceRecords(equipId, maintId);
        if (success) {
            operationLogService.record("维保撤销", "maintenance_record", maintId.toString(),
                    "撤销待指派维保工单 " + maintId + "，设备 " + equipId + " 恢复为在用", 1, null);
        }
        return success;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int putMaintenanceRecords(Integer maintId, MaintenanceRecord maintenanceRecord) {
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }
        if (oldRecord.getMaintStatus() == 0) {
            return assignMaintenance(maintId, maintenanceRecord.getMaintPersonId());
        } else if (oldRecord.getMaintStatus() == 1) {
            return completeMaintenance(maintId, maintenanceRecord);
        } else {
            throw new BusinessException("操作失败：该维保工单已完成，禁止二次修改！");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int assignMaintenance(Integer maintId, Integer maintPersonId) {
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole == null || currentRole != 2) {
            throw new BusinessException("操作失败：只有资产管理员可以指派工单！");
        }

        if (oldRecord.getMaintStatus() != 0) {
            throw new BusinessException("操作失败：当前工单状态不支持指派！");
        }

        if (maintPersonId == null) {
            throw new BusinessException("操作失败：指派工单必须指定维修工！");
        }

        User targetUser = userDao.getById(maintPersonId);
        if (targetUser == null || targetUser.getRole() != 1) {
            throw new BusinessException("操作失败：被指派人不存在或不是维修工程师！");
        }

        Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
        if (equipment == null || !"维修".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：关联设备未处于维修状态！");
        }

        String currentUnitCode = BaseContext.getCurrentUnitCode();
        if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
            throw new ForbiddenException("越权操作：无权指派其他单位的设备维保工单");
        }

        int rows = maintenanceRecordDao.assignMaintenance(maintId, maintPersonId, targetUser.getRealName());
        if (rows > 0) {
            operationLogService.record("维保指派", "maintenance_record", maintId.toString(), 
                "指派维保工单 " + maintId + " 给工程师 " + targetUser.getRealName(), 1, null);
        }
        return rows;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int completeMaintenance(Integer maintId, MaintenanceRecord record) {
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole == null || (currentRole != 1 && currentRole != 2)) {
            throw new BusinessException("操作失败：只有维修工或资产管理员可以登记维修结果！");
        }

        if (oldRecord.getMaintStatus() == 0) {
            throw new BusinessException("操作失败：工单尚未指派！");
        }
        if (oldRecord.getMaintStatus() == 2) {
            throw new BusinessException("操作失败：该维保工单已登记完工，禁止二次修改！");
        }
        if (oldRecord.getMaintStatus() == 3 || oldRecord.getMaintStatus() == 4) {
            throw new BusinessException("操作失败：该维保工单已完成，禁止二次修改！");
        }
        if (oldRecord.getMaintStatus() != 1) {
            throw new BusinessException("操作失败：当前工单状态不支持完工登记！");
        }

        if (currentRole == 1) {
            Integer currentUserId = BaseContext.getCurrentId();
            if (oldRecord.getMaintPersonId() == null || !oldRecord.getMaintPersonId().equals(currentUserId)) {
                throw new BusinessException("操作失败：您没有权限登记他人的维保工单！");
            }
        }

        Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
        if (equipment == null || !"维修".equals(equipment.getStatus())) {
            throw new BusinessException("操作失败：关联设备不处于维修状态！");
        }

        if (currentRole == 2) {
            String currentUnitCode = BaseContext.getCurrentUnitCode();
            if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权操作：无权登记其他单位的设备维保工单");
            }
        }

        int rows = maintenanceRecordDao.completeMaintenance(maintId, oldRecord.getEquipId(), record);
        if (rows > 0) {
            operationLogService.record("维保完工", "maintenance_record", maintId.toString(), 
                "完成维保工单 " + maintId + "，维修费用: " + record.getMaintCost() + ", 检修内容: " + record.getMaintContent(), 1, null);
        }
        return rows;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean reviewMaintenance(Integer maintId, String reviewer, String reviewComments) {
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole == null || currentRole != 2) {
            throw new ForbiddenException("越权操作：只有资产管理员可以复核工单");
        }

        if (oldRecord.getMaintStatus() == 0 || oldRecord.getMaintStatus() == 1) {
            throw new BusinessException("操作失败：工单尚未登记完工，无法复核！");
        }
        if (oldRecord.getMaintStatus() == 3 || oldRecord.getMaintStatus() == 4) {
            throw new BusinessException("操作失败：该工单已复核，禁止二次修改！");
        }
        if (oldRecord.getMaintStatus() != 2) {
            throw new BusinessException("操作失败：当前工单状态不支持复核！");
        }

        Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
        if (equipment == null) {
            throw new BusinessException("关联设备不存在");
        }

        String currentUnitCode = BaseContext.getCurrentUnitCode();
        if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
            throw new ForbiddenException("越权操作：无权复核其他单位的设备维保工单");
        }

        boolean success = maintenanceRecordDao.reviewMaintenance(maintId, oldRecord.getEquipId(), reviewer, reviewComments);
        if (success) {
            operationLogService.record("维保复核", "maintenance_record", maintId.toString(),
                "复核通过维保工单 " + maintId + "，结论：恢复在用。意见: " + reviewComments, 1, null);
        }
        return success;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean reviewToScrap(Integer maintId, String reviewer, String reviewComments, String scrapNo) {
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole == null || currentRole != 2) {
            throw new ForbiddenException("越权操作：只有资产管理员可以复核工单");
        }

        if (oldRecord.getMaintStatus() == 0 || oldRecord.getMaintStatus() == 1) {
            throw new BusinessException("操作失败：工单尚未登记完工，无法转报废！");
        }
        if (oldRecord.getMaintStatus() == 3 || oldRecord.getMaintStatus() == 4) {
            throw new BusinessException("操作失败：该工单已复核，禁止二次修改！");
        }
        if (oldRecord.getMaintStatus() != 2) {
            throw new BusinessException("操作失败：当前工单状态不支持转报废！");
        }

        Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
        if (equipment == null) {
            throw new BusinessException("关联设备不存在");
        }

        String currentUnitCode = BaseContext.getCurrentUnitCode();
        if (equipment.getUnitCode() == null || !equipment.getUnitCode().equals(currentUnitCode)) {
            throw new ForbiddenException("越权操作：无权复核其他单位的设备维保工单");
        }

        if (scrapNo == null || scrapNo.trim().isEmpty()) {
            scrapNo = "SCRAP-" + System.currentTimeMillis();
        }

        String oldCustodian = equipment.getCustodian();

        boolean success = maintenanceRecordDao.reviewToScrap(maintId, oldRecord.getEquipId(), reviewer, reviewComments, scrapNo, oldCustodian);
        if (success) {
            operationLogService.record("维保复核转报废", "maintenance_record", maintId.toString(),
                "复核维保工单 " + maintId + " 转报废，报废单号: " + scrapNo + "。意见: " + reviewComments, 1, null);
            
            operationLogService.record("设备报废", "equipment", oldRecord.getEquipId(),
                "设备报废：" + oldRecord.getEquipId() + "，原保管人：" + (oldCustodian != null ? oldCustodian : "无") + "，报废原因：" + reviewComments, 1, null);
        }
        return success;
    }
}
