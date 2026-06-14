<template>
  <div class="maintenance-container">
    <el-card shadow="never" class="maintenance-card">
      <div slot="header" class="page-header">
        <div class="page-header-main">
          <div class="page-title-block">
            <span class="page-title">设备检修记录管理</span>
            <span class="page-subtitle">聚焦待指派、待完工和待复核工单，消息跳转后优先展示目标记录。</span>
          </div>
          <el-button
            v-if="role === 2"
            type="primary"
            icon="el-icon-plus"
            size="small"
            @click="openAddDialog"
          >
            新增检修补录
          </el-button>
        </div>

        <div v-if="showQuickFilters" class="quick-filter-bar">
          <button
            type="button"
            class="quick-filter-chip"
            :class="{ 'is-active': activeQuickFilter === 'all' }"
            @click="setQuickFilter('all')"
          >
            <span class="chip-label">全部工单</span>
            <span class="chip-count">{{ filteredCounts.all }}</span>
          </button>
          <button
            v-for="item in quickFilterOptions"
            :key="item.key"
            type="button"
            class="quick-filter-chip"
            :class="[`is-${item.tone}`, { 'is-active': activeQuickFilter === item.key }]"
            @click="setQuickFilter(item.key)"
          >
            <span class="chip-label">{{ item.label }}</span>
            <span class="chip-count">{{ filteredCounts[item.key] }}</span>
          </button>
        </div>

        <el-alert
          v-if="highlightedMaintId && !hasHighlightedRow && !loading"
          title="目标工单不存在或已不在当前列表，已为您保留当前视图以继续处理其他工单。"
          type="warning"
          :closable="false"
          show-icon
          class="inline-alert"
        />
      </div>

      <el-table
        v-if="displayTableData.length > 0"
        ref="maintenanceTable"
        :data="displayTableData"
        border
        v-loading="loading"
        size="small"
        stripe
        :row-class-name="tableRowClassName"
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
            <el-popover
              placement="right-start"
              title="工单流转时间线"
              width="250"
              trigger="hover"
              popper-class="maint-popover"
              :open-delay="100"
              :close-delay="100"
              :disabled="!scope.row.assignTime && !scope.row.completeTime && !scope.row.reviewDate"
            >
              <el-timeline size="mini" class="maint-timeline">
                <el-timeline-item :timestamp="scope.row.maintDate" placement="top">
                  发起报修 (报修人: {{ scope.row.reporter || '-' }})
                </el-timeline-item>
                <el-timeline-item 
                  v-if="scope.row.assignTime" 
                  :timestamp="formatDateTime(scope.row.assignTime)" 
                  placement="top" 
                  type="primary"
                >
                  工单指派 (维修工: {{ scope.row.maintPerson || '-' }})
                </el-timeline-item>
                <el-timeline-item 
                  v-if="scope.row.completeTime" 
                  :timestamp="formatDateTime(scope.row.completeTime)" 
                  placement="top" 
                  type="success"
                >
                  登记完工 (费用: ￥{{ scope.row.maintCost || 0 }})
                </el-timeline-item>
                <el-timeline-item 
                  v-if="scope.row.reviewDate" 
                  :timestamp="formatDateTime(scope.row.reviewDate)" 
                  placement="top" 
                  :type="scope.row.maintStatus === 4 ? 'danger' : 'warning'"
                >
                  {{ scope.row.maintStatus === 4 ? '复核转报废' : '复核通过 (恢复在用)' }}
                </el-timeline-item>
              </el-timeline>
              <el-tag slot="reference" :type="formatStatusType(scope.row.maintStatus)" size="mini" style="cursor: pointer">
                {{ formatStatusLabel(scope.row.maintStatus) }}
              </el-tag>
            </el-popover>
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
        <el-table-column prop="reviewComments" label="管理员意见" show-overflow-tooltip min-width="150">
          <template slot-scope="scope">
            {{ scope.row.reviewComments || "-" }}
          </template>
        </el-table-column>
        <el-table-column v-if="role !== 3" label="处理" align="center" width="196" fixed="right">
          <template slot-scope="scope">
            <div
              class="action-cell"
              :class="{ 'is-highlighted': isHighlightedRow(scope.row) }"
            >
              <template v-if="hasRowActions(scope.row)">
                <el-button
                  v-if="canEdit(scope.row)"
                  size="mini"
                  :type="scope.row.maintStatus === 0 ? 'primary' : 'success'"
                  @click="handleEdit(scope.row)"
                >
                  {{ scope.row.maintStatus === 0 ? '派工' : '完工' }}
                </el-button>
                <el-button
                  v-if="scope.row.maintStatus === 2 && role === 2"
                  size="mini"
                  type="warning"
                  @click="handleReview(scope.row)"
                >
                  复核
                </el-button>
                <el-button
                  v-if="canDelete(scope.row)"
                  size="mini"
                  type="danger"
                  @click="confirmDelete(scope.row)"
                >
                  删除
                </el-button>
              </template>
              <span v-else class="action-empty-tip">
                {{ getActionHint(scope.row) }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="!loading" class="table-empty-state">
        <el-empty :description="emptyStateDescription" :image-size="108" />
        <div class="table-empty-actions">
          <el-button size="small" @click="setQuickFilter('all')">查看全部工单</el-button>
          <el-button v-if="role === 2" type="primary" size="small" plain @click="openAddDialog">新增检修补录</el-button>
        </div>
      </div>
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
        <el-alert
          v-if="isEdit && form.maintStatus === 1 && form.reviewComments"
          title="该工单曾被驳回重修"
          type="warning"
          :description="`退回原因：${form.reviewComments}`"
          show-icon
          :closable="false"
          style="margin-bottom: 15px;"
        />
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
            <el-radio :label="1">驳回重新检修</el-radio>
            <el-radio :label="4">转报废</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="复核意见" prop="reviewComments">
          <el-input
            type="textarea"
            v-model="reviewForm.reviewComments"
            :rows="3"
            :placeholder="reviewForm.action === 1 ? '请输入退回重新检修的原因' : '请输入复核意见'"
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
          :type="reviewForm.action === 4 ? 'danger' : (reviewForm.action === 1 ? 'warning' : 'primary')"
          @click="submitReview"
          :loading="reviewSubmitLoading"
        >
          {{ reviewForm.action === 4 ? '确认转报废' : (reviewForm.action === 1 ? '退回重新检修' : '批准恢复在用') }}
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
import { getMaintenanceStatusMeta } from "@/utils/uiStatus";

export default {
  name: "MaintenanceRecord",
  data() {
    return {
      role: null,
      realName: "",
      maintainers: [], // 存储维修工列表
      loading: false,
      submitLoading: false,
      tableData: [],
      equipOptions: [],
      activeQuickFilter: "all",
      tableReady: false,
      autoScrolledMaintId: null,
      pendingScrollMaintId: null,
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
        reviewComments: [
          {
            validator: (rule, value, callback) => {
              if (!value) {
                if (this.reviewForm.action === 1) {
                  callback(new Error("请输入退回重新检修的原因"));
                } else {
                  callback(new Error("请输入复核意见"));
                }
              } else {
                callback();
              }
            },
            trigger: "blur",
          },
        ],
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
  computed: {
    highlightedMaintId() {
      const maintId = this.$route.query.maintId;
      return maintId ? String(maintId) : "";
    },
    showQuickFilters() {
      return this.role === 1 || this.role === 2;
    },
    quickFilterOptions() {
      if (this.role === 1) {
        return [
          { key: "pending-complete", label: "待完工", tone: "success" },
        ];
      }
      if (this.role === 2) {
        return [
          { key: "pending-assign", label: "待指派", tone: "primary" },
          { key: "pending-complete", label: "待完工", tone: "success" },
          { key: "pending-review", label: "待复核", tone: "warning" },
        ];
      }
      return [];
    },
    filteredCounts() {
      return {
        all: this.tableData.length,
        "pending-assign": this.tableData.filter(row => row.maintStatus === 0).length,
        "pending-complete": this.tableData.filter(row => this.isPendingComplete(row)).length,
        "pending-review": this.tableData.filter(row => row.maintStatus === 2).length,
      };
    },
    displayTableData() {
      let rows = [...this.tableData];
      if (this.activeQuickFilter === "pending-assign") {
        rows = rows.filter(row => row.maintStatus === 0);
      } else if (this.activeQuickFilter === "pending-complete") {
        rows = rows.filter(row => this.isPendingComplete(row));
      } else if (this.activeQuickFilter === "pending-review") {
        rows = rows.filter(row => row.maintStatus === 2);
      }
      rows.sort((left, right) => this.sortRows(left, right));
      return rows;
    },
    hasHighlightedRow() {
      if (!this.highlightedMaintId) {
        return false;
      }
      return this.tableData.some(row => String(row.maintId) === this.highlightedMaintId);
    },
    emptyStateDescription() {
      if (this.activeQuickFilter === "pending-assign") {
        return "当前没有待指派工单";
      }
      if (this.activeQuickFilter === "pending-complete") {
        return "当前没有待完工工单";
      }
      if (this.activeQuickFilter === "pending-review") {
        return "当前没有待复核工单";
      }
      return "当前没有检修工单记录";
    },
  },
  watch: {
    '$route.query.maintId': {
      immediate: true,
      handler() {
        this.syncQuickFilterWithRoute();
      },
    },
    displayTableData() {
      this.scheduleScrollToHighlightedRow();
    },
  },
  created() {
    const roleStr = localStorage.getItem("role");
    this.role = roleStr !== null ? parseInt(roleStr, 10) : null;
    this.realName = localStorage.getItem("realName") || "";
    this.syncQuickFilterWithRoute();
    this.loadList();
    this.fetchMaintainers();
  },
  methods: {
    setQuickFilter(filterKey) {
      this.activeQuickFilter = filterKey;
      this.scheduleScrollToHighlightedRow();
    },
    syncQuickFilterWithRoute() {
      const highlightedRow = this.tableData.find(
        row => String(row.maintId) === this.highlightedMaintId
      );
      if (highlightedRow) {
        if (highlightedRow.maintStatus === 0 && this.role === 2) {
          this.activeQuickFilter = "pending-assign";
        } else if (this.isPendingComplete(highlightedRow) && (this.role === 1 || this.role === 2)) {
          this.activeQuickFilter = "pending-complete";
        } else if (highlightedRow.maintStatus === 2 && this.role === 2) {
          this.activeQuickFilter = "pending-review";
        } else {
          this.activeQuickFilter = "all";
        }
      } else if (!this.showQuickFilters || this.activeQuickFilter === "all") {
        this.activeQuickFilter = "all";
      }
      this.pendingScrollMaintId = this.highlightedMaintId || null;
    },
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return "-";
      // 将 "2026-06-13T15:21:40" 转换为 "2026-06-13 15:21"
      return dateTimeStr.replace("T", " ").substring(0, 16);
    },
    formatStatusLabel(status) {
      return getMaintenanceStatusMeta(status).label;
    },
    formatStatusType(status) {
      return getMaintenanceStatusMeta(status).type;
    },
    isPendingComplete(row) {
      if (row.maintStatus !== 1) {
        return false;
      }
      if (this.role === 1) {
        return this.canEdit(row);
      }
      return true;
    },
    sortRows(left, right) {
      const highlightedId = this.highlightedMaintId;
      if (highlightedId) {
        if (String(left.maintId) === highlightedId) {
          return -1;
        }
        if (String(right.maintId) === highlightedId) {
          return 1;
        }
      }
      const leftPriority = this.getRowPriority(left);
      const rightPriority = this.getRowPriority(right);
      if (leftPriority !== rightPriority) {
        return leftPriority - rightPriority;
      }
      return String(right.maintDate || "").localeCompare(String(left.maintDate || ""));
    },
    getRowPriority(row) {
      const statusPriorityMap = {
        0: 1,
        2: 2,
        1: 3,
        4: 4,
        3: 5,
      };
      return statusPriorityMap[row.maintStatus] || 99;
    },
    isHighlightedRow(row) {
      return this.highlightedMaintId && String(row.maintId) === this.highlightedMaintId;
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
    hasRowActions(row) {
      return this.canEdit(row) || (row.maintStatus === 2 && this.role === 2) || this.canDelete(row);
    },
    getActionHint(row) {
      if (row.maintStatus === 3 || row.maintStatus === 4) {
        return "已处理完成";
      }
      if (row.maintStatus === 2) {
        return this.role === 1 ? "等待管理员复核" : "当前阶段无需处理";
      }
      if (row.maintStatus === 1) {
        return this.role === 1 ? "等待本人登记完工" : "等待检修登记完工";
      }
      if (row.maintStatus === 0) {
        return this.role === 1 ? "等待管理员派工" : "当前阶段无需处理";
      }
      return "当前阶段无需处理";
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
        this.syncQuickFilterWithRoute();
        if (this.highlightedMaintId) {
          const hasHighlightedRow = this.tableData.some(
            row => String(row.maintId) === this.highlightedMaintId
          );
          if (!hasHighlightedRow) {
            this.$message.warning("目标工单不存在或已不在当前列表");
          }
        }
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
    tableRowClassName({ row }) {
      if (this.isHighlightedRow(row)) {
        return 'highlight-row';
      }
      return '';
    },
    scheduleScrollToHighlightedRow() {
      if (!this.highlightedMaintId || this.autoScrolledMaintId === this.highlightedMaintId) {
        return;
      }
      this.$nextTick(() => {
        this.scrollToHighlightedRow();
      });
    },
    scrollToHighlightedRow() {
      const highlightedRow = this.$el.querySelector(".el-table__body-wrapper tbody tr.highlight-row");
      if (!highlightedRow) {
        return;
      }
      highlightedRow.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
      this.autoScrolledMaintId = this.highlightedMaintId;
    },
  },
};
</script>

<style>
/* 维保时间线悬浮框全局样式优化 */
.maint-popover {
  pointer-events: none !important; /* 避免悬浮框抢占鼠标焦点导致闪烁与残留 */
  padding: 14px 16px !important;
  border-radius: 8px !important;
  border: 1px solid #ebeef5 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important;
}
.maint-popover .el-popover__title {
  font-size: 13px !important;
  font-weight: bold !important;
  color: #303133 !important;
  margin-bottom: 12px !important;
  padding-bottom: 6px !important;
  border-bottom: 1px solid #ebeef5 !important;
}
.maint-timeline {
  padding: 4px 0 0 4px !important;
}
.maint-timeline .el-timeline-item {
  padding-bottom: 10px !important;
}
.maint-timeline .el-timeline-item__content {
  font-size: 12px !important;
  color: #303133 !important;
  font-weight: bold !important;
}
.maint-timeline .el-timeline-item__timestamp {
  font-size: 11px !important;
  color: #909399 !important;
  margin-top: 4px !important;
}
</style>

<style scoped>
.maintenance-card {
  margin-top: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.page-subtitle {
  font-size: 12px;
  line-height: 1.5;
  color: #7a8797;
}

.inline-alert {
  margin-top: 4px;
}

.quick-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-filter-chip {
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 12px;
  min-width: 120px;
  padding: 12px 14px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-filter-chip:hover {
  border-color: #409eff;
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.quick-filter-chip.is-active {
  border-color: #409eff;
  background: #ecf5ff;
}

.quick-filter-chip.is-success.is-active {
  border-color: #67c23a;
  background: #f0f9eb;
}

.quick-filter-chip.is-warning.is-active {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.chip-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.chip-count {
  min-width: 24px;
  height: 24px;
  border-radius: 12px;
  background: #f4f4f5;
  color: #606266;
  font-size: 12px;
  line-height: 24px;
  text-align: center;
  padding: 0 8px;
  box-sizing: border-box;
}

.quick-filter-chip.is-active .chip-count {
  background: #409eff;
  color: #fff;
}

.quick-filter-chip.is-success.is-active .chip-count {
  background: #67c23a;
}

.quick-filter-chip.is-warning.is-active .chip-count {
  background: #e6a23c;
}

.action-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 44px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.action-cell.is-highlighted {
  background: rgba(64, 158, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.18);
}

.action-cell ::v-deep .el-button + .el-button {
  margin-left: 0;
}

.action-empty-tip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f4f6f8;
  color: #7a8797;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
}

.table-empty-state {
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  padding: 18px;
  background: #fafbfd;
}

.table-empty-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

::v-deep .el-table .highlight-row {
  background: #fdf6ec !important;
}

::v-deep .el-table .highlight-row > td {
  background: #fdf6ec !important;
}

@media (max-width: 1280px) {
  .page-header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .quick-filter-chip {
    min-width: 0;
    flex: 1 1 180px;
  }
}
</style>
