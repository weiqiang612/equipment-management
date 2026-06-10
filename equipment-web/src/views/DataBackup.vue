<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold; font-size: 16px">系统数据安全管理</span>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-cloudy"
          @click="handleBackup"
          :loading="backupLoading"
          style="float: right"
          >立即执行全量备份</el-button
        >
      </div>

      <el-alert
        title="重要提示"
        type="warning"
        description="数据恢复操作会将数据库回滚至备份时的状态，当前所有未备份的新增数据将会丢失。执行恢复前建议先进行一次当前数据的备份。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <div class="backup-info">
        <p>
          <strong>存储路径：</strong>
          <code>{{ backupPath || "加载中..." }}</code>
        </p>
        <p>
          <strong>备份说明：</strong> 系统将调用 <code>mysqldump</code> 工具生成
          SQL 脚本文件。
        </p>
      </div>

      <el-divider content-position="left">已有备份列表</el-divider>

      <el-table
        :data="fileList"
        size="small"
        border
        stripe
        v-loading="listLoading"
      >
        <el-table-column type="index" label="序号" width="50" align="center" />

        <el-table-column label="备份文件名称" prop="name">
          <template slot-scope="scope">
            <i class="el-icon-document"></i>
            <span style="margin-left: 10px">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column label="文件大小" width="120">
          <template slot-scope="scope">
            {{ (scope.row.size / 1024).toFixed(2) }} KB
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-refresh-left"
              @click="handleRestore(scope.row.name)"
              >恢复</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import {
  backupDB,
  restoreDB,
  getBackupFiles,
  getBackupConfig,
} from "@/api/database";

export default {
  data() {
    return {
      backupLoading: false,
      restoreLoading: false,
      listLoading: false,
      fileList: [], // 后端返回的 File 对象数组
      backupPath: "", // 备份文件存储路径
    };
  },
  created() {
    this.fetchFiles();
    this.fetchConfig();
  },
  methods: {
    // 获取后端配置的备份路径
    async fetchConfig() {
      try {
        const res = await getBackupConfig();
        console.log("接口完整返回：", res);
        console.log("尝试获取的路径：", res.path);
        // 根据你的 Result 包装类结构获取数据，通常是 res.path
        this.backupPath = res.path;
      } catch (error) {
        console.error("获取路径失败", error);
        this.backupPath = "未获取到路径";
      }
    },
    // 获取文件列表
    async fetchFiles() {
      this.listLoading = true;
      try {
        const res = await getBackupFiles();
        // 后端 Result.success(files) 返回的是数组
        this.fileList = res || [];
      } catch (error) {
        this.$message.error("获取备份列表失败");
      } finally {
        this.listLoading = false;
      }
    },

    // 备份逻辑
    async handleBackup() {
      try {
        await this.$confirm("确定要立即备份数据库吗？", "操作确认");
        this.backupLoading = true;
        await backupDB();
        this.$message.success("备份成功！");
        this.fetchFiles(); // 备份成功后刷新列表
      } catch (error) {
        if (error !== "cancel") this.$message.error("备份失败");
      } finally {
        this.backupLoading = false;
      }
    },

    // 恢复逻辑
    async handleRestore(fileName) {
      try {
        await this.$confirm(
          `警告：确定要将数据库恢复到文件 [${fileName}] 的状态吗？此操作不可逆！`,
          "严重警告",
          {
            confirmButtonText: "确定恢复",
            cancelButtonText: "取消",
            type: "error",
          }
        );
        this.restoreLoading = true;
        this.$message.warning("正在执行恢复，请稍候...");

        await restoreDB(fileName);

        this.$message({
          message: "数据恢复成功！",
          type: "success",
          duration: 5000,
        });
      } catch (error) {
        if (error !== "cancel") {
          this.$message.error("恢复失败，请检查后端控制台日志");
        }
      } finally {
        this.restoreLoading = false;
      }
    },
  },
};
</script>

<style scoped>
.backup-info {
  background-color: #f8f9fa;
  padding: 12px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}
</style>
