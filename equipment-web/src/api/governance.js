import request from '@/utils/request'

// TODO: Remove mock when backend endpoints are ready

// 全局 Mock 数据源
const mockSummary = {
  qualityScore: 92.5,
  totalIssues: 8,
  issueSummary: [
    { type: 'missing_category', name: '设备分类缺失', count: 2 },
    { type: 'custodian_mismatch', name: '保管人与单位不一致', count: 3 },
    { type: 'missing_purchase_date', name: '购入日期缺失', count: 1 },
    { type: 'duplicate_equipment', name: '疑似重复设备', count: 2 }
  ],
  riskDistribution: {
    high: 4,
    medium: 8,
    low: 38
  },
  departmentDistribution: [
    { unitCode: 'D001', unitName: '研发部', highCount: 2, mediumCount: 4, lowCount: 15 },
    { unitCode: 'D002', unitName: '行政部', highCount: 1, mediumCount: 2, lowCount: 13 },
    { unitCode: 'D003', unitName: '财务部', highCount: 1, mediumCount: 2, lowCount: 10 }
  ],
  categoryDistribution: [
    { categoryId: 'C001', categoryName: '计算机设备', highCount: 2, mediumCount: 4, lowCount: 18 },
    { categoryId: 'C002', categoryName: '网络设备', highCount: 1, mediumCount: 3, lowCount: 12 },
    { categoryId: 'C003', categoryName: '办公设备', highCount: 1, mediumCount: 1, lowCount: 8 }
  ],
  costAnomaliesCount: 4,
  costAnomalies: [
    { equipId: 'E001', equipName: '高配研发服务器', originalValue: 50000.00, maintCost: 25000.00, costRatio: 0.50 },
    { equipId: 'E008', equipName: '激光打印机', originalValue: 4000.00, maintCost: 1800.00, costRatio: 0.45 },
    { equipId: 'E015', equipName: '办公笔记本电脑', originalValue: 8000.00, maintCost: 2800.00, costRatio: 0.35 },
    { equipId: 'E022', equipName: '会议室投影仪', originalValue: 6000.00, maintCost: 1900.00, costRatio: 0.31 }
  ],
  idleCount: 6,
  idleEquipments: [
    { equipId: 'E003', equipName: '空闲测试手机', originalValue: 3000.00, purchaseDate: '2025-10-10', unitName: '研发部' },
    { equipId: 'E010', equipName: '备用路由器', originalValue: 1500.00, purchaseDate: '2025-12-01', unitName: '行政部' },
    { equipId: 'E018', equipName: '未分配台式机', originalValue: 6000.00, purchaseDate: '2026-02-15', unitName: '财务部' },
    { equipId: 'E029', equipName: '备用投影幕布', originalValue: 800.00, purchaseDate: '2025-05-20', unitName: '研发部' },
    { equipId: 'E033', equipName: '备用核心交换机', originalValue: 8000.00, purchaseDate: '2025-08-11', unitName: '研发部' },
    { equipId: 'E041', equipName: '开发板芯片', originalValue: 1200.00, purchaseDate: '2026-03-01', unitName: '行政部' }
  ]
}

