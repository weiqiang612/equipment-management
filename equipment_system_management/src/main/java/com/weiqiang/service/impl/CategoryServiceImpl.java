package com.weiqiang.service.impl;

import com.weiqiang.dao.CategoryDao;
import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Category;
import com.weiqiang.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private EquipmentDao equipmentDao;

    @Override
    public List<Category> getCategories() {
        return categoryDao.getCategories();
    }

    @Override
    public Category getCategoryById(String categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }

    @Override
    public int addCategory(Category category) {
        return categoryDao.addCategory(category);
    }

    @Override
    public int deleteCategoryById(String categoryId) {
        // 前置校验：该分类下若有关联的设备，禁止删除
        Long count = equipmentDao.getEquipmentsNum(null, null, categoryId, null, null, null);
        if (count != null && count > 0) {
            throw new BusinessException("操作失败：该分类下有关联设备，无法删除！");
        }
        return categoryDao.deleteCategoryById(categoryId);
    }

    @Override
    public int updateCategory(Category category, String categoryId) {
        return categoryDao.updateCategory(category, categoryId);
    }
}
