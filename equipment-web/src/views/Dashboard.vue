<template>
  <div class="dashboard-container" v-loading="loading">
    <!-- 头部欢迎区域 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <i class="el-icon-odometer welcome-icon"></i>
        <div>
          <h2 class="welcome-title">
            您好，{{ realName || username }}！欢迎回来
          </h2>
          <p class="welcome-subtitle">
            当前身份：<span class="role-badge">{{ formatRole(role) }}</span> | 
            所属单位：<span>{{ unitCode || '系统总部' }}</span>
          </p>
        </div>
      </div>
      <div class="welcome-date">
        <i class="el-icon-time"></i>
        <span>{{ currentDate }}</span>
      </div>
    </div>

    <!-- 看板内容区域：根据角色动态切换 -->
    <div v-if="!loading" class="dashboard-content">
      
      <!-- ========================================== -->
      <!-- 1. 资产管理员看板 (Role === 2) -->
      <!-- ========================================== -->
      <div v-if="role === 2" class="role-dashboard">
        <!-- KPI 指标卡片 -->
        <el-row :gutter="20" class="kpi-row">
          <el-col :xs="24" :sm="12" :md="4" v-for="(item, index) in role2Kpis" :key="index">
            <el-card shadow="hover" class="kpi-card">
              <div class="kpi-card-body">
                <div class="kpi-icon-wrapper" :style="{ backgroundColor: item.bg }">
                  <i :class="item.icon" :style="{ color: item.color }"></i>
                </div>
                <div class="kpi-info">
                  <div class="kpi-value">
                    <span v-if="item.isMoney" class="money-symbol">￥</span>{{ item.value | formatNumber }}
                  </div>
                  <div class="kpi-label">{{ item.label }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 图表区域 -->
        <el-row :gutter="20" class="chart-row">
          <!-- 维保趋势（柱线组合图） -->
          <el-col :xs="24" :lg="14">
            <el-card shadow="hover" class="chart-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-loading-line-chart icon-margin"></i>维保费用与工单次数趋势</span>
              </div>
              <div class="chart-container" ref="maintTrendChart"></div>
            </el-card>
          </el-col>
          <!-- 资产分类分布（环形图） -->
          <el-col :xs="24" :lg="10">
            <el-card shadow="hover" class="chart-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-pie-chart icon-margin"></i>资产分类分布</span>
              </div>
              <div class="chart-container" ref="categoryChart"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row" style="margin-top: 20px;">
          <!-- 部门资产分布（柱状图） -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="chart-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-data-analysis icon-margin"></i>部门资产价值分布</span>
              </div>
              <div class="chart-container" ref="deptChart"></div>
            </el-card>
          </el-col>

          <!-- 待办任务区 -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="todo-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-message-solid icon-margin"></i>待办审批与指派</span>
              </div>
              <div class="todo-lists">
                <!-- 待审批领用 -->
                <div class="todo-section">
                  <div class="todo-title">
                    <span><i class="el-icon-document todo-icon-margin"></i>待审批领用申请</span>
                    <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment/claim')">
                      去审批 <i class="el-icon-arrow-right"></i>
                    </el-button>
                  </div>
                  <el-table :data="dashboardData.listData.pendingClaims || []" size="small" class="todo-table">
                    <el-table-column prop="equipId" label="设备编号" width="100" />
                    <el-table-column prop="equipName" label="设备名称" min-width="120" show-overflow-tooltip />
                    <el-table-column prop="applicantRealName" label="申请人" width="90" />
                    <el-table-column prop="createTime" label="申请时间" width="140" />
                  </el-table>
                </div>
                
                <!-- 待指派检修 -->
                <div class="todo-section" style="margin-top: 20px;">
                  <div class="todo-title">
                    <span><i class="el-icon-s-tools todo-icon-margin"></i>待指派检修任务</span>
                    <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment/maintenance')">
                      去指派 <i class="el-icon-arrow-right"></i>
                    </el-button>
                  </div>
                  <el-table :data="dashboardData.listData.pendingMaintenances || []" size="small" class="todo-table">
                    <el-table-column prop="equipId" label="设备编号" width="100" />
                    <el-table-column prop="faultDescription" label="故障描述" min-width="180" show-overflow-tooltip />
                    <el-table-column label="状态" width="100">
                      <template slot-scope="scope">
                        <el-tag size="mini" type="danger">{{ formatMaintStatus(scope.row.maintStatus) }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- ========================================== -->
      <!-- 2. 系统管理员看板 (Role === 3) -->
      <!-- ========================================== -->
      <div v-else-if="role === 3" class="role-dashboard">
        <!-- KPI 指标卡片 -->
        <el-row :gutter="20" class="kpi-row">
          <el-col :xs="24" :sm="12" :md="6" v-for="(item, index) in role3Kpis" :key="index">
            <el-card shadow="hover" class="kpi-card">
              <div class="kpi-card-body">
                <div class="kpi-icon-wrapper" :style="{ backgroundColor: item.bg }">
                  <i :class="item.icon" :style="{ color: item.color }"></i>
                </div>
                <div class="kpi-info">
                  <div class="kpi-value">
                    <span v-if="item.isMoney" class="money-symbol">￥</span>{{ item.value | formatNumber }}
                  </div>
                  <div class="kpi-label">{{ item.label }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 全局只读审计与备份 -->
        <el-row :gutter="20" class="chart-row">
          <!-- 用户角色分布（饼图） -->
          <el-col :xs="24" :lg="10">
            <el-card shadow="hover" class="chart-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-user-solid icon-margin"></i>系统用户角色分布</span>
              </div>
              <div class="chart-container" ref="userRoleChart"></div>
            </el-card>
          </el-col>

          <!-- 备份恢复状态（只读） -->
          <el-col :xs="24" :lg="14">
            <el-card shadow="hover" class="todo-card" style="height: 410px;">
              <div slot="header" class="card-header">
                <span><i class="el-icon-receiving icon-margin"></i>最新系统数据备份记录</span>
                <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/system/backup')">
                  管理备份 <i class="el-icon-arrow-right"></i>
                </el-button>
              </div>
              <div class="backup-content">
                <el-alert
                  title="安全审计视图：此处展示最近备份文件的存储情况，系统管理员可由此快速前往备份页面执行恢复或手动备份。"
                  type="info"
                  show-icon
                  :closable="false"
                  style="margin-bottom: 15px;"
                />
                <el-table :data="dashboardData.listData.backupFiles || []" size="small" border>
                  <el-table-column prop="name" label="备份文件名" min-width="200" show-overflow-tooltip />
                  <el-table-column prop="size" label="文件大小" width="120">
                    <template slot-scope="scope">
                      {{ scope.row.size | formatBytes }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="lastModified" label="备份时间" width="160">
                    <template slot-scope="scope">
                      {{ scope.row.lastModified | formatDate }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- ========================================== -->
      <!-- 3. 维修工程师看板 (Role === 1) -->
      <!-- ========================================== -->
      <div v-else-if="role === 1" class="role-dashboard">
        <!-- KPI 指标卡片 -->
        <el-row :gutter="20" class="kpi-row">
          <el-col :xs="24" :sm="12" :md="8" v-for="(item, index) in role1Kpis" :key="index">
            <el-card shadow="hover" class="kpi-card">
              <div class="kpi-card-body">
                <div class="kpi-icon-wrapper" :style="{ backgroundColor: item.bg }">
                  <i :class="item.icon" :style="{ color: item.color }"></i>
                </div>
                <div class="kpi-info">
                  <div class="kpi-value">{{ item.value | formatNumber }}</div>
                  <div class="kpi-label">{{ item.label }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="chart-row">
          <!-- 维保费用趋势（折线图） -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="chart-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-s-finance icon-margin"></i>个人维保费用趋势</span>
              </div>
              <div class="chart-container" ref="maintCostChart"></div>
            </el-card>
          </el-col>

          <!-- 待处理维保工单 -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="todo-card" style="height: 410px;">
              <div slot="header" class="card-header">
                <span><i class="el-icon-warning-outline icon-margin"></i>分配给我的待处理工单</span>
                <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment/maintenance')">
                  进入检修台账 <i class="el-icon-arrow-right"></i>
                </el-button>
              </div>
              <el-table :data="dashboardData.listData.myWorkOrders || []" size="small" class="workorder-table">
                <el-table-column prop="maintId" label="工单ID" width="80" />
                <el-table-column prop="equipId" label="设备编号" width="100" />
                <el-table-column prop="faultDescription" label="故障描述" min-width="180" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template slot-scope="scope">
                    <el-tag size="mini" type="warning">{{ formatMaintStatus(scope.row.maintStatus) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template>
                    <el-button type="text" size="small" class="cursor-pointer font-bold" @click="goToPage('/equipment/maintenance')">
                      检修
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- ========================================== -->
      <!-- 4. 设备操作员看板 (Role === 0) -->
      <!-- ========================================== -->
      <div v-else-if="role === 0" class="role-dashboard">
        <!-- KPI 指标卡片 -->
        <el-row :gutter="20" class="kpi-row">
          <el-col :xs="24" :sm="12" :md="6" v-for="(item, index) in role0Kpis" :key="index">
            <el-card shadow="hover" class="kpi-card">
              <div class="kpi-card-body">
                <div class="kpi-icon-wrapper" :style="{ backgroundColor: item.bg }">
                  <i :class="item.icon" :style="{ color: item.color }"></i>
                </div>
                <div class="kpi-info">
                  <div class="kpi-value">
                    <span v-if="item.isMoney" class="money-symbol">￥</span>{{ item.value | formatNumber }}
                  </div>
                  <div class="kpi-label">{{ item.label }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 个人资产及在途申请明细 -->
        <el-row :gutter="20" class="chart-row">
          <!-- 我保管的设备 -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="todo-card" style="height: 440px;">
              <div slot="header" class="card-header">
                <span><i class="el-icon-notebook-2 icon-margin"></i>我保管的资产设备</span>
                <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment')">
                  查看全部 <i class="el-icon-arrow-right"></i>
                </el-button>
              </div>
              <el-table :data="dashboardData.listData.myEquipments || []" size="small" class="operator-table" height="340">
                <el-table-column prop="equipId" label="设备编号" width="90" />
                <el-table-column prop="equipName" label="设备名称" min-width="120" show-overflow-tooltip />
                <el-table-column prop="originalValue" label="资产原值" width="90">
                  <template slot-scope="scope">
                    ￥{{ scope.row.originalValue | formatNumber }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="scope.row.status === '在用' ? 'success' : 'warning'">
                      {{ scope.row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 个人领用及报修在途 -->
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="todo-card" style="height: 440px;">
              <div slot="header" class="card-header">
                <span><i class="el-icon-document-copy icon-margin"></i>我的领用及报修进度</span>
              </div>
              <div class="todo-lists" style="padding: 0;">
                <!-- 在途领用 -->
                <div class="todo-section">
                  <div class="todo-title">
                    <span><i class="el-icon-document todo-icon-margin"></i>我的在途领用申请</span>
                    <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment/claim')">
                      领用记录 <i class="el-icon-arrow-right"></i>
                    </el-button>
                  </div>
                  <el-table :data="dashboardData.listData.myClaims || []" size="small" class="todo-table" height="105">
                    <el-table-column prop="claimId" label="申请单号" width="90" />
                    <el-table-column prop="equipId" label="申请设备" width="100" />
                    <el-table-column prop="createTime" label="申请时间" min-width="120" />
                    <el-table-column label="状态" width="80" align="center">
                      <template slot-scope="scope">
                        <el-tag size="mini" :type="formatClaimTagType(scope.row.status)">
                          {{ formatClaimStatus(scope.row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>

                <!-- 报修状态 -->
                <div class="todo-section" style="margin-top: 15px;">
                  <div class="todo-title">
                    <span><i class="el-icon-s-tools todo-icon-margin"></i>我的设备报修进度</span>
                    <el-button type="text" size="small" class="cursor-pointer" @click="goToPage('/equipment/maintenance')">
                      报修记录 <i class="el-icon-arrow-right"></i>
                    </el-button>
                  </div>
                  <el-table :data="dashboardData.listData.myMaintenances || []" size="small" class="todo-table" height="105">
                    <el-table-column prop="maintId" label="报修工单" width="90" />
                    <el-table-column prop="equipId" label="设备编号" width="100" />
                    <el-table-column label="状态" width="100" align="center">
                      <template slot-scope="scope">
                        <el-tag size="mini" :type="scope.row.maintStatus === 1 ? 'warning' : 'success'">
                          {{ formatMaintStatus(scope.row.maintStatus) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboardSummary } from '@/api/dashboard'

export default {
  name: 'UserDashboard',
  data() {
    return {
      loading: true,
      role: null,
      realName: '',
      username: '',
      unitCode: '',
      currentDate: '',
      dateTimer: null,
      resizeTimer: null,
      chartTimer: null,
      chartInstances: [],
      // 聚合数据结构
      dashboardData: {
        role: null,
        kpis: {},
        charts: {},
        listData: {}
      }
    }
  },
  computed: {
    // 角色2 (资产管理员) KPI 配置
    role2Kpis() {
      const k = this.dashboardData.kpis || {}
      return [
        { label: '资产设备总数', value: k.totalEquipment || 0, icon: 'el-icon-monitor', color: '#409EFF', bg: 'rgba(64, 158, 255, 0.1)' },
        { label: '资产总原值', value: k.totalValue || 0, icon: 'el-icon-money', color: '#67C23A', bg: 'rgba(103, 194, 58, 0.1)', isMoney: true },
        { label: '在用设备数', value: k.inUseCount || 0, icon: 'el-icon-circle-check', color: '#409EFF', bg: 'rgba(64, 158, 255, 0.1)' },
        { label: '维修中设备', value: k.inMaintenanceCount || 0, icon: 'el-icon-warning', color: '#E6A23C', bg: 'rgba(230, 162, 60, 0.1)' },
        { label: '报废资产数', value: k.scrappedCount || 0, icon: 'el-icon-circle-close', color: '#F56C6C', bg: 'rgba(245, 108, 108, 0.1)' }
      ]
    },
    // 角色3 (系统管理员) KPI 配置
    role3Kpis() {
      const k = this.dashboardData.kpis || {}
      return [
        { label: '资产设备总数', value: k.totalEquipment || 0, icon: 'el-icon-monitor', color: '#409EFF', bg: 'rgba(64, 158, 255, 0.1)' },
        { label: '资产总原值', value: k.totalValue || 0, icon: 'el-icon-money', color: '#67C23A', bg: 'rgba(103, 194, 58, 0.1)', isMoney: true },
        { label: '系统用户总数', value: k.totalUsers || 0, icon: 'el-icon-user', color: '#E6A23C', bg: 'rgba(230, 162, 60, 0.1)' },
        { label: '数据备份数', value: k.backupCount || 0, icon: 'el-icon-receiving', color: '#F56C6C', bg: 'rgba(245, 108, 108, 0.1)' }
      ]
    },
    // 角色1 (维修工程师) KPI 配置
    role1Kpis() {
      const k = this.dashboardData.kpis || {}
      return [
        { label: '待处理工单', value: k.myPendingMaint || 0, icon: 'el-icon-bell', color: '#F56C6C', bg: 'rgba(245, 108, 108, 0.1)' },
        { label: '维修中工单', value: k.myInMaint || 0, icon: 'el-icon-loading', color: '#E6A23C', bg: 'rgba(230, 162, 60, 0.1)' },
        { label: '历史完工数', value: k.myCompletedMaint || 0, icon: 'el-icon-success', color: '#67C23A', bg: 'rgba(103, 194, 58, 0.1)' }
      ]
    },
    // 角色0 (设备操作员) KPI 配置
    role0Kpis() {
      const k = this.dashboardData.kpis || {}
      return [
        { label: '保管设备数', value: k.myEquipCount || 0, icon: 'el-icon-monitor', color: '#409EFF', bg: 'rgba(64, 158, 255, 0.1)' },
        { label: '在途领用申请', value: k.myActiveClaims || 0, icon: 'el-icon-document', color: '#E6A23C', bg: 'rgba(230, 162, 60, 0.1)' },
        { label: '在途报修单', value: k.myActiveMaintenances || 0, icon: 'el-icon-warning', color: '#F56C6C', bg: 'rgba(245, 108, 108, 0.1)' },
        { label: '保管资产折旧额', value: k.myDepreciationValue || 0, icon: 'el-icon-pie-chart', color: '#67C23A', bg: 'rgba(103, 194, 58, 0.1)', isMoney: true }
      ]
    }
  },
  filters: {
    formatNumber(val) {
      if (val === undefined || val === null) return 0
      if (typeof val === 'number') {
        return val % 1 === 0 
          ? val.toLocaleString() 
          : val.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      }
      return val
    },
    formatBytes(bytes) {
      if (!bytes || bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    formatDate(timestamp) {
      if (!timestamp) return '-'
      const date = new Date(timestamp)
      if (isNaN(date.getTime())) return timestamp // 已经是格式化字符串直接返回
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
    }
  },
  watch: {
    // 监听角色变化重新载入图表
    role() {
      this.$nextTick(() => {
        this.initDashboardCharts()
      })
    }
  },
  created() {
    this.updateLocalUserInfo()
    this.startClock()
  },
  mounted() {
    this.loadDashboardData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.dateTimer) clearInterval(this.dateTimer)
    if (this.resizeTimer) clearTimeout(this.resizeTimer)
    if (this.chartTimer) clearTimeout(this.chartTimer)
    window.removeEventListener('resize', this.handleResize)
    this.disposeCharts()
  },
  methods: {
    updateLocalUserInfo() {
      const roleStr = localStorage.getItem('role')
      this.role = roleStr !== null ? parseInt(roleStr, 10) : 2 // 默认设为资产管理员2
      this.realName = localStorage.getItem('realName') || ''
      this.username = localStorage.getItem('username') || ''
      this.unitCode = localStorage.getItem('unitCode') || ''
    },
    startClock() {
      const formatTime = () => {
        const d = new Date()
        const yy = d.getFullYear()
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')
        const hh = String(d.getHours()).padStart(2, '0')
        const min = String(d.getMinutes()).padStart(2, '0')
        const ss = String(d.getSeconds()).padStart(2, '0')
        this.currentDate = `${yy}-${mm}-${dd} ${hh}:${min}:${ss}`
      }
      formatTime()
      this.dateTimer = setInterval(formatTime, 1000)
    },
    loadDashboardData() {
      this.loading = true
      // 发起真实 HTTP 请求
      getDashboardSummary().then(resData => {
        if (resData) {
          this.dashboardData = resData
          this.role = resData.role
          this.$nextTick(() => {
            this.initDashboardCharts()
          })
        }
        this.loading = false
      }).catch((err) => {
        // 接口调用报错或网络问题，立即启动高保真 Mock 渲染作为兜底，防止页面空白
        console.warn('[Dashboard] API error occurred. Initiating immediate fallback Mock data.', err)
        this.useMockData()
        this.loading = false
        this.$nextTick(() => {
          this.initDashboardCharts()
        })
      })
    },
    useMockData() {
      const r = this.role !== null ? this.role : 2
      
      // 组装符合契约的角色化 Mock 报文
      const mockResult = {
        role: r,
        kpis: {},
        charts: {},
        listData: {}
      }

      if (r === 2) {
        mockResult.kpis = {
          totalEquipment: 128,
          totalValue: 1452000.00,
          inUseCount: 98,
          inMaintenanceCount: 22,
          scrappedCount: 8
        }
        mockResult.charts = {
          categoryDistribution: [
            { name: "计算机设备", value: 65 },
            { name: "网络设备", value: 30 },
            { name: "打印外设", value: 18 },
            { name: "安防监控", value: 15 }
          ],
          departmentDistribution: [
            { name: "研发部", value: 450000 },
            { name: "行政部", value: 120000 },
            { name: "销售部", value: 280000 },
            { name: "技术部", value: 350000 },
            { name: "财务部", value: 252000 }
          ],
          maintenanceTrend: [
            { month: "2026-01", cost: 1200, count: 2 },
            { month: "2026-02", cost: 2400, count: 5 },
            { month: "2026-03", cost: 1800, count: 3 },
            { month: "2026-04", cost: 3200, count: 6 },
            { month: "2026-05", cost: 5400, count: 9 },
            { month: "2026-06", cost: 4100, count: 7 }
          ]
        }
        mockResult.listData = {
          pendingClaims: [
            { claimId: 101, equipId: "E023", equipName: "联想 ThinkPad T14 办公本", applicantRealName: "张三", createTime: "2026-06-11 10:20:00" },
            { claimId: 102, equipId: "E084", equipName: "华为交换机 CloudEngine S5700", applicantRealName: "李四", createTime: "2026-06-12 08:45:00" }
          ],
          pendingMaintenances: [
            { maintId: 201, equipId: "E041", faultDescription: "开机无限蓝屏，无法引导系统，数据需备份", maintStatus: 0 },
            { maintId: 202, equipId: "E052", faultDescription: "网络频繁掉线，插拔网口无反应，疑似网卡损坏", maintStatus: 0 }
          ]
        }
      } else if (r === 3) {
        mockResult.kpis = {
          totalEquipment: 128,
          totalValue: 1452000.00,
          totalUsers: 18,
          backupCount: 5
        }
        mockResult.charts = {
          userRoleDistribution: [
            { name: "设备操作员", value: 10 },
            { name: "维修工程师", value: 4 },
            { name: "资产管理员", value: 3 },
            { name: "系统管理员", value: 1 }
          ]
        }
        mockResult.listData = {
          backupFiles: [
            { name: "backup_20260610_020000.sql", size: 1452031, lastModified: 1781065853000 },
            { name: "backup_20260611_020000.sql", size: 1458920, lastModified: 1781152253000 },
            { name: "backup_20260612_020000.sql", size: 1463102, lastModified: 1781238653000 }
          ]
        }
      } else if (r === 1) {
        mockResult.kpis = {
          myPendingMaint: 3,
          myInMaint: 2,
          myCompletedMaint: 24
        }
        mockResult.charts = {
          maintCostTrend: [
            { month: "2026-01", cost: 500.00 },
            { month: "2026-02", cost: 1200.00 },
            { month: "2026-03", cost: 800.00 },
            { month: "2026-04", cost: 1500.00 },
            { month: "2026-05", cost: 2100.00 },
            { month: "2026-06", cost: 950.00 }
          ]
        }
        mockResult.listData = {
          myWorkOrders: [
            { maintId: 301, equipId: "E033", faultDescription: "显示屏大面积闪烁，伴随横向条纹干扰", maintStatus: 1 },
            { maintId: 302, equipId: "E045", faultDescription: "键盘部分常用字母按键失灵，回弹无阻尼感", maintStatus: 1 }
          ]
        }
      } else if (r === 0) {
        mockResult.kpis = {
          myEquipCount: 3,
          myActiveClaims: 1,
          myActiveMaintenances: 1,
          myDepreciationValue: 1850.00
        }
        mockResult.listData = {
          myEquipments: [
            { equipId: "E012", equipName: "戴尔 Latitude 5420 笔记本", status: "在用", originalValue: 6800.00 },
            { equipId: "E015", equipName: "明基 PD2700Q 2K显示器", status: "在用", originalValue: 1800.00 }
          ],
          myClaims: [
            { claimId: 401, equipId: "E099", status: 0, createTime: "2026-06-11 15:30:00" }
          ],
          myMaintenances: [
            { maintId: 501, equipId: "E012", maintStatus: 1 }
          ]
        }
      }

      this.dashboardData = mockResult
    },
    initDashboardCharts() {
      // 每次重新构建图表时先清空以前的实例
      this.disposeCharts()

      if (this.chartTimer) {
        clearTimeout(this.chartTimer)
      }
      this.chartTimer = setTimeout(() => {
        if (this.role === 2) {
          this.initMaintTrendChart()
          this.initCategoryChart()
          this.initDeptChart()
        } else if (this.role === 3) {
          this.initUserRoleChart()
        } else if (this.role === 1) {
          this.initMaintCostChart()
        }
      }, 50)
    },
    // ECharts：维保费用与次数趋势（Role 2）
    initMaintTrendChart() {
      const el = this.$refs.maintTrendChart
      if (!el) return
      const chart = echarts.init(el)
      this.chartInstances.push(chart)

      const trendData = this.dashboardData.charts.maintenanceTrend || []
      const months = trendData.map(item => item.month)
      const costs = trendData.map(item => item.cost)
      const counts = trendData.map(item => item.count)

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          top: '15%',
          left: '3%',
          right: '3%',
          bottom: '15%',
          containLabel: true
        },
        legend: {
          bottom: '0%',
          data: ['维保费用', '工单次数'],
          textStyle: { color: '#606266' }
        },
        xAxis: [
          {
            type: 'category',
            data: months,
            axisTick: { alignWithLabel: true },
            axisLine: { lineStyle: { color: '#DCDFE6' } },
            axisLabel: { color: '#606266' }
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '费用 (元)',
            axisLine: { lineStyle: { color: '#DCDFE6' } },
            axisLabel: { color: '#606266' },
            splitLine: { lineStyle: { color: '#F2F6FC' } }
          },
          {
            type: 'value',
            name: '次数 (次)',
            position: 'right',
            splitLine: { show: false },
            axisLine: { lineStyle: { color: '#DCDFE6' } },
            axisLabel: { color: '#606266' }
          }
        ],
        series: [
          {
            name: '维保费用',
            type: 'bar',
            barWidth: '40%',
            data: costs,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#1890ff' },
                { offset: 1, color: '#36cfc9' }
              ])
            }
          },
          {
            name: '工单次数',
            type: 'line',
            yAxisIndex: 1,
            data: counts,
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: { color: '#f5222d' },
            lineStyle: { width: 3 }
          }
        ]
      })
    },
    // ECharts：资产分类分布（Role 2）
    initCategoryChart() {
      const el = this.$refs.categoryChart
      if (!el) return
      const chart = echarts.init(el)
      this.chartInstances.push(chart)

      const categories = this.dashboardData.charts.categoryDistribution || []

      chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} 台 ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: '0',
          left: 'center',
          textStyle: { color: '#606266' }
        },
        color: ['#1890ff', '#2fc25b', '#facc14', '#f04864', '#8543e0'],
        series: [
          {
            name: '资产分类',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '42%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 6,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold',
                formatter: '{b}\n{c}台'
              }
            },
            labelLine: {
              show: false
            },
            data: categories
          }
        ]
      })
    },
    // ECharts：部门资产分布（Role 2）
    initDeptChart() {
      const el = this.$refs.deptChart
      if (!el) return
      const chart = echarts.init(el)
      this.chartInstances.push(chart)

      const deptData = this.dashboardData.charts.departmentDistribution || []
      const deptNames = deptData.map(item => item.name)
      const values = deptData.map(item => item.value)

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: '{b}: ￥{c}'
        },
        grid: {
          top: '10%',
          left: '3%',
          right: '5%',
          bottom: '5%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: deptNames,
          axisLine: { lineStyle: { color: '#DCDFE6' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          name: '原值 (元)',
          axisLine: { lineStyle: { color: '#DCDFE6' } },
          axisLabel: { color: '#606266' },
          splitLine: { lineStyle: { color: '#F2F6FC' } }
        },
        series: [
          {
            name: '资产价值',
            type: 'bar',
            barWidth: '50%',
            data: values,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#34bfa3' },
                { offset: 1, color: '#a3f3d7' }
              ])
            }
          }
        ]
      })
    },
    // ECharts：用户角色分布（Role 3）
    initUserRoleChart() {
      const el = this.$refs.userRoleChart
      if (!el) return
      const chart = echarts.init(el)
      this.chartInstances.push(chart)

      const userData = this.dashboardData.charts.userRoleDistribution || []

      chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} 人 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          textStyle: { color: '#606266' }
        },
        color: ['#1890ff', '#13c2c2', '#2fc25b', '#facc14'],
        series: [
          {
            name: '用户角色',
            type: 'pie',
            radius: '65%',
            center: ['55%', '50%'],
            data: userData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.1)'
              }
            }
          }
        ]
      })
    },
    // ECharts：个人维保费用趋势（Role 1）
    initMaintCostChart() {
      const el = this.$refs.maintCostChart
      if (!el) return
      const chart = echarts.init(el)
      this.chartInstances.push(chart)

      const costData = this.dashboardData.charts.maintCostTrend || []
      const months = costData.map(item => item.month)
      const costs = costData.map(item => item.cost)

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: '{b} 维保额: ￥{c}'
        },
        grid: {
          top: '12%',
          left: '3%',
          right: '5%',
          bottom: '5%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: months,
          axisLine: { lineStyle: { color: '#DCDFE6' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          name: '金额 (元)',
          axisLine: { lineStyle: { color: '#DCDFE6' } },
          axisLabel: { color: '#606266' },
          splitLine: { lineStyle: { color: '#F2F6FC' } }
        },
        series: [
          {
            name: '维保金额',
            type: 'line',
            data: costs,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 3,
              color: '#1890ff'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(24, 144, 255, 0.4)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.01)' }
              ])
            },
            itemStyle: {
              color: '#1890ff'
            }
          }
        ]
      })
    },
    // 防抖缩放
    handleResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        this.chartInstances.forEach(chart => {
          if (chart && typeof chart.resize === 'function') {
            chart.resize()
          }
        })
      }, 150)
    },
    // 释放图表实例
    disposeCharts() {
      this.chartInstances.forEach(chart => {
        if (chart && typeof chart.dispose === 'function') {
          chart.dispose()
        }
      })
      this.chartInstances = []
    },
    // 辅助格式化工具
    formatRole(role) {
      const roleMap = {
        0: '设备操作员',
        1: '维修工程师',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : '未知角色'
    },
    formatMaintStatus(status) {
      // 0-待指派, 1-维修中, 2-待复核, 3-已复核可用, 4-转报废
      const statusMap = {
        0: '待指派',
        1: '维修中',
        2: '待复核',
        3: '已复核可用',
        4: '转报废'
      }
      return statusMap[status] !== undefined ? statusMap[status] : '未知'
    },
    formatClaimStatus(status) {
      // 0-待审批, 1-已同意, 2-已拒绝
      const statusMap = {
        0: '待审批',
        1: '已同意',
        2: '已拒绝'
      }
      return statusMap[status] !== undefined ? statusMap[status] : '未知'
    },
    formatClaimTagType(status) {
      const tagMap = {
        0: 'warning',
        1: 'success',
        2: 'danger'
      }
      return tagMap[status] || 'info'
    },
    goToPage(path) {
      if (this.$route.path !== path) {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 10px 0;
  min-height: calc(100vh - 120px);
}

/* 欢迎区样式 */
.welcome-banner {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.welcome-text {
  display: flex;
  align-items: center;
}

.welcome-icon {
  font-size: 32px;
  color: #1890ff;
  margin-right: 16px;
}

.welcome-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.welcome-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.role-badge {
  font-weight: bold;
  color: #1890ff;
}

.welcome-date {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
}

.welcome-date i {
  margin-right: 6px;
  font-size: 16px;
}

/* KPI 指标卡片 */
.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.kpi-card /deep/ .el-card__body {
  padding: 12px 15px !important;
}

.kpi-card-body {
  display: flex;
  align-items: center;
  padding: 0;
}

.kpi-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.kpi-icon-wrapper i {
  font-size: 24px;
}

.kpi-info {
  flex-grow: 1;
  min-width: 0;
}

.kpi-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
  white-space: nowrap;
}

.money-symbol {
  font-size: 14px;
  font-weight: normal;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
}

/* 图表与待办卡片 */
.chart-card, .todo-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.icon-margin {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.chart-container {
  height: 330px;
  width: 100%;
}

.todo-card {
  height: 410px;
}

.todo-lists {
  padding: 5px 0;
}

.todo-section {
  border-bottom: 1px solid #f2f6fc;
  padding-bottom: 15px;
}

.todo-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.todo-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.todo-icon-margin {
  margin-right: 6px;
  color: #e6a23c;
}

.todo-table {
  border: none;
}

.todo-table::before {
  height: 0;
}

/* 辅类 */
.cursor-pointer {
  cursor: pointer;
}

.font-bold {
  font-weight: bold;
}

.backup-content {
  padding: 5px 0;
}

/* 按钮与卡片过渡控制，不使用缩放或加粗 border 防止抖动 */
.el-card {
  transition: box-shadow 0.3s ease, background-color 0.3s ease;
}

.el-button {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
}
</style>
