import request from '@/utils/request'

// 1. 登录 (POST /users/login)
export function login(data) {
  return request({
    url: '/users/login',
    method: 'post',
    data
  })
}

// 2. 注册 (POST /users/register)
export function register(data) {
  return request({
    url: '/users/register',
    method: 'post',
    data
  })
}

// 3. 获取所有用户列表 (GET /users)
export function getUsers() {
  return request({
    url: '/users',
    method: 'get'
  })
}

// 4. 兼容旧的角色单位修改接口 (PUT /users/role)
export function updateUserRole(data) {
  return request({
    url: '/users/role',
    method: 'put',
    data
  })
}

// 5. 统一更新用户资料 (PUT /users/{id})
export function updateUserProfile(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

// 6. 管理员重置用户密码 (PUT /users/{id}/password/reset)
export function resetUserPassword(id, data) {
  return request({
    url: `/users/${id}/password/reset`,
    method: 'put',
    data
  })
}

// 7. 当前登录用户修改本人密码 (PUT /users/password)
export function changeCurrentPassword(data) {
  return request({
    url: '/users/password',
    method: 'put',
    data
  })
}

// 8. 删除用户 (DELETE /users/{id})
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

// 9. 获取所有维修工程师列表 (GET /users/maintainers)
export function getMaintainers() {
  return request({
    url: '/users/maintainers',
    method: 'get'
  })
}
