import request from '@/utils/request'

// 分页查询操作审计日志
export const getAuditLogs = (params) => request({
    url: '/system/log/list',
    method: 'get',
    params
})
