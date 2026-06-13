import request from '@/utils/request'

/**
 * 生成资产运营报告草案
 * @param {Object} data 包含 period (weekly/monthly)
 */
export function draftOperationReport(data) {
  return request({
    url: '/ai/reports/operations/draft',
    method: 'post',
    timeout: 60000,
    data
  })
}

/**
 * 生成单台设备生命周期摘要与处置建议
 * @param {String} equipId 设备编号
 */
export function getEquipmentAiSummary(equipId) {
  return request({
    url: `/ai/equipment/${equipId}/summary`,
    method: 'post',
    timeout: 60000
  })
}
