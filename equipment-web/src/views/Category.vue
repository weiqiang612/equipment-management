<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>资产分类管理</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd" style="float: right;">新增分类</el-button>
      </div>

      <el-table :data="categoryList" border stripe v-loading="loading">
        <el-table-column prop="categoryId" label="分类编号" align="center" width="120" />
        <el-table-column prop="categoryName" label="分类名称" align="center" />
        <el-table-column prop="usefulLife" label="折旧年限(年)" align="center" width="150" />
        <el-table-column prop="residualRate" label="残值率(%)" align="center" width="150">
          <template slot-scope="scope">
            {{ scope.row.residualRate * 100 }}%
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="450px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类编号">
          <el-input v-model="form.categoryId" :disabled="isEdit" placeholder="如: C01"></el-input>
        </el-form-item>
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" placeholder="如: 电子设备"></el-input>
        </el-form-item>
        <el-form-item label="折旧年限">
          <el-input-number v-model="form.usefulLife" :min="1" label="年限"></el-input-number>
        </el-form-item>
        <el-form-item label="残值率">
          <el-input-number v-model="form.residualRate" :precision="2" :step="0.01" :max="1" label="残值率"></el-input-number>
          <span style="color: #999; margin-left: 10px;">(0.05 代表 5%)</span>
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
import { getCategories, addCategory, updateCategory, deleteCategory } from '@/api/category'

export default {
  data() {
    return {
      categoryList: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      form: {
        categoryId: '',
        categoryName: '',
        usefulLife: 5,
        residualRate: 0.05
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      getCategories().then(data => {
        this.categoryList = data // 你的拦截器已处理 Result.data
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.isEdit = false
      this.dialogTitle = '新增资产分类'
      this.form = { categoryId: '', categoryName: '', usefulLife: 5, residualRate: 0.05 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑资产分类'
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      const action = this.isEdit ? updateCategory : addCategory
      action(this.form).then(() => {
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.fetchData()
      })
    },
    handleDelete(row) {
      // 增加 .catch 处理取消逻辑，避免 Uncaught runtime error
      this.$confirm(`确定删除分类【${row.categoryName}】吗？`, '警告', {
        type: 'warning'
      }).then(() => {
        deleteCategory(row.categoryId).then(() => {
          this.$message.success('删除成功')
          this.fetchData()
        })
      }).catch(() => {
        // 捕获取消动作，不再报错
        console.log('用户取消了删除')
      })
    }
  }
}
</script>