<template>
  <div id="app">
    <!-- 全屏路由页面 (登录、注册、403) 直接渲染 -->
    <router-view v-slot="{ Component }" v-if="isFullScreen">
      <component :is="Component" />
    </router-view>

    <!-- 带侧边栏与头部的后台常规布局 -->
    <el-container v-else style="height: 100vh">
      <el-aside width="200px" style="background-color: #304156">
        <el-menu
          router
          :default-active="$route.path"
          background-color="#304156"
          text-color="#fff"
          unique-opened
        >
          <!-- 1. 设备操作员菜单 (role === 0) -->
          <template v-if="role === 0">
            <el-menu-item index="/equipment">
              <i class="el-icon-notebook-2"></i>
              <span slot="title">我的设备</span>
            </el-menu-item>
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>
            <el-menu-item index="/equipment/claim">
              <i class="el-icon-document"></i>
              <span slot="title">领用记录</span>
            </el-menu-item>
            <el-menu-item index="/equipment/maintenance">
              <i class="el-icon-s-tools"></i>
              <span slot="title">报修申请</span>
            </el-menu-item>
          </template>

          <!-- 2. 非操作员菜单 (role === 1, 2, 3) -->
          <template v-else>
            <el-submenu index="1">
              <template slot="title">
                <i class="el-icon-monitor"></i>
                <span>设备资产管理</span>
              </template>
              <el-menu-item index="/equipment">
                <i class="el-icon-notebook-2"></i>设备台账
              </el-menu-item>
              <el-menu-item v-if="role === 2 || role === 3" index="/equipment/claim">
                <i class="el-icon-document-copy"></i>领用审批
              </el-menu-item>
              <el-menu-item v-if="role === 2 || role === 3" index="/equipment/transfer">
                <i class="el-icon-refresh"></i>调拨记录
              </el-menu-item>
              <el-menu-item index="/equipment/maintenance">
                <i class="el-icon-s-tools"></i>检修记录
              </el-menu-item>
              <el-menu-item v-if="role === 2 || role === 3" index="/equipment/scrap">
                <i class="el-icon-delete"></i>报废记录
              </el-menu-item>
            </el-submenu>
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>

            <el-menu-item v-if="role === 2 || role === 3" index="/category">
              <i class="el-icon-menu"></i> <span>分类管理</span>
            </el-menu-item>
            <el-menu-item v-if="role === 2 || role === 3" index="/department">
              <i class="el-icon-office-building"></i> <span>单位管理</span>
            </el-menu-item>
            <el-menu-item v-if="role === 3" index="/user-manage">
              <i class="el-icon-user-solid"></i>
              <span>用户权限管理</span>
            </el-menu-item>
            <el-menu-item v-if="role === 3" index="/system/backup">
              <i class="el-icon-receiving"></i>
              <span>备份与恢复</span>
            </el-menu-item>
            <el-menu-item v-if="role === 3" index="/system/log">
              <i class="el-icon-document"></i>
              <span>操作审计</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header
          style="
            border-bottom: 1px solid #ddd;
            line-height: 60px;
            background-color: #fff;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 20px;
          "
        >
          <strong style="font-size: 18px">设备管理系统</strong>
          
          <!-- 当前登录用户信息及登出按钮 -->
          <div v-if="username" class="user-info">
            <span class="user-name">
              欢迎您，{{ realName || username }}
              <el-tag size="mini" :type="getRoleTagType(role)" style="margin-left: 5px">
                {{ formatRole(role) }}
              </el-tag>
            </span>
            <el-button
              type="text"
              icon="el-icon-switch-button"
              style="margin-left: 20px; color: #f56c6c; font-weight: bold;"
              @click="handleLogout"
            >
              退出登录
            </el-button>
          </div>
        </el-header>
        <el-main style="background-color: #f0f2f5">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      role: null,
      realName: '',
      username: ''
    }
  },
  computed: {
    isFullScreen() {
      const fullScreenPaths = ['/login', '/register', '/403']
      return fullScreenPaths.includes(this.$route.path)
    }
  },
  watch: {
    $route() {
      this.updateUserInfo()
    }
  },
  created() {
    this.updateUserInfo()
  },
  methods: {
    updateUserInfo() {
      const roleStr = localStorage.getItem('role')
      this.role = roleStr !== null ? parseInt(roleStr, 10) : null
      this.realName = localStorage.getItem('realName') || ''
      this.username = localStorage.getItem('username') || ''
    },
    handleLogout() {
      this.$confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          localStorage.removeItem('token')
          localStorage.removeItem('role')
          localStorage.removeItem('realName')
          localStorage.removeItem('username')
          this.$message({
            type: 'success',
            message: '已安全退出登录'
          })
          this.$router.push('/login')
        })
        .catch(() => {})
    },
    formatRole(role) {
      const roleMap = {
        0: '操作员',
        1: '维修工',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : ''
    },
    getRoleTagType(role) {
      const tagMap = {
        0: 'info',
        1: 'warning',
        2: 'primary',
        3: 'success'
      }
      return tagMap[role] || ''
    }
  }
}
</script>

<style>
/* 1. 彻底清除浏览器默认的外边距和内边距 */
html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%; /* 确保高度铺满 */
}

/* 2. 移除 el-header 左右默认的 20px 内边距（如果需要紧贴的话） */
.el-header {
  padding: 0 20px;
  background-color: #fff;
  display: flex;
  align-items: center; /* 让文字垂直居中 */
}

/* 3. 确保侧边栏高度撑满，且没有边框缝隙 */
.el-aside {
  background-color: #304156;
  color: #333;
  height: 100vh; /* 视口高度 */
}

.el-menu {
  border-right: none; /* 去掉菜单右侧自带的 1px 细线缝隙 */
}

/* 4. 优化主区域背景 */
.el-main {
  background-color: #f0f2f5;
  padding: 20px; /* 内部内容留白 */
}

.user-info {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
}

.user-name {
  display: flex;
  align-items: center;
}
</style>