const mockEquipmentRisks = [
  // 高风险设备 (AC-002: ageRatio >= 0.9 或 maintCount >= 3 或 costRatio >= 0.3)
  {
    equipId: 'E001',
    equipName: '高配研发服务器',
    categoryId: 'C001',
    categoryName: '计算机设备',
    unitCode: 'D001',
    unitName: '研发部',
    riskLevel: 'high',
    riskReason: '维修费用占比达 50% (超过高风险阈值 30%)，且维修次数达 3 次',
    healthScore: 40,
    maintCount: 3,
    originalValue: 50000.00,
    maintCost: 25000.00,
    costRatio: 0.50,
    ageRatio: 0.60,
    custodian: 'operator1',
    custodianRealName: '张三'
  },
  {
    equipId: 'E008',
    equipName: '激光打印机',
    categoryId: 'C003',
    categoryName: '办公设备',
    unitCode: 'D002',
    unitName: '行政部',
    riskLevel: 'high',
    riskReason: '使用年限占比达 92% (超过高风险阈值 90%)，设备严重老化',
    healthScore: 35,
    maintCount: 2,
    originalValue: 4000.00,
    maintCost: 1800.00,
    costRatio: 0.45,
    ageRatio: 0.92,
    custodian: 'operator2',
    custodianRealName: '李四'
  },
  {
    equipId: 'E012',
    equipName: '防火墙网关',
    categoryId: 'C002',
    categoryName: '网络设备',
    unitCode: 'D001',
    unitName: '研发部',
    riskLevel: 'high',
    riskReason: '累计维修 4 次 (超过高风险阈值 3 次)',
    healthScore: 45,
    maintCount: 4,
    originalValue: 12000.00,
    maintCost: 3200.00,
    costRatio: 0.27,
    ageRatio: 0.55,
    custodian: 'operator1',
    custodianRealName: '张三'
  },
  {
    equipId: 'E015',
    equipName: '办公笔记本电脑',
    categoryId: 'C001',
    categoryName: '计算机设备',
    unitCode: 'D002',
    unitName: '行政部',
    riskLevel: 'high',
    riskReason: '维修费用占比达 35% (超过高风险阈值 30%)',
    healthScore: 48,
    maintCount: 2,
    originalValue: 8000.00,
    maintCost: 2800.00,
    costRatio: 0.35,
    ageRatio: 0.70,
    custodian: 'operator3',
    custodianRealName: '王五'
  },

  // 中风险设备 (ageRatio >= 0.75 或 maintCount >= 2 或 costRatio >= 0.15 或 status = '维修')
  {
    equipId: 'E022',
    equipName: '会议室投影仪',
    categoryId: 'C003',
    categoryName: '办公设备',
    unitCode: 'D002',
    unitName: '行政部',
    riskLevel: 'medium',
    riskReason: '设备当前状态处于 维修 状态',
    healthScore: 65,
    maintCount: 1,
    originalValue: 6000.00,
    maintCost: 1900.00,
    costRatio: 0.31,
    ageRatio: 0.40,
    custodian: 'operator2',
    custodianRealName: '李四'
  },
  {
    equipId: 'E025',
    equipName: '开发测试PC',
    categoryId: 'C001',
    categoryName: '计算机设备',
    unitCode: 'D001',
    unitName: '研发部',
    riskLevel: 'medium',
    riskReason: '使用年限占比达 80% (超过中风险阈值 75%)',
    healthScore: 70,
    maintCount: 1,
    originalValue: 7000.00,
    maintCost: 800.00,
    costRatio: 0.11,
    ageRatio: 0.80,
    custodian: 'operator1',
    custodianRealName: '张三'
  },
  {
    equipId: 'E028',
    equipName: '千兆核心交换机',
    categoryId: 'C002',
    categoryName: '网络设备',
    unitCode: 'D001',
    unitName: '研发部',
    riskLevel: 'medium',
    riskReason: '维修次数达 2 次，维修费用占比 18% (超过中风险阈值 15%)',
    healthScore: 68,
    maintCount: 2,
    originalValue: 15000.00,
    maintCost: 2700.00,
    costRatio: 0.18,
    ageRatio: 0.45,
    custodian: 'operator1',
    custodianRealName: '张三'
  },
  {
    equipId: 'E030',
    equipName: '多媒体音响',
    categoryId: 'C003',
    categoryName: '办公设备',
    unitCode: 'D003',
    unitName: '财务部',
    riskLevel: 'medium',
    riskReason: '维修次数达 2 次 (超过中风险阈值 2 次)',
    healthScore: 72,
    maintCount: 2,
    originalValue: 3000.00,
    maintCost: 400.00,
    costRatio: 0.13,
    ageRatio: 0.50,
    custodian: 'operator4',
    custodianRealName: '赵六'
  },

  // 低风险设备 (未命中中高风险)
  {
    equipId: 'E035',
    equipName: '财务记账电脑',
    categoryId: 'C001',
    categoryName: '计算机设备',
    unitCode: 'D003',
    unitName: '财务部',
    riskLevel: 'low',
    riskReason: '运行状况良好，各项指标均在安全阈值内',
    healthScore: 95,
    maintCount: 0,
    originalValue: 6500.00,
    maintCost: 0.00,
    costRatio: 0.00,
    ageRatio: 0.20,
    custodian: 'operator4',
    custodianRealName: '赵六'
  },
  {
    equipId: 'E038',
    equipName: '无线AP',
    categoryId: 'C002',
    categoryName: '网络设备',
    unitCode: 'D002',
    unitName: '行政部',
    riskLevel: 'low',
    riskReason: '各项指标在安全区间，无故障记录',
    healthScore: 90,
    maintCount: 1,
    originalValue: 1200.00,
    maintCost: 100.00,
    costRatio: 0.08,
    ageRatio: 0.35,
    custodian: 'operator3',
    custodianRealName: '王五'
  },
  {
    equipId: 'E045',
    equipName: '部门碎纸机',
    categoryId: 'C003',
    categoryName: '办公设备',
    unitCode: 'D003',
    unitName: '财务部',
    riskLevel: 'low',
    riskReason: '指标正常',
    healthScore: 88,
    maintCount: 1,
    originalValue: 1500.00,
    maintCost: 150.00,
    costRatio: 0.10,
    ageRatio: 0.60,
    custodian: 'operator4',
    custodianRealName: '赵六'
  }
]

// 1. 获取数据治理总览
export async function getGovernanceSummary() {
  // eslint-disable-next-line no-unused-vars
  const _req = request
  return Promise.resolve({
    code: 1,
    msg: 'success',
    data: mockSummary
  })
}

// 2. 分页获取风险设备清单
export async function getEquipmentRisks(params) {
  const { riskLevel, unitCode, categoryId, page = 1, pageSize = 10 } = params || {}
  
  // 模拟后端过滤逻辑
  let filtered = [...mockEquipmentRisks]
  if (riskLevel) {
    filtered = filtered.filter(item => item.riskLevel === riskLevel)
  }
  if (unitCode) {
    filtered = filtered.filter(item => item.unitCode === unitCode)
  }
  if (categoryId) {
    filtered = filtered.filter(item => item.categoryId === categoryId)
  }

  // 模拟单位级隔离数据逻辑 (Role 2 权限裁剪)
  const loggedRoleStr = localStorage.getItem('role')
  const loggedRole = loggedRoleStr !== null ? parseInt(loggedRoleStr, 10) : null
  const loggedUnitCode = localStorage.getItem('unitCode')
  
  if (loggedRole === 2 && loggedUnitCode) {
    // 资产管理员只能看本单位设备
    filtered = filtered.filter(item => item.unitCode === loggedUnitCode)
  }

  // 模拟分页
  const total = filtered.length
  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + parseInt(pageSize, 10)
  const rows = filtered.slice(startIndex, endIndex)

  return Promise.resolve({
    code: 1,
    msg: 'success',
    data: {
      total,
      rows
    }
  })
}
