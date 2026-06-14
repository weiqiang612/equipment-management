<template>
  <div class="app-container">
    <el-card class="box-card header-card" shadow="hover">
      <div class="header-info">
        <div class="header-copy">
          <div class="info-title">
            <i class="el-icon-document" style="color: #409eff; margin-right: 8px;"></i>
            <span>设备领用与审批工作流</span>
          </div>
          <p class="header-subtitle">消息中心跳转到此页后，优先定位目标申请并保持审批动作首屏可见。</p>
        </div>
        <div class="info-tag">
          <span style="font-size: 14px; color: #606266; margin-right: 10px;">当前登录用户：{{ realName || username }}</span>
          <el-tag :type="roleTagType(role)" size="small">{{ formatRole(role) }}</el-tag>
        </div>
      </div>
      <div class="summary-strip">
        <div class="summary-chip is-primary">
          <span class="summary-chip-label">当前视图</span>
          <strong>{{ role === 0 ? '我的领用记录' : activeTabLabel }}</strong>
        </div>
        <div class="summary-chip" :class="{ 'is-warning': highlightedClaimId }">
          <span class="summary-chip-label">目标申请</span>
          <strong>{{ highlightedClaimId || '未指定' }}</strong>
        </div>
      </div>
    </el-card>

    <!-- 操作员视图 (role === 0) -->
    <div v-if="role === 0" class="operator-view" style="margin-top: 20px;">
      <el-card shadow="hover">
        <div slot="header" class="clearfix card-header-flex">
          <span class="card-title-text"><i class="el-icon-tickets"></i> 我的领用申请记录</span>
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="fetchOperatorClaims">刷新</el-button>
        </div>

        <el-alert
          v-if="highlightedClaimId && !hasHighlightedOperatorClaim && !loading"
          title="目标申请不存在或不在当前页，已展示当前账号可见的领用记录。"
          type="warning"
          :closable="false"
          show-icon
          class="inline-alert"
        />

        <el-table v-if="displayOperatorClaims.length > 0" :data="displayOperatorClaims" border v-loading="loading" style="width: 100%" :row-class-name="tableRowClassName">
          <el-table-column prop="createTime" label="申请时间" width="160" />
          <el-table-column prop="equipId" label="设备编号" width="120" />
          <el-table-column prop="equipName" label="设备名称">
            <template slot-scope="scope">
              <span>{{ scope.row.equipName || '未知设备' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="领用原因/备注" show-overflow-tooltip />
          <el-table-column prop="approver" label="审批人/指派人" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.approver || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="审批状态" width="120" align="center">
            <template slot-scope="scope">
              <el-tag :type="statusTagType(scope.row.status)">
                {{ formatStatus(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.status === 0"
                size="mini"
                type="danger"
                icon="el-icon-close"
                @click="handleCancel(scope.row)"
              >
                撤回
              </el-button>
              <span v-else style="color: #c0c4cc;">--</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-else-if="!loading" class="table-empty-state">
          <el-empty description="当前没有领用申请记录" :image-size="108"></el-empty>
        </div>

        <!-- 分页 -->
        <el-pagination
          background
          @size-change="handleOperatorSizeChange"
          @current-change="handleOperatorCurrentChange"
          :current-page="queryParams.page"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          style="margin-top: 20px; text-align: right;"
        />
      </el-card>
    </div>

    <!-- 管理员视图 (role === 2 || role === 3) -->
    <div v-else-if="role === 2 || role === 3" class="admin-view" style="margin-top: 20px;">
      <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
        <!-- 待审批列表 Tab -->
        <el-tab-pane name="pending">
          <span slot="label"><i class="el-icon-bell"></i> 待审批列表</span>
          <div style="margin-bottom: 15px; text-align: right;">
            <el-button type="primary" size="small" icon="el-icon-refresh" @click="fetchPendingClaims">刷新</el-button>
          </div>

          <el-alert
            v-if="highlightedClaimId && activeTab === 'pending' && !hasHighlightedPendingClaim && !loading"
            title="目标申请不在待审批列表，可能已处理，已为您切换到更合适的可见视图。"
            type="warning"
            :closable="false"
            show-icon
            class="inline-alert"
          />

          <el-table v-if="displayPendingClaims.length > 0" :data="displayPendingClaims" border v-loading="loading" style="width: 100%" :row-class-name="tableRowClassName">
            <el-table-column prop="createTime" label="申请时间" width="160" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="equipId" label="设备编号" width="120" />
            <el-table-column prop="equipName" label="设备名称">
              <template slot-scope="scope">
                <span>{{ scope.row.equipName || '未知设备' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="领用原因" show-overflow-tooltip />
            <el-table-column label="操作" width="200" align="center">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="success"
                  icon="el-icon-check"
                  @click="openApproveDialog(scope.row, 1)"
                >
                  同意
                </el-button>
                <el-button
                  size="mini"
                  type="danger"
                  icon="el-icon-close"
                  @click="openApproveDialog(scope.row, 2)"
                >
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else-if="!loading" class="table-empty-state">
            <el-empty description="当前没有待审批申请" :image-size="108"></el-empty>
          </div>

          <!-- 分页 -->
          <el-pagination
            background
            @size-change="handlePendingSizeChange"
            @current-change="handlePendingCurrentChange"
            :current-page="pendingParams.page"
            :page-sizes="[5, 10, 20, 50]"
            :page-size="pendingParams.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pendingTotal"
            style="margin-top: 20px; text-align: right;"
          />
        </el-tab-pane>

        <!-- 审计历史记录 Tab -->
        <el-tab-pane name="history">
          <span slot="label"><i class="el-icon-search"></i> 审计历史记录</span>
          
          <!-- 查询过滤器 -->
          <div class="filter-container" style="margin-bottom: 15px;">
            <el-form :inline="true" :model="historyParams" size="small" class="demo-form-inline">
              <el-form-item label="设备编号/名称">
                <el-input v-model="historyParams.equipId" placeholder="设备编号" clearable style="width: 150px" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="historyParams.status" placeholder="选择状态" clearable style="width: 120px">
                  <el-option label="待审批" :value="0" />
                  <el-option label="已同意" :value="1" />
                  <el-option label="已拒绝" :value="2" />
                  <el-option label="已撤回" :value="3" />
                  <el-option label="已退还" :value="4" />
                  <el-option label="直接分配" :value="5" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="el-icon-search" @click="handleHistorySearch">查询</el-button>
                <el-button icon="el-icon-refresh" @click="resetHistorySearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table v-if="displayHistoryClaims.length > 0" :data="displayHistoryClaims" border v-loading="loading" style="width: 100%" :row-class-name="tableRowClassName">
            <el-table-column prop="updateTime" label="操作时间" width="160" />
            <el-table-column prop="equipId" label="设备编号" width="120" />
            <el-table-column prop="equipName" label="设备名称" width="150">
              <template slot-scope="scope">
                <span>{{ scope.row.equipName || '未知设备' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="保管人/申请人" width="120" />
            <el-table-column prop="approver" label="办理人/审批人" width="120">
              <template slot-scope="scope">
                <span>{{ scope.row.approver || '--' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template slot-scope="scope">
                <el-tag :type="statusTagType(scope.row.status)">
                  {{ formatStatus(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注/审批意见" show-overflow-tooltip />
          </el-table>

          <div v-else-if="!loading" class="table-empty-state">
            <el-empty description="当前没有符合条件的历史记录" :image-size="108"></el-empty>
          </div>

          <!-- 分页 -->
          <el-pagination
            background
            @size-change="handleHistorySizeChange"
            @current-change="handleHistoryCurrentChange"
            :current-page="historyParams.page"
            :page-sizes="[5, 10, 20, 50]"
            :page-size="historyParams.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="historyTotal"
            style="margin-top: 20px; text-align: right;"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 审批弹出框 -->
    <el-dialog
      :title="approveDialogTitle"
      :visible.sync="approveVisible"
      width="500px"
      @close="closeApproveDialog"
    >
      <el-form :model="approveForm" ref="approveForm" :rules="approveRules" label-width="80px">
        <el-form-item label="设备信息">
          <span>{{ selectedClaim.equipName || '未知设备' }} ({{ selectedClaim.equipId }})</span>
        </el-form-item>
        <el-form-item label="申请人">
          <span>{{ selectedClaim.applicant }}</span>
        </el-form-item>
        <el-form-item label="申请原因">
          <span style="color: #666;">{{ selectedClaim.remark || '无' }}</span>
        </el-form-item>
        <el-form-item label="审批意见" prop="remark">
          <el-input
            type="textarea"
            v-model="approveForm.remark"
            placeholder="请输入审批意见 (选填)"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="approveVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitApprove">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getClaims, cancelClaim, approveClaim } from '@/api/claim'
import { getClaimStatusMeta } from '@/utils/uiStatus'

export default {
  name: 'EquipmentClaim',
  data() {
    return {
      role: null,
      username: '',
      realName: '',
      loading: false,
      submitLoading: false,

      // 操作员参数
      operatorClaims: [],
      total: 0,
      queryParams: {
        page: 1,
        pageSize: 10
      },

      // 管理员参数
      activeTab: 'pending',
      
      // 待审批列表
      pendingClaims: [],
      pendingTotal: 0,
      pendingParams: {
        status: 0, // 仅待审批
        page: 1,
        pageSize: 10
      },

      // 历史记录
      historyClaims: [],
      historyTotal: 0,
      historyParams: {
        equipId: '',
        status: undefined,
        page: 1,
        pageSize: 10
      },

      // 审批弹窗
      approveVisible: false,
      approveDialogTitle: '审批领用申请',
      selectedClaim: {},
      approveAction: 1, // 1-同意, 2-拒绝
      approveForm: {
        remark: ''
      },
      approveRules: {
        remark: []
      }
    }
  },
  computed: {
    highlightedClaimId() {
      const claimId = this.$route.query.claimId
      return claimId ? String(claimId) : ''
    },
    activeTabLabel() {
      return this.activeTab === 'pending' ? '待审批列表' : '审计历史记录'
    },
    displayOperatorClaims() {
      return this.sortClaims(this.operatorClaims)
    },
    displayPendingClaims() {
      return this.sortClaims(this.pendingClaims)
    },
    displayHistoryClaims() {
      return this.sortClaims(this.historyClaims)
    },
    hasHighlightedOperatorClaim() {
      return this.operatorClaims.some(row => String(row.claimId) === this.highlightedClaimId)
    },
    hasHighlightedPendingClaim() {
      return this.pendingClaims.some(row => String(row.claimId) === this.highlightedClaimId)
    },
    hasHighlightedHistoryClaim() {
      return this.historyClaims.some(row => String(row.claimId) === this.highlightedClaimId)
    }
  },
  watch: {
    '$route.query.claimId': {
      immediate: true,
      handler() {
        this.syncTabWithRoute()
      }
    }
  },
  created() {
    this.role = parseInt(localStorage.getItem('role') || '0', 10)
    this.username = localStorage.getItem('username') || ''
    this.realName = localStorage.getItem('realName') || ''

    this.initData()
  },
  methods: {
    initData() {
      if (this.role === 0) {
        this.fetchOperatorClaims()
      } else {
        this.syncTabWithRoute()
        this.fetchPendingClaims()
        this.fetchHistoryClaims()
      }
    },
    syncTabWithRoute() {
      if (!this.highlightedClaimId || this.role === 0) {
        return
      }
      if (this.hasHighlightedPendingClaim) {
        this.activeTab = 'pending'
        return
      }
      if (this.hasHighlightedHistoryClaim) {
        this.activeTab = 'history'
      }
    },
    sortClaims(rows) {
      const claimId = this.highlightedClaimId
      if (!claimId) {
        return [...rows]
      }
      return [...rows].sort((left, right) => {
        if (String(left.claimId) === claimId) {
          return -1
        }
        if (String(right.claimId) === claimId) {
          return 1
        }
        return 0
      })
    },
    scrollToHighlightedRow() {
      this.$nextTick(() => {
        const highlightedRow = this.$el.querySelector('.el-table__body-wrapper tbody tr.highlight-row')
        if (highlightedRow && typeof highlightedRow.scrollIntoView === 'function') {
          highlightedRow.scrollIntoView({
            behavior: 'smooth',
            block: 'center'
          })
        }
      })
    },
    // 操作员查询自己的申请
    async fetchOperatorClaims() {
      this.loading = true
      try {
        // 后端接口对于操作员自动进行隔离，只拉取自己的记录，或通过 query 参数
        const params = {
          page: this.queryParams.page,
          pageSize: this.queryParams.pageSize
        }
        const res = await getClaims(params)
        if (res) {
          this.operatorClaims = res.rows || []
          this.total = res.total || 0
          if (this.hasHighlightedOperatorClaim) {
            this.scrollToHighlightedRow()
          }
        }
      } catch (error) {
        console.error('获取领用申请失败', error)
      } finally {
        this.loading = false
      }
    },
    // 管理员获取待审批列表
    async fetchPendingClaims() {
      this.loading = true
      try {
        const res = await getClaims(this.pendingParams)
        if (res) {
          this.pendingClaims = res.rows || []
          this.pendingTotal = res.total || 0
          this.syncTabWithRoute()
          if (this.hasHighlightedPendingClaim && this.activeTab === 'pending') {
            this.scrollToHighlightedRow()
          }
        }
      } catch (error) {
        console.error('获取待审批记录失败', error)
      } finally {
        this.loading = false
      }
    },
    // 管理员获取审计历史记录
    async fetchHistoryClaims() {
      this.loading = true
      try {
        const res = await getClaims(this.historyParams)
        if (res) {
          this.historyClaims = res.rows || []
          this.historyTotal = res.total || 0
          this.syncTabWithRoute()
          if (this.hasHighlightedHistoryClaim && this.activeTab === 'history') {
            this.scrollToHighlightedRow()
          }
        }
      } catch (error) {
        console.error('获取审计历史失败', error)
      } finally {
        this.loading = false
      }
    },
    handleTabClick(tab) {
      if (tab.name === 'pending') {
        this.pendingParams.page = 1
        this.fetchPendingClaims()
      } else if (tab.name === 'history') {
        this.historyParams.page = 1
        this.fetchHistoryClaims()
      }
    },
    // 操作员操作
    handleCancel(row) {
      this.$confirm('确定要撤回该设备领用申请吗?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await cancelClaim(row.claimId)
          this.$message.success('申请撤回成功')
          this.fetchOperatorClaims()
        } catch (error) {
          console.error('撤回申请失败', error)
        }
      }).catch(() => {})
    },
    // 审批弹框打开
    openApproveDialog(row, action) {
      this.selectedClaim = row
      this.approveAction = action
      this.approveDialogTitle = action === 1 ? '同意领用申请' : '拒绝领用申请'
      this.approveForm.remark = ''
      this.approveVisible = true
    },
    closeApproveDialog() {
      this.approveVisible = false
      this.selectedClaim = {}
    },
    // 提交审批意见
    async submitApprove() {
      this.submitLoading = true
      try {
        const params = {
          action: this.approveAction,
          remark: this.approveForm.remark
        }
        await approveClaim(this.selectedClaim.claimId, params)
        this.$message.success('审批操作成功')
        this.approveVisible = false
        this.fetchPendingClaims()
      } catch (error) {
        console.error('提交审批失败', error)
      } finally {
        this.submitLoading = false
      }
    },
    // 历史搜索
    handleHistorySearch() {
      this.historyParams.page = 1
      this.fetchHistoryClaims()
    },
    resetHistorySearch() {
      this.historyParams = {
        equipId: '',
        status: undefined,
        page: 1,
        pageSize: 10
      }
      this.fetchHistoryClaims()
    },
    // 分页处理
    handleOperatorSizeChange(val) {
      this.queryParams.pageSize = val
      this.fetchOperatorClaims()
    },
    handleOperatorCurrentChange(val) {
      this.queryParams.page = val
      this.fetchOperatorClaims()
    },
    handlePendingSizeChange(val) {
      this.pendingParams.pageSize = val
      this.fetchPendingClaims()
    },
    handlePendingCurrentChange(val) {
      this.pendingParams.page = val
      this.fetchPendingClaims()
    },
    handleHistorySizeChange(val) {
      this.historyParams.pageSize = val
      this.fetchHistoryClaims()
    },
    handleHistoryCurrentChange(val) {
      this.historyParams.page = val
      this.fetchHistoryClaims()
    },
    // 格式化与辅助方法
    formatRole(role) {
      const roleMap = {
        0: '操作员',
        1: '维修工',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] || '未知角色'
    },
    roleTagType(role) {
      const tagMap = {
        0: 'info',
        1: 'warning',
        2: 'primary',
        3: 'success'
      }
      return tagMap[role] || ''
    },
    formatStatus(status) {
      return getClaimStatusMeta(status).label
    },
    statusTagType(status) {
      return getClaimStatusMeta(status).type
    },
    tableRowClassName({ row }) {
      if (this.highlightedClaimId && String(row.claimId) === this.highlightedClaimId) {
        return 'highlight-row'
      }
      return ''
    }
  }
}
</script>

<style scoped>
.header-card {
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);
  border: 1px solid #e4e7ed;
}

.header-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
}

.header-subtitle {
  margin: 0;
  color: #7a8797;
  font-size: 12px;
  line-height: 1.5;
}

.summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.summary-chip {
  min-width: 148px;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #e6ebf2;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-chip strong {
  color: #1f2d3d;
  font-size: 14px;
}

.summary-chip.is-primary {
  border-color: #d9ecff;
  background: #edf6ff;
}

.summary-chip.is-warning {
  border-color: #faecd8;
  background: #fff8ee;
}

.summary-chip-label {
  color: #7a8797;
  font-size: 12px;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title-text {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.card-title-text i {
  color: #409eff;
  margin-right: 5px;
}

.filter-container {
  background: #fcfcfc;
  padding: 10px 15px 0 15px;
  border-radius: 4px;
  border: 1px dashed #e4e7ed;
}

.inline-alert {
  margin-bottom: 15px;
}

.table-empty-state {
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  padding: 18px;
  background: #fafbfd;
}

::v-deep .el-table .highlight-row {
  background: #fdf6ec !important;
}

::v-deep .el-table .highlight-row > td {
  background: #fdf6ec !important;
}

@media (max-width: 1280px) {
  .header-info {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
