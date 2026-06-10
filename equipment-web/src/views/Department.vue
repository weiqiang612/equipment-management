<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>单位管理</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd" style="float: right;">新增单位</el-button>
      </div>

      <el-table :data="deptList" border stripe v-loading="loading">
        <el-table-column prop="unitCode" label="单位代码" align="center" width="120" />
        <el-table-column prop="unitName" label="单位名称" align="center" />
        <el-table-column prop="manager" label="负责人" align="center" />
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="单位代码">
          <el-input v-model="form.unitCode" :disabled="isEdit" placeholder="请输入单位代码"></el-input>
        </el-form-item>
        <el-form-item label="单位名称">
          <el-input v-model="form.unitName" placeholder="请输入单位名称"></el-input>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" placeholder="请输入负责人姓名"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getDepts, addDept, updateDept, deleteDept } from '@/api/department'

export default {
  data() {
    return {
      deptList: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      form: {
        unitCode: '',
        unitName: '',
        manager: ''
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      getDepts().then(data => {
        this.deptList = data // 这里的 data 已经是 Result.data
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.isEdit = false
      this.dialogTitle = '新增单位'
      this.form = { unitCode: '', unitName: '', manager: '' }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑单位'
      this.form = { ...row } // 浅拷贝，防止直接修改表格行数据
      this.dialogVisible = true
    },
    submitForm() {
      const action = this.isEdit ? updateDept : addDept
      action(this.form).then(() => {
        this.$message.success('操作成功')
        this.dialogVisible = false
        this.fetchData() // 刷新列表
      })
    },
    handleDelete(row) {
        this.$confirm(`确定要删除【${row.unitName}】吗？`, '警告', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
    })
    .then(() => {
        // 用户点击了“确定”
        deleteDept(row.unitCode).then(() => {
        this.$message.success('删除成功');
        this.fetchData();
        });
    })
    .catch(() => {
        // 用户点击了“取消”或点击了遮罩层关闭
        // 这里留空或写一条日志，不再会抛出 Uncaught runtime error
        console.log('用户取消了删除操作');
    });
    }
  }
}
</script>