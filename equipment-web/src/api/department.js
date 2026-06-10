import request from '@/utils/request'

// 获取所有部门
export function getDepts() {
    return request({
        url: '/departments',
        method: 'get'
    })
}

// 新增部门
export function addDept(data) {
    return request({
        url: '/departments',
        method: 'post',
        data
    })
}

// 修改部门
export function updateDept(data) {
    return request({
        url: `/departments/${data.unitCode}`,
        method: 'put',
        data
    })
}

// 删除部门
export function deleteDept(unitCode) {
    return request({
        url: `/departments/${unitCode}`,
        method: 'delete'
    })
}