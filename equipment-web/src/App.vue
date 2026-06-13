<template>
  <div id="app">
    <router-view v-slot="{ Component }" v-if="isFullScreen">
      <component :is="Component" />
    </router-view>

    <el-container v-else style="height: 100vh">
      <el-aside width="200px" style="background-color: #304156">
        <el-menu
          router
          :default-active="$route.path"
          background-color="#304156"
          text-color="#fff"
          unique-opened
        >
          <template v-if="role === 0">
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>
            <el-menu-item index="/equipment">
              <i class="el-icon-notebook-2"></i>
              <span slot="title">我的设备</span>
            </el-menu-item>
            <el-menu-item index="/equipment/claim">
              <i class="el-icon-document"></i>
              <span slot="title">领用记录</span>
            </el-menu-item>
            <el-menu-item index="/equipment/maintenance">
              <i class="el-icon-s-tools"></i>
              <span slot="title">报修申请</span>
            </el-menu-item>
            <el-menu-item index="/message-center">
              <i class="el-icon-bell"></i>
              <span slot="title" class="menu-title-container">
                消息中心
                <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="menu-badge" />
              </span>
            </el-menu-item>
          </template>

          <template v-else>
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>
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
              <el-menu-item v-if="role === 2 || role === 3" index="/governance">
                <i class="el-icon-pie-chart"></i>数据治理
              </el-menu-item>
            </el-submenu>

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
            <el-menu-item v-if="role === 2 || role === 3" index="/ai-assistant">
              <i class="el-icon-magic-stick"></i>
              <span>AI 辅助决策</span>
            </el-menu-item>
            <el-menu-item index="/message-center">
              <i class="el-icon-bell"></i>
              <span slot="title" class="menu-title-container">
                消息中心
                <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="menu-badge" />
              </span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="app-header">
          <strong style="font-size: 18px">设备管理系统</strong>

          <div v-if="username" class="user-info">
            <div class="header-bell-wrapper" @click="$router.push('/message-center').catch(() => {})">
              <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="bell-badge">
                <i class="el-icon-bell bell-icon"></i>
              </el-badge>
            </div>
            <el-dropdown trigger="click" @command="handleUserCommand">
              <span class="user-dropdown-trigger">
                <span class="user-name">{{ realName || username }}</span>
                <el-tag :type="getRoleTagType(role)" effect="plain" class="role-tag">
                  {{ formatRole(role) }}
                </el-tag>
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="change-password" icon="el-icon-lock">
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" icon="el-icon-switch-button" divided>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </el-header>
        <el-main style="background-color: #f0f2f5">
          <router-view @refresh-unread="fetchUnreadCount" />
        </el-main>
      </el-container>
    </el-container>

    <el-dialog
      title="修改密码"
      :visible.sync="passwordDialogVisible"
      width="420px"
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordForm"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            clearable
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            clearable
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            clearable
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPasswordChange">
          确认修改
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { changeCurrentPassword } from '@/api/user'
import { getUnreadCount } from '@/api/message'

export default {
  name: 'App',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
      } else if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
      } else {
        callback()
      }
    }

    return {
      role: null,
      realName: '',
      username: '',
      unreadCount: 0,
      pollingTimer: null,
      passwordDialogVisible: false,
      passwordLoading: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度必须在 6 到 20 个字符之间', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度必须在 6 到 20 个字符之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
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
      this.fetchUnreadCount()
    }
  },
  created() {
    this.updateUserInfo()
    this.fetchUnreadCount()
    this.startUnreadPolling()
  },
  beforeDestroy() {
    this.stopUnreadPolling()
  },
  methods: {
    updateUserInfo() {
      const roleStr = localStorage.getItem('role')
      this.role = roleStr !== null ? parseInt(roleStr, 10) : null
      this.realName = localStorage.getItem('realName') || ''
      this.username = localStorage.getItem('username') || ''
    },
    clearAuth() {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('realName')
      localStorage.removeItem('username')
      localStorage.removeItem('unitCode')
    },
    handleLogout() {
      this.$confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.clearAuth()
          this.$message({
            type: 'success',
            message: '已安全退出登录'
          })
          this.$router.push('/login')
        })
        .catch(() => {})
    },
    handleUserCommand(command) {
      if (command === 'change-password') {
        this.openPasswordDialog()
        return
      }
      if (command === 'logout') {
        this.handleLogout()
      }
    },
    openPasswordDialog() {
      this.passwordDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.passwordForm) {
          this.$refs.passwordForm.clearValidate()
        }
      })
    },
    resetPasswordForm() {
      this.passwordLoading = false
      this.passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      if (this.$refs.passwordForm) {
        this.$refs.passwordForm.resetFields()
      }
    },
    submitPasswordChange() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.passwordLoading = true
        changeCurrentPassword({
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword
        })
          .then(() => {
            this.passwordLoading = false
            this.passwordDialogVisible = false
            this.clearAuth()
            this.$message({
              type: 'success',
              message: '密码修改成功，请重新登录'
            })
            this.$router.push('/login')
          })
          .catch(() => {
            this.passwordLoading = false
          })
      })
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
    },
    async fetchUnreadCount() {
      const token = localStorage.getItem('token')
      if (!token) {
        this.unreadCount = 0
        return
      }
      try {
        const count = await getUnreadCount()
        this.unreadCount = typeof count === 'number' ? count : 0
      } catch (err) {
        console.error('Failed to fetch unread count in App.vue:', err)
      }
    },
    startUnreadPolling() {
      this.stopUnreadPolling()
      this.pollingTimer = setInterval(() => {
        this.fetchUnreadCount()
      }, 30000)
    },
    stopUnreadPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    }
  }
}
</script>

<style>
html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>

<style scoped>
.el-header {
  padding: 0 20px;
  background-color: #fff;
  display: flex;
  align-items: center;
}

.el-aside {
  background-color: #304156;
  color: #333;
  height: 100vh;
}

.el-menu {
  border-right: none;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.app-header {
  border-bottom: 1px solid #ddd;
  line-height: 60px;
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.user-info {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
}

.header-bell-wrapper {
  margin-right: 22px;
  cursor: pointer;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.header-bell-wrapper:hover {
  opacity: 0.85;
}

.bell-icon {
  font-size: 20px;
  color: #606266;
}

.menu-title-container {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 120px;
}

.menu-badge {
  margin-left: auto;
}

.user-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  color: #409eff;
  cursor: pointer;
  outline: none;
  padding: 6px 0;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
}

.role-tag {
  margin-left: 10px;
  height: 28px;
  line-height: 26px;
  border-radius: 14px;
  padding: 0 10px;
}

.user-dropdown-trigger:hover .user-name,
.user-dropdown-trigger:hover .el-icon-arrow-down {
  color: #66b1ff;
}
</style>
