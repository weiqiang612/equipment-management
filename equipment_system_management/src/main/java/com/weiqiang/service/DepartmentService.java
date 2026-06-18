package com.weiqiang.service;

import com.weiqiang.entity.Department;

import java.util.List;

/**
 * @author 袁志刚
 * @version 1.0
 */


public interface DepartmentService {
    List<Department> getDepts();

    int addDept(Department department);

    int deleteDept(String unitCode);

    Department getDeptById(String unitCode);

    int updateDept(Department department,String unitCode);
}
