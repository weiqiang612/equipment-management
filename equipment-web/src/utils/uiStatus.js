export const CLAIM_STATUS_META = {
  0: { label: '待审批', type: 'warning' },
  1: { label: '已同意', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '已撤回', type: 'info' },
  4: { label: '已退还', type: 'info' },
  5: { label: '直接分配', type: 'primary' }
}

export const MAINTENANCE_STATUS_META = {
  0: { label: '待指派', type: 'info' },
  1: { label: '维修中', type: 'warning' },
  2: { label: '待复核', type: 'primary' },
  3: { label: '已复核可用', type: 'success' },
  4: { label: '转报废', type: 'danger' }
}

export const RISK_LEVEL_META = {
  high: { label: '高风险', type: 'danger' },
  medium: { label: '中风险', type: 'warning' },
  low: { label: '低风险', type: 'success' },
  '高风险': { label: '高风险', type: 'danger' },
  '中风险': { label: '中风险', type: 'warning' },
  '低风险': { label: '低风险', type: 'success' }
}

export const EQUIPMENT_STATUS_META = {
  '在用': { type: 'success' },
  '维修中': { type: 'warning' },
  '待指派': { type: 'info' },
  '待复核': { type: 'primary' },
  '报废': { type: 'danger' }
}

export function getClaimStatusMeta(status) {
  return CLAIM_STATUS_META[status] || { label: '未知', type: 'info' }
}

export function getMaintenanceStatusMeta(status) {
  return MAINTENANCE_STATUS_META[status] || { label: '未知', type: 'info' }
}

export function getRiskLevelMeta(level) {
  return RISK_LEVEL_META[level] || { label: level || '未知', type: 'info' }
}

export function getEquipmentStatusMeta(status) {
  return EQUIPMENT_STATUS_META[status] || { type: 'info' }
}
