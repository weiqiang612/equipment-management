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
        StringBuilder sql = new StringBuilder("SELECT mr.maint_id maintId, mr.equip_id equipId, e.equip_name equipName, " +
                "mr.maint_date maintDate, mr.maint_content maintContent, mr.maint_cost maintCost, " +
                "mr.maint_person maintPerson, mr.reporter reporter, mr.fault_description faultDescription, " +
                "mr.maint_status maintStatus, mr.maint_person_id maintPersonId, mr.reviewer reviewer, " +
                "mr.review_comments reviewComments, mr.review_date reviewDate " +
                "FROM maintenance_record mr " +
                "LEFT JOIN equipment e ON mr.equip_id = e.equip_id ");
        
        ArrayList<Object> params = new ArrayList<>();
        // 【P0 级数据隔离逻辑】若为操作员（role=0），仅能查询其作为报修人的维保记录
        Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 0) {
            sql.append("WHERE mr.reporter = ? ");
            params.add(BaseContext.getCurrentName());
        } else if (currentRole != null && currentRole == 2) {
            sql.append("WHERE e.unit_code = ? ");
            params.add(BaseContext.getCurrentUnitCode());
        }
        
        sql.append("ORDER BY maint_id DESC ");
        
        return mutiSelect(String.valueOf(sql), MaintenanceRecord.class, params.isEmpty() ? null : params.toArray());
    }

    public MaintenanceRecord getById(Integer maintId) {
        String sql = "SELECT mr.maint_id maintId, mr.equip_id equipId, e.equip_name equipName, " +
                "mr.maint_date maintDate, mr.maint_content maintContent, mr.maint_cost maintCost, " +
                "mr.maint_person maintPerson, mr.reporter reporter, mr.fault_description faultDescription, " +
                "mr.maint_status maintStatus, mr.maint_person_id maintPersonId, mr.reviewer reviewer, " +
                "mr.review_comments reviewComments, mr.review_date reviewDate " +
                "FROM maintenance_record mr " +
                "LEFT JOIN equipment e ON mr.equip_id = e.equip_id " +
                "WHERE mr.maint_id = ?";
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

    // 完成维保逻辑：登记维修结果并完结工单 (状态置为 2)，设备保持维修态
    public int completeMaintenance(Integer maintId, String equipId, MaintenanceRecord record) {
        String sql = "UPDATE maintenance_record SET maint_date = ?, maint_content = ?, maint_cost = ?, maint_person = ?, maint_status = 2 WHERE maint_id = ?";
        return update(sql, 
                record.getMaintDate() != null ? record.getMaintDate() : java.time.LocalDate.now(),
                record.getMaintContent(),
                record.getMaintCost(),
                record.getMaintPerson(),
                maintId);
    }

    // 仅撤销待指派工单，并将设备状态恢复为在用
    public boolean deleteMaintenanceRecords(String equipId, Integer maintId) {
        String sql1 = "DELETE FROM maintenance_record WHERE maint_id = ? AND equip_id = ? AND maint_status = 0";
        String sql2 = "UPDATE equipment SET `status` = '在用' WHERE equip_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(maintId);
        params1.add(equipId);
        List<Object> params2 = new ArrayList<>();
        params2.add(equipId);
        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);
        return updateWithTransaction(sqlTasks);
    }

    // 复核通过：工单置 3，设备状态置 在用，记录复核字段
    public boolean reviewMaintenance(Integer maintId, String equipId, String reviewer, String reviewComments) {
        String sqlMaint = "UPDATE maintenance_record SET maint_status = 3, reviewer = ?, review_comments = ?, review_date = ? WHERE maint_id = ?";
        List<Object> paramsMaint = new ArrayList<>();
        paramsMaint.add(reviewer);
        paramsMaint.add(reviewComments);
        paramsMaint.add(java.time.LocalDateTime.now());
        paramsMaint.add(maintId);

        String sqlEquip = "UPDATE equipment SET status = '在用' WHERE equip_id = ?";
        List<Object> paramsEquip = new ArrayList<>();
        paramsEquip.add(equipId);

        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sqlMaint, paramsMaint);
        sqlTasks.put(sqlEquip, paramsEquip);

        return updateWithTransaction(sqlTasks);
    }

    // 复核转报废：工单置 4，设备状态置 报废，清空保管人，生成报废记录并插入退还流水
    public boolean reviewToScrap(Integer maintId, String equipId, String reviewer, String reviewComments, String scrapNo, String oldCustodian) {
        String sqlMaint = "UPDATE maintenance_record SET maint_status = 4, reviewer = ?, review_comments = ?, review_date = ? WHERE maint_id = ?";
        List<Object> paramsMaint = new ArrayList<>();
        paramsMaint.add(reviewer);
        paramsMaint.add(reviewComments);
        paramsMaint.add(java.time.LocalDateTime.now());
        paramsMaint.add(maintId);

        String sqlEquip = "UPDATE equipment SET status = '报废', custodian = NULL WHERE equip_id = ?";
        List<Object> paramsEquip = new ArrayList<>();
        paramsEquip.add(equipId);

        String sqlScrap = "INSERT INTO scrap_record (equip_id, scrap_no, scrap_date, approver, reason) VALUES (?, ?, ?, ?, ?)";
        List<Object> paramsScrap = new ArrayList<>();
        paramsScrap.add(equipId);
        paramsScrap.add(scrapNo);
        paramsScrap.add(java.time.LocalDate.now());
        paramsScrap.add(reviewer);
        paramsScrap.add(reviewComments);

        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sqlMaint, paramsMaint);
        sqlTasks.put(sqlEquip, paramsEquip);
        sqlTasks.put(sqlScrap, paramsScrap);

        if (oldCustodian != null && !oldCustodian.trim().isEmpty()) {
            String sqlClaim = "INSERT INTO t_equipment_claim (equip_id, applicant, approver, status, remark) VALUES (?, ?, ?, ?, ?)";
            List<Object> paramsClaim = new ArrayList<>();
            paramsClaim.add(equipId);
            paramsClaim.add(oldCustodian);
            paramsClaim.add(reviewer);
            paramsClaim.add(4); // STATUS_RETURNED
            paramsClaim.add("设备报废导致保管关系清退");
            sqlTasks.put(sqlClaim, paramsClaim);
        }

        return updateWithTransaction(sqlTasks);
    }
}
