<template>
  <div class="app-container">
    <!-- 顶部卡片：介绍与当前角色展示 -->
    <el-card class="box-card header-card" shadow="hover">
      <div class="header-info">
        <div class="info-title">
          <i class="el-icon-document" style="color: #409eff; margin-right: 8px;"></i>
          <span>设备领用与审批工作流</span>
        </div>
        <div class="info-tag">
          <span style="font-size: 14px; color: #606266; margin-right: 10px;">当前登录用户：{{ realName || username }}</span>
          <el-tag :type="roleTagType(role)" size="small">{{ formatRole(role) }}</el-tag>
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

        <el-table :data="operatorClaims" border v-loading="loading" style="width: 100%">
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

          <el-table :data="pendingClaims" border v-loading="loading" style="width: 100%">
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

          <el-table :data="historyClaims" border v-loading="loading" style="width: 100%">
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
        this.fetchPendingClaims()
      }
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
      const statusMap = {
        0: '待审批',
        1: '已同意',
        2: '已拒绝',
        3: '已撤回',
        4: '已退还',
        5: '直接分配'
      }
      return statusMap[status] !== undefined ? statusMap[status] : '未知'
    },
    statusTagType(status) {
      const tagMap = {
        0: 'warning',
        1: 'success',
        2: 'danger',
        3: 'info',
        4: 'info',
        5: 'primary'
      }
      return tagMap[status] || 'info'
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
  align-items: center;
}

.info-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
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
</style>
