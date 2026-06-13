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
        <el-table-column label="工单状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="formatStatusType(scope.row.maintStatus)" size="mini">
              {{ formatStatusLabel(scope.row.maintStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="faultDescription" label="故障描述" show-overflow-tooltip min-width="180">
          <template slot-scope="scope">
            {{ scope.row.faultDescription || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="检修内容" show-overflow-tooltip min-width="180">
          <template slot-scope="scope">
            {{ scope.row.maintContent || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="maintCost" label="费用(元)" width="100">
          <template slot-scope="scope">
            {{ scope.row.maintCost | formatMoney }}
          </template>
        </el-table-column>
        <el-table-column prop="maintPerson" label="检修人" width="100" />
        <el-table-column v-if="role !== 3" label="操作" align="center" width="200">
          <template slot-scope="scope">
            <!-- 维修工(1)只能登记名下负责工单，资产管理员(2)可以派工指派或登记完工 -->
            <el-button
              v-if="canEdit(scope.row)"
              size="mini"
              :type="scope.row.maintStatus === 0 ? 'primary' : 'success'"
              @click="handleEdit(scope.row)"
            >
              {{ scope.row.maintStatus === 0 ? '派工指派' : '登记完工' }}
            </el-button>
            <!-- 复核按钮：对于状态为 2 且登录用户是资产管理员 (role=2) 展示 -->
            <el-button
              v-if="scope.row.maintStatus === 2 && role === 2"
              size="mini"
              type="warning"
              @click="handleReview(scope.row)"
              >复核</el-button
            >
            <!-- 只有资产管理员(2)可以删除 -->
            <el-button
              v-if="canDelete(scope.row)"
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

        <el-form-item v-if="!isEdit || form.maintStatus !== 0" label="检修费用" prop="maintCost">
          <el-input-number
            v-model="form.maintCost"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="检修人" prop="maintPersonId">
          <el-select v-model="form.maintPersonId" placeholder="请选择检修人" style="width: 100%" @change="handleMaintainerChange" :disabled="isEdit && form.maintStatus === 1">
            <el-option
              v-for="item in maintainers"
              :key="item.id"
              :label="item.realName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="!isEdit || form.maintStatus !== 0" label="检修内容" prop="maintContent">
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

    <el-dialog
      title="完工复核"
      :visible.sync="reviewDialogVisible"
      width="500px"
      @closed="resetReviewForm"
    >
      <el-form
        :model="reviewForm"
        ref="reviewForm"
        :rules="reviewRules"
        label-width="100px"
        size="small"
      >
        <el-form-item label="复核结论" prop="action">
          <el-radio-group v-model="reviewForm.action">
            <el-radio :label="3">复核通过恢复可用</el-radio>
            <el-radio :label="4">转报废</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="复核意见" prop="reviewComments">
          <el-input
            type="textarea"
            v-model="reviewForm.reviewComments"
            :rows="3"
            placeholder="请输入复核意见"
          />
        </el-form-item>

        <el-form-item
          v-if="reviewForm.action === 4"
          label="报废原因"
          prop="reason"
        >
          <el-input
            type="textarea"
            v-model="reviewForm.reason"
            :rows="3"
            placeholder="请输入报废原因"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="reviewDialogVisible = false">取 消</el-button>
        <el-button
          :type="reviewForm.action === 4 ? 'danger' : 'primary'"
          @click="submitReview"
          :loading="reviewSubmitLoading"
        >
          {{ reviewForm.action === 4 ? '确认转报废' : '批准恢复在用' }}
        </el-button>
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
  assignMaintenance,
  completeMaintenance,
  reviewMaintenance,
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
        maintPersonId: null,
        maintStatus: null,
      },
      rules: {
        equipId: [{ required: true, message: "请选择设备", trigger: "change" }],
        maintDate: [
          { required: true, message: "请选择日期", trigger: "change" },
        ],
        maintContent: [
          {
            validator: (rule, value, callback) => {
              if (this.form.maintStatus !== 0 && !value) {
                callback(new Error("请输入检修内容"));
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
        ],
        maintPersonId: [
          { required: true, message: "请选择检修人", trigger: "change" },
        ],
      },
      reviewDialogVisible: false,
      reviewSubmitLoading: false,
      reviewForm: {
        maintId: null,
        action: 3,
        reviewComments: "",
        reason: "",
      },
      reviewRules: {
        action: [{ required: true, message: "请选择复核结论", trigger: "change" }],
        reviewComments: [{ required: true, message: "请输入复核意见", trigger: "blur" }],
        reason: [
          {
            validator: (rule, value, callback) => {
              if (this.reviewForm.action === 4 && !value) {
                callback(new Error("请输入报废原因"));
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
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
    formatStatusLabel(status) {
      const statusMap = {
        0: "待指派",
        1: "维修中",
        2: "待复核",
        3: "已复核可用",
        4: "转报废",
      };
      return statusMap[status] || "未知";
    },
    formatStatusType(status) {
      const statusMap = {
        0: "info",
        1: "warning",
        2: "primary",
        3: "success",
        4: "danger",
      };
      return statusMap[status] || "info";
    },
    canEdit(row) {
      if (row.maintStatus >= 2) {
        return false;
      }
      if (this.role === 2) {
        return true;
      }
      return this.role === 1 && row.maintStatus === 1 && row.maintPerson === this.realName;
    },
    canDelete(row) {
      return this.role === 2 && row.maintStatus === 0;
    },
    async fetchMaintainers() {
      try {
        const res = await getMaintainers();
        this.maintainers = res || [];
      } catch (error) {
        console.error("获取维修工列表失败", error);
      }
    },
    handleMaintainerChange(val) {
      const maintainer = this.maintainers.find(item => item.id === val);
      if (maintainer) {
        const { realName } = maintainer;
        this.form.maintPerson = realName;
      } else {
        this.form.maintPerson = "";
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
        const { maintId, equipId } = this.form;
        try {
          if (this.isEdit) {
            if (this.form.maintStatus === 0) {
              await assignMaintenance(maintId, this.form);
              this.$message.success("指派成功");
            } else if (this.form.maintStatus === 1) {
              await completeMaintenance(maintId, this.form);
              this.$message.success("登记完工成功");
            } else {
              await updateMaintenance(maintId, this.form);
              this.$message.success("修改成功");
            }
          } else {
            // 补录：对应后端 @PostMapping("/{equipId}")
            await addMaintenance(equipId, this.form);
            this.$message.success("新增成功");
          }
          this.dialogVisible = false;
          this.loadList();
        } finally {
          this.submitLoading = false;
        }
      });
    },
    handleReview(row) {
      this.reviewForm.maintId = row.maintId;
      this.reviewDialogVisible = true;
    },
    submitReview() {
      this.$refs.reviewForm.validate(async (valid) => {
        if (!valid) return;
        this.reviewSubmitLoading = true;
        try {
          await reviewMaintenance(this.reviewForm.maintId, {
            maintStatus: this.reviewForm.action,
            reviewComments: this.reviewForm.action === 4 ? this.reviewForm.reason : this.reviewForm.reviewComments,
            scrapNo: this.reviewForm.action === 4 ? ("SCRAP-" + Date.now()) : null
          });
          this.$message.success("复核完成");
          this.reviewDialogVisible = false;
          this.loadList();
        } finally {
          this.reviewSubmitLoading = false;
        }
      });
    },
    resetReviewForm() {
      if (this.$refs.reviewForm) {
        this.$refs.reviewForm.resetFields();
      }
      this.reviewForm = {
        maintId: null,
        action: 3,
        reviewComments: "",
        reason: "",
      };
    },
    confirmDelete(row) {
      this.$confirm('仅可撤销待指派工单，撤销后设备将恢复为在用，确认继续？', "提示", {
        type: "warning",
      })
        .then(async () => {
          await deleteMaintenance(row.maintId, row.equipId);
          this.$message.success("工单已撤销，设备已恢复为在用");
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
        maintPersonId: null,
      };
    },
  },
};
</script>
