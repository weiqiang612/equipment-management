<template>
  <div class="ai-assistant-container">
    <!-- 顶栏头部 -->
    <div class="page-header">
      <div class="header-left">
        <i class="el-icon-cpu ai-icon-animate"></i>
        <div class="header-text">
          <h1>AI 智能辅助决策系统</h1>
          <p>基于数据看板与资产治理结果，一键生成资产运营报告草案与设备健康诊断建议</p>
        </div>
      </div>
    </div>

    <!-- 主要控制区域 -->
    <el-row :gutter="20" class="main-content">
      <el-col :span="24">
        <!-- 如果配置了凭证错误，显示在页面顶部，保持卡片干净 -->
        <el-collapse-transition>
          <div v-if="globalCredentialError" class="global-error-panel">
            <el-alert
              title="AI 智能服务优雅降级通知"
              type="warning"
              :description="globalCredentialError"
              show-icon
              :closable="false"
            >
            </el-alert>
            <div class="help-info">
              <h4>💡 如何启用 AI 辅助服务？</h4>
              <p>系统检测到后端大模型 API 凭证未配置。请联系系统管理员，在服务器中配置以下环境变量并重启服务：</p>
              <ul>
                <li><code>AI_API_KEY</code>：大模型服务商授权 Token/Key。</li>
                <li><code>AI_BASE_URL</code>：OpenAI 兼容协议地址（如 <code>https://api.openai.com/v1</code> 或您的专属中转地址）。</li>
                <li><code>AI_MODEL</code>：选用大模型代号（默认：<code>gpt-4o-mini</code>，亦可切换为 <code>deepseek-chat</code> 等）。</li>
              </ul>
            </div>
          </div>
        </el-collapse-transition>

        <el-card class="glass-card" shadow="never">
          <div slot="header" class="card-header">
            <span><i class="el-icon-document"></i> 运营报告一键生成</span>
            <el-tag type="success" size="small" effect="dark">AI Draft</el-tag>
          </div>

          <div class="control-panel">
            <div class="control-item">
              <span class="label">选择分析周期：</span>
              <el-radio-group v-model="period" size="medium">
                <el-radio-button label="weekly">资产运营周报</el-radio-button>
                <el-radio-button label="monthly">资产运营月报</el-radio-button>
              </el-radio-group>
            </div>
            <el-button 
              type="primary" 
              icon="el-icon-magic-stick" 
              :loading="currentReport.generating" 
              size="medium"
              class="action-btn"
              @click="handleGenerate"
            >
              {{ currentReport.generating ? 'AI 正在深度分析中...' : '生成报告草案' }}
            </el-button>
          </div>

          <!-- 报告展示区域 -->
          <div class="result-area" v-loading="currentReport.generating" :element-loading-text="period === 'weekly' ? '正在读取本周看板 KPI 及异常数据，请求大模型深度分析并撰写周报中...' : '正在汇总本月资产运营与数据治理绩效，请求大模型生成月报建议中...'" element-loading-spinner="el-icon-loading">
            <!-- 报告错误（且非凭证类报错） -->
            <div v-if="currentReport.error && !globalCredentialError" class="error-panel">
              <el-alert
                title="AI 报告生成失败"
                type="error"
                :description="currentReport.error"
                show-icon
                :closable="false"
              >
              </el-alert>
              <div class="retry-hint">
                <p>建议操作：</p>
                <ol>
                  <li>检查后端服务控制台的外部网络连接日志是否超时。</li>
                  <li>点击上方“生成报告草案”按钮重新尝试。</li>
                </ol>
              </div>
            </div>

            <!-- 全局凭证错误时的备用卡片内提示 -->
            <div v-else-if="globalCredentialError" class="error-panel">
              <el-alert
                title="AI 智能辅助服务未启用"
                type="info"
                description="请先在上方阅读并配置大模型 API 凭证以解锁自动报告服务。"
                show-icon
                :closable="false"
              >
              </el-alert>
            </div>

            <!-- 报告生成成功 -->
            <div v-else-if="currentReport.data" class="report-wrapper">
              <div class="report-meta">
                <div class="title-bar">
                  <h3>{{ currentReport.data.title }}</h3>
                  <span class="time"><i class="el-icon-time"></i> 生成时间：{{ formatTime(currentReport.data.generatedTime) }}</span>
                </div>
                <el-button 
                  type="success" 
                  plain 
                  size="mini" 
                  icon="el-icon-document-copy"
                  @click="copyReportContent"
                >
                  复制 Markdown 原文
                </el-button>
              </div>
              <div class="report-content-box">
                <div class="report-content" v-html="renderedContent"></div>
              </div>
            </div>

            <!-- 空白状态 -->
            <div v-else class="empty-state">
              <div class="magic-icon-wrapper">
                <i class="el-icon-magic-stick empty-icon-animate"></i>
              </div>
              <p class="empty-title">暂无已生成的{{ period === 'weekly' ? '周报' : '月报' }}草案</p>
              <p class="empty-desc">
                {{ period === 'weekly' 
                  ? '一键自动读取本周的看板KPI及数据治理异常指标，由大模型为您深度撰写资产运营分析报告草案。' 
                  : '深度汇总本月的资产原值走势、质量评分、疑似重复项和高风险异常设备，为您生成本月盘点与具体改进建议。' }}
              </p>
              <el-button
                type="primary"
                plain
                icon="el-icon-magic-stick"
                size="small"
                class="empty-action-btn"
                @click="handleGenerate"
              >
                生成报告草案
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { draftOperationReport } from '@/api/aiAssistant'
import { renderMarkdown } from '@/utils/markdown'

