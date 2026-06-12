import request from '@/utils/request'

/**
 * 1. 获取数据治理与运营风险总览
 * 权限控制: Role 2, 3
 */
export function getGovernanceSummary() {
  return request({
    url: '/governance/summary',
    method: 'get'
  })
}

/**
 * 2. 分页查询风险设备清单
 * 权限控制: Role 2, 3
 * @param {Object} params 包含 riskLevel, unitCode, categoryId, page, pageSize
 */
export function getEquipmentRisks(params) {
  return request({
    url: '/governance/equipment-risks',
    method: 'get',
    params
  })
}
