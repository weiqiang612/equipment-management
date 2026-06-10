package com.weiqiang.dao;

import com.weiqiang.pojo.MaintenanceRecord;
import com.weiqiang.utils.BaseContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 维修记录数据访问对象
 */
@Repository
public class MaintenanceRecordDao extends BasicDao<MaintenanceRecord>{

    public List<MaintenanceRecord> getMaintenanceRecords() {
        StringBuilder sql = new StringBuilder("SELECT maint_id maintId, equip_id equipId, maint_date maintDate, maint_content maintContent, " +
                "maint_cost maintCost, maint_person maintPerson, reporter, fault_description faultDescription, maint_status maintStatus, maint_person_id maintPersonId " +
                "FROM maintenance_record ");
        
        ArrayList<Object> params = new ArrayList<>();
        // 【P0 级数据隔离逻辑】若为操作员（role=0），仅能查询其作为报修人的维保记录
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 0) {
            sql.append("WHERE reporter = ? ");
            params.add(BaseContext.getCurrentName());
        }
        
        sql.append("ORDER BY maint_id DESC ");
        
        return mutiSelect(String.valueOf(sql), MaintenanceRecord.class, params.isEmpty() ? null : params.toArray());
    }

    public MaintenanceRecord getById(Integer maintId) {
        String sql = "SELECT maint_id maintId, equip_id equipId, maint_date maintDate, maint_content maintContent, " +
                "maint_cost maintCost, maint_person maintPerson, reporter, fault_description faultDescription, maint_status maintStatus, maint_person_id maintPersonId " +
                "FROM maintenance_record WHERE maint_id = ?";
        return selectOne(sql, MaintenanceRecord.class, maintId);
    }

    public boolean maintenanceEquip(String equipId, MaintenanceRecord maintenanceRecord) {
        String sql1 = "UPDATE equipment SET `status` = '维修' WHERE equip_id = ?";
        String sql2 = "INSERT INTO maintenance_record (equip_id, maint_date, maint_content, maint_cost, maint_person, reporter, fault_description, maint_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        ArrayList<Object> params1 = new ArrayList<>();
        params1.add(equipId);
        
        ArrayList<Object> params2 = new ArrayList<>();
        params2.add(equipId);
        params2.add(maintenanceRecord.getMaintDate());
        params2.add(maintenanceRecord.getMaintContent());
        params2.add(maintenanceRecord.getMaintCost());
        params2.add(maintenanceRecord.getMaintPerson());
        params2.add(maintenanceRecord.getReporter());
        params2.add(maintenanceRecord.getFaultDescription());
        params2.add(maintenanceRecord.getMaintStatus());

        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);
        return updateWithTransaction(sqlTasks);
    }

    // 指派工单逻辑：将工单状态置为 1 (维修中)，并设定指派的维修工ID与姓名
    public int assignMaintenance(Integer maintId, Integer maintPersonId, String maintPersonName) {
        String sql = "UPDATE maintenance_record SET maint_status = 1, maint_person_id = ?, maint_person = ? WHERE maint_id = ?";
        return update(sql, maintPersonId, maintPersonName, maintId);
    }

    // 完成维保逻辑：登记维修结果并完结工单 (状态置为 2)，且将关联设备状态重新设置回'在用'
    public int completeMaintenance(Integer maintId, String equipId, MaintenanceRecord record) {
        String sql1 = "UPDATE maintenance_record SET maint_date = ?, maint_content = ?, maint_cost = ?, maint_person = ?, maint_status = 2 WHERE maint_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(record.getMaintDate() != null ? record.getMaintDate() : java.time.LocalDate.now());
        params1.add(record.getMaintContent());
        params1.add(record.getMaintCost());
        params1.add(record.getMaintPerson());
        params1.add(maintId);

        String sql2 = "UPDATE equipment SET `status` = '在用' WHERE equip_id = ?";
        List<Object> params2 = new ArrayList<>();
        params2.add(equipId);

        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);
        boolean success = updateWithTransaction(sqlTasks);
        return success ? 1 : 0;
    }

    // 先将记录从维修表中删除 后将设备状态设为在用
    public boolean deleteMaintenanceRecords(String equipId, Integer maintId) {
        String sql1 = "DELETE FROM maintenance_record WHERE maint_id = ?";
        String sql2 = "UPDATE equipment SET `status` = '在用' WHERE equip_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(maintId);
        List<Object> params2 = new ArrayList<>();
        params2.add(equipId);
        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);
        return updateWithTransaction(sqlTasks);
    }
}
