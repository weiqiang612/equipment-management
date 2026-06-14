<template>
  <div class="message-center-container" v-loading="loading">
    <div class="page-header">
      <div class="page-header-main">
        <div class="title-area">
          <i class="el-icon-bell header-icon"></i>
          <div>
            <h2 class="page-title">消息中心</h2>
            <p class="page-subtitle">从通知直接进入审批、检修和风险处理入口，避免消息与待办割裂。</p>
          </div>
        </div>
        <el-button type="primary" size="small" icon="el-icon-refresh" @click="loadMessages">
          刷新消息
        </el-button>
      </div>

      <div class="summary-strip">
        <div class="summary-chip is-primary">
          <span class="summary-chip-label">当前视图</span>
          <strong>{{ getCategoryTitle(activeCategory) }}</strong>
        </div>
        <div class="summary-chip" :class="{ 'is-warning': unreadCount > 0 }">
          <span class="summary-chip-label">未读待处理</span>
          <strong>{{ unreadCount }}</strong>
        </div>
        <div class="summary-tip">
          已读消息保留为历史线索，未读消息默认优先展示。
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="content-row">
      <!-- 左侧分类选择器 -->
      <el-col :xs="24" :sm="24" :md="6" :lg="5">
        <el-card class="menu-card" shadow="hover">
          <div class="menu-header">
            <i class="el-icon-chat-dot-round"></i>
            <span>视图筛选</span>
          </div>
          <el-menu
            :default-active="activeCategory"
            class="category-menu"
            @select="handleCategorySelect"
          >
            <el-menu-item index="all">
              <i class="el-icon-message"></i>
              <span slot="title" class="category-item-content">
                <span class="category-item-label">全部消息</span>
              </span>
            </el-menu-item>
            <el-menu-item index="unread">
              <i class="el-icon-warning-outline"></i>
              <span slot="title" class="category-item-content">
                <span class="category-item-label">未读消息</span>
                <span v-if="unreadCount > 0" class="message-notification-badge">
                  {{ formatUnreadCount(unreadCount) }}
                </span>
              </span>
            </el-menu-item>
            <el-menu-item index="read">
              <i class="el-icon-circle-check"></i>
              <span slot="title" class="category-item-content">
                <span class="category-item-label">已读消息</span>
              </span>
            </el-menu-item>
          </el-menu>

          <div class="action-btn-wrapper" v-if="unreadCount > 0">
            <el-button
              type="primary"
              size="small"
              icon="el-icon-check"
              plain
              class="all-read-btn"
              @click="handleReadAll"
            >
              一键全部已读
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧信息流展示 -->
      <el-col :xs="24" :sm="24" :md="18" :lg="19">
        <el-card class="list-card" shadow="hover" v-loading="listLoading">
          <div slot="header" class="list-header">
            <span class="list-title">
              {{ getCategoryTitle(activeCategory) }}
            </span>
            <span class="list-subtitle">点击“去处理”后进入对应业务页面并保留当前消息语境</span>
          </div>

          <div class="message-list-wrapper">
            <!-- 空状态 -->
            <div v-if="messageList.length === 0" class="empty-state-card">
              <el-empty
                :description="getEmptyDescription(activeCategory)"
                :image-size="110"
              />
              <div class="empty-state-actions">
                <el-button size="small" @click="handleCategorySelect('all')">查看全部消息</el-button>
                <el-button type="primary" size="small" plain @click="loadMessages">重新刷新</el-button>
              </div>
            </div>

            <!-- 消息项 -->
            <div
              v-for="msg in messageList"
              :key="msg.id"
              class="message-item-card"
              :class="[
                `event-${msg.refType || 'default'}`,
                { 'is-unread': msg.status === 0 }
              ]"
            >
              <div class="msg-card-header">
                <div class="msg-title-area">
                  <el-tag
                    :type="getEventTagType(msg.refType)"
                    size="mini"
                    effect="dark"
                    class="msg-type-tag"
                  >
                    {{ getEventLabel(msg.refType) }}
                  </el-tag>
                  <span class="msg-title">{{ msg.title }}</span>
                  <span class="unread-dot" v-if="msg.status === 0"></span>
                </div>
                <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
              </div>

              <div class="msg-card-content">
                {{ msg.content }}
              </div>

              <div class="msg-card-footer">
                <div class="ref-info" v-if="msg.refId">
                  <i class="el-icon-link"></i>
                  <span>关联对象ID: {{ msg.refId }}</span>
                </div>
                <div class="ref-info" v-else>
                  <i class="el-icon-info"></i>
                  <span>该消息仅供提示，无直接关联对象。</span>
                </div>
                <div class="msg-actions">
                  <el-button
                    v-if="msg.status === 0"
                    type="text"
                    size="small"
                    icon="el-icon-check"
                    class="mark-read-btn"
                    @click="handleReadSingle(msg.id)"
                  >
                    标记已读
                  </el-button>
                  <el-button
                    type="primary"
                    size="mini"
                    icon="el-icon-right"
                    class="process-btn"
                    :disabled="!hasProcessEntry(msg)"
                    @click="handleProcess(msg)"
                  >
                    {{ hasProcessEntry(msg) ? '去处理' : '仅查看通知' }}
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination-container" v-if="total > 0">
              <el-pagination
                background
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="queryParams.page"
                :page-sizes="[5, 10, 20, 50]"
                :page-size="queryParams.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getMessages, getUnreadCount, readMessage, readAllMessages } from '@/api/message'

