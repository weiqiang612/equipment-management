package com.weiqiang.dao;


import com.weiqiang.entity.ScrapRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author 袁志刚
 * @version 1.0
 */

@Repository
public class ScrapRecordDao extends BasicDao<ScrapRecord> {
    public List<ScrapRecord> getScrapRecords() {
        String sql = "SELECT equip_id equipId,scrap_no scrapNo,scrap_date scrapDate,approver,reason " +
                "FROM scrap_record";
        return mutiSelect(sql,ScrapRecord.class,null);
    }


    /*
    UPDATE equipment SET `status` = '报废' WHERE equip_id = 'E2024014';
    INSERT INTO scrap_record (equip_id, scrap_no, scrap_date, approver, reason) VALUES
    ('E2024014', 'SCRAP-2025-001', '2025-12-10', '资产处老王', '硬件老化无法修复');
    */
    public boolean scrapEquip(String equipId, ScrapRecord scrapRecord, String oldCustodian) {
        String sql1 = "UPDATE equipment SET `status` = '报废', `custodian` = NULL WHERE equip_id = ?";
        String sql2 = "INSERT INTO scrap_record (equip_id, scrap_no, scrap_date, approver, reason) VALUES " +
                "    (?, ?, ?, ?, ?)";
        ArrayList<Object> params1 = new ArrayList<>();
        params1.add(equipId);
        ArrayList<Object> params2 = new ArrayList<>();
        params2.add(scrapRecord.getEquipId());
        params2.add(scrapRecord.getScrapNo());
        params2.add(scrapRecord.getScrapDate());
        params2.add(scrapRecord.getApprover());
        params2.add(scrapRecord.getReason());

        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);

        if (oldCustodian != null && !oldCustodian.trim().isEmpty()) {
            String sql3 = "INSERT INTO t_equipment_claim (equip_id, applicant, approver, status, remark) VALUES (?, ?, ?, ?, ?)";
            List<Object> params3 = new ArrayList<>();
            params3.add(equipId);
            params3.add(oldCustodian);
            params3.add(scrapRecord.getApprover());
            params3.add(4); // 已退还
            params3.add("设备报废导致保管关系清退");
            sqlTasks.put(sql3, params3);
        }

        return updateWithTransaction(sqlTasks);
    }

    /*
    DELETE FROM scrap_record WHERE scrap_no = 'SCRAP-2025-001';
    UPDATE equipment SET `status` = '在用' WHERE equip_id = 'E2024014';
    */
    // 删除报废记录
    // 将设备从表中删除，然后将设备状态设为在用，
    public boolean deleteScrapRecord(String equipId, String scrapNo) {
        String sql1 = "DELETE FROM scrap_record WHERE scrap_no = ?";
        String sql2 = "UPDATE equipment SET `status` = '在用' WHERE equip_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(scrapNo);
        List<Object> params2 = new ArrayList<>();
        params2.add(equipId);
        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1,params1);
        sqlTasks.put(sql2,params2);
        return updateWithTransaction(sqlTasks);
    }

    // 修改报废表除报废单号和设备编号外的其他字段
    /*
    UPDATE scrap_record SET scrap_date = '2025-12-10',approver = '资产处老李',reason = '硬件老化无法修复'
    WHERE scrap_no = 'SCRAP-2025-001';
    */
    public int putScrapRecord(String scrapNo, ScrapRecord scrapRecord) {
        String sql = "UPDATE scrap_record SET scrap_date = ?,approver = ?,reason = ?  " +
                "    WHERE scrap_no = ?";
        return update(sql,scrapRecord.getScrapDate(),scrapRecord.getApprover(),scrapRecord.getReason(),
                scrapNo);
    }
}
