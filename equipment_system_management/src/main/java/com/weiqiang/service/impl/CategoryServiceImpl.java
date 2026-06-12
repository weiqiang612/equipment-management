package com.weiqiang.service.impl;

import com.weiqiang.dao.CategoryDao;
import com.weiqiang.dao.EquipmentDao;
import com.weiqiang.exception.BusinessException;
import com.weiqiang.pojo.Category;
import com.weiqiang.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 分类服务实现类
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;

    private final EquipmentDao equipmentDao;

    @Override
    public List<Category> getCategories() {
        return categoryDao.getCategories();
    }

    @Override
    public Category getCategoryById(final String categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }

    @Override
    public int addCategory(final Category category) {
        return categoryDao.addCategory(category);
    }

    @Override
    public int deleteCategoryById(final String categoryId) {
        // 前置校验：该分类下若有关联的设备，禁止删除
        final Long count = equipmentDao.getEquipmentsNum(null, null, categoryId, null, null, null, null);
        if (count != null && count > 0) {
            throw new BusinessException("操作失败：该分类下有关联设备，无法删除！");
        }
        return categoryDao.deleteCategoryById(categoryId);
    }

    @Override
    public int updateCategory(final Category category, final String categoryId) {
        return categoryDao.updateCategory(category, categoryId);
    }
}
