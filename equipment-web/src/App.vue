<template>
  <div id="app">
    <router-view v-slot="{ Component }" v-if="isFullScreen">
      <component :is="Component" />
    </router-view>

    <el-container v-else style="height: 100vh">
      <el-aside width="232px" class="app-aside">
        <div class="app-brand">
          <div class="app-brand-text">
            <strong>设备管理系统</strong>
          </div>
        </div>
        <el-menu
          router
          :default-active="$route.path"
          class="app-menu"
          background-color="#304156"
          text-color="#fff"
          active-text-color="#ffffff"
          unique-opened
        >
          <template v-if="role === 0">
            <div class="menu-section-label">工作台</div>
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>
            <el-menu-item index="/message-center" class="message-menu-item">
              <i class="el-icon-bell"></i>
              <span slot="title">消息中心</span>
              <span v-if="unreadCount > 0" class="app-notification-badge menu-item-badge">
                {{ formatUnreadCount(unreadCount) }}
              </span>
            </el-menu-item>
            <div class="menu-section-label">我的处理</div>
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
              <span slot="title">报修记录</span>
            </el-menu-item>
          </template>

          <template v-else>
            <div class="menu-section-label">工作台</div>
            <el-menu-item index="/dashboard">
              <i class="el-icon-data-line"></i>
              <span slot="title">数据看板</span>
            </el-menu-item>
            <el-menu-item index="/message-center" class="message-menu-item">
              <i class="el-icon-bell"></i>
              <span slot="title">消息中心</span>
              <span v-if="unreadCount > 0" class="app-notification-badge menu-item-badge">
                {{ formatUnreadCount(unreadCount) }}
              </span>
            </el-menu-item>
            <div class="menu-section-label">资产执行</div>
            <el-submenu index="workbench-assets">
              <template slot="title">
                <i class="el-icon-monitor"></i>
                <span>资产与流程</span>
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

            <div v-if="role === 2 || role === 3" class="menu-section-label">基础资料</div>
            <el-menu-item v-if="role === 2 || role === 3" index="/category">
              <i class="el-icon-menu"></i> <span>分类档案</span>
            </el-menu-item>
            <el-menu-item v-if="role === 2 || role === 3" index="/department">
              <i class="el-icon-office-building"></i> <span>单位档案</span>
            </el-menu-item>
            <el-menu-item v-if="role === 2 || role === 3" index="/ai-assistant">
              <i class="el-icon-magic-stick"></i>
              <span>AI 建议草案</span>
            </el-menu-item>
            <div v-if="role === 3" class="menu-section-label">系统治理</div>
            <el-menu-item v-if="role === 3" index="/user-manage">
              <i class="el-icon-user-solid"></i>
              <span>用户与权限</span>
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
        <el-header class="app-header">
          <div class="header-page-meta">
            <strong class="header-page-title">{{ currentPageMeta.title }}</strong>
            <span class="header-page-subtitle">{{ currentPageMeta.subtitle }}</span>
          </div>

          <div v-if="username" class="user-info">
            <div class="header-bell-wrapper" @click="goToMessageCenter">
              <div class="bell-icon-container">
                <i class="el-icon-bell bell-icon"></i>
                <span
                  v-if="unreadCount > 0"
                  class="app-notification-badge app-notification-badge--floating"
                >
                  {{ formatUnreadCount(unreadCount) }}
                </span>
              </div>
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

