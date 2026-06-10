import request from '@/utils/request'

// 1. 获取所有分类 (GET /categories)
export function getCategories() {
    return request({
        url: '/categories',
        method: 'get'
    })
}

// 2. 根据ID获取分类 (GET /categories/{categoryId})
export function getCategoryById(id) {
    return request({
        url: `/categories/${id}`,
        method: 'get'
    })
}

// 3. 增加分类 (POST /categories)
export function addCategory(data) {
    return request({
        url: '/categories',
        method: 'post',
        data
    })
}

// 4. 修改分类 (PUT /categories/{categoryId})
export function updateCategory(data) {
    return request({
        url: `/categories/${data.categoryId}`,
        method: 'put',
        data
    })
}

// 5. 删除分类 (DELETE /categories/{categoryId})
export function deleteCategory(id) {
    return request({
        url: `/categories/${id}`,
        method: 'delete'
    })
}