import request from '@/utils/request'

// --- 设备管理接口 ---
export function getEquipments(params) {
    return request({
        url: '/equipments',
        method: 'get',
        params // axios 会自动将 params 拼接到 URL 后面
    })
}
export const addEquipment = (data) => request({
    url: '/equipments',
    method: 'post',
    data
})
export const updateEquipment = (data) => request({
    url: `/equipments/${data.equipId}`,
    method: 'put',
    data
})
export const deleteEquipment = (equipId) => request({
    url: `/equipments/${equipId}`, // 确保 ID 直接拼在路径上
    method: 'delete'
})

// --- 单位管理接口 ---
// 获取所有部门用于下拉框
export const getDeptList = () => request({
    url: '/departments',
    method: 'get'
})

// --- 分类管理接口 ---
// 获取所有分类用于下拉框
export const getCategoryList = () => request({
    url: '/categories',
    method: 'get'
})


// 1. 登记检修
export const maintenanceEquip = (equipId, data) => request({
    url: `/equipments/maint/${equipId}`,
    method: 'post',
    data
})

// 2. 设备报废
export const scrapEquipment = (equipId, data) => request({
    url: `/equipments/scrap/${equipId}`,
    method: 'post',
    data
})

// 3. 设备调拨
export const transferEquipment = (equipId, data) => request({
    url: `/equipments/transfer/${equipId}`,
    method: 'post',
    data
})


// 查看某台设备的折旧信息
export const getCalculateAccumulated = (equipId) => request({
    url: `/equipments/calculateAccumulated/${equipId}`,
    method: 'get'
})

// 导出带有折旧信息的设备列表（不带分页参数）
export const getExportEquipments = (params) => {
    return request.get('/equipments/export', { params })
}

// 获取设备详情聚合信息
export const getEquipmentDetail = (equipId) => request({
    url: `/equipments/detail/${equipId}`,
    method: 'get'
})