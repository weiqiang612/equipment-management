import request from '@/utils/request'

/**
 * 获取当前登录角色的聚合数据看板
 * 后端根据 Token 解析出的角色、用户名及所属单位动态裁剪数据
 * @returns {Promise<any>}
 */
export function getDashboardSummary() {
  return request({
    url: '/dashboard/summary',
    method: 'get'
  })
}
