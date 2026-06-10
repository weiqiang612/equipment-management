package com.weiqiang.service.impl;

import com.weiqiang.dao.DepartmentDao;
import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Department;
import com.weiqiang.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务实现类
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private EquipmentDao equipmentDao;

    @Override
    public List<Department> getDepts() {
        return departmentDao.getDepts();
    }

    @Override
    public int addDept(Department department) {
        return departmentDao.addDept(department);
    }

    @Override
    public int deleteDept(String unitCode) {
        // 前置校验：该部门下若有关联的设备，禁止删除
        Long count = equipmentDao.getEquipmentsNum(null, unitCode, null, null, null, null);
        if (count != null && count > 0) {
            throw new BusinessException("操作失败：该部门下有关联设备，无法删除！");
        }
        return departmentDao.deleteDept(unitCode);
    }

    @Override
    public Department getDeptById(String unitCode) {
        return departmentDao.getDeptById(unitCode);
    }

    @Override
    public int updateDept(Department department, String unitCode) {
        return departmentDao.updateDept(department, unitCode);
    }
}
