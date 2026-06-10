<template>
  <div class="app-container">
    <el-card shadow="never" style="margin-top: 20px">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold">设备调拨记录管理</span>
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="small"
          @click="openAddDialog"
          style="float: right"
          >新增调拨补录</el-button
        >
      </div>

      <el-table
        :data="recordList"
        border
        v-loading="loading"
        size="small"
        stripe
      >
        <el-table-column
          prop="transferId"
          label="单号"
          width="80"
          align="center"
        />
        <el-table-column prop="equipId" label="设备编号" width="120" />
        <el-table-column prop="equipName" label="设备名称" />

        <el-table-column label="调拨轨迹" width="280">
          <template slot-scope="scope">
            <el-tag type="info" size="mini" effect="plain">{{
              scope.row.outUnitName
            }}</el-tag>
            <i class="el-icon-right" style="margin: 0 5px"></i>
            <el-tag type="success" size="mini" effect="plain">{{
              scope.row.inUnitName
            }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="transferDate" label="调拨日期" width="110" />
        <el-table-column prop="operator" label="经办人" width="100" />
        <el-table-column
          prop="reason"
          label="原因/备注"
          show-overflow-tooltip
        />

        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)"
              >修改</el-button
            >
            <el-button
              size="mini"
              type="danger"
              @click="handleRevoke(scope.row)"
              >撤销</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="isEdit ? '修改调拨记录' : '新增调拨补录'"
      :visible.sync="dialogVisible"
      width="500px"
      @closed="resetForm"
    >
      <el-form
        :model="form"
        ref="form"
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
            @change="handleEquipChange"
          >
            <el-option
              v-for="item in equipOptions"
              :key="item.equipId"
              :label="item.equipName + ' (' + item.equipId + ')'"
              :value="item.equipId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="原单位">
          <el-input
            v-model="form.outUnitName"
            disabled
            placeholder="自动带出"
          />
        </el-form-item>

        <el-form-item label="调入单位" prop="inUnitCode">
          <el-select
            v-model="form.inUnitCode"
            :disabled="isEdit"
            placeholder="请选择新单位"
            style="width: 100%"
          >
            <el-option
              v-for="item in deptOptions"
              :key="item.unitCode"
              :label="item.unitName"
              :value="item.unitCode"
              :disabled="item.unitCode === form.outUnitCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="调拨日期" prop="transferDate">
          <el-date-picker
            v-model="form.transferDate"
            type="date"
            placeholder="选择日期"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="经办人" prop="operator">
          <el-input v-model="form.operator" placeholder="请输入经办人" />
        </el-form-item>

        <el-form-item label="原因/备注" prop="reason">
          <el-input
            type="textarea"
            v-model="form.reason"
            :rows="3"
            placeholder="请输入说明"
          />
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
  getTransferRecords,
  updateTransfer,
  deleteTransfer,
  addTransfer,
} from "@/api/transfer";
import { getEquipments, getDeptList } from "@/api/equipment";

