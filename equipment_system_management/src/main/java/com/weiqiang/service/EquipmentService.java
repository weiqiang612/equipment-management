package com.weiqiang.service;


import com.weiqiang.entity.Equipment;
import com.weiqiang.vo.EquipmentDepreciationVO;
import com.weiqiang.common.PageBean;

import java.time.LocalDate;
import java.util.List;

/**
 * @author 袁志刚
 * @version 1.0
 */


import com.weiqiang.vo.EquipmentDetailVO;

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
