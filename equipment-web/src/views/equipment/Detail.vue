<template>
  <div class="equipment-detail-container">
    <!-- 头部面包屑与返回 -->
    <el-page-header @back="goBack" content="设备生命周期详情" style="margin-bottom: 20px;"></el-page-header>

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
                    <el-tag :type="scope.row.maintStatus === 2 ? 'success' : 'warning'" size="mini">
                      {{ scope.row.maintStatus === 2 ? '完工' : scope.row.maintStatus === 1 ? '维修中' : '待指派' }}
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
  </div>
</template>

<script>
import { getEquipmentDetail } from '@/api/equipment'

export default {
  name: 'EquipmentDetail',
  data() {
    return {
      equipId: '',
      activeTab: 'claims',
      loading: false,
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
      const map = {
        '在用': 'success',
        '维修中': 'warning',
        '报废': 'danger',
        '待指派': 'info'
      }
      return map[status] || ''
    },
    getClaimStatusTag(status) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger'
      }
      return map[status] || ''
    },
    formatClaimStatus(status) {
      const map = {
        0: '待审批',
        1: '同意',
        2: '拒绝'
      }
      return map[status] !== undefined ? map[status] : ''
    }
  }
}
</script>

<style scoped>
.equipment-detail-container {
  padding: 10px;
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
</style>