export default {
  data() {
    return {
      loading: false,
      submitLoading: false,
      dialogVisible: false,
      isEdit: false,
      recordList: [],
      equipOptions: [],
      deptOptions: [],
      selectLoading: false,
      form: {
        transferId: null,
        equipId: "",
        equipName: "",
        outUnitCode: "",
        outUnitName: "",
        inUnitCode: "",
        transferDate: "",
        operator: "管理员",
        reason: "",
      },
      rules: {
        equipId: [{ required: true, message: "请选择设备", trigger: "change" }],
        inUnitCode: [
          { required: true, message: "请选择调入单位", trigger: "change" },
        ],
        transferDate: [
          { required: true, message: "请选择日期", trigger: "change" },
        ],
        operator: [
          { required: true, message: "请输入经办人", trigger: "blur" },
        ],
      },
    };
  },
  created() {
    this.getList();
    this.loadBaseData();
  },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const res = await getTransferRecords();
        this.recordList = res;
      } finally {
        this.loading = false;
      }
    },
    // 仿照报废页：初始化加载所有可用设备和部门
    async loadBaseData() {
      try {
        // 1. 加载设备列表 (用于下拉搜索)
        // 注意：根据你后端逻辑，如果不传参，默认返回所有或默认分页
        const res = await getEquipments({ pageSize: 1000 });
        this.equipOptions = res.rows || res || [];

        // 2. 加载部门列表 (用于调入单位)
        this.deptOptions = await getDeptList();
      } catch (error) {
        console.error("基础数据加载失败", error);
      }
    },
    openAddDialog() {
      this.isEdit = false;
      this.dialogVisible = true;
      this.form.transferDate = new Date().toISOString().split("T")[0];
    },
    handleEdit(row) {
      this.isEdit = true;
      this.form = { ...row };
      this.dialogVisible = true;
    },
    // 关键联动：选中设备后，自动带出“原单位”
    handleEquipChange(val) {
      if (!val) return;
      // 从现有的选项池里匹配出完整的对象
      const selectedEquip = this.equipOptions.find(
        (item) => item.equipId === val
      );
      if (selectedEquip) {
        // 自动回填原单位信息
        this.form.outUnitCode = selectedEquip.unitCode;
        this.form.outUnitName = selectedEquip.unitName;
        this.form.equipName = selectedEquip.equipName;
      }
    },
    // 提交逻辑
    async submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return;

        try {
          if (this.isEdit) {
            await updateTransfer(this.form.transferId, this.form);
            this.$message.success("修改成功");
          } else {
            // 补录：调用 POST /transferRecords/{equipId}
            await addTransfer(this.form.equipId, {
              ...this.form,
              changeType: "补录",
            });
            this.$message.success("调拨信息补录成功");
          }
          this.dialogVisible = false;
          this.getList();
          // 提交后建议刷新一下基础数据，因为设备的 unitCode 变了
          this.loadBaseData();
        } catch (error) {
          console.error("操作失败", error);
        }
      });
    },
    handleRevoke(row) {
      this.$confirm(
        `确定要撤销单号为 ${row.transferId} 的调拨记录吗？撤销后设备将自动调回 ${row.outUnitName}。`,
        "提示",
        {
          type: "warning",
        }
      )
        .then(async () => {
          await deleteTransfer(row.transferId);
          this.$message.success("已撤销并恢复设备单位");
          this.getList();
        })
        .catch(() => {});
    },
    resetForm() {
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
      this.form = {
        transferId: null,
        equipId: "",
        equipName: "",
        outUnitCode: "",
        outUnitName: "",
        inUnitCode: "",
        transferDate: new Date().toISOString().split("T")[0],
        operator: "管理员",
        reason: "",
      };
    },
    // 核心：远程搜索方法
    async getRemoteEquipList(query) {
      if (query !== "") {
        this.selectLoading = true;
        try {
          // 调用你后端的 GET /equipments 接口
          // 传入 query，后端会走你提到的 AND (equip_id LIKE ? OR e.equip_name like ?) 逻辑
          const res = await getEquipments({ equipName: query });

          // 注意：根据你后端 Result 的封装，这里可能需要处理 res.data 或 res
          this.equipOptions = res || [];
        } catch (error) {
          console.error("搜索设备失败", error);
        } finally {
          this.selectLoading = false;
        }
      } else {
        this.equipOptions = [];
      }
    },

    // 选中后的逻辑：自动填充原单位
    handleEquipChange(val) {
      const equip = this.equipOptions.find((item) => item.equipId === val);
      if (equip) {
        this.form.outUnitCode = equip.unitCode;
        this.form.outUnitName = equip.unitName;
        // 记录设备名称，防止修改模式下丢失
        this.form.equipName = equip.equipName;
      }
    },

    // 打开弹窗逻辑统一
    openAddDialog() {
      this.isEdit = false;
      this.resetForm(); // 确保表单清空
      this.dialogVisible = true;
    },
  },
};
</script>
