import request from '@/utils/request'

/**
 * 获取所有报废记录
 * 对应后端: GET /scrapRecords
 */
export const getScrapList = () => request({
    url: '/scrapRecords',
    method: 'get'
})

/**
 * 修改报废信息
 * 对应后端: PUT /scrapRecords/{scrapNo}
 */
export const updateScrapRecord = (scrapNo, data) => request({
    url: `/scrapRecords/${scrapNo}`,
    method: 'put',
    data
})

/**
 * 撤销报废（删除记录并恢复设备状态）
 * 对应后端: DELETE /scrapRecords/{scrapNo}
 * 需要传递 equipId 作为请求参数来配合后端事务
 */
export const deleteScrapRecord = (scrapNo, equipId) => request({
    url: `/scrapRecords/${scrapNo}`,
    method: 'delete',
    params: {
        equipId
    }
})