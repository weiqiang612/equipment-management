import request from '@/utils/request'

// 获取所有检修记录
export const getMaintenanceList = () => request.get('/maintenanceRecords')

// 发起设备检修（新增记录并改状态）
export const addMaintenance = (equipId, data) => request.post(`/maintenanceRecords/${equipId}`, data)

// 修改检修记录
export const updateMaintenance = (maintId, data) => request.put(`/maintenanceRecords/${maintId}`, data)

// 删除待指派检修记录（撤销误报并恢复状态）
export const deleteMaintenance = (maintId, equipId) => request.delete(`/maintenanceRecords/${maintId}`, {
    params: {
        equipId
    }
})

// 维保指派
export const assignMaintenance = (maintId, data) => request.put(`/maintenanceRecords/assign/${maintId}`, data)

// 完工登记
export const completeMaintenance = (maintId, data) => request.put(`/maintenanceRecords/complete/${maintId}`, data)

// 完工复核
export const reviewMaintenance = (maintId, data) => request.put(`/maintenanceRecords/review/${maintId}`, data)