export default {
  name: 'MessageCenter',
  data() {
    return {
      loading: false,
      listLoading: false,
      unreadCount: 0,
      activeCategory: 'unread', // 默认聚焦在未读消息
      messageList: [],
      total: 0,
      queryParams: {
        status: 0, // 0-未读, 1-已读, undefined-全部
        page: 1,
        pageSize: 10
      }
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      this.fetchUnreadCount()
      this.loadMessages()
    },
    // 获取未读消息数量
    async fetchUnreadCount() {
      try {
        const count = await getUnreadCount()
        this.unreadCount = typeof count === 'number' ? count : 0
        // 通知父组件 App.vue 更新侧边栏和头部角标
        this.$emit('refresh-unread')
      } catch (err) {
        console.error('Failed to fetch unread count:', err)
      }
    },
    // 加载消息列表
    async loadMessages() {
      this.listLoading = true
      try {
        const res = await getMessages(this.queryParams)
        const { rows, total } = res || {}
        this.messageList = rows || []
        this.total = total || 0
      } catch (err) {
        this.$message.error('加载消息列表失败，请重试')
        console.error('Failed to load messages:', err)
      } finally {
        this.listLoading = false
      }
    },
    // 菜单类别切换
    handleCategorySelect(key) {
      this.activeCategory = key
      this.queryParams.page = 1
      if (key === 'all') {
        this.queryParams.status = undefined
      } else if (key === 'unread') {
        this.queryParams.status = 0
      } else if (key === 'read') {
        this.queryParams.status = 1
      }
      this.loadMessages()
    },
    // 一键全部已读
    async handleReadAll() {
      this.loading = true
      try {
        await readAllMessages()
        this.$message.success('已将所有未读消息标记为已读')
        this.fetchUnreadCount()
        this.loadMessages()
      } catch (err) {
        this.$message.error('操作失败，请重试')
        console.error('Failed to mark all read:', err)
      } finally {
        this.loading = false
      }
    },
    // 标记单条已读
    async handleReadSingle(id) {
      try {
        await readMessage(id)
        this.fetchUnreadCount()
        this.loadMessages()
      } catch (err) {
        this.$message.error('操作失败，请重试')
        console.error('Failed to mark message read:', err)
      }
    },
    // 跳转闭环处理
    async handleProcess(msg) {
      const { id, refType, refId, status } = msg
      
      // 如果未读，跳转前自动标记为已读
      if (status === 0) {
        try {
          await readMessage(id)
          this.fetchUnreadCount()
        } catch (err) {
          console.error('Auto mark read before navigation failed:', err)
        }
      }

      // 依据 refType 进行跳转
      if (refType === 'equipment') {
        this.$router.push(`/equipment/detail/${refId}`).catch(() => {})
      } else if (refType === 'claim') {
        this.$router.push({
          path: '/equipment/claim',
          query: { claimId: refId }
        }).catch(() => {})
      } else if (refType === 'maintenance') {
        this.$router.push({
          path: '/equipment/maintenance',
          query: { maintId: refId }
        }).catch(() => {})
      } else {
        this.$message.warning('该通知无对应的处理入口')
      }
    },
    // 辅助转换
    getCategoryTitle(key) {
      const titles = {
        all: '全部通知消息',
        unread: '未读待处理消息',
        read: '已读历史消息'
      }
      return titles[key] || '消息列表'
    },
    getEmptyDescription(key) {
      const descriptions = {
        all: '当前没有可展示的通知消息',
        unread: '当前没有未读待处理消息，可以返回全部消息查看历史线索',
        read: '当前没有已读历史消息'
      }
      return descriptions[key] || '暂无相关消息'
    },
    getEventLabel(refType) {
      const labels = {
        equipment: '设备风险',
        claim: '领用审批',
        maintenance: '检修超时'
      }
      return labels[refType] || '系统通知'
    },
    getEventTagType(refType) {
      const types = {
        equipment: 'danger',
        claim: 'primary',
        maintenance: 'warning'
      }
      return types[refType] || 'info'
    },
    hasProcessEntry(msg) {
      if (!msg || !msg.refId) {
        return false
      }
      return ['equipment', 'claim', 'maintenance'].includes(msg.refType)
    },
    formatTime(val) {
      if (!val) return ''
      return val.replace('T', ' ').substring(0, 19)
    },
    formatUnreadCount(count) {
      if (typeof count !== 'number' || count <= 0) {
        return ''
      }
      return count > 99 ? '99+' : String(count)
    },
    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.queryParams.page = 1
      this.loadMessages()
    },
    handleCurrentChange(val) {
      this.queryParams.page = val
      this.loadMessages()
    }
  }
}
</script>

