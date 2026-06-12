<template>
  <div class="user-manage-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span class="card-title"><i class="el-icon-user-solid"></i> 用户权限管理</span>
      </div>

      <!-- 搜索与筛选区域 -->
      <div class="filter-wrapper">
        <el-form :inline="true" :model="filterForm" size="small" style="margin-bottom: -10px;">
          <el-form-item label="模糊搜索">
            <el-input
              v-model="filterForm.query"
              placeholder="输入用户名 / 真实姓名"
              prefix-icon="el-icon-search"
              clearable
              style="width: 220px;"
            />
          </el-form-item>
          <el-form-item label="系统角色">
            <el-select v-model="filterForm.role" placeholder="全部角色" clearable style="width: 140px;">
              <el-option
                v-for="item in roleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属单位">
            <el-select v-model="filterForm.unitCode" placeholder="全部单位" clearable style="width: 160px;">
              <el-option
                v-for="item in departments"
                :key="item.unitCode"
                :label="item.unitName"
                :value="item.unitCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button icon="el-icon-refresh" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户列表表格 -->
      <el-table
        v-loading="loading"
        :data="filteredUsers"
        style="width: 100%"
        border
        stripe
      >
        <el-table-column prop="id" label="用户 ID" width="80" align="center"></el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="120"></el-table-column>
        
        <el-table-column label="系统角色" width="140" align="center">
          <template slot-scope="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">
              {{ formatRole(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属单位" min-width="140" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.role === 3" type="info">全局角色</el-tag>
            <span v-else>{{ getDeptName(scope.row.unitCode) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" min-width="160" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="updateTime" label="更新时间" min-width="160" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.updateTime) }}
          </template>
        </el-table-column>

        <el-table-column label="角色分配操作" width="160" align="center">
          <template slot-scope="scope">
            <!-- 限制不能修改自己（防止管理员把自己权限改成操作员导致无法管理） -->
            <el-select
              v-model="scope.row.role"
              :disabled="scope.row.username === currentUsername"
              placeholder="修改角色"
              size="small"
              @change="handleRoleChange(scope.row)"
            >
              <el-option
                v-for="item in roleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              ></el-option>
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="单位分配操作" width="160" align="center">
          <template slot-scope="scope">
            <el-select
              v-model="scope.row.unitCode"
              :disabled="scope.row.role === 3 || scope.row.username === currentUsername"
              placeholder="分配单位"
              size="small"
              @change="handleDeptChange(scope.row)"
            >
              <el-option
                v-for="item in departments"
                :key="item.unitCode"
                :label="item.unitName"
                :value="item.unitCode"
              ></el-option>
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getUsers, updateUserRole } from '@/api/user'
import { getDepts } from '@/api/department'

export default {
  name: 'UserManage',
  data() {
    return {
      users: [],
      departments: [],
      loading: false,
      currentUsername: localStorage.getItem('username') || '',
      filterForm: {
        query: '',
        role: '',
        unitCode: ''
      },
      roleOptions: [
        { value: 0, label: '设备操作员' },
        { value: 1, label: '维修工程师' },
        { value: 2, label: '资产管理员' },
        { value: 3, label: '系统管理员' }
      ]
    }
  },
  computed: {
    filteredUsers() {
      return this.users.filter(u => {
        const matchesQuery = !this.filterForm.query ||
          (u.username && u.username.toLowerCase().includes(this.filterForm.query.toLowerCase())) ||
          (u.realName && u.realName.toLowerCase().includes(this.filterForm.query.toLowerCase()));
        
        const matchesRole = this.filterForm.role === '' || this.filterForm.role === null || u.role === this.filterForm.role;
        
        const matchesDept = !this.filterForm.unitCode || u.unitCode === this.filterForm.unitCode;
        
        return matchesQuery && matchesRole && matchesDept;
      })
    }
  },
  created() {
    this.fetchUsers()
    this.fetchDepartments()
  },
  methods: {
    // 重置筛选条件
    resetFilters() {
      this.filterForm.query = ''
      this.filterForm.role = ''
      this.filterForm.unitCode = ''
    },
    // 获取所有用户列表
    fetchUsers() {
      this.loading = true
      getUsers()
        .then(data => {
          this.loading = false
          // 如果返回的数据是数组，则赋值
          if (Array.isArray(data)) {
            this.users = data
          } else if (data && Array.isArray(data.list)) {
            this.users = data.list
          } else {
            this.users = []
          }
        })
        .catch(() => {
          this.loading = false
        })
    },
    // 获取所有部门列表
    fetchDepartments() {
      getDepts()
        .then(data => {
          this.departments = data || []
        })
        .catch(() => {})
    },
    // 根据单位代码映射单位名称
    getDeptName(unitCode) {
      if (!unitCode) return '未分配'
      const dept = this.departments.find(d => d.unitCode === unitCode)
      return dept ? dept.unitName : unitCode
    },
    // 处理修改用户角色
    handleRoleChange(row) {
      this.$confirm(`确定要将用户 "${row.username}" 的角色修改为 "${this.formatRole(row.role)}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.loading = true
          // 提交 PUT 请求到后端，角色为管理员时单位自动重置为空
          const unitCode = row.role === 3 ? null : row.unitCode
          updateUserRole({ id: row.id, role: row.role, unitCode })
            .then(() => {
              this.$message({
                type: 'success',
                message: '角色修改成功!'
              })
              this.fetchUsers()
            })
            .catch(() => {
              this.loading = false
              // 如果修改失败，重新获取列表以还原选择器的值
              this.fetchUsers()
            })
        })
        .catch(() => {
          // 取消修改，重新拉取还原下拉框的值
          this.fetchUsers()
        })
    },
    // 处理修改用户所属单位
    handleDeptChange(row) {
      this.$confirm(`确定要将用户 "${row.username}" 的所属单位修改为 "${this.getDeptName(row.unitCode)}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.loading = true
          updateUserRole({ id: row.id, role: row.role, unitCode: row.unitCode })
            .then(() => {
              this.$message({
                type: 'success',
                message: '所属单位修改成功!'
              })
              this.fetchUsers()
            })
            .catch(() => {
              this.loading = false
              this.fetchUsers()
            })
        })
        .catch(() => {
          this.fetchUsers()
        })
    },
    // 角色格式化
    formatRole(role) {
      const roleMap = {
        0: '设备操作员',
        1: '维修工程师',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : '未知角色'
    },
    // 角色对应标签类型
    getRoleTagType(role) {
      const tagMap = {
        0: 'info',
        1: 'warning',
        2: 'primary',
        3: 'success'
      }
      return tagMap[role] || ''
    },
    // 格式化时间戳/时间字符串
    formatTime(time) {
      if (!time) return '--'
      const date = new Date(time)
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
    }
  }
}
</script>

<style scoped>
.user-manage-container {
  padding: 10px;
}

.box-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.clearfix::after,
.clearfix::before {
  display: table;
  content: "";
}
.clearfix::after {
  clear: both;
}

.filter-wrapper {
  margin-bottom: 15px;
  background-color: #fcfcfd;
  padding: 14px 14px 0 14px;
  border-radius: 6px;
  border: 1px dashed #e2e8f0;
}
</style>
