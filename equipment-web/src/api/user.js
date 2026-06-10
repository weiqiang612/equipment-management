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

// 4. 修改用户角色 (PUT /users/role)
export function updateUserRole(data) {
  return request({
    url: '/users/role',
    method: 'put',
    data
  })
}

// 5. 获取所有维修工程师列表 (GET /users/maintainers)
export function getMaintainers() {
  return request({
    url: '/users/maintainers',
    method: 'get'
  })
}
