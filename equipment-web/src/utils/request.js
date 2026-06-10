import axios from 'axios'
import {
    Message
} from 'element-ui'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
    // baseURL 会根据环境变量自动切换，开发环境通常是 '/api'
    baseURL: process.env.VUE_APP_BASE_API,
    timeout: 10000 // 请求超时时间
})

// 1. 请求拦截器
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['token'] = token
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        console.log(error)
        return Promise.reject(error)
    }
)

// 2. 响应拦截器
service.interceptors.response.use(
    response => {
        // res 就是你后端返回的 Result 对象
        const res = response.data

        // 根据你后端的约定：code 为 1 表示成功
        if (res.code === 1) {
            // 成功时直接返回 data 字段。如果 data 为空（如增删改），则返回空
            return res.data
        } else {
            // 业务失败：弹出后端传来的错误描述 (res.msg)
            Message({
                message: res.msg || 'Error',
                type: 'error',
                duration: 5 * 1000
            })
            return new Promise(() => {})
        }
    },
    error => {
        // 接口层面的错误（如 404, 500, 网络断开）
        console.log('err' + error)
        let message = '后端接口连接异常'

        // 截获 401 错误清空 LocalStorage 并跳转登录页
        if (error.response && error.response.status === 401) {
            message = '登录已失效，请重新登录'
            localStorage.removeItem('token')
            localStorage.removeItem('role')
            localStorage.removeItem('realName')
            localStorage.removeItem('username')

            Message({
                message: message,
                type: 'error',
                duration: 5 * 1000
            })

            if (router.currentRoute.path !== '/login') {
                router.push('/login')
            }
            return new Promise(() => {})
        }

        // 如果后端返回了 JSON 格式 of 错误信息 (即使状态码是 500)
        if (error.response && error.response.data) {
            message = error.response.data.msg || '服务器内部错误'
        } else if (error.message.includes('timeout')) {
            message = '请求超时'
        }

        Message({
            message: message,
            type: 'error',
            duration: 5 * 1000
        })
        return new Promise(() => {})
    }
)

export default service