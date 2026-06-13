import request from '@/utils/request'

/**
 * 获取消息列表
 * @param {Object} params { page, pageSize, status }
 */
export function getMessages(params) {
  return request({
    url: '/messages',
    method: 'get',
    params
  })
}

/**
 * 获取未读数量
 */
export function getUnreadCount() {
  return request({
    url: '/messages/unread-count',
    method: 'get'
  })
}

/**
 * 标记单条消息为已读
 * @param {number|string} id 消息ID
 */
export function readMessage(id) {
  return request({
    url: `/messages/${id}/read`,
    method: 'put'
  })
}

/**
 * 标记所有消息为已读
 */
export function readAllMessages() {
  return request({
    url: '/messages/read-all',
    method: 'put'
  })
}
