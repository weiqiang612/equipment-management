package com.weiqiang.service.impl;

import com.weiqiang.dao.DepartmentDao;
import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.entity.Department;
import com.weiqiang.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 部门服务实现类
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentDao departmentDao;

    private final EquipmentDao equipmentDao;

    @Override
    public List<Department> getDepts() {
        return departmentDao.getDepts();
    }

    @Override
    public int addDept(final Department department) {
        return departmentDao.addDept(department);
    }

    @Override
    public int deleteDept(final String unitCode) {
        // 前置校验：该部门下若有关联的设备，禁止删除
        final Long count = equipmentDao.getEquipmentsNum(null, unitCode, null, null, null, null, null);
        if (count != null && count > 0) {
            throw new BusinessException("操作失败：该部门下有关联设备，无法删除！");
        }
        return departmentDao.deleteDept(unitCode);
    }

    @Override
    public Department getDeptById(final String unitCode) {
        return departmentDao.getDeptById(unitCode);
    }

    @Override
    public int updateDept(final Department department, final String unitCode) {
        return departmentDao.updateDept(department, unitCode);
    }
}
