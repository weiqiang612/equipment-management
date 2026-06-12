<template>
  <div class="governance-container" v-loading="loading">
    <!-- 顶部标题与背景 -->
    <div class="page-header">
      <div class="title-area">
        <i class="el-icon-pie-chart header-icon"></i>
        <div>
          <h2 class="page-title">数据治理与运营风险分析</h2>
          <p class="page-subtitle">实时监控设备数据质量、分析年限与维保费用风险，为决策提供可靠的实证依据。</p>
        </div>
      </div>
    </div>

    <!-- 1. 顶部 KPI 数据卡片 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :xs="24" :sm="12" :md="4" :lg="4">
        <el-card shadow="hover" class="kpi-card quality-card">
          <div class="kpi-card-body">
            <div class="kpi-icon-wrapper score-bg">
              <i class="el-icon-medal"></i>
            </div>
            <div class="kpi-info">
              <div class="kpi-value primary-color">{{ summaryData.qualityScore }}分</div>
              <div class="kpi-label">数据质量评分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="5" :lg="5">
        <el-card shadow="hover" class="kpi-card high-risk-card">
          <div class="kpi-card-body">
            <div class="kpi-icon-wrapper high-bg">
              <i class="el-icon-warning"></i>
            </div>
            <div class="kpi-info">
              <div class="kpi-value danger-color">{{ summaryData.riskDistribution.high || 0 }}台</div>
              <div class="kpi-label">高风险设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="5" :lg="5">
        <el-card shadow="hover" class="kpi-card medium-risk-card">
          <div class="kpi-card-body">
            <div class="kpi-icon-wrapper medium-bg">
              <i class="el-icon-info"></i>
            </div>
            <div class="kpi-info">
              <div class="kpi-value warning-color">{{ summaryData.riskDistribution.medium || 0 }}台</div>
              <div class="kpi-label">中风险设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="5" :lg="5">
        <el-card shadow="hover" class="kpi-card idle-card">
          <div class="kpi-card-body">
            <div class="kpi-icon-wrapper idle-bg">
              <i class="el-icon-video-pause"></i>
            </div>
            <div class="kpi-info">
              <div class="kpi-value info-color">{{ summaryData.idleCount || 0 }}台</div>
              <div class="kpi-label">长期空闲设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="5" :lg="5">
        <el-card shadow="hover" class="kpi-card anomaly-card">
          <div class="kpi-card-body">
            <div class="kpi-icon-wrapper anomaly-bg">
              <i class="el-icon-money"></i>
            </div>
            <div class="kpi-info">
              <div class="kpi-value anomaly-color">{{ summaryData.costAnomaliesCount || 0 }}台</div>
              <div class="kpi-label">维保成本异常</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 中部图表与细节汇总 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 风险等级分布 (饼图) -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-header">
            <span><i class="el-icon-pie-chart icon-margin"></i>风险等级分布</span>
          </div>
          <div class="chart-container" ref="riskLevelChart"></div>
        </el-card>
      </el-col>

      <!-- 部门风险分布 (堆叠柱状图) -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-header">
            <span><i class="el-icon-office-building icon-margin"></i>各单位风险分布</span>
          </div>
          <div class="chart-container" ref="deptRiskChart"></div>
        </el-card>
      </el-col>

      <!-- 分类风险分布 (堆叠柱状图) -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-header">
            <span><i class="el-icon-menu icon-margin"></i>各分类风险分布</span>
          </div>
          <div class="chart-container" ref="categoryRiskChart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 空闲与维修成本异常快速总览 -->
    <el-row :gutter="20" class="summary-lists-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="list-card">
          <div slot="header" class="card-header list-header">
            <span><i class="el-icon-money icon-margin"></i>高额维保成本异常分析 (Top 4)</span>
            <el-tag size="mini" type="danger">成本占原值较高</el-tag>
          </div>
          <el-table :data="summaryData.costAnomalies" size="small" stripe border>
            <el-table-column prop="equipId" label="设备编号" width="90" />
            <el-table-column prop="equipName" label="设备名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="设备原值" width="100">
              <template slot-scope="scope">
                ￥{{ scope.row.originalValue | formatNumber }}
              </template>
            </el-table-column>
            <el-table-column label="已付维修费" width="100">
              <template slot-scope="scope">
                ￥{{ scope.row.maintCost | formatNumber }}
              </template>
            </el-table-column>
            <el-table-column label="费用占比" width="90" align="center">
              <template slot-scope="scope">
                <span :class="scope.row.costRatio >= 0.3 ? 'text-bold danger-color' : 'text-bold warning-color'">
                  {{ (scope.row.costRatio * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="viewDetail(scope.row.equipId)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="list-card">
          <div slot="header" class="card-header list-header">
            <span><i class="el-icon-video-pause icon-margin"></i>长期空闲/保管人缺失预警 (Top 6)</span>
            <el-tag size="mini" type="warning">暂无保管人且是在用状态</el-tag>
          </div>
          <el-table :data="summaryData.idleEquipments" size="small" stripe border>
            <el-table-column prop="equipId" label="设备编号" width="90" />
            <el-table-column prop="equipName" label="设备名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="unitName" label="所属单位" width="100" />
            <el-table-column prop="purchaseDate" label="购入日期" width="110" />
            <el-table-column label="设备原值" width="100">
              <template slot-scope="scope">
                ￥{{ scope.row.originalValue | formatNumber }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="viewDetail(scope.row.equipId)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4. 底部风险设备清单表格 -->
    <el-card shadow="hover" class="table-card">
      <div slot="header" class="card-header">
        <span><i class="el-icon-notebook-2 icon-margin"></i>风险设备治理清单</span>
      </div>

      <!-- 搜索与筛选区域 -->
      <div class="filter-wrapper">
        <el-form :inline="true" :model="queryForm" size="small" class="demo-form-inline">
          <el-form-item label="风险等级">
            <el-select v-model="queryForm.riskLevel" placeholder="选择风险等级" clearable style="width: 130px;" @change="handleSearch">
              <el-option label="高风险" value="高风险" />
              <el-option label="中风险" value="中风险" />
              <el-option label="低风险" value="低风险" />
            </el-select>
          </el-form-item>

          <el-form-item label="所属单位">
            <el-select 
              v-model="queryForm.unitCode" 
              :placeholder="role === 2 ? unitPlaceholder : '选择单位'" 
              :clearable="role !== 2" 
              style="width: 160px;" 
              :disabled="isUnitCodeDisabled" 
              @change="handleSearch"
            >
              <el-option v-for="item in depts" :key="item.unitCode" :label="item.unitName" :value="item.unitCode" />
            </el-select>
          </el-form-item>

          <el-form-item label="设备分类">
            <el-select v-model="queryForm.categoryId" placeholder="选择分类" clearable style="width: 160px;" @change="handleSearch">
              <el-option v-for="item in categories" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据质量问题快速提示区域（若过滤条件下有数据） -->
      <el-alert
        v-if="summaryData.totalIssues > 0"
        :title="`提示：系统当前检测到有 ${summaryData.totalIssues} 个数据质量质量问题，包含保管人与单位不匹配、缺少购入日期或分类缺失等。`"
        type="warning"
        show-icon
        :closable="false"
        class="quality-alert"
      >
        <div class="quality-badges">
          <el-tag
            v-for="issue in summaryData.issueSummary"
            :key="issue.type"
            size="mini"
            type="warning"
            style="margin-right: 10px;"
          >
            {{ issue.name }}: {{ issue.count }}台
          </el-tag>
        </div>
      </el-alert>

      <!-- 风险设备列表 -->
      <el-table :data="tableData" size="small" stripe border style="width: 100%; margin-top: 15px;" v-loading="tableLoading">
        <el-table-column prop="equipId" label="设备编号" width="90" align="center" />
        <el-table-column prop="equipName" label="设备名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="设备分类" width="100" show-overflow-tooltip />
        <el-table-column prop="unitName" label="所属单位" width="100" show-overflow-tooltip />
        <el-table-column label="风险等级" width="90" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="getRiskTagType(scope.row.riskLevel)">
              {{ formatRiskLevel(scope.row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康评分" width="90" align="center">
          <template slot-scope="scope">
            <span :style="{ color: getHealthColor(scope.row.healthScore), fontWeight: 'bold' }">
              {{ scope.row.healthScore }}分
            </span>
          </template>
        </el-table-column>
        <el-table-column label="使用年限占比" width="100" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.ageRatio >= 0.9 ? 'danger-color text-bold' : (scope.row.ageRatio >= 0.75 ? 'warning-color text-bold' : '')">
              {{ (scope.row.ageRatio * 100).toFixed(0) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="maintenanceCount" label="维修次数" width="80" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.maintenanceCount >= 3 ? 'danger-color text-bold' : (scope.row.maintenanceCount >= 2 ? 'warning-color text-bold' : '')">
              {{ scope.row.maintenanceCount ? scope.row.maintenanceCount + '次' : '无' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="维保费用占比" width="100" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.costRatio >= 0.3 ? 'danger-color text-bold' : (scope.row.costRatio >= 0.15 ? 'warning-color text-bold' : '')">
              {{ (scope.row.costRatio * 100).toFixed(0) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="riskReasons" label="判定原因说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="custodianRealName" label="保管人" width="80" align="center">
          <template slot-scope="scope">
            {{ scope.row.custodianRealName || '无保管人' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-search" @click="viewDetail(scope.row.equipId)">全生命周期</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="queryForm.page"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="queryForm.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getGovernanceSummary, getEquipmentRisks } from '@/api/governance'
import { getDepts } from '@/api/department'
import { getCategories } from '@/api/category'

export default {
  name: 'DataGovernance',
  filters: {
    formatNumber(val) {
      if (val === undefined || val === null) return '0.00'
      return parseFloat(val).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }
  },
  data() {
    return {
      loading: true,
      tableLoading: false,
      role: null,
      unitCode: null,
      depts: [],
      categories: [],
      // 汇总数据
      summaryData: {
        qualityScore: 100,
        totalIssues: 0,
        issueSummary: [],
        riskDistribution: { high: 0, medium: 0, low: 0 },
        departmentDistribution: [],
        categoryDistribution: [],
        costAnomaliesCount: 0,
        costAnomalies: [],
        idleCount: 0,
        idleEquipments: []
      },
      // 表格过滤
      queryForm: {
        riskLevel: '',
        unitCode: '',
        categoryId: '',
        page: 1,
        pageSize: 10
      },
      tableData: [],
      total: 0,
      // Chart 实例引用
      riskChartInstance: null,
      deptChartInstance: null,
      categoryChartInstance: null
    }
  },
  computed: {
    // 资产管理员 (role === 2) 只能查看本单位数据，禁止修改单位筛选框
    isUnitCodeDisabled() {
      return this.role === 2
    },
    unitPlaceholder() {
      if (this.role === 2 && this.unitCode) {
        const dept = this.depts.find(d => d.unitCode === this.unitCode)
        return dept ? dept.unitName : '加载中...'
      }
      return '选择单位'
    }
  },
  created() {
    this.initRoleInfo()
    this.fetchFilterData()
    this.loadSummary()
    this.loadRisks()
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.destroyCharts()
  },
  methods: {
    initRoleInfo() {
      const roleStr = localStorage.getItem('role')
      this.role = roleStr !== null ? parseInt(roleStr, 10) : null
      this.unitCode = localStorage.getItem('unitCode')
      
      // 资产管理员限制本单位
      if (this.role === 2 && this.unitCode) {
        this.queryForm.unitCode = this.unitCode
      }
    },
    // 加载筛选器字典
    async fetchFilterData() {
      try {
        const [deptsRes, categoriesRes] = await Promise.all([
          getDepts(),
          getCategories()
        ])
        this.depts = deptsRes || []
        this.categories = categoriesRes || []
      } catch (err) {
        console.error('Fetch filter dictionary error:', err)
      }
    },
    // 加载治理总览
    async loadSummary() {
      this.loading = true
      try {
        const data = await getGovernanceSummary() || {}
        this.summaryData = {
          ...this.summaryData,
          qualityScore: data.qualityScore || 100,
          totalIssues: data.issueCount || 0,
          issueCount: data.issueCount || 0,
          idleCount: data.idleCount || 0,
          costAnomaliesCount: data.costAnomalyCount || 0,
          // 组装 issueSummary 数组
          issueSummary: [
            { type: 'missing_fields', name: '关键字段缺失', count: data.missingFieldsCount || 0 },
            { type: 'mismatch', name: '保管人与单位不匹配', count: data.mismatchCount || 0 },
            { type: 'duplicate', name: '疑似重复设备', count: data.duplicateCount || 0 }
          ],
          // 组装 riskDistribution
          riskDistribution: {
            high: data.highRiskCount || 0,
            medium: data.mediumRiskCount || 0,
            low: data.lowRiskCount || 0
          },
          departmentDistribution: data.departmentDistribution || [],
          categoryDistribution: data.categoryDistribution || []
        }
        // 渲染图表
        this.$nextTick(() => {
          this.renderCharts()
        })
      } catch (err) {
        this.$message.error('加载治理总览数据失败')
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    // 加载风险清单明细
    async loadRisks() {
      this.tableLoading = true
      try {
        const data = await getEquipmentRisks(this.queryForm) || {}
        this.tableData = data.rows || []
        this.total = data.total || 0
        
        // 优雅降级：如果真实列表数据有，则组装统计信息
        if (!this.summaryData.costAnomalies || this.summaryData.costAnomalies.length === 0) {
          this.summaryData.costAnomalies = [...this.tableData]
            .filter(item => item.costRatio > 0)
            .sort((a, b) => b.costRatio - a.costRatio)
            .slice(0, 4)
        }
        if (!this.summaryData.idleEquipments || this.summaryData.idleEquipments.length === 0) {
          this.summaryData.idleEquipments = [...this.tableData]
            .filter(item => !item.custodian && item.status === '在用')
            .slice(0, 6)
        }
      } catch (err) {
        this.$message.error('加载风险清单失败')
        console.error(err)
      } finally {
        this.tableLoading = false
      }
    },
    handleSearch() {
      this.queryForm.page = 1
      this.loadRisks()
    },
    resetQuery() {
      this.queryForm.riskLevel = ''
      this.queryForm.categoryId = ''
      // 只有系统管理员能重置单位条件
      if (this.role !== 2) {
        this.queryForm.unitCode = ''
      } else {
        this.queryForm.unitCode = this.unitCode || ''
      }
      this.queryForm.page = 1
      this.loadRisks()
    },
    handleSizeChange(val) {
      this.queryForm.pageSize = val
      this.queryForm.page = 1
      this.loadRisks()
    },
    handleCurrentChange(val) {
      this.queryForm.page = val
      this.loadRisks()
    },
    viewDetail(equipId) {
      this.$router.push(`/equipment/detail/${equipId}`)
    },
    // 渲染所有 ECharts
    renderCharts() {
      this.renderRiskLevelChart()
      this.renderDeptRiskChart()
      this.renderCategoryRiskChart()
    },
    renderRiskLevelChart() {
      const el = this.$refs.riskLevelChart
      if (!el) return
      if (this.riskChartInstance) {
        this.riskChartInstance.dispose()
      }
      this.riskChartInstance = echarts.init(el)
      
      const dist = this.summaryData.riskDistribution
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}台 ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: '10',
          left: 'center'
        },
        color: ['#F56C6C', '#E6A23C', '#67C23A'],
        series: [
          {
            name: '风险等级',
            type: 'pie',
            radius: ['45%', '70%'],
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
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: dist.high, name: '高风险' },
              { value: dist.medium, name: '中风险' },
              { value: dist.low, name: '低风险' }
            ]
          }
        ]
      }
      this.riskChartInstance.setOption(option)
    },
    renderDeptRiskChart() {
      const el = this.$refs.deptRiskChart
      if (!el) return
      if (this.deptChartInstance) {
        this.deptChartInstance.dispose()
      }
      this.deptChartInstance = echarts.init(el)

      let list = this.summaryData.departmentDistribution || []
      // 资产管理员视角，裁剪只能看本部门的统计
      if (this.role === 2 && this.unitCode) {
        list = list.filter(item => item.unitCode === this.unitCode)
      }

      const xData = list.map(i => i.unitName)
      const highData = list.map(i => i.highCount)
      const mediumData = list.map(i => i.mediumCount)
      const lowData = list.map(i => i.lowCount)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        legend: {
          bottom: '10',
          left: 'center'
        },
        grid: {
          left: '3%',
          right: '4%',
          top: '10%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: xData
          }
        ],
        yAxis: [
          {
            type: 'value',
            title: '台数'
          }
        ],
        color: ['#F56C6C', '#E6A23C', '#67C23A'],
        series: [
          {
            name: '高风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: highData
          },
          {
            name: '中风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: mediumData
          },
          {
            name: '低风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: lowData
          }
        ]
      }
      this.deptChartInstance.setOption(option)
    },
    renderCategoryRiskChart() {
      const el = this.$refs.categoryRiskChart
      if (!el) return
      if (this.categoryChartInstance) {
        this.categoryChartInstance.dispose()
      }
      this.categoryChartInstance = echarts.init(el)

      const list = this.summaryData.categoryDistribution || []
      const xData = list.map(i => i.categoryName)
      const highData = list.map(i => i.highCount)
      const mediumData = list.map(i => i.mediumCount)
      const lowData = list.map(i => i.lowCount)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        legend: {
          bottom: '10',
          left: 'center'
        },
        grid: {
          left: '3%',
          right: '4%',
          top: '10%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: xData
          }
        ],
        yAxis: [
          {
            type: 'value',
            title: '台数'
          }
        ],
        color: ['#F56C6C', '#E6A23C', '#67C23A'],
        series: [
          {
            name: '高风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: highData
          },
          {
            name: '中风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: mediumData
          },
          {
            name: '低风险',
            type: 'bar',
            stack: 'Total',
            emphasis: { focus: 'series' },
            data: lowData
          }
        ]
      }
      this.categoryChartInstance.setOption(option)
    },
    handleResize() {
      if (this.riskChartInstance) this.riskChartInstance.resize()
      if (this.deptChartInstance) this.deptChartInstance.resize()
      if (this.categoryChartInstance) this.categoryChartInstance.resize()
    },
    destroyCharts() {
      if (this.riskChartInstance) {
        this.riskChartInstance.dispose()
        this.riskChartInstance = null
      }
      if (this.deptChartInstance) {
        this.deptChartInstance.dispose()
        this.deptChartInstance = null
      }
      if (this.categoryChartInstance) {
        this.categoryChartInstance.dispose()
        this.categoryChartInstance = null
      }
    },
    // 状态和样式格式化辅助
    formatRiskLevel(lvl) {
      const map = { 
        high: '高风险', medium: '中风险', low: '低风险',
        '高风险': '高风险', '中风险': '中风险', '低风险': '低风险'
      }
      return map[lvl] || lvl
    },
    getRiskTagType(lvl) {
      const map = { 
        high: 'danger', medium: 'warning', low: 'success',
        '高风险': 'danger', '中风险': 'warning', '低风险': 'success'
      }
      return map[lvl] || 'info'
    },
    getHealthColor(score) {
      if (score >= 85) return '#67C23A' // 成功色
      if (score >= 60) return '#E6A23C' // 警告色
      return '#F56C6C' // 危险色
    }
  }
}
</script>

<style scoped>
.governance-container {
  padding: 10px 5px;
}

/* 头部样式 */
.page-header {
  background: linear-gradient(135deg, #1f2d3d 0%, #304156 100%);
  padding: 20px 24px;
  border-radius: 8px;
  color: #ffffff;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.title-area {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 36px;
  margin-right: 18px;
  color: #409eff;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #afb9c4;
}

/* KPI 统计卡片 */
.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  border: none;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.kpi-card-body {
  display: flex;
  align-items: center;
  padding: 10px 5px;
}

.kpi-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 22px;
}

.score-bg { background-color: rgba(64, 158, 255, 0.12); color: #409eff; }
.high-bg { background-color: rgba(245, 108, 108, 0.12); color: #f56c6c; }
.medium-bg { background-color: rgba(230, 162, 60, 0.12); color: #e6a23c; }
.idle-bg { background-color: rgba(144, 147, 153, 0.12); color: #909399; }
.anomaly-bg { background-color: rgba(103, 194, 58, 0.12); color: #67c23a; }

.kpi-info {
  flex: 1;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 4px;
  letter-spacing: -0.2px;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
}

/* 图表区域 */
.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.card-header {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  color: #303133;
}

.icon-margin {
  margin-right: 8px;
  color: #409eff;
  font-size: 16px;
}

.chart-container {
  height: 280px;
  width: 100%;
}

/* 快照列表区 */
.summary-lists-row {
  margin-bottom: 20px;
}

.list-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.list-header {
  justify-content: space-between;
}

/* 治理列表卡片 */
.table-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.filter-wrapper {
  margin-bottom: 15px;
  background-color: #fcfcfd;
  padding: 14px 14px 0 14px;
  border-radius: 6px;
  border: 1px dashed #e2e8f0;
}

.quality-alert {
  margin-bottom: 15px;
  border-radius: 6px;
}

.quality-badges {
  margin-top: 8px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 颜色常量定义 */
.primary-color { color: #409eff; }
.danger-color { color: #f56c6c; }
.warning-color { color: #e6a23c; }
.info-color { color: #909399; }
.anomaly-color { color: #67c23a; }

.text-bold {
  font-weight: bold;
}
</style>
