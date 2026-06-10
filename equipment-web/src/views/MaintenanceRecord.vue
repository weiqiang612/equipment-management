<template>
  <div class="maintenance-container">
    <el-card shadow="never" style="margin-top: 20px">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold">设备检修记录管理</span>
        <el-button
          v-if="role === 2"
          type="primary"
          icon="el-icon-plus"
          size="small"
          @click="openAddDialog"
          style="float: right"
          >新增检修补录</el-button
        >
      </div>

      <el-table
        :data="tableData"
        border
        v-loading="loading"
        size="small"
        stripe
      >
        <el-table-column
          prop="maintId"
          label="单号"
          width="80"
          align="center"
        />
        <el-table-column prop="equipId" label="设备编号" width="120" />
        <el-table-column prop="equipName" label="设备名称" />
        <el-table-column prop="maintDate" label="检修日期" width="130" />
        <el-table-column
          prop="maintContent"
          label="内容"
          show-overflow-tooltip
        />
        <el-table-column prop="maintCost" label="费用(元)" width="100">
          <template slot-scope="scope">
            {{ scope.row.maintCost | formatMoney }}
          </template>
        </el-table-column>
        <el-table-column prop="maintPerson" label="检修人" width="100" />
        <el-table-column v-if="role !== 3" label="操作" align="center" width="160">
          <template slot-scope="scope">
            <!-- 维修工(1)只能修改自己负责的名下工单，资产管理员(2)可以修改所有 -->
            <el-button
              v-if="role === 2 || (role === 1 && scope.row.maintPerson === realName)"
              size="mini"
              type="primary"
              @click="handleEdit(scope.row)"
              >修改</el-button
            >
            <!-- 只有资产管理员(2)可以删除 -->
            <el-button
              v-if="role === 2"
              size="mini"
              type="danger"
              @click="confirmDelete(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="isEdit ? '修改检修记录' : '新增检修补录'"
      :visible.sync="dialogVisible"
      width="500px"
      @closed="resetForm"
    >
      <el-form
        :model="form"
        ref="maintForm"
        :rules="rules"
        label-width="100px"
        size="small"
      >
        <el-form-item v-if="!isEdit" label="选择设备" prop="equipId">
          <el-select
            v-model="form.equipId"
            filterable
            placeholder="输入编号或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in equipOptions"
              :key="item.equipId"
              :label="`${item.equipName} (${item.equipId})`"
              :value="item.equipId"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-else label="设备编号">
          <el-input v-model="form.equipId" disabled />
        </el-form-item>

        <el-form-item label="检修日期" prop="maintDate">
          <el-date-picker
            v-model="form.maintDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="检修费用" prop="maintCost">
          <el-input-number
            v-model="form.maintCost"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="检修人" prop="maintPerson">
          <el-select v-model="form.maintPerson" placeholder="请选择检修人" style="width: 100%">
            <el-option
              v-for="item in maintainers"
              :key="item.id"
              :label="item.realName"
              :value="item.realName"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="检修内容" prop="maintContent">
          <el-input type="textarea" v-model="form.maintContent" :rows="3" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading"
          >确 定</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getMaintenanceList,
  addMaintenance,
  updateMaintenance,
  deleteMaintenance,
} from "@/api/MaintenanceRecord";
import { getEquipments } from "@/api/equipment";
import { getMaintainers } from "@/api/user";

export default {
  data() {
    return {
      role: null,
      realName: "",
      maintainers: [], // 存储维修工列表
      loading: false,
      submitLoading: false,
      tableData: [],
      equipOptions: [],
      dialogVisible: false,
      isEdit: false,
      form: {
        maintId: null,
        equipId: "",
        maintDate: "",
        maintContent: "",
        maintCost: 0,
        maintPerson: "",
      },
      rules: {
        equipId: [{ required: true, message: "请选择设备", trigger: "change" }],
        maintDate: [
          { required: true, message: "请选择日期", trigger: "change" },
        ],
        maintContent: [
          { required: true, message: "请输入检修内容", trigger: "blur" },
        ],
        maintPerson: [
          { required: true, message: "请选择检修人", trigger: "change" },
        ],
      },
    };
  },
  filters: {
    formatMoney(val) {
      return val ? `￥${Number(val).toFixed(2)}` : "￥0.00";
    },
  },
  created() {
    const roleStr = localStorage.getItem("role");
    this.role = roleStr !== null ? parseInt(roleStr, 10) : null;
    this.realName = localStorage.getItem("realName") || "";
    this.loadList();
    this.fetchMaintainers();
  },
  methods: {
    async fetchMaintainers() {
      try {
        const res = await getMaintainers();
        this.maintainers = res || [];
      } catch (error) {
        console.error("获取维修工列表失败", error);
      }
    },
    async loadList() {
      this.loading = true;
      try {
        this.tableData = await getMaintenanceList();
      } finally {
        this.loading = false;
      }
    },
    // 打开新增弹窗并加载设备列表
    async openAddDialog() {
      this.isEdit = false;
      this.dialogVisible = true;
      try {
        // 加载“在用”和“检修中”设备供补录选择
        const res = await getEquipments({ pageSize: 1000 });
        this.equipOptions = res.rows || res || [];
      } catch (error) {
        this.$message.error("加载设备列表失败");
      }
      this.form.maintDate = new Date().toISOString().split("T")[0];
      this.fetchMaintainers();
    },
    handleEdit(row) {
      this.isEdit = true;
      this.form = { ...row };
      this.fetchMaintainers();
      this.dialogVisible = true;
    },
    async submitForm() {
      this.$refs.maintForm.validate(async (valid) => {
        if (!valid) return;
        this.submitLoading = true;
        try {
          if (this.isEdit) {
            await updateMaintenance(this.form.maintId, this.form);
            this.$message.success("修改成功");
          } else {
            // 补录：对应后端 @PostMapping("/{equipId}")
            await addMaintenance(this.form.equipId, this.form);
            this.$message.success("新增成功");
          }
          this.dialogVisible = false;
          this.loadList();
        } finally {
          this.submitLoading = false;
        }
      });
    },
    confirmDelete(row) {
      this.$confirm('删除记录将尝试恢复设备为"在用"状态，确定吗？', "提示", {
        type: "warning",
      })
        .then(async () => {
          await deleteMaintenance(row.maintId, row.equipId);
          this.$message.success("已删除并恢复状态");
          this.loadList();
        })
        .catch(() => {});
    },
    resetForm() {
      if (this.$refs.maintForm) {
        this.$refs.maintForm.resetFields();
      }
      this.form = {
        maintId: null,
        equipId: "",
        maintDate: "",
        maintContent: "",
        maintCost: 0,
        maintPerson: "",
      };
    },
  },
};
</script>
