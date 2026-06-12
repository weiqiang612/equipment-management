package com.weiqiang.dao;

import com.weiqiang.pojo.Equipment;
import com.weiqiang.utils.BaseContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

/**
 * 设备数据访问对象
 */
@Repository
public class EquipmentDao extends BasicDao<Equipment> {

    public List<Equipment> getEquipments() {
        String sql = "SELECT equip_id equipId, equip_name equipName, model, status, " +
                "purchase_date purchaseDate, original_value originalValue, d.unit_code unitCode ,unit_name unitName " +
                ", c.category_id categoryId ,category_name categoryName, e.custodian custodian " +
                "FROM equipment e " +
                "JOIN department d " +
                "ON e.unit_code = d.unit_code " +
                "JOIN category c " +
                "ON e.category_id = c.category_id";
        return mutiSelect(sql, Equipment.class, null);
    }

    public Equipment getEquipmentById(String equipId) {
        String sql = "SELECT equip_id equipId, equip_name equipName, model, status, " +
                "purchase_date purchaseDate, original_value originalValue, d.unit_code unitCode ,unit_name unitName " +
                ", c.category_id categoryId ,category_name categoryName, c.useful_life usefulLife, c.residual_rate residualRate, e.custodian custodian " +
                "FROM equipment e " +
                "JOIN department d " +
                "ON e.unit_code = d.unit_code " +
                "JOIN category c " +
                "ON e.category_id = c.category_id " +
                "where equip_id = ?";
        return selectOne(sql, Equipment.class, equipId);
    }

    public int addEquipment(Equipment equipment) {
        String sql = "INSERT INTO equipment (equip_id, equip_name, model, status, purchase_date, original_value, unit_code, category_id, custodian) VALUES" +
                "(? , ? , ? , ? , ? , ? , ? , ? , ?)";
        return update(sql, equipment.getEquipId(), equipment.getEquipName(), equipment.getModel(),
                equipment.getStatus(), equipment.getPurchaseDate(), equipment.getOriginalValue(),
                equipment.getUnitCode(), equipment.getCategoryId(), equipment.getCustodian());
    }

    public int updateEquipment(Equipment equipment) {
        String sql = "UPDATE equipment" +
                " SET equip_name = ?, model = ?, status = ?, purchase_date = ?," +
                " original_value = ?, unit_code = ?, category_id = ?, custodian = ?" +
                " WHERE equip_id = ?";
        return update(sql, equipment.getEquipName(), equipment.getModel(), equipment.getStatus(),
                equipment.getPurchaseDate(), equipment.getOriginalValue(), equipment.getUnitCode(),
                equipment.getCategoryId(), equipment.getCustodian(), equipment.getEquipId());
    }

    // 删除设备，需要将检修表 调拨表 报废表的记录一并删除
    public boolean deleteEquipment(String equipId) {
        LinkedHashMap<String, List<Object>> sqls = new LinkedHashMap<>();
        List<Object> params = new ArrayList<>(Collections.singletonList(equipId));
        String[] sqlList = {
                "DELETE FROM maintenance_record WHERE equip_id = ?",
                "DELETE FROM scrap_record WHERE equip_id = ?",
                "DELETE FROM transfer_record WHERE equip_id = ?",
                "DELETE FROM equipment WHERE equip_id = ?"
        };
        for (String s : sqlList) {
            sqls.put(s, params);
        }
        return updateWithTransaction(sqls);
    }

    // 根据所给条件进行动态查询
    public List<Equipment> getEquipmentsDynamic(final String equipName, final String unitCode, final String categoryId, final String status, final LocalDate begin, final LocalDate end, final String custodian, final Integer page, final Integer pageSize) {
        final StringBuilder sql = new StringBuilder("SELECT equip_id equipId, equip_name equipName, model, status, " +
                "purchase_date purchaseDate, original_value originalValue, d.unit_code unitCode ,unit_name unitName " +
                ", c.category_id categoryId ,category_name categoryName ,c.useful_life usefulLife, c.residual_rate residualRate, e.custodian custodian " +
                "FROM equipment e " +
                "JOIN department d " +
                "ON e.unit_code = d.unit_code " +
                "JOIN category c " +
                "ON e.category_id = c.category_id " +
                "LEFT JOIN sys_user u " +
                "ON e.custodian = u.username ");
        sql.append("where 1=1 ");
        final ArrayList<Object> params = new ArrayList<>(16);

        // 【P0 级数据隔离逻辑】若为操作员（role=0），强制追加部门及保管人条件限制
        final Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 0) {
            final String currentUsername = BaseContext.getCurrentName();
            final String currentUnitCode = BaseContext.getCurrentUnitCode();
            sql.append("AND (e.custodian = ? OR (e.custodian IS NULL AND e.unit_code = ?)) ");
            params.add(currentUsername);
            params.add(currentUnitCode);
        }