export default {
  name: 'AiAssistant',
  data() {
    return {
      period: 'weekly',
      reports: {
        weekly: {
          generating: false,
          data: null,
          error: ''
        },
        monthly: {
          generating: false,
          data: null,
          error: ''
        }
      }
    }
  },
  computed: {
    currentReport() {
      return this.reports[this.period]
    },
    renderedContent() {
      if (!this.currentReport || !this.currentReport.data || !this.currentReport.data.content) {
        return ''
      }
      return this.renderMarkdown(this.currentReport.data.content)
    },
    globalCredentialError() {
      const wErr = this.reports.weekly.error || ''
      const mErr = this.reports.monthly.error || ''
      if (wErr.includes('凭证') || wErr.includes('未启用')) return wErr
      if (mErr.includes('凭证') || mErr.includes('未启用')) return mErr
      return ''
    }
  },
  methods: {
    async handleGenerate() {
      const activePeriod = this.period
      const targetReport = this.reports[activePeriod]
      
      targetReport.generating = true
      targetReport.error = ''
      targetReport.data = null
      
      try {
        const res = await draftOperationReport({ period: activePeriod })
        if (res) {
          targetReport.data = res
        } else {
          targetReport.error = '生成报告失败，请重试'
        }
      } catch (err) {
        targetReport.error = err.message || '网络连接超时，请检查后端服务运行状态'
      } finally {
        targetReport.generating = false
      }
    },
    renderMarkdown(text) {
      return renderMarkdown(text)
    },
    formatTime(timeArray) {
      if (!timeArray) return ''
      // 后端 LocalDateTime 传到前端如果是 array 形式如 [2026, 6, 13, 16, 30] 
      if (Array.isArray(timeArray)) {
        const y = timeArray[0]
        const m = String(timeArray[1]).padStart(2, '0')
        const d = String(timeArray[2]).padStart(2, '0')
        const hh = String(timeArray[3]).padStart(2, '0')
        const mm = String(timeArray[4]).padStart(2, '0')
        return `${y}-${m}-${d} ${hh}:${mm}`
      }
      return timeArray.replace('T', ' ').substring(0, 16)
    },
    copyReportContent() {
      const report = this.currentReport.data
      if (!report || !report.content) return
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(report.content)
          .then(() => {
            this.$message({
              message: 'Markdown 原文已成功复制至剪贴板',
              type: 'success'
            })
          })
          .catch(() => {
            this.fallbackCopy(report.content)
          })
      } else {
        this.fallbackCopy(report.content)
      }
    },
    fallbackCopy(text) {
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      try {
        document.execCommand('copy')
        this.$message({
          message: 'Markdown 原文已成功复制至剪贴板',
          type: 'success'
        })
      } catch (err) {
        this.$message.error('复制失败，请手动选择复制内容')
      }
      document.body.removeChild(textArea)
    }
  }
}
</script>

<style scoped>
.ai-assistant-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  padding: 28px 36px;
  border-radius: 16px;
  color: #ffffff;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.15), 0 8px 10px -6px rgba(15, 23, 42, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
}

