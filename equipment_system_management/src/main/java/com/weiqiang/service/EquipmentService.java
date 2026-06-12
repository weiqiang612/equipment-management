package com.weiqiang.service;


import com.weiqiang.pojo.Equipment;
import com.weiqiang.pojo.EquipmentDepreciationVO;
import com.weiqiang.pojo.PageBean;

import java.time.LocalDate;
import java.util.List;

/**
 * @author 袁志刚
 * @version 1.0
 */


import com.weiqiang.pojo.EquipmentDetailVO;

public interface EquipmentService {
    EquipmentDetailVO getEquipmentDetail(String equipId);
    List<Equipment> getEquipments();

    Equipment getEquipmentById(String equipId);

    int addEquipment(Equipment equipment);

    int updateEquipment(Equipment equipment);

    boolean deleteEquipment(String equipId);

    PageBean getEquipmentsDynamic(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end, String custodian, Integer page, Integer pageSize);

    List<EquipmentDepreciationVO> getEquipmentsDynamicForExport(String equipName, String unitCode, String categoryId, String status, LocalDate begin, LocalDate end, String custodian);
}