<style scoped>
.message-center-container {
  padding: 0 10px;
}

.page-header {
  background: #fff;
  padding: 18px 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-area {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 28px;
  color: #409eff;
  margin-right: 15px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.page-subtitle {
  margin: 5px 0 0 0;
  font-size: 13px;
  color: #909399;
}

.summary-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.summary-chip {
  min-width: 148px;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #e6ebf2;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-chip strong {
  color: #1f2d3d;
  font-size: 14px;
}

.summary-chip.is-primary {
  border-color: #d9ecff;
  background: #edf6ff;
}

.summary-chip.is-warning {
  border-color: #faecd8;
  background: #fff8ee;
}

.summary-chip-label {
  color: #7a8797;
  font-size: 12px;
}

.summary-tip {
  color: #7a8797;
  font-size: 12px;
}

.content-row {
  margin-top: 10px;
}

.menu-card {
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.menu-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
}

.menu-header i {
  color: #409eff;
  margin-right: 8px;
  font-size: 16px;
}

.category-menu {
  border-right: none;
  margin-top: 10px;
}

.category-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
  padding: 0 10px !important;
  border-radius: 4px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.category-menu .el-menu-item.is-active {
  background-color: #ecf5ff;
}

.category-menu .el-menu-item i {
  font-size: 16px;
  margin-right: 10px;
}

.category-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.category-item-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-notification-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  margin-left: 12px;
  border-radius: 10px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  box-sizing: border-box;
  flex-shrink: 0;
}

.action-btn-wrapper {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.all-read-btn {
  width: 100%;
}

.list-card {
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  min-height: 500px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.list-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.list-subtitle {
  color: #909399;
  font-size: 12px;
  text-align: right;
}

.message-list-wrapper {
  padding: 5px 0;
}

.empty-state-card {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafbfd;
  padding: 18px;
}

.empty-state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.message-item-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 15px;
  transition: all 0.25s ease;
  position: relative;
  background-color: #fafafa;
}

.message-item-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  transform: translateY(-1px);
}

.message-item-card.is-unread {
  background-color: #fff;
  border-color: #dcdfe6;
}

/* 类别小竖条色彩层级 */
.message-item-card.event-equipment {
  border-left: 4px solid #f56c6c;
}

.message-item-card.event-claim {
  border-left: 4px solid #409eff;
}

.message-item-card.event-maintenance {
  border-left: 4px solid #e6a23c;
}

.message-item-card.event-default {
  border-left: 4px solid #909399;
}

.msg-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.msg-title-area {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.msg-type-tag {
  margin-right: 8px;
  flex-shrink: 0;
}

.msg-title {
  font-size: 14px;
  color: #303133;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-unread .msg-title {
  color: #1f2f3d;
}

.unread-dot {
  width: 7px;
  height: 7px;
  background-color: #f56c6c;
  border-radius: 50%;
  margin-left: 8px;
  display: inline-block;
  flex-shrink: 0;
}

.msg-time {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
  flex-shrink: 0;
}

.msg-card-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  word-break: break-all;
}

.is-unread .msg-card-content {
  color: #485a6a;
}

.msg-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.ref-info {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
}

.ref-info i {
  margin-right: 5px;
}

.msg-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.mark-read-btn {
  margin-right: 12px;
  color: #606266;
}

.mark-read-btn:hover {
  color: #409eff;
}

.process-btn {
  border-radius: 4px;
}

.pagination-container {
  margin-top: 25px;
  text-align: right;
}

@media (max-width: 1280px) {
  .page-header-main {
    flex-direction: column;
    align-items: stretch;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .list-subtitle {
    text-align: left;
  }
}
</style>
