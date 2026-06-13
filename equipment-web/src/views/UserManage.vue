<template>
  <div class="user-manage-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span class="card-title"><i class="el-icon-user-solid"></i> 用户权限管理</span>
      </div>

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

      <el-table
        v-loading="loading"
        :data="filteredUsers"
        style="width: 100%"
        border
        stripe
      >
        <el-table-column prop="id" label="用户 ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />

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

        <el-table-column label="账号操作" width="250" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="primary"
              size="mini"
              plain
              @click="openEditDialog(scope.row)"
            >
              修改
            </el-button>
            <el-button
              type="warning"
              size="mini"
              plain
              :disabled="scope.row.username === currentUsername"
              @click="openResetPasswordDialog(scope.row)"
            >
              重置密码
            </el-button>
            <el-button
              type="danger"
              size="mini"
              plain
              :disabled="scope.row.username === currentUsername"
              @click="handleDeleteUser(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      title="修改用户"
      :visible.sync="editDialogVisible"
      width="520px"
      @closed="resetEditForm"
    >
      <el-form
        ref="editForm"
        :model="editForm"
        :rules="editRules"
        label-position="top"
      >
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" clearable />
        </el-form-item>
        <el-form-item label="系统角色" prop="role">
          <el-select v-model="editForm.role" :disabled="isEditingCurrentUser" style="width: 100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单位" prop="unitCode">
          <el-select
            v-model="editForm.unitCode"
            :disabled="editForm.role === 3 || isEditingCurrentUser"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in departments"
              :key="item.unitCode"
              :label="item.unitName"
              :value="item.unitCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitEdit">
          保存修改
        </el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="重置密码"
      :visible.sync="resetPasswordDialogVisible"
      width="420px"
      @closed="resetPasswordForm"
    >
      <div class="reset-password-tip">
        当前将为用户 <strong>{{ passwordTarget.username }}</strong> 重置密码。
      </div>
      <el-form
        ref="passwordForm"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            clearable
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitLoading" @click="submitResetPassword">
          确认重置
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { deleteUser, getUsers, resetUserPassword, updateUserProfile } from '@/api/user'
import { getDepts } from '@/api/department'

export default {
  name: 'UserManage',
  data() {
    const validatePasswordConfirm = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
      } else if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
      } else {
        callback()
      }
    }

    return {
      users: [],
      departments: [],
      loading: false,
      submitLoading: false,
      currentUsername: localStorage.getItem('username') || '',
      editDialogVisible: false,
      resetPasswordDialogVisible: false,
      editForm: {
        id: null,
        username: '',
        realName: '',
        role: null,
        unitCode: ''
      },
      passwordTarget: {
        id: null,
        username: ''
      },
      passwordForm: {
        newPassword: '',
        confirmPassword: ''
      },
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
      ],
      editRules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' },
          { min: 2, max: 20, message: '长度必须在 2 到 20 个字符之间', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择系统角色', trigger: 'change' }
        ],
        unitCode: [
          {
            validator: (rule, value, callback) => {
              if (this.editForm.role !== 3 && !value) {
                callback(new Error('该角色必须绑定所属单位'))
              } else {
                callback()
              }
            },
            trigger: 'change'
          }
        ]
      },
      passwordRules: {
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度必须在 6 到 20 个字符之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validatePasswordConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    isEditingCurrentUser() {
      return this.editForm.username === this.currentUsername
    },
    filteredUsers() {
      return this.users.filter(u => {
        const matchesQuery = !this.filterForm.query ||
          (u.username && u.username.toLowerCase().includes(this.filterForm.query.toLowerCase())) ||
          (u.realName && u.realName.toLowerCase().includes(this.filterForm.query.toLowerCase()))

        const matchesRole = this.filterForm.role === '' || this.filterForm.role === null || u.role === this.filterForm.role
        const matchesDept = !this.filterForm.unitCode || u.unitCode === this.filterForm.unitCode

        return matchesQuery && matchesRole && matchesDept
      })
    }
  },
  created() {
    this.fetchUsers()
    this.fetchDepartments()
  },
  methods: {
    resetFilters() {
      this.filterForm.query = ''
      this.filterForm.role = ''
      this.filterForm.unitCode = ''
    },
    fetchUsers() {
      this.loading = true
      getUsers()
        .then(data => {
          this.loading = false
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
    fetchDepartments() {
      getDepts()
        .then(data => {
          this.departments = data || []
        })
        .catch(() => {})
    },
    getDeptName(unitCode) {
      if (!unitCode) return '未分配'
      const dept = this.departments.find(d => d.unitCode === unitCode)
      return dept ? dept.unitName : unitCode
    },
    openEditDialog(row) {
      this.editForm = {
        id: row.id,
        username: row.username,
        realName: row.realName || '',
        role: row.role,
        unitCode: row.role === 3 ? '' : (row.unitCode || '')
      }
      this.editDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.editForm) {
          this.$refs.editForm.clearValidate()
        }
      })
    },
    resetEditForm() {
      this.submitLoading = false
      if (this.$refs.editForm) {
        this.$refs.editForm.resetFields()
      }
      this.editForm = {
        id: null,
        username: '',
        realName: '',
        role: null,
        unitCode: ''
      }
    },
    submitEdit() {
      this.$refs.editForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.submitLoading = true
        updateUserProfile(this.editForm.id, {
          realName: this.editForm.realName,
          role: this.editForm.role,
          unitCode: this.editForm.role === 3 ? null : this.editForm.unitCode
        })
          .then(() => {
            this.submitLoading = false
            this.editDialogVisible = false
            this.$message({
              type: 'success',
              message: '用户资料修改成功!'
            })
            this.fetchUsers()
          })
          .catch(() => {
            this.submitLoading = false
          })
      })
    },
    openResetPasswordDialog(row) {
      this.passwordTarget = {
        id: row.id,
        username: row.username
      }
      this.resetPasswordDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.passwordForm) {
          this.$refs.passwordForm.clearValidate()
        }
      })
    },
    resetPasswordForm() {
      this.submitLoading = false
      this.passwordTarget = {
        id: null,
        username: ''
      }
      this.passwordForm = {
        newPassword: '',
        confirmPassword: ''
      }
      if (this.$refs.passwordForm) {
        this.$refs.passwordForm.resetFields()
      }
    },
    submitResetPassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.submitLoading = true
        resetUserPassword(this.passwordTarget.id, {
          newPassword: this.passwordForm.newPassword
        })
          .then(() => {
            this.submitLoading = false
            this.resetPasswordDialogVisible = false
            this.$message({
              type: 'success',
              message: '密码重置成功!'
            })
            this.fetchUsers()
          })
          .catch(() => {
            this.submitLoading = false
          })
      })
    },
    handleDeleteUser(row) {
      this.$confirm(
        `确定要删除用户 "${row.username}" 吗？删除后将自动清退其名下保管设备；若仍有关联未完结检修工单，后端会阻止删除。`,
        '危险操作确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
        .then(() => {
          this.loading = true
          deleteUser(row.id)
            .then(() => {
              this.$message({
                type: 'success',
                message: '用户删除成功!'
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
    formatRole(role) {
      const roleMap = {
        0: '设备操作员',
        1: '维修工程师',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : '未知角色'
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
  content: '';
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

.reset-password-tip {
  margin-bottom: 12px;
  color: #606266;
}
</style>
