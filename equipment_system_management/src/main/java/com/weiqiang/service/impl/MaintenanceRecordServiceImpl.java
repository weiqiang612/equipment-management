package com.weiqiang.service.impl;

import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.dao.MaintenanceRecordDao;
import com.weiqiang.dao.UserDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.MaintenanceRecord;
import com.weiqiang.pojo.User;
import com.weiqiang.service.MaintenanceRecordService;
import com.weiqiang.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public List<MaintenanceRecord> getMaintenanceRecords() {
        return maintenanceRecordDao.getMaintenanceRecords();
    }

    @Override
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

        return maintenanceRecordDao.maintenanceEquip(equipId, maintenanceRecord);
    }

    @Override
    public boolean deleteMaintenanceRecords(String equipId, Integer maintId) {
        MaintenanceRecord record = maintenanceRecordDao.getById(maintId);
        if (record == null) {
            throw new BusinessException("该维保工单不存在");
        }
        return maintenanceRecordDao.deleteMaintenanceRecords(equipId, maintId);
    }

    @Override
    public int putMaintenanceRecords(Integer maintId, MaintenanceRecord maintenanceRecord) {
        // 1. 获取原工单信息
        MaintenanceRecord oldRecord = maintenanceRecordDao.getById(maintId);
        if (oldRecord == null) {
            throw new BusinessException("该维保工单不存在");
        }

        Integer currentRole = BaseContext.getCurrentRole();
        
        // 2. 根据原工单的状态执行不同的业务分支
        if (oldRecord.getMaintStatus() == 0) {
            // 分支 A: 指派工单 (状态 0 -> 1)
            // 校验 A1: 只有资产管理员(2)可以指派工单
            if (currentRole == null || currentRole != 2) {
                throw new BusinessException("操作失败：只有资产管理员可以指派工单！");
            }
            
            // 校验 A2: 被指派的维修工ID不能为空
            Integer targetPersonId = maintenanceRecord.getMaintPersonId();
            if (targetPersonId == null) {
                throw new BusinessException("操作失败：指派工单必须指定维修工！");
            }
            
            // 校验 A3: 被指派人必须在 sys_user 中存在且角色为 1(维修工)
            User targetUser = userDao.getById(targetPersonId);
            if (targetUser == null || targetUser.getRole() != 1) {
                throw new BusinessException("操作失败：被指派人不存在或不是维修工程师！");
            }
            
            // 校验 A4: 关联设备必须处于'维修'状态
            Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
            if (equipment == null || !"维修".equals(equipment.getStatus())) {
                throw new BusinessException("操作失败：关联设备未处于维修状态！");
            }
            
            // 执行指派：更新工单状态为 1，并写入指派的维修工ID和姓名
            return maintenanceRecordDao.assignMaintenance(maintId, targetPersonId, targetUser.getRealName());
            
        } else if (oldRecord.getMaintStatus() == 1) {
            // 分支 B: 登记维保结果 (状态 1 -> 2)
            // 校验 B1: 只有维修工(1)或资产管理员(2)可以登记维保结果，且维修工只能登记分配给自己的工单
            if (currentRole == null || (currentRole != 1 && currentRole != 2)) {
                throw new BusinessException("操作失败：只有维修工或资产管理员可以登记维修结果！");
            }
            if (currentRole == 1) {
                Integer currentUserId = BaseContext.getCurrentId();
                if (oldRecord.getMaintPersonId() == null || !oldRecord.getMaintPersonId().equals(currentUserId)) {
                    throw new BusinessException("操作失败：您没有权限登记他人的维保工单！");
                }
            }
            
            // 校验 B2: 关联设备必须处于'维修'状态
            Equipment equipment = equipmentDao.getEquipmentById(oldRecord.getEquipId());
            if (equipment == null || !"维修".equals(equipment.getStatus())) {
                throw new BusinessException("操作失败：关联设备不处于维修状态！");
            }
            
            // 执行登记维保结果：状态置为 2 且设备改回在用
            return maintenanceRecordDao.completeMaintenance(maintId, oldRecord.getEquipId(), maintenanceRecord);
            
        } else {
            // 分支 C: 工单已完结 (状态为 2)
            throw new BusinessException("操作失败：该维保工单已完成，禁止二次修改！");
        }
    }
}
