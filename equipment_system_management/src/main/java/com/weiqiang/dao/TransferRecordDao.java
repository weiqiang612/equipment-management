package com.weiqiang.dao;

import com.weiqiang.entity.TransferRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 调拨记录数据访问对象
 */
@Repository
public class TransferRecordDao extends BasicDao<TransferRecord> {

    public List<TransferRecord> getTransferRecords() {
        String sql = "SELECT transfer_id transferId, t.equip_id equipId,equip_name equipName, " +
                " out_unit_code outUnitCode, d1.unit_name outUnitName, " +
                " in_unit_code inUnitCode, d2.unit_name inUnitName,transfer_date transferDate, change_type changeType, operator, reason " +
                "FROM transfer_record t " +
                "LEFT JOIN " +
                " equipment e ON t.equip_id = e.equip_id" +
                " LEFT JOIN " +
                " department d1 ON t.out_unit_code = d1.unit_code " +
                " LEFT JOIN " +
                " department d2 ON t.in_unit_code = d2.unit_code " +
                "ORDER BY " +
                " t.transfer_date DESC ";
        return mutiSelect(sql, TransferRecord.class, null);
    }

    public TransferRecord getTransferRecordById(Integer transferId) {
        String sql = "SELECT transfer_id transferId, t.equip_id equipId,equip_name equipName, " +
                " out_unit_code outUnitCode, d1.unit_name outUnitName, " +
                " in_unit_code inUnitCode, d2.unit_name inUnitName,transfer_date transferDate, change_type changeType, operator, reason " +
                "FROM transfer_record t " +
                "LEFT JOIN " +
                " equipment e ON t.equip_id = e.equip_id" +
                " LEFT JOIN " +
                " department d1 ON t.out_unit_code = d1.unit_code " +
                " LEFT JOIN " +
                " department d2 ON t.in_unit_code = d2.unit_code " +
                " WHERE transfer_id = ? " +
                " ORDER BY " +
                " t.transfer_date DESC ";
        return selectOne(sql, TransferRecord.class, transferId);
    }

    public boolean transferEquip(String equipId, TransferRecord transferRecord, String oldCustodian) {
        // 调拨时自动将设备的 custodian 清空为 NULL
        String sql1 = "UPDATE equipment SET unit_code = ?, custodian = NULL WHERE equip_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(transferRecord.getInUnitCode());
        params1.add(equipId);
        
        String sql2 = "INSERT INTO transfer_record (equip_id, out_unit_code, in_unit_code, transfer_date, change_type, operator, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object> params2 = new ArrayList<>();
        params2.add(equipId);
        params2.add(transferRecord.getOutUnitCode());
        params2.add(transferRecord.getInUnitCode());
        params2.add(transferRecord.getTransferDate());
        params2.add(transferRecord.getChangeType());
        params2.add(transferRecord.getOperator());
        params2.add(transferRecord.getReason());
        
        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);

        if (oldCustodian != null && !oldCustodian.trim().isEmpty()) {
            String sql3 = "INSERT INTO t_equipment_claim (equip_id, applicant, approver, status, remark) VALUES (?, ?, ?, ?, ?)";
            List<Object> params3 = new ArrayList<>();
            params3.add(equipId);
            params3.add(oldCustodian);
            params3.add(transferRecord.getOperator());
            params3.add(4); // 已退还
            params3.add("设备调拨导致保管关系清退");
            sqlTasks.put(sql3, params3);
        }
        
        return updateWithTransaction(sqlTasks);
    }

    public int updateTransferRecord(Integer transferId, TransferRecord transferRecord) {
        String sql = "UPDATE transfer_record SET transfer_date = ?," +
                "change_type = ?,operator = ?,reason = ? WHERE transfer_id = ?";
        return update(sql, transferRecord.getTransferDate(), transferRecord.getChangeType(),
                transferRecord.getOperator(), transferRecord.getReason(), transferId);
    }

    // 将设备单位信息改回原单位代码 并从表中删除
    public boolean deleteTransferRecord(Integer transferId, String equipId, String outUnitCode) {
        String sql1 = "UPDATE equipment SET unit_code = ? WHERE equip_id = ?";
        List<Object> params1 = new ArrayList<>();
        params1.add(outUnitCode);
        params1.add(equipId);
        String sql2 = "DELETE FROM transfer_record WHERE transfer_id = ?";
        List<Object> params2 = new ArrayList<>();
        params2.add(transferId);
        LinkedHashMap<String, List<Object>> sqlTasks = new LinkedHashMap<>();
        sqlTasks.put(sql1, params1);
        sqlTasks.put(sql2, params2);
        return updateWithTransaction(sqlTasks);
    }
}
