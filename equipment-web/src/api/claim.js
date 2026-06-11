import request from '@/utils/request'

// 1. 提交领用申请 (POST /claims/apply)
// data 格式: { equipId: String, remark: String }
export function applyClaim(data) {
  return request({
    url: '/claims/apply',
    method: 'post',
    data
  })
}

// 2. 撤回待审批的申请 (PUT /claims/{claimId}/cancel)
export function cancelClaim(claimId) {
  return request({
    url: `/claims/${claimId}/cancel`,
    method: 'put'
  })
}

// 3. 资产管理员审批申请 (PUT /claims/{claimId}/approve)
// data 格式: { action: Integer, remark: String } (action: 1-同意, 2-拒绝)
export function approveClaim(claimId, data) {
  return request({
    url: `/claims/${claimId}/approve`,
    method: 'put',
    data
  })
}

// 4. 保管人退还设备 (POST /claims/return)
// data 格式: { equipId: String, remark: String }
export function returnEquipment(data) {
  return request({
    url: '/claims/return',
    method: 'post',
    data
  })
}

// 5. 查询领用记录/待审批列表/审计历史记录，支持分页 (GET /claims)
// params 格式: { equipId, status, page, pageSize }
export function getClaims(params) {
  return request({
    url: '/claims',
    method: 'get',
    params
  })
}
