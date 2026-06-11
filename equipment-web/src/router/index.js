import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
    {
        path: '/',
        redirect: '/equipment'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/UserLogin.vue'),
        meta: {
            title: '登录'
        }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/UserRegister.vue'),
        meta: {
            title: '注册'
        }
    },
    {
        path: '/403',
        name: 'Unauthorized',
        component: () => import('../views/Unauthorized403.vue'),
        meta: {
            title: '无权限访问'
        }
    },
    {
        path: '/equipment',
        name: 'Equipment',
        component: () => import('../views/Equipment.vue'),
        meta: {
            title: '设备台账',
            requiresAuth: true
        }
    },
    {
        path: '/system/backup',
        name: 'Backup',
        component: () => import('../views/DataBackup.vue'),
        meta: {
            title: '备份恢复',
            requiresAuth: true,
            roles: [3]
        }
    },
    {
        path: '/equipment/maintenance',
        name: 'Maintenance',
        component: () => import('../views/MaintenanceRecord.vue'),
        meta: {
            title: '设备检修记录',
            requiresAuth: true
        }
    },
    {
        path: '/equipment/claim',
        name: 'EquipmentClaim',
        component: () => import('../views/EquipmentClaim.vue'),
        meta: {
            title: '设备领用与审批',
            requiresAuth: true,
            roles: [0, 2, 3]
        }
    },
    {
        path: '/equipment/transfer',
        name: 'Transfer',
        component: () => import('../views/TransferRecord.vue'),
        meta: {
            title: '设备调拨记录',
            requiresAuth: true,
            roles: [2, 3]
        }
    },
    {
        path: '/equipment/scrap',
        name: 'Scrap',
        component: () => import('../views/ScrapRecord.vue'),
        meta: {
            title: '设备报废记录',
            requiresAuth: true,
            roles: [2, 3]
        }
    },
    {
        path: '/category',
        name: 'Category',
        component: () => import('../views/Category.vue'),
        meta: {
            requiresAuth: true,
            roles: [2, 3]
        }
    },
    {
        path: '/department',
        name: 'Department',
        component: () => import('../views/Department.vue'),
        meta: {
            requiresAuth: true,
            roles: [2, 3]
        }
    },
    {
        path: '/user-manage',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: {
            title: '用户权限管理',
            requiresAuth: true,
            roles: [3]
        }
    }
]

const router = new VueRouter({
    routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
    // 动态修改文档标题
    if (to.meta && to.meta.title) {
        document.title = to.meta.title + ' - 设备管理系统'
    }

    const token = localStorage.getItem('token')
    const whiteList = ['/login', '/register', '/403']

    // 1. 如果在白名单内，直接放行
    if (whiteList.includes(to.path)) {
        return next()
    }

    // 2. 如果没有 token，拦截重定向到登录页
    if (!token) {
        return next('/login')
    }

    // 3. 登录后进行角色鉴权
    if (to.meta && to.meta.roles) {
        const userRoleStr = localStorage.getItem('role')
        const userRole = userRoleStr !== null ? parseInt(userRoleStr, 10) : null
        
        // 检查路由配置的角色列表中是否包含当前用户的角色
        if (userRole === null || !to.meta.roles.includes(userRole)) {
            // 无权访问，重定向到 403 页面
            return next('/403')
        }
    }

    // 放行
    next()
})

export default router
