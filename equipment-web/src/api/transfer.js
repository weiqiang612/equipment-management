import request from '@/utils/request'

// 获取所有调拨记录
export const getTransferRecords = () => request({
    url: '/transferRecords',
    method: 'get'
})

// 根据 ID 获取单条记录
export const getTransferById = (transferId) => request({
    url: `/transferRecords/${transferId}`,
    method: 'get'
})

// 提交调拨申请
export const addTransfer = (equipId, data) => request({
    url: `/transferRecords/${equipId}`,
    method: 'post',
    data
})

// 修改调拨信息（备注/经办人等）
export const updateTransfer = (transferId, data) => request({
    url: `/transferRecords/${transferId}`,
    method: 'put',
    data
})

// 删除/撤销调拨（触发后端回滚逻辑）
export const deleteTransfer = (transferId) => request({
    url: `/transferRecords/${transferId}`,
    method: 'delete'
})