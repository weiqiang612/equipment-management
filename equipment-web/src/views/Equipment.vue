<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <div class="search-container" style="margin: 15px 0">
      <el-form
        :inline="true"
        :model="queryParams"
        size="small"
        class="compact-form"
      >
        <el-form-item label="编号/名称">
          <el-input
            v-model="queryParams.equipName"
            placeholder="模糊查询"
            style="width: 120px"
            clearable
          />
        </el-form-item>

        <el-form-item label="单位">
          <el-select
            v-model="queryParams.unitCode"
            placeholder="请选择"
            style="width: 130px"
            clearable
          >
            <el-option
              v-for="item in deptOptions"
              :key="item.unitCode"
              :label="item.unitName"
              :value="item.unitCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="queryParams.categoryId"
            placeholder="请选择"
            style="width: 130px"
            clearable
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.categoryId"
              :label="item.categoryName"
              :value="item.categoryId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            placeholder="状态"
            style="width: 90px"
            clearable
          >
            <el-option label="在用" value="在用" />
            <el-option label="维修" value="维修" />
            <el-option label="报废" value="报废" />
          </el-select>
        </el-form-item>

        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="yyyy-MM-dd"
            style="width: 210px"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch"
            >查询</el-button
          >
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            type="success"
            icon="el-icon-download"
            @click="handleExport"
            >导出明细表</el-button
          >
        </el-form-item>
      </el-form>
    </div>
    <!-- 新增按钮 -->
    <el-button type="primary" @click="handleAdd">新增设备</el-button>
    <!-- 设备表格 -->
    <el-table :data="equipmentList" border style="margin-top: 20px">
      <el-table-column prop="equipId" label="设备编号" />
      <el-table-column prop="equipName" label="设备名称" />
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="规格型号" />
      <el-table-column prop="purchaseDate" label="购入日期" />
      <el-table-column prop="originalValue" label="原值" />
      <el-table-column prop="categoryName" label="分类名称" />
      <el-table-column prop="unitName" label="单位名称" />
      <el-table-column label="资产价值" align="center" width="100">
        <template slot-scope="scope">
          <el-popover
            placement="top"
            width="240"
            trigger="click"
            @show="handleShowDepreciation(scope.row.equipId)"
          >
            <div v-loading="depLoading" style="padding: 10px; line-height: 2">
              <div>
                <b>月折旧额：</b>￥{{
                  Number(currentDep.monthlyDepreciation).toFixed(2)
                }}
              </div>
              <div>
                <b>累计折旧：</b>￥{{
                  Number(currentDep.accumulated).toFixed(2)
                }}
              </div>
              <div>
                <b>当前净值：</b
                ><span style="color: #f56c6c; font-weight: bold"
                  >￥{{ Number(currentDep.netValue).toFixed(2) }}</span
                >
              </div>
              <div v-if="currentDep.isFullyDepreciated" style="margin-top: 5px">
                <el-tag type="danger" size="mini">已提足折旧</el-tag>
              </div>
            </div>
            <el-button slot="reference" type="text" icon="el-icon-pie-chart"
              >查看价值</el-button
            >
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" @click="handleEdit(scope.row)"
            >编辑</el-button
          >

          <el-dropdown style="margin-left: 10px" trigger="click">
            <el-button size="mini" type="info">
              更多<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item
                :disabled="scope.row.status !== '在用'"
                @click.native="handleMaintenance(scope.row)"
                >维修</el-dropdown-item
              >
              <el-dropdown-item
                :disabled="scope.row.status !== '在用'"
                @click.native="handleTransfer(scope.row)"
                >调拨</el-dropdown-item
              >
              <el-dropdown-item
                :disabled="scope.row.status === '报废'"
                @click.native="handleScrap(scope.row)"
                >报废</el-dropdown-item
              >

              <el-dropdown-item
                icon="el-icon-delete"
                style="color: #f56c6c"
                divided
                @click.native="handleDelete(scope.row)"
                >删除设备</el-dropdown-item
              >
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页器 -->
    <el-pagination
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="queryParams.page"
      :page-sizes="[5, 10, 20, 50]"
      :page-size="queryParams.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      style="margin-top: 25px; text-align: right"
    >
    </el-pagination>
    <!-- 新增按钮的弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备编号">
          <el-input
            v-model="form.equipId"
            :disabled="isEdit"
            placeholder="请输入编号"
          />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="form.equipName" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input
            v-model="form.model"
            placeholder="如：联想 T480 / 16G内存"
          />
        </el-form-item>
        <el-form-item label="购入日期">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            placeholder="选择日期"
            value-format="yyyy-MM-dd"
          />
        </el-form-item>
        <el-form-item label="所属单位">
          <el-select v-model="form.unitCode" placeholder="请选择单位">
            <el-option
              v-for="item in deptOptions"
              :key="item.unitCode"
              :label="item.unitName"
              :value="item.unitCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option
              v-for="item in categoryOptions"
              :key="item.categoryId"
              :label="item.categoryName"
              :value="item.categoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原值">
          <el-input-number v-model="form.originalValue" :min="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
    <!-- 检修弹窗 -->
    <el-dialog
      title="设备检修登记"
      :visible.sync="maintDialogVisible"
      width="500px"
      append-to-body
    >
      <el-form :model="maintForm" label-width="100px" size="small">
        <el-form-item label="设备编号">
          <el-input v-model="maintForm.equipId" disabled />
        </el-form-item>
        <el-form-item label="检修日期">
          <el-date-picker
            v-model="maintForm.maintDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="检修内容">
          <el-input
            type="textarea"
            v-model="maintForm.maintContent"
            placeholder="请输入本次检修的具体内容"
          />
        </el-form-item>
        <el-form-item label="检修费用">
          <el-input-number
            v-model="maintForm.maintCost"
            :precision="2"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="检修人">
          <el-input v-model="maintForm.maintPerson" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="maintDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitMaint">提交记录</el-button>
      </div>
    </el-dialog>
    <!-- 报废记录弹窗 -->
    <el-dialog
      title="设备报废登记"
      :visible.sync="scrapDialogVisible"
      width="500px"
    >
      <el-form :model="scrapForm" label-width="100px" size="small">
        <el-form-item label="设备编号">
          <el-input v-model="scrapForm.equipId" disabled />
        </el-form-item>
        <el-form-item label="报废日期">
          <el-date-picker
            v-model="scrapForm.scrapDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报废原因">
          <el-input
            type="textarea"
            v-model="scrapForm.scrapReason"
            rows="3"
            placeholder="请输入报废原因"
          />
        </el-form-item>
        <el-form-item label="经办人">
          <el-input
            v-model="scrapForm.handler"
            placeholder="请输入经办人姓名"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="scrapDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitScrap">确 定</el-button>
      </div>
    </el-dialog>
    <!-- 设备调拨弹窗 -->
    <el-dialog title="资产调拨" :visible.sync="transferVisible" width="450px">
      <el-form :model="transferForm" label-width="100px" size="small">
        <el-form-item label="设备信息">
          <span>{{ transferForm.equipName }} ({{ transferForm.equipId }})</span>
        </el-form-item>
        <el-form-item label="当前单位">
          <el-tag type="info">{{ transferForm.outUnitName }}</el-tag>
        </el-form-item>
        <el-form-item label="调入单位">
          <el-select
            v-model="transferForm.inUnitCode"
            placeholder="请选择新单位"
            style="width: 100%"
          >
            <el-option
              v-for="item in deptOptions"
              :key="item.unitCode"
              :label="item.unitName"
              :value="item.unitCode"
              :disabled="item.unitCode === transferForm.outUnitCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="经办人">
          <el-input v-model="transferForm.operator" />
        </el-form-item>
        <el-form-item label="调拨说明">
          <el-input type="textarea" v-model="transferForm.reason" rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="transferVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="submitTransfer"
          :loading="transferLoading"
          >提交调拨</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getEquipments,
  addEquipment,
  updateEquipment,
  deleteEquipment,
  getDeptList,
  getCategoryList,
  maintenanceEquip,
  scrapEquipment,
  getEquipmentsForExport,
  getCalculateAccumulated,
} from "@/api/equipment";
import { addTransfer } from "@/api/transfer";
import * as XLSX from "xlsx";
import { saveAs } from "file-saver";

