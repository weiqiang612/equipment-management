import request from '@/utils/request'

// 获取所有检修记录
export const getMaintenanceList = () => request.get('/maintenanceRecords')

// 发起设备检修（新增记录并改状态）
export const addMaintenance = (equipId, data) => request.post(`/maintenanceRecords/${equipId}`, data)

// 修改检修记录
export const updateMaintenance = (maintId, data) => request.put(`/maintenanceRecords/${maintId}`, data)

// 删除检修记录（并恢复状态）
export const deleteMaintenance = (maintId, equipId) => request.delete(`/maintenanceRecords/${maintId}`, {
    params: {
        equipId
    }
})