const PAGE_META = {
  '/dashboard': {
    title: '数据看板',
    subtitle: '查看当前角色的待办、风险和资产运营概览'
  },
  '/message-center': {
    title: '消息中心',
    subtitle: '从通知进入待办处理链路，统一跟进审批、检修和风险事项'
  },
  '/equipment': {
    title: '设备台账',
    subtitle: '管理设备基础档案、生命周期状态和使用归属'
  },
  '/equipment/claim': {
    title: '领用审批',
    subtitle: '处理设备领用申请，并回看历史审批与在途记录'
  },
  '/equipment/maintenance': {
    title: '检修记录',
    subtitle: '聚焦待指派、待完工和待复核工单，保持首屏可处理'
  },
  '/equipment/transfer': {
    title: '调拨记录',
    subtitle: '跟踪资产跨单位流转与交接过程'
  },
  '/equipment/scrap': {
    title: '报废记录',
    subtitle: '维护设备退出使用链路与处置记录'
  },
  '/governance': {
    title: '数据治理',
    subtitle: '识别高风险资产、数据质量问题与运营异常'
  },
  '/category': {
    title: '分类档案',
    subtitle: '维护设备分类、折旧年限和残值率基线'
  },
  '/department': {
    title: '单位档案',
    subtitle: '维护组织结构与设备归属边界'
  },
  '/ai-assistant': {
    title: 'AI 建议草案',
    subtitle: '生成报告和摘要草案，仅作解释与建议，不自动执行'
  },
  '/user-manage': {
    title: '用户与权限',
    subtitle: '维护账号、角色与单位归属，保持权限边界清晰'
  },
  '/system/backup': {
    title: '备份与恢复',
    subtitle: '查看备份状态并管理系统恢复入口'
  },
  '/system/log': {
    title: '操作审计',
    subtitle: '审查关键操作日志与系统治理记录'
  }
}

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
    },
    currentPageMeta() {
      const path = this.$route.path
      if (PAGE_META[path]) {
        return PAGE_META[path]
      }
      if (path.indexOf('/equipment/detail/') === 0) {
        return {
          title: '设备生命周期详情',
          subtitle: '查看资产全链路记录、审计轨迹和 AI 建议草案'
        }
      }
      return {
        title: '设备管理系统',
        subtitle: '统一管理设备台账、流程审批、风险治理与系统运维'
      }
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
    goToMessageCenter() {
      this.$router.push('/message-center').catch(() => {})
    },
    formatUnreadCount(count) {
      if (typeof count !== 'number' || count <= 0) {
        return ''
      }
      return count > 99 ? '99+' : String(count)
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
.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.app-aside {
  background: #304156;
  color: #dce6f2;
  height: 100vh;
  overflow-y: auto;
}

.app-brand {
  padding: 14px 18px 12px;
  background: transparent;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-brand-text {
  display: block;
}

.app-brand-text strong {
  display: block;
  color: rgba(255, 255, 255, 0.92);
  font-size: 16px;
  line-height: 1.2;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.app-menu {
  border-right: none;
  padding: 10px 10px 18px;
}

.menu-section-label {
  padding: 14px 12px 8px;
  color: rgba(220, 230, 242, 0.58);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.app-menu /deep/ .el-menu-item,
.app-menu /deep/ .el-submenu__title {
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  margin-bottom: 6px;
}

.app-menu /deep/ .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.42) 0%, rgba(64, 158, 255, 0.22) 100%) !important;
}

.app-menu /deep/ .el-submenu .el-menu-item {
  min-width: auto;
}

.app-menu /deep/ .el-submenu .el-menu {
  background-color: transparent !important;
}

.app-header {
  border-bottom: 1px solid #e8edf3;
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  min-height: 72px;
}

.header-page-meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.header-page-title {
  font-size: 20px;
  color: #1f2d3d;
  line-height: 1.2;
}

.header-page-subtitle {
  color: #7a8797;
  font-size: 12px;
  line-height: 1.4;
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
  width: 40px;
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

.bell-icon-container {
  position: relative;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.message-menu-item {
  position: relative;
  padding-right: 52px !important;
}

.app-notification-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  margin-left: 12px;
  border-radius: 10px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  box-sizing: border-box;
  border: 2px solid #304156;
  flex-shrink: 0;
}

.menu-item-badge {
  position: absolute;
  top: 50%;
  right: 18px;
  transform: translateY(-50%);
  margin-left: 0;
}

.app-notification-badge--floating {
  position: absolute;
  top: -8px;
  right: -14px;
  margin-left: auto;
  border-color: #fff;
  min-width: 22px;
  height: 22px;
  line-height: 18px;
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

@media (max-width: 1280px) {
  .app-aside {
    width: 208px !important;
  }

  .app-header {
    padding: 0 18px;
  }

  .header-page-subtitle {
    max-width: 360px;
  }
}
</style>