        if (equipName != null && !equipName.trim().isEmpty()) {
            sql.append("AND (equip_id LIKE ? OR e.equip_name like ?) ");
            params.add("%" + equipName + "%");
            params.add("%" + equipName + "%");
        }
        if (unitCode != null && !unitCode.trim().isEmpty()){
            sql.append("AND e.unit_code = ? ");
            params.add(unitCode);
        }
        if (categoryId != null && !categoryId.trim().isEmpty()){
            sql.append("AND e.category_id = ? ");
            params.add(categoryId);
        }
        if (status != null && !status.trim().isEmpty()){
            sql.append("AND status = ? ");
            params.add(status);
        }
        if (custodian != null && !custodian.trim().isEmpty()) {
            sql.append("AND (e.custodian LIKE ? OR u.real_name LIKE ?) ");
            params.add("%" + custodian + "%");
            params.add("%" + custodian + "%");
        }
        if (begin != null && end != null) {
            sql.append("AND purchase_date BETWEEN ? AND ? ");
            params.add(begin);
            params.add(end);
        }
        sql.append("order by purchase_date desc ");
        if (page != null && pageSize != null) {
            final int offset = (page - 1) * pageSize;
            sql.append("limit ?,? ");
            params.add(offset);
            params.add(pageSize);
        }
        return mutiSelect(String.valueOf(sql), Equipment.class, params.toArray());
    }

    // 查询总数量
    public Long getEquipmentsNum(final String equipName, final String unitCode, final String categoryId, final String status, final LocalDate begin, final LocalDate end, final String custodian) {
        final StringBuilder sql = new StringBuilder("SELECT count(*) " +
                " FROM equipment e " +
                " JOIN department d " +
                " ON e.unit_code = d.unit_code " +
                " JOIN category c " +
                " ON e.category_id = c.category_id  " +
                " LEFT JOIN sys_user u " +
                " ON e.custodian = u.username ");
        sql.append("where 1=1 ");
        final ArrayList<Object> params = new ArrayList<>(16);

        // 【P0 级数据隔离逻辑】若为操作员（role=0），强制追加部门及保管人条件限制
        final Integer currentRole = BaseContext.getCurrentRole();
        if (currentRole != null && currentRole == 0) {
            final String currentUsername = BaseContext.getCurrentName();
            final String currentUnitCode = BaseContext.getCurrentUnitCode();
            sql.append("AND (e.custodian = ? OR (e.custodian IS NULL AND e.unit_code = ?)) ");
            params.add(currentUsername);
            params.add(currentUnitCode);
        }

        if (equipName != null && !equipName.trim().isEmpty()) {
            sql.append("AND (equip_id LIKE ? OR e.equip_name like ?) ");
            params.add("%" + equipName + "%");
            params.add("%" + equipName + "%");
        }
        if (unitCode != null && !unitCode.trim().isEmpty()){
            sql.append("AND e.unit_code = ? ");
            params.add(unitCode);
        }
        if (categoryId != null && !categoryId.trim().isEmpty()){
            sql.append("AND e.category_id = ? ");
            params.add(categoryId);
        }
        if (status != null && !status.trim().isEmpty()){
            sql.append("AND status = ? ");
            params.add(status);
        }
        if (custodian != null && !custodian.trim().isEmpty()) {
            sql.append("AND (e.custodian LIKE ? OR u.real_name LIKE ?) ");
            params.add("%" + custodian + "%");
            params.add("%" + custodian + "%");
        }
        if (begin != null && end != null) {
            sql.append("AND purchase_date BETWEEN ? AND ? ");
            params.add(begin);
            params.add(end);
        }
        return (Long) singleSelect(String.valueOf(sql), params.toArray());
    }
}
