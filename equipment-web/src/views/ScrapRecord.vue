<template>
  <div class="app-container">
    <el-card shadow="never" style="margin-top: 20px">
      <div slot="header" class="clearfix">
        <span>设备报废记录管理</span>
        <el-button
          type="primary"
          icon="el-icon-plus"
          style="float: right"
          @click="handleCreate"
          >新增报废补录</el-button
        >
      </div>

      <el-table
        :data="scrapList"
        border
        v-loading="loading"
        size="small"
        stripe
      >
        <el-table-column prop="scrapNo" label="报废单号" width="150" />
        <el-table-column prop="equipId" label="设备编号" width="120" />
        <el-table-column prop="scrapDate" label="报废日期" width="120" />
        <el-table-column prop="reason" label="报废原因" show-overflow-tooltip />
        <el-table-column prop="approver" label="审批人" width="100" />
        <el-table-column label="操作" align="center" width="180">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)"
              >修改</el-button
            >
            <el-button
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)"
              >撤销</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="isEdit ? '修改报废记录' : '新增报废补录'"
      :visible.sync="dialogVisible"
      width="500px"
    >
      <el-form :model="form" ref="scrapForm" :rules="rules" label-width="100px" size="small">
        <el-form-item v-if="!isEdit" label="选择设备" prop="equipId">
          <el-select
            v-model="form.equipId"
            filterable
            placeholder="输入编号或名称模糊搜索"
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
        <el-form-item label="报废日期" prop="scrapDate">
          <el-date-picker
            v-model="form.scrapDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报废原因" prop="reason">
          <el-input
            type="textarea"
            v-model="form.reason"
            rows="3"
            placeholder="请输入详细原因"
          />
        </el-form-item>
        <el-form-item label="审批人" prop="approver">
          <el-input v-model="form.approver" placeholder="请输入审批人姓名" />
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
import {
  getScrapList,
  updateScrapRecord,
  deleteScrapRecord,
} from "@/api/ScrapRecord";
// 需要引入设备报废的 API (通常在 equipment.js 中)
import { scrapEquipment, getEquipments } from "@/api/equipment";

export default {
  data() {
    return {
      scrapList: [],
      dialogVisible: false,
      isEdit: false,
      loading: false, // 搜索加载状态
      equipOptions: [], // 存放搜索到的设备下拉列表
      form: {
        scrapNo: "",
        equipId: "",
        scrapDate: "",
        reason: "",
        approver: "",
      },
      rules: {
        equipId: [{ required: true, message: "请选择设备", trigger: "change" }],
        scrapDate: [
          { required: true, message: "请选择日期", trigger: "change" },
        ],
        reason: [
          { required: true, message: "请输入报废原因", trigger: "blur" },
        ],
        approver: [
          { required: true, message: "请输入审批人姓名", trigger: "blur" },
        ],
      },
    };
  },
  created() {
    this.fetchScrapRecords();
  },
  methods: {
    // 远程搜索设备的方法
    async remoteSearchEquip(query) {
      if (query !== "") {
        this.loading = true;
        try {
          // 调用你后端已经实现模糊查询的接口
          // 传参为 equipName，后端逻辑会同时匹配 ID 和 Name
          const res = await getEquipments({
            equipName: query,
            page: 1,
            pageSize: 50,
          });
          // 注意：根据 request.js 的封装，res 可能是直接的列表数据，也可能是带 rows 的对象
          // 请根据你 getEquipments 接口返回的具体结构调整，通常是 res.rows 或直接 res
          this.equipOptions = res.rows || res;
        } finally {
          this.loading = false;
        }
      } else {
        this.equipOptions = [];
      }
    },
    // 进入“补录”模式时清空下拉选项
    async handleCreate() {
      this.isEdit = false;
      this.resetForm();

      try {
        // 加载设备列表。后端接口会走你提到的动态SQL逻辑
        // 建议加载状态为“在用(1)”或“检修(2)”的设备，因为已报废的不能再报废
        const res = await getEquipments({ pageSize: 1000 });
        this.equipOptions = res.rows || res || [];

        this.dialogVisible = true;
      } catch (error) {
        this.$message.error("获取设备列表失败");
      }
    },
    async fetchScrapRecords() {
      this.scrapList = await getScrapList();
    },
    // 进入“修改”模式
    handleEdit(row) {
      this.isEdit = true;
      this.form = { ...row }; // 深拷贝，避免同步修改表格
      this.dialogVisible = true;
    },
    // 3. 提交表单后的逻辑处理
    async submitForm() {
      this.$refs.scrapForm.validate(async (valid) => {
        if (!valid) return;
        const { scrapNo, equipId } = this.form;
        try {
          if (this.isEdit) {
            await updateScrapRecord(scrapNo, this.form);
            this.$message.success("修改成功");
          } else {
            // 报废补录调用 equipment.js 中的接口
            await scrapEquipment(equipId, this.form);
            this.$message.success("报废补录成功");
          }
          this.dialogVisible = false;
          this.fetchScrapRecords(); // 刷新列表
        } catch (error) {
          console.error(error);
        }
      });
    },
    handleDelete(row) {
      this.$confirm(`确定要撤销设备 ${row.equipId} 的报废记录吗？`, "警告", {
        type: "warning",
      }).then(async () => {
        await deleteScrapRecord(row.scrapNo, row.equipId);
        this.$message.success("已撤销并恢复设备状态");
        this.fetchScrapRecords();
      }).catch(() => {});
    },
    // 4. 重置表单方法
    resetForm() {
      this.form = {
        scrapNo: "",
        equipId: "",
        scrapDate: new Date().toISOString().split("T")[0],
        reason: "",
        approver: "管理员",
      };
      if (this.$refs.scrapForm) {
        this.$refs.scrapForm.resetFields();
      }
    },
  },
};
</script>