.ai-icon-animate {
  font-size: 48px;
  margin-right: 20px;
  color: #06b6d4;
  animation: pulse 2.5s infinite;
  filter: drop-shadow(0 0 8px rgba(6, 182, 212, 0.5));
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.08);
    opacity: 1;
    filter: drop-shadow(0 0 14px rgba(6, 182, 212, 0.8));
  }
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
}

.header-text h1 {
  margin: 0 0 6px 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.025em;
  background: linear-gradient(135deg, #ffffff 0%, #cbd5e1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-text p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
}

.global-error-panel {
  margin-bottom: 24px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.glass-card {
  border: 1px solid rgba(226, 232, 240, 0.8);
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 20px -2px rgba(148, 163, 184, 0.08) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(148, 163, 184, 0.15) !important;
}

.card-header {
  font-size: 18px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #1e293b;
}

.card-header i {
  margin-right: 8px;
  color: #3b82f6;
}

.control-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #f1f5f9;
  border-radius: 12px;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.control-item {
  display: flex;
  align-items: center;
}

.control-item .label {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-right: 12px;
}

.action-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border: none;
  font-weight: 600;
  padding: 10px 20px;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.25);
  transition: all 0.3s;
  color: #ffffff;
}

.action-btn:hover {
  opacity: 0.95;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}

.result-area {
  min-height: 350px;
  background-color: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 24px;
  transition: border-color 0.3s;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 24px;
}

.magic-icon-wrapper {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(6, 182, 212, 0.1) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-icon-animate {
  font-size: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: pulseRotate 4s infinite ease-in-out;
}

@keyframes pulseRotate {
  0% {
    transform: scale(1) rotate(0deg);
  }
  50% {
    transform: scale(1.15) rotate(15deg);
  }
  100% {
    transform: scale(1) rotate(0deg);
  }
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #64748b;
  max-width: 480px;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.empty-action-btn {
  border-color: #3b82f6;
  color: #3b82f6;
  font-weight: 600;
}

.empty-action-btn:hover {
  background-color: rgba(59, 130, 246, 0.05);
  border-color: #3b82f6;
}

.error-panel {
  padding: 8px;
}

.retry-hint {
  margin-top: 16px;
  padding: 16px 20px;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 8px;
}

.retry-hint p {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: #991b1b;
}

.retry-hint ol {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #b91c1c;
  line-height: 1.6;
}

.help-info {
  margin-top: 12px;
  background-color: #fffbeb;
  border: 1px solid #fef3c7;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.05);
}

.help-info h4 {
  margin: 0 0 12px 0;
  color: #d97706;
  font-size: 15px;
  font-weight: 600;
}

.help-info p {
  font-size: 13.5px;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.help-info ul {
  margin: 0;
  padding-left: 20px;
}

.help-info li {
  font-size: 13.5px;
  color: #475569;
  margin-bottom: 8px;
  line-height: 1.5;
}

.help-info code {
  background-color: #f1f5f9;
  color: #334155;
  padding: 2px 6px;
  border-radius: 6px;
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 90%;
  border: 1px solid #e2e8f0;
}

.report-wrapper {
  animation: fadeIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.report-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
  margin-bottom: 20px;
}

.title-bar h3 {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.title-bar .time {
  font-size: 12.5px;
  color: #64748b;
  display: flex;
  align-items: center;
}

.title-bar .time i {
  margin-right: 4px;
}

.report-content-box {
  background-color: #fdfdfd;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 28px 36px;
  box-shadow: inset 0 2px 8px rgba(15, 23, 42, 0.015);
}

.report-content {
  color: #334155;
  line-height: 1.85;
  font-size: 14.5px;
}

.report-content >>> h1 {
  font-size: 22px;
  font-weight: 700;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 8px;
  margin-top: 28px;
  margin-bottom: 16px;
  color: #0f172a;
}

.report-content >>> h2 {
  font-size: 18px;
  font-weight: 700;
  margin-top: 24px;
  margin-bottom: 14px;
  color: #1e293b;
}

.report-content >>> h3 {
  font-size: 16px;
  font-weight: 600;
  margin-top: 20px;
  margin-bottom: 12px;
  color: #334155;
}

.report-content >>> strong {
  color: #ef4444;
  background-color: #fef2f2;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  border: 1px solid #fee2e2;
}

.report-content >>> ul {
  padding-left: 24px;
  margin-top: 8px;
  margin-bottom: 14px;
}

.report-content >>> li {
  list-style-type: disc;
  margin-bottom: 8px;
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

.report-content >>> br {
  margin-bottom: 6px;
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
</style>
