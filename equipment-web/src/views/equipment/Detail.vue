<template>
  <div class="equipment-detail-container">
    <div class="detail-page-header">
      <div class="detail-header-copy">
        <el-page-header @back="goBack" content="设备生命周期详情" style="margin-bottom: 0;"></el-page-header>
        <p class="detail-header-subtitle">统一查看资产状态、审批轨迹、维保记录和审计时间线。</p>
      </div>
      <el-button 
        v-if="userRole === 2 || userRole === 3" 
        type="primary" 
        size="small" 
        icon="el-icon-magic-stick" 
        style="background: linear-gradient(90deg, #409eff 0%, #00adb5 100%); border: none; box-shadow: 0 2px 8px rgba(0, 173, 181, 0.25);"
        @click="showAiSummary"
      >
        AI 辅助诊断
      </el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <!-- 左侧：卡片信息 -->
      <el-col :span="16">
        <!-- 基础信息卡片 -->
        <el-card class="box-card" style="margin-bottom: 20px;">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;"><i class="el-icon-info"></i> 基础属性</span>
            <el-tag :type="getStatusTagType(detailData.status)" size="small" style="float: right;">
              {{ detailData.status }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="设备编号">{{ detailData.equipId }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ detailData.equipName }}</el-descriptions-item>
            <el-descriptions-item label="规格型号">{{ detailData.model || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备分类">{{ detailData.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="购入日期">{{ detailData.purchaseDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="保管人">
              {{ detailData.custodianRealName ? `${detailData.custodianRealName} (${detailData.custodian})` : '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="所属单位">{{ detailData.unitName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 折旧价值分析卡片 -->
        <el-card class="box-card" style="margin-bottom: 20px;">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;"><i class="el-icon-pie-chart"></i> 折旧价值分析</span>
          </div>
          <el-row :gutter="20" class="value-analysis">
            <el-col :span="6">
              <div class="analysis-item">
                <div class="label">设备原值</div>
                <div class="value">￥{{ formatMoney(detailData.originalValue) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="analysis-item">
                <div class="label">残值率</div>
                <div class="value">{{ detailData.residualRate !== undefined ? detailData.residualRate * 100 : 0 }}%</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="analysis-item">
                <div class="label">使用年限</div>
                <div class="value">{{ detailData.usefulLife }} 年</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="analysis-item highlight">
                <div class="label">当前实时折旧净值</div>
                <div class="value">￥{{ formatMoney(detailData.netValue) }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 历史记录明细分区 (Tabs) -->
        <el-card class="box-card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="领用历史" name="claims">
              <el-table :data="detailData.claims" border style="width: 100%" size="small">
                <el-table-column prop="createTime" label="申请时间" width="160"></el-table-column>
                <el-table-column prop="applicantRealName" label="申请人" width="120">
                  <template slot-scope="scope">
                    {{ scope.row.applicantRealName }} ({{ scope.row.applicant }})
                  </template>
                </el-table-column>
                <el-table-column prop="approverRealName" label="审批人" width="120">
                  <template slot-scope="scope">
                    {{ scope.row.approverRealName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="审批状态" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="getClaimStatusTag(scope.row.status)" size="mini">
                      {{ formatClaimStatus(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注"></el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="维保历史" name="maintenances">
              <el-table :data="detailData.maintenances" border style="width: 100%" size="small">
                <el-table-column prop="createTime" label="报修时间" width="160"></el-table-column>
                <el-table-column prop="faultDescription" label="故障描述" min-width="150"></el-table-column>
                <el-table-column prop="engineerRealName" label="检修工程师" width="120"></el-table-column>
                <el-table-column prop="maintCost" label="维修费用" width="100">
                  <template slot-scope="scope">
                    ￥{{ formatMoney(scope.row.maintCost) }}
                  </template>
                </el-table-column>
                <el-table-column label="维保状态" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="getMaintStatusTag(scope.row.maintStatus)" size="mini">
                      {{ formatMaintStatus(scope.row.maintStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="completeTime" label="完工时间" width="160"></el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="调拨历史" name="transfers">
              <el-table :data="detailData.transfers" border style="width: 100%" size="small">
                <el-table-column prop="transferDate" label="调拨时间" width="160"></el-table-column>
                <el-table-column prop="srcDeptName" label="调出单位" width="150"></el-table-column>
                <el-table-column prop="destDeptName" label="调入单位" width="150"></el-table-column>
                <el-table-column prop="operatorRealName" label="操作人" width="120"></el-table-column>
                <el-table-column prop="remark" label="调拨原因/备注"></el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="报废信息" name="scrap">
              <div v-if="detailData.scrap" class="scrap-info-pane">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="报废单号">{{ detailData.scrap.scrapNo }}</el-descriptions-item>
                  <el-descriptions-item label="报废日期">{{ detailData.scrap.scrapDate }}</el-descriptions-item>
                  <el-descriptions-item label="报废原因">{{ detailData.scrap.scrapReason }}</el-descriptions-item>
                  <el-descriptions-item label="操作人">{{ detailData.scrap.operatorRealName }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <el-empty v-else description="暂无报废记录"></el-empty>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <!-- 右侧：流式时间轴时间线 -->
      <el-col :span="8">
        <el-card class="box-card" style="height: 100%; min-height: 400px;">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;"><i class="el-icon-time"></i> 全生命周期追溯时间轴</span>
          </div>
          <div class="timeline-wrapper">
            <el-timeline v-if="detailData.auditTimeline && detailData.auditTimeline.length > 0">
              <el-timeline-item
                v-for="(log, index) in detailData.auditTimeline"
                :key="index"
                :timestamp="log.opTime"
                :type="log.status === 1 ? 'primary' : 'danger'"
                :icon="log.status === 1 ? 'el-icon-check' : 'el-icon-close'"
                placement="top"
              >
                <el-card shadow="hover" class="timeline-card">
                  <h4 style="margin: 0 0 8px 0; color: #303133; font-size: 14px;">
                    {{ log.opType }}
                    <el-tag :type="log.status === 1 ? 'success' : 'danger'" size="mini" style="margin-left: 5px;">
                      {{ log.status === 1 ? '成功' : '失败' }}
                    </el-tag>
                  </h4>
                  <p style="margin: 0 0 5px 0; color: #606266; font-size: 12px; line-height: 1.5;">
                    {{ log.summary }}
                  </p>
                  <div style="font-size: 11px; color: #909399; display: flex; justify-content: space-between;">
                    <span>操作人: {{ log.operatorRealName }}</span>
                    <span>角色: {{ formatRole(log.operatorRole) }}</span>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无生命周期追溯日志"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI 辅助诊断弹窗 -->
    <el-dialog 
      title="AI 设备生命周期分析摘要" 
      :visible.sync="aiDialogVisible" 
      width="60%"
    >
      <div v-loading="aiLoading" element-loading-text="AI 正在深度解析设备历史轨迹中..." style="min-height: 150px; padding: 10px 0;">
        <!-- 降级/未配置警告 -->
        <div v-if="aiErrorMsg">
          <el-alert
            title="AI 诊断暂不可用"
            type="warning"
            :description="aiErrorMsg"
            show-icon
            :closable="false"
          >
          </el-alert>
          <div style="margin-top: 15px; font-size: 13px; color: #606266; line-height: 1.6; background-color: #fafafb; padding: 12px; border-radius: 4px; border: 1px solid #ebeef5;">
            <p><strong>💡 提示：</strong> 外部大模型 API Key 尚未配置。若想启用此功能，请联系系统管理员，在服务器上配置环境变量：</p>
            <ul style="margin: 5px 0 0 15px; padding: 0;">
              <li><code>AI_API_KEY</code> - API 秘钥凭证</li>
              <li><code>AI_BASE_URL</code> - OpenAI 协议基准地址</li>
            </ul>
          </div>
        </div>

        <!-- 生成报告成功 -->
        <div v-else-if="aiSummaryData">
          <div class="ai-report-meta">
            <div class="ai-report-title-bar">
              <h3>{{ aiReportTitle }}</h3>
              <div class="ai-meta-row">
                <span class="period-badge">设备生命周期诊断</span>
                <span class="time"><i class="el-icon-time"></i> 生成时间：{{ formatTime(aiSummaryData.generatedTime) }}</span>
              </div>
            </div>
            <el-button 
              class="report-action-button"
              type="primary"
              size="mini"
              icon="el-icon-download"
              @click="exportAiSummaryPdf"
            >
              导出 PDF
            </el-button>
          </div>
          <article class="report-content-box report-print-surface">
            <div class="report-summary-grid">
              <div class="summary-item">
                <span class="summary-label">风险等级</span>
                <strong>{{ formatRiskLevel(aiSummaryData.riskLevel) }}</strong>
              </div>
              <div class="summary-item">
                <span class="summary-label">输出对象</span>
                <strong>{{ detailData.equipName || '-' }} ({{ detailData.equipId || '-' }})</strong>
              </div>
              <div class="summary-item">
                <span class="summary-label">输出性质</span>
                <strong>AI 辅助诊断</strong>
              </div>
            </div>
            <div class="ai-report-content report-content" v-html="renderedAiSummary"></div>
          </article>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="aiDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <iframe ref="printFrame" class="print-frame"></iframe>
  </div>
</template>

<script>
import { getEquipmentDetail } from '@/api/equipment'
import { getEquipmentAiSummary } from '@/api/aiAssistant'
import { buildAiReportHtml, formatReportTime, printAiReportFromFrame } from '@/utils/aiReportPrint'
import { getClaimStatusMeta, getEquipmentStatusMeta, getMaintenanceStatusMeta, getRiskLevelMeta } from '@/utils/uiStatus'

export default {
  name: 'EquipmentDetail',
  data() {
    return {
      equipId: '',
      activeTab: 'claims',
      loading: false,
      userRole: 0,
      aiDialogVisible: false,
      aiLoading: false,
      aiSummaryData: null,
      aiErrorMsg: '',
      detailData: {
        equipId: '',
        equipName: '',
        model: '',
        originalValue: 0,
        residualRate: 0,
        usefulLife: 0,
        purchaseDate: '',
        status: '',
        unitCode: '',
        unitName: '',
        custodianRealName: '',
        custodian: '',
        depreciationValue: 0,
        netValue: 0,
        claims: [],
        maintenances: [],
        transfers: [],
        scrap: null,
        auditTimeline: []
      }
    }
  },
  created() {
    this.equipId = this.$route.params.equipId
    this.userRole = parseInt(localStorage.getItem('role') || '0', 10)
    if (this.equipId) {
      this.getDetail()
    }
  },
  methods: {
    async getDetail() {
      this.loading = true
      try {
        const data = await getEquipmentDetail(this.equipId)
        if (data) {
          this.detailData = data
        }
      } catch (error) {
        this.$message.error('请求接口失败')
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.push('/equipment')
    },
    formatMoney(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    formatRole(role) {
      const roleMap = {
        0: '操作员',
        1: '维修工',
        2: '资产管理员',
        3: '系统管理员'
      }
      return roleMap[role] !== undefined ? roleMap[role] : '未知'
    },
    getStatusTagType(status) {
      return getEquipmentStatusMeta(status).type
    },
    getClaimStatusTag(status) {
      return getClaimStatusMeta(status).type
    },
    formatClaimStatus(status) {
      return getClaimStatusMeta(status).label
    },
    getMaintStatusTag(status) {
      return getMaintenanceStatusMeta(status).type
    },
    formatMaintStatus(status) {
      return getMaintenanceStatusMeta(status).label
    },
    async showAiSummary() {
      this.aiDialogVisible = true
      this.aiLoading = true
      this.aiErrorMsg = ''
      this.aiSummaryData = null
      try {
        const res = await getEquipmentAiSummary(this.equipId)
        if (res) {
          this.aiSummaryData = res
        } else {
          this.aiErrorMsg = '获取 AI 生命周期摘要失败'
        }
      } catch (err) {
        this.aiErrorMsg = err.message || '网络连接失败，请稍后重试'
      } finally {
        this.aiLoading = false
      }
    },
    getRiskTagType(risk) {
      return getRiskLevelMeta(risk).type
    },
    formatRiskLevel(risk) {
      return getRiskLevelMeta(risk).label
    },
    formatTime(timeValue) {
      return formatReportTime(timeValue)
    },
    exportAiSummaryPdf() {
      if (!this.aiSummaryData || !this.aiSummaryData.summary) return

      try {
        printAiReportFromFrame(this.$refs.printFrame, {
          title: this.aiReportTitle,
          badge: '设备生命周期诊断',
          content: this.aiSummaryData.summary,
          generatedTime: this.formatTime(this.aiSummaryData.generatedTime),
          metaItems: [
            { label: '生成时间', value: this.formatTime(this.aiSummaryData.generatedTime) },
            { label: '风险等级', value: this.formatRiskLevel(this.aiSummaryData.riskLevel) },
            { label: '输出对象', value: `${this.detailData.equipName || '-'} (${this.detailData.equipId || '-'})` }
          ]
        })
      } catch (error) {
        this.$message.error(error.message || '打印容器初始化失败，请刷新页面后重试')
      }
    }
  },
  computed: {
    aiReportTitle() {
      return '设备生命周期 AI 诊断报告'
    },
    renderedAiSummary() {
      if (!this.aiSummaryData || !this.aiSummaryData.summary) {
        return ''
      }
      return buildAiReportHtml({
        content: this.aiSummaryData.summary,
        title: this.aiReportTitle,
        generatedTime: this.formatTime(this.aiSummaryData.generatedTime)
      })
    }
  }
}
</script>

<style scoped>
.equipment-detail-container {
  padding: 10px;
}

.detail-page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-header-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-header-subtitle {
  margin: 0;
  color: #7a8797;
  font-size: 12px;
  line-height: 1.5;
}
.box-card {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  border-radius: 4px;
}
.value-analysis {
  padding: 10px 0;
}
.analysis-item {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  border-left: 4px solid #909399;
}
.analysis-item.highlight {
  background: #ecf5ff;
  border-left: 4px solid #409eff;
}
.analysis-item .label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.analysis-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.analysis-item.highlight .value {
  color: #409eff;
}
.timeline-wrapper {
  max-height: 650px;
  overflow-y: auto;
  padding-right: 5px;
}
.timeline-card {
  border: 1px solid #e4e7ed;
  background-color: #fff;
  border-radius: 4px;
}
.scrap-info-pane {
  padding: 10px 0;
}
.ai-report-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
  margin-bottom: 20px;
}
.ai-report-title-bar h3 {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.ai-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.period-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}
.time {
  font-size: 12.5px;
  color: #64748b;
  display: flex;
  align-items: center;
}
.time i {
  margin-right: 4px;
}
.report-action-button {
  flex-shrink: 0;
}
.report-content-box {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 28px 32px 32px;
}
.report-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.summary-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  padding: 12px 14px;
}
.summary-label {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}
.summary-item strong {
  color: #0f172a;
  font-size: 14px;
}
.ai-report-content {
  color: #334155;
  line-height: 1.85;
  font-size: 14px;
}
.report-content >>> h1 {
  font-size: 24px;
  font-weight: 700;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 10px;
  margin-top: 0;
  margin-bottom: 18px;
  color: #0f172a;
}
.report-content >>> h2 {
  font-size: 18px;
  font-weight: 700;
  margin-top: 28px;
  margin-bottom: 12px;
  color: #1e293b;
}
.report-content >>> h3 {
  font-size: 16px;
  font-weight: 600;
  margin-top: 22px;
  margin-bottom: 10px;
  color: #334155;
}
.report-content >>> h4 {
  font-size: 15px;
  font-weight: 600;
  margin-top: 18px;
  margin-bottom: 10px;
  color: #334155;
}
.report-content >>> h5 {
  font-size: 14px;
  font-weight: 600;
  margin-top: 16px;
  margin-bottom: 8px;
  color: #475569;
}
.report-content >>> h6 {
  font-size: 13px;
  font-weight: 600;
  margin-top: 14px;
  margin-bottom: 8px;
  color: #64748b;
}
.report-content >>> p {
  margin: 0 0 14px;
}
.report-content >>> ol,
.report-content >>> ul {
  padding-left: 24px;
  margin: 10px 0 16px;
}
.report-content >>> strong {
  color: #1e293b;
  font-weight: 600;
}
.report-content >>> li {
  margin-bottom: 8px;
}
.report-content >>> .task-list-item {
  list-style: none;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-left: -22px;
}
.report-content >>> .task-checkbox {
  width: 14px;
  height: 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 4px;
  background: #ffffff;
  flex: 0 0 auto;
  margin-top: 5px;
}
.report-content >>> .task-checkbox.is-checked {
  background: #2563eb;
  border-color: #2563eb;
  position: relative;
}
.report-content >>> .task-checkbox.is-checked::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 0px;
  width: 4px;
  height: 8px;
  border: solid #ffffff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.report-content >>> .task-text {
  flex: 1;
}
.report-content >>> code {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
  padding: 2px 6px;
  border-radius: 6px;
  font-family: SFMono-Regular, Consolas, Monaco, monospace;
  font-size: 85%;
}
.report-content >>> .table-responsive {
  width: 100%;
  overflow-x: auto;
  margin: 18px 0;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.report-content >>> .markdown-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13.5px;
}
.report-content >>> .markdown-table th {
  background-color: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 10px 14px;
  border-bottom: 2px solid #e2e8f0;
}
.report-content >>> .markdown-table td {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  line-height: 1.5;
}
.report-content >>> .markdown-table tr:last-child td {
  border-bottom: none;
}
.report-content >>> .markdown-table tr:hover td {
  background-color: #f8fafc;
}
.report-content >>> hr {
  border: none;
  height: 1px;
  background-color: #e2e8f0;
  margin: 24px 0;
}
.print-frame {
  position: fixed;
  width: 0;
  height: 0;
  border: 0;
  right: 0;
  bottom: 0;
  opacity: 0;
  pointer-events: none;
}

@media (max-width: 1280px) {
  .detail-page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-report-meta {
    flex-direction: column;
    align-items: stretch;
  }

  .report-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
