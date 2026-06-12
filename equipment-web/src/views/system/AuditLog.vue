<template>
  <div class="audit-log-container">
    <!-- 头部卡片与标题 -->
    <el-card class="filter-card" style="margin-bottom: 20px;">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold; font-size: 16px;"><i class="el-icon-search"></i> 操作审计检索</span>
      </div>
      <!-- 过滤表单 -->
      <el-form :inline="true" :model="queryParams" size="small" class="demo-form-inline">
        <el-form-item label="操作人">
          <el-input v-model="queryParams.operator" placeholder="请输入真实姓名/账号" clearable style="width: 180px;"></el-input>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.opType" placeholder="请选择" clearable style="width: 150px;">
            <el-option label="设备新增" value="设备新增"></el-option>
            <el-option label="设备修改" value="设备修改"></el-option>
            <el-option label="设备删除" value="设备删除"></el-option>
            <el-option label="领用申请" value="领用申请"></el-option>
            <el-option label="领用审批" value="领用审批"></el-option>
            <el-option label="设备退还" value="设备退还"></el-option>
            <el-option label="维修申报" value="维修申报"></el-option>
            <el-option label="指派检修" value="指派检修"></el-option>
            <el-option label="维修完成" value="维修完成"></el-option>
            <el-option label="设备调拨" value="设备调拨"></el-option>
            <el-option label="设备报废" value="设备报废"></el-option>
            <el-option label="数据库备份" value="数据库备份"></el-option>
            <el-option label="数据库恢复" value="数据库恢复"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="结果状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px;">
            <el-option label="成功" :value="1"></el-option>
            <el-option label="失败" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            align="right"
            unlink-panels
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            :picker-options="pickerOptions"
            style="width: 250px;"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格卡片 -->
    <el-card class="table-card">
      <el-table v-loading="loading" :data="logList" border style="width: 100%" size="small">
        <el-table-column prop="opTime" label="操作时间" width="160" align="center"></el-table-column>
        <el-table-column prop="operatorRealName" label="操作人姓名" width="120" align="center"></el-table-column>
        <el-table-column prop="operator" label="操作账号" width="120" align="center"></el-table-column>
        <el-table-column prop="operatorRole" label="操作角色" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="getRoleTagType(scope.row.operatorRole)" size="mini">
              {{ formatRole(scope.row.operatorRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opType" label="操作类型" width="120" align="center"></el-table-column>
        <el-table-column prop="targetType" label="业务类型" width="120" align="center"></el-table-column>
        <el-table-column prop="targetId" label="业务ID" width="120" align="center"></el-table-column>
        <el-table-column prop="summary" label="操作摘要" min-width="200"></el-table-column>
        <el-table-column label="执行结果" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ scope.row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <el-pagination
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="queryParams.page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="queryParams.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 20px; text-align: right;"
      >
      </el-pagination>
    </el-card>
  </div>
</template>

<script>
import { getAuditLogs } from '@/api/audit'

export default {
  name: 'AuditLog',
  data() {
    return {
      loading: false,
      logList: [],
      total: 0,
      dateRange: [],
      queryParams: {
        page: 1,
        pageSize: 10,
        operator: '',
        opType: '',
        status: undefined,
        begin: '',
        end: ''
      },
      pickerOptions: {
        shortcuts: [
          {
            text: '最近一周',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近三个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              picker.$emit('pick', [start, end])
            }
          }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      // 解析日期范围
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.begin = this.dateRange[0]
        this.queryParams.end = this.dateRange[1]
      } else {
        this.queryParams.begin = ''
        this.queryParams.end = ''
      }

      try {
        const data = await getAuditLogs(this.queryParams)
        if (data) {
          this.logList = data.rows || []
          this.total = data.total || 0
        }
      } catch (error) {
        this.$message.error('请求接口失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.queryParams.page = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams = {
        page: 1,
        pageSize: 10,
        operator: '',
        opType: '',
        status: undefined,
        begin: '',
        end: ''
      }
      this.getList()
    },
    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.getList()
    },
    handleCurrentChange(val) {
      this.queryParams.page = val
      this.getList()
    },
    formatRole(role) {
      const roleMap = {
        0: '操作员',
        1: '维修工',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : '未知'
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

<style scoped>
.audit-log-container {
  padding: 10px;
}
.filter-card, .table-card {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  border-radius: 4px;
}
</style>