export default {
  data() {
    return {
      equipmentList: [], // 设备列表数据
      deptOptions: [], // 单位下拉列表
      categoryOptions: [], // 分类下拉列表
      dialogVisible: false,
      isEdit: false,
      currentDep: {
        monthlyDepreciation: 0,
        accumulated: 0,
        netValue: 0,
        isFullyDepreciated: false,
      }, // 存储当前点击的折旧信息
      depLoading: false, // 折旧信息加载状态
      // 表单对象字段需与后端实体类及数据库字段对应
      form: {
        equipId: "", // 设备编号 (主键)
        equipName: "", // 名称
        status: "在用", // 状态
        purchaseDate: "", // 购入日期
        originalValue: 0, // 原值
        model: "", // 规格型号
        unitCode: "", // 对应单位表的单位代码 (外键)
        categoryId: "", // 对应分类表的分类编码 (外键)
        categoryName: "", // 分类名称
        unitName: "", // 单位名称
      },
      total: 0,
      dateRange: [],
      // 搜索参数对象
      queryParams: {
        equipName: "",
        status: "",
        unitCode: "", // 新增：单位筛选
        categoryId: "", // 新增：分类筛选
        begin: "",
        end: "",
        page: 1,
        pageSize: 10,
      },
      maintDialogVisible: false, // 控制弹窗显示
      maintForm: {
        // 弹窗表单对象
        equipId: "",
        maintDate: "",
        maintContent: "",
        maintCost: 0,
        maintPerson: "",
      },
      scrapDialogVisible: false, // 控制报废弹窗
      scrapForm: {
        // 报废表单数据
        equipId: "",
        scrapDate: "",
        scrapReason: "",
        handler: "",
      },
      transferVisible: false,
      transferLoading: false,
      transferForm: {
        equipId: "",
        equipName: "",
        outUnitCode: "",
        outUnitName: "",
        inUnitCode: "",
        reason: "",
        operator: "管理员",
      },
    };
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? "修改设备" : "新增设备";
    },
  },
  created() {
    this.initAllData();
  },
  methods: {
    // 并发请求所有必要数据进行联调
    async initAllData() {
      try {
        const [resDept, resCate] = await Promise.all([
          getDeptList(),
          getCategoryList(),
        ]);
        this.deptOptions = resDept;
        this.categoryOptions = resCate;
        this.fetchEquipmentList(); // 拆分出专门获取列表的方法
      } catch (error) {
        console.error("初始化数据失败", error);
      }
    },
    // 新增专门获取设备列表的方法
    async fetchEquipmentList() {
      try {
        // 此时的 res 已经是后端 Result 里的 data 对象，即 PageBean
        const res = await getEquipments(this.queryParams);

        if (res) {
          // 核心修改：明确指向 rows 和 total
          this.equipmentList = res.rows || [];
          this.total = res.total || 0;
        }
        console.log("加载到的列表数据：", this.equipmentList);
      } catch (error) {
        console.error("获取设备列表失败", error);
      }
    },
    handleAdd() {
      this.isEdit = false;
      // 彻底重置表单，防止上次编辑的数据残留在表单里
      this.form = {
        equipId: "",
        equipName: "",
        status: "在用",
        purchaseDate: new Date().toISOString().split("T")[0], // 默认今天
        originalValue: 0,
        model: "",
        unitCode: "",
        categoryId: "",
      };
      this.dialogVisible = true;
    },
    handleEdit(row) {
      this.isEdit = true;
      this.form = { ...row };
      this.dialogVisible = true;
    },
    async submitForm() {
      try {
        if (this.isEdit) {
          await updateEquipment(this.form); // 联调修改
        } else {
          await addEquipment(this.form); // 联调新增
        }
        this.$message.success("操作成功");
        this.dialogVisible = false;
        this.initAllData();
      } catch (error) {
        // request.js 会处理错误提示，此处不再弹出红遮罩
      }
    },
    // Equipment.vue 中的 submitMaint 优化版
    async submitMaint() {
      if (!this.maintForm.maintContent) {
        this.$message.warning("请填写检修内容");
        return;
      }
      try {
        const res = await maintenanceEquip(
          this.maintForm.equipId,
          this.maintForm
        );
        // 这里要注意：如果你的 Axios 拦截器直接返回了数据，则 res 为真
        this.$message.success("检修登记成功，设备状态已更新");
        this.maintDialogVisible = false;
        this.fetchEquipmentList();
      } catch (error) {
        // 错误会被 request.js 拦截并弹窗，这里只需要停止加载动画或处理局部逻辑
        console.error("提交检修失败", error);
      }
    },
    handleDelete(row) {
      this.$confirm("确定删除该设备及所有关联信息吗？", "警告", {
        type: "warning",
      })
        .then(async () => {
          await deleteEquipment(row.equipId); // 联调删除
          this.$message.success("删除成功");
          this.initAllData();
        })
        .catch(() => {
          // 点击取消或关闭弹窗的逻辑
          // 这里留空即可，或者打印一条信息，这样就不会报运行时错误了
          console.log("已取消删除");
        });
    },
    // 处理日期区间变化
    handleDateChange(val) {
      if (val) {
        this.queryParams.begin = val[0];
        this.queryParams.end = val[1];
      } else {
        this.queryParams.begin = "";
        this.queryParams.end = "";
      }
    },
    // 执行查询
    handleSearch() {
      this.queryParams.page = 1;
      this.fetchEquipmentList();
    },
    // 重置查询
    resetQuery() {
      this.dateRange = [];
      this.queryParams = {
        equipName: "",
        status: "",
        unitCode: "",
        categoryId: "",
        begin: "",
        end: "",
        page: 1,
        pageSize: 10,
      };
      this.fetchEquipmentList();
    },
    // 每页条数改变
    handleSizeChange(val) {
      this.queryParams.pageSize = val;
      this.fetchEquipmentList();
    },
    // 当前页码改变
    handleCurrentChange(val) {
      this.queryParams.page = val;
      this.fetchEquipmentList();
    },
    handleMaintenance(row) {
      // 1. 初始化检修表单数据，并预填当前行的设备 ID
      this.maintForm = {
        equipId: row.equipId,
        maintDate: new Date().toISOString().split("T")[0], // 默认今天
        maintContent: "",
        maintCost: 0,
        maintPerson: "",
      };
      // 2. 显示弹窗（maintDialogVisible 需要在 data 中定义）
      this.maintDialogVisible = true;
    },
    // 2. 触发报废弹窗 [参考 handleMaintenance 逻辑]
    handleScrap(row) {
      this.scrapForm = {
        equipId: row.equipId,
        scrapDate: new Date().toISOString().split("T")[0], // 默认当天
        scrapReason: "",
        handler: "",
      };
      this.scrapDialogVisible = true;
    },

    // 3. 提交报废请求 [对应后端 POST /equipments/scrap/{equipId}]
    async submitScrap() {
      // 1. 字段校验
      if (!this.scrapForm.scrapReason) {
        this.$message.warning("请填写报废原因");
        return;
      }

      try {
        // 注意：这里要把 scrapReason 映射给后端需要的 reason 字段
        const submitData = {
          equipId: this.scrapForm.equipId,
          scrapDate: this.scrapForm.scrapDate,
          reason: this.scrapForm.scrapReason, // 关键：映射字段
          approver: this.scrapForm.handler, // 关键：映射字段
        };

        // 2. 调用 API
        // 注意：request.js 里的响应拦截器如果返回的是 res.data，
        // 那么这里 res 就不再包含 .code 属性了。
        await scrapEquipment(this.scrapForm.equipId, submitData);

        // 3. 执行成功后的反馈
        this.$message.success("设备报废登记成功");
        this.scrapDialogVisible = false; // 关闭弹窗
        this.fetchEquipmentList(); // 必须刷新列表，查看状态是否变更为“报废”
      } catch (error) {
        // request.js 已经处理了 Message 报错，这里只需要捕获异常防止程序崩溃
        console.error("报废操作失败", error);
      }
    },
    statusTagType(status) {
      const statusMap = {
        在用: "success", // 绿色
        维修: "warning", // 黄色/橙色
        报废: "danger", // 红色
      };
      return statusMap[status] || "info";
    },
    async handleShowDepreciation(equipId) {
      // 1. 进入加载状态
      this.depLoading = true;

      // 2. 初始化/重置数据（防止看到上一条设备的数据）
      this.currentDep = {
        monthlyDepreciation: 0,
        accumulated: 0,
        netValue: 0,
        isFullyDepreciated: false,
      };

      try {
        // 3. 调用 API
        const res = await getCalculateAccumulated(equipId);

        // 4. 关键调试日志：如果控制台报错，看这里输出什么
        console.log("设备 ID:", equipId, "接口返回结果:", res);

        // 5. 根据 Result 对象的结构进行判断
        // 注意：如果你的 request.js 拦截器直接返回了 data 这一层，
        // 那么这里可能不需要 res.code === 1，直接 if(res) 即可
        if (res && res.code === 1) {
          // 必须使用解构赋值 {...res.data} 触发 Vue 2 的响应式更新
          this.currentDep = { ...res.data };
        } else if (res && !res.code) {
          // 兼容某些拦截器直接返回 data 的情况
          this.currentDep = { ...res };
        } else {
          this.$message.error(res.msg || "获取折旧数据失败");
        }
      } catch (error) {
        console.error("折旧查询异常:", error);
        this.$message.error("网络请求失败，请检查后端服务");
      } finally {
        // 6. 无论成功失败都关闭 Loading
        this.depLoading = false;
      }
    },
    async handleExport() {
      this.$confirm("确定导出包含折旧状态信息的设备明细报表吗?", "提示", {
        type: "info",
      })
        .then(async () => {
          const loading = this.$loading({
            text: "正在校验折旧数据并生成报表...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          try {
            const res = await getEquipments(this.queryParams);
            const list = res.rows || []; // 基于你拦截器的返回结构

            if (list.length === 0) {
              this.$message.warning("无数据导出");
              return;
            }

            // 1. 准备报表基础信息
            const unitName =
              this.deptOptions.find(
                (d) => d.unitCode === this.queryParams.unitCode
              )?.unitName || "全部单位";
            const categoryName =
              this.categoryOptions.find(
                (c) => c.categoryId === this.queryParams.categoryId
              )?.categoryName || "全部分类";

            const excelData = [
              ["设备资产价值及折旧状态明细表"],
              [
                `导出时间：${new Date().toLocaleString()}`,
                "",
                "",
                "",
                `范围：单位[${unitName}] | 分类[${categoryName}]`,
              ],
              [
                "设备编号",
                "设备名称",
                "所属单位",
                "购入日期",
                "状态",
                "是否提足折旧",
                "原值(元)",
                "累计折旧",
                "当前净值",
              ],
            ];

            let totalOrg = 0,
              totalAcc = 0,
              totalNet = 0;

            // 2. 遍历并利用 isFullyDepreciated 字段
            list.forEach((item) => {
              const original = Number(item.originalValue || 0);
              const usefulLife = Number(item.usefulLife || 5);
              const residualRate = Number(item.residualRate || 0.05);
              const isDepreciated = item.isFullyDepreciated; // 后端返回的布尔值

              // --- 核心财务逻辑修正 ---
              const totalDepreciable = original * (1 - residualRate); // 应提总额
              let acc = 0;
              let net = 0;

              if (isDepreciated) {
                // 如果已提足折旧：累计折旧 = 应提总额；净值 = 残值
                acc = totalDepreciable;
                net = original * residualRate;
              } else {
                // 如果未提足：按日期实时计算
                const start = new Date(item.purchaseDate);
                const now = new Date();
                const monthsUsed = Math.max(
                  0,
                  (now.getFullYear() - start.getFullYear()) * 12 +
                    (now.getMonth() - start.getMonth())
                );
                const totalMonths = usefulLife * 12;
                acc =
                  (totalDepreciable / totalMonths) *
                  Math.min(monthsUsed, totalMonths);
                net = original - acc;
              }

              totalOrg += original;
              totalAcc += acc;
              totalNet += net;

              excelData.push([
                item.equipId || "",
                item.equipName || "",
                item.unitName || "",
                item.purchaseDate || "",
                item.status || "",
                isDepreciated ? "是 (已提足)" : "否 (计提中)", // 逻辑利用点
                original.toFixed(2),
                acc.toFixed(2),
                net.toFixed(2),
              ]);
            });

            // 3. 添加合计行
            excelData.push([
              "合计",
              "",
              "",
              "",
              "",
              "",
              totalOrg.toFixed(2),
              totalAcc.toFixed(2),
              totalNet.toFixed(2),
            ]);

            // 4. 生成并保存
            const worksheet = XLSX.utils.aoa_to_sheet(excelData);
            worksheet["!merges"] = [
              { s: { r: 0, c: 0 }, e: { r: 0, c: 8 } },
              { s: { r: 1, c: 0 }, e: { r: 1, c: 3 } },
              { s: { r: 1, c: 4 }, e: { r: 1, c: 8 } },
            ];

            const workbook = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(workbook, worksheet, "资产价值报表");
            XLSX.writeFile(
              workbook,
              `资产折旧报表_${new Date().getTime()}.xlsx`
            );

            this.$message.success("导出成功！提足折旧设备已自动标记。");
          } catch (error) {
            console.error(error);
            this.$message.error("导出异常");
          } finally {
            loading.close();
          }
        })
        .catch((action) => {
          // 关键修正：捕获取消操作
          if (action === "cancel") {
            console.log("用户取消了导出");
          } else {
            // 其他可能的非预期错误
            console.error("确认框异常", action);
          }
        });
    },
    formatStatus(status) {
      const map = { 1: "在用", 2: "检修", 3: "报废" };
      return map[status] || "未知";
    },
    // 2. 处理下拉菜单指令分发
    handleCommand(command, row) {
      // 这种写法要求 method 名必须和 command 字符串完全一致
      if (typeof this[command] === "function") {
        this[command](row);
      }
    },

    // 确保 handleTransfer 逻辑正确
    handleTransfer(row) {
      console.log("准备调拨设备:", row.equipId); // 调试用
      this.transferForm = {
        equipId: row.equipId,
        equipName: row.equipName,
        outUnitCode: row.unitCode,
        outUnitName: row.unitName,
        inUnitCode: "",
        reason: "",
        operator: "管理员",
      };
      this.transferVisible = true;
    },

    // 提交调拨，注意这里刷新列表的方法名
    async submitTransfer() {
      if (!this.transferForm.inUnitCode) {
        return this.$message.warning("请选择接收单位");
      }
      this.transferLoading = true;
      try {
        await addTransfer(this.transferForm.equipId, {
          outUnitCode: this.transferForm.outUnitCode,
          inUnitCode: this.transferForm.inUnitCode,
          reason: this.transferForm.reason,
          operator: this.transferForm.operator,
          transferDate: new Date().toISOString().split("T")[0],
          changeType: "调拨",
        });
        this.$message.success("设备调拨成功");
        this.transferVisible = false;

        // 注意：你的组件中获取列表的方法名是 fetchEquipmentList 而不是 getList
        this.fetchEquipmentList();
      } catch (error) {
        console.error("调拨提交失败", error);
      } finally {
        this.transferLoading = false;
      }
    },
  },
};
</script>
