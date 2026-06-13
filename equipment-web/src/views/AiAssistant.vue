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
              :loading="generating" 
              size="medium"
              class="action-btn"
              @click="handleGenerate"
            >
              {{ generating ? 'AI 正在深度分析中...' : '生成报告草案' }}
            </el-button>
          </div>

          <!-- 报告展示区域 -->
          <div class="result-area" v-loading="generating" element-loading-text="正在读取看板KPI及异常数据，请求大模型生成中..." element-loading-spinner="el-icon-loading">
            <!-- 降级/未启用警告 -->
            <div v-if="errorMsg" class="error-panel">
              <el-alert
                title="AI 服务不可用"
                type="warning"
                :description="errorMsg"
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

            <!-- 报告生成成功 -->
            <div v-else-if="reportData" class="report-wrapper">
              <div class="report-meta">
                <div class="title-bar">
                  <h3>{{ reportData.title }}</h3>
                  <span class="time"><i class="el-icon-time"></i> 生成时间：{{ formatTime(reportData.generatedTime) }}</span>
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
              <div class="report-content" v-html="renderedContent"></div>
            </div>

            <!-- 空白状态 -->
            <div v-else class="empty-state">
              <i class="el-icon-magic-stick"></i>
              <p>暂无已生成的报告。请在上方选择周期并点击“生成报告草案”开始分析。</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { draftOperationReport } from '@/api/aiAssistant'

export default {
  name: 'AiAssistant',
  data() {
    return {
      period: 'weekly',
      generating: false,
      reportData: null,
      errorMsg: ''
    }
  },
  computed: {
    renderedContent() {
      if (!this.reportData || !this.reportData.content) {
        return ''
      }
      return this.renderMarkdown(this.reportData.content)
    }
  },
  methods: {
    async handleGenerate() {
      this.generating = true
      this.errorMsg = ''
      this.reportData = null
      
      try {
        const res = await draftOperationReport({ period: this.period })
        if (res.code === 1) {
          this.reportData = res.data
        } else {
          this.errorMsg = res.msg || '生成报告失败，请重试'
        }
      } catch (err) {
        this.errorMsg = err.message || '网络连接超时，请检查后端服务运行状态'
      } finally {
        this.generating = false
      }
    },
    // 极简且稳健的 Markdown 转换 HTML
    renderMarkdown(text) {
      if (!text) return ''
      
      let html = text
        // 转义 HTML 特殊字符防止 XSS
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        // 渲染 H3
        .replace(/^### (.*$)/gim, '<h3>$1</h3>')
        // 渲染 H2
        .replace(/^## (.*$)/gim, '<h2>$1</h2>')
        // 渲染 H1
        .replace(/^# (.*$)/gim, '<h1>$1</h1>')
        // 渲染粗体
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        // 渲染无序列表
        .replace(/^\s*-\s+(.*$)/gim, '<li>$1</li>')
        // 渲染行内代码
        .replace(/`(.*?)`/g, '<code>$1</code>')
        // 渲染换行
        .replace(/\n/g, '<br/>')

      // 用 ul 包裹连续的 li 列表元素
      html = html.replace(/(<li>.*?<\/li>)+/g, '<ul>$&</ul>')
      return html
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
      if (!this.reportData || !this.reportData.content) return
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(this.reportData.content)
          .then(() => {
            this.$message({
              message: 'Markdown 原文已成功复制至剪贴板',
              type: 'success'
            })
          })
          .catch(() => {
            this.fallbackCopy(this.reportData.content)
          })
      } else {
        this.fallbackCopy(this.reportData.content)
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
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1f4068 0%, #162447 100%);
  padding: 24px 30px;
  border-radius: 12px;
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
}

.ai-icon-animate {
  font-size: 44px;
  margin-right: 18px;
  color: #00adb5;
  animation: pulse 2.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.1);
    opacity: 1;
    text-shadow: 0 0 10px #00adb5;
  }
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
}

.header-text h1 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
}

.header-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.85;
}

.glass-card {
  border: 1px solid rgba(220, 223, 230, 0.7);
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03) !important;
}

.card-header {
  font-size: 16px;
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #303133;
}

.card-header i {
  margin-right: 6px;
  color: #409eff;
}

.control-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.control-item {
  display: flex;
  align-items: center;
}

.control-item .label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-right: 10px;
}

.action-btn {
  background: linear-gradient(90deg, #409eff 0%, #00adb5 100%);
  border: none;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(0, 173, 181, 0.2);
  transition: all 0.3s;
}

.action-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.result-area {
  min-height: 300px;
  background-color: #ffffff;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  padding-top: 60px;
}

.empty-state i {
  font-size: 54px;
  color: #dcdfe6;
  margin-bottom: 15px;
}

.empty-state p {
  font-size: 14px;
}

.error-panel {
  padding: 10px;
}

.help-info {
  margin-top: 24px;
  background-color: #fffaf0;
  border: 1px solid #ffe8cc;
  padding: 16px 20px;
  border-radius: 6px;
}

.help-info h4 {
  margin: 0 0 10px 0;
  color: #e6a23c;
}

.help-info p {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px 0;
}

.help-info ul {
  margin: 0;
  padding-left: 20px;
}

.help-info li {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  line-height: 1.5;
}

.help-info code {
  background-color: #f4f4f5;
  color: #909399;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.report-wrapper {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
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
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.title-bar h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #303133;
}

.title-bar .time {
  font-size: 12px;
  color: #909399;
}

.report-content {
  color: #2c3e50;
  line-height: 1.8;
  font-size: 14px;
}

.report-content >>> h1 {
  font-size: 22px;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 8px;
  margin-top: 24px;
  margin-bottom: 16px;
  color: #1a202c;
}

.report-content >>> h2 {
  font-size: 18px;
  margin-top: 20px;
  margin-bottom: 14px;
  color: #2d3748;
}

.report-content >>> h3 {
  font-size: 15px;
  margin-top: 16px;
  margin-bottom: 10px;
  color: #4a5568;
}

.report-content >>> strong {
  color: #e53e3e;
  background-color: #fff5f5;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 600;
}

.report-content >>> ul {
  padding-left: 20px;
  margin-top: 8px;
  margin-bottom: 12px;
}

.report-content >>> li {
  list-style-type: disc;
  margin-bottom: 6px;
}

.report-content >>> code {
  background-color: #f7fafc;
  border: 1px solid #e2e8f0;
  color: #4a5568;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: SFMono-Regular, Consolas, Monaco, monospace;
  font-size: 85%;
}

.report-content >>> br {
  margin-bottom: 4px;
}
</style>
