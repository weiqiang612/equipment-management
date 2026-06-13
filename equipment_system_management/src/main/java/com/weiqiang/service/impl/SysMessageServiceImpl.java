package com.weiqiang.service.impl;

import com.weiqiang.dao.SysMessageDao;
import com.weiqiang.pojo.SysMessage;
import com.weiqiang.pojo.PageBean;
import com.weiqiang.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统消息业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMessageServiceImpl implements SysMessageService {

    private final SysMessageDao sysMessageDao;

    // 内存同步冷却缓存 (用户 -> 最后同步时间戳)
    private final Map<String, Long> lastSyncTimeMap = new ConcurrentHashMap<>((int) ((500 / 0.75f) + 1));
    private static final long COOL_DOWN_MS = 30000L;

    @Override
    @Transactional
    public void syncMessages(final String username, final Integer role, final String unitCode) {
        if (username == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Long lastSync = lastSyncTimeMap.get(username);
        if (lastSync != null && (now - lastSync) < COOL_DOWN_MS) {
            log.info("用户 {} 同步请求处于30秒冷却期内，跳过本次扫描", username);
            return;
        }

        log.info("开始为用户 {} (角色: {}, 单位: {}) 同步并扫描消息", username, role, unitCode);
        final List<SysMessage> scannedList = new ArrayList<>((int) ((20 / 0.75f) + 1));
        final Set<String> targetEventTypes = new java.util.HashSet<>((int) ((3 / 0.75f) + 1));

        if (role != null) {
            if (role == 2) {
                targetEventTypes.add("high_risk_equipment");
                targetEventTypes.add("pending_claim");
                targetEventTypes.add("overdue_maintenance");
                // 资产管理员：扫描高风险设备、积压审批、本单位超时工单抄送
                if (unitCode != null && !unitCode.trim().isEmpty()) {
                    final List<SysMessage> highRiskEquips = sysMessageDao.listHighRiskEquipment(unitCode);
                    if (highRiskEquips != null) {
                        for (final SysMessage scan : highRiskEquips) {
                            final SysMessage msg = new SysMessage();
                            msg.setTitle("高风险设备: " + scan.getTitle());
                            msg.setContent("设备[" + scan.getRefId() + "]已满足高风险设备特征(超期/多维修/高费用)，请尽快处理。");
                            msg.setEventType("high_risk_equipment");
                            msg.setTargetUser(username);
                            msg.setStatus(0);
                            msg.setIsValid(1);
                            msg.setRefType("equipment");
                            msg.setRefId(scan.getRefId());
                            scannedList.add(msg);
                        }
                    }

                    final List<SysMessage> pendingClaims = sysMessageDao.listPendingClaim(unitCode);
                    if (pendingClaims != null) {
                        for (final SysMessage scan : pendingClaims) {
                            final SysMessage msg = new SysMessage();
                            msg.setTitle("积压审批: " + scan.getTitle());
                            msg.setContent("有待您审批的领用申请已超时24小时，设备: " + scan.getTitle() + " (ID: " + scan.getRefId() + ")");
                            msg.setEventType("pending_claim");
                            msg.setTargetUser(username);
                            msg.setStatus(0);
                            msg.setIsValid(1);
                            msg.setRefType("claim");
                            msg.setRefId(scan.getRefId());
                            scannedList.add(msg);
                        }
                    }

                    final List<SysMessage> overdueMaints = sysMessageDao.listOverdueMaintenanceByUnit(unitCode);
                    if (overdueMaints != null) {
                        for (final SysMessage scan : overdueMaints) {
                            final SysMessage msg = new SysMessage();
                            msg.setTitle("超时未完工维保工单(抄送): " + scan.getTitle());
                            msg.setContent("本单位的维保工单已超过48小时未完成，设备: " + scan.getTitle() + " (工单号: " + scan.getRefId() + ")");
                            msg.setEventType("overdue_maintenance");
                            msg.setTargetUser(username);
                            msg.setStatus(0);
                            msg.setIsValid(1);
                            msg.setRefType("maintenance");
                            msg.setRefId(scan.getRefId());
                            scannedList.add(msg);
                        }
                    }
                }
            } else if (role == 1) {
                targetEventTypes.add("overdue_maintenance");
                // 维修工程师：扫描超时维保工单
                final List<SysMessage> overdueMaints = sysMessageDao.listOverdueMaintenance(username);
                if (overdueMaints != null) {
                    for (final SysMessage scan : overdueMaints) {
                        final SysMessage msg = new SysMessage();
                        msg.setTitle("超时未完工维保工单: " + scan.getTitle());
                        msg.setContent("指派给您的维保工单已超过48小时未完成，设备: " + scan.getTitle() + " (工单号: " + scan.getRefId() + ")");
                        msg.setEventType("overdue_maintenance");
                        msg.setTargetUser(username);
                        msg.setStatus(0);
                        msg.setIsValid(1);
                        msg.setRefType("maintenance");
                        msg.setRefId(scan.getRefId());
                        scannedList.add(msg);
                    }
                }
            }
        }

        // 增量去重与失效逻辑
        final Set<String> scannedMessageKeys = new HashSet<>((int) ((scannedList.size() / 0.75f) + 1));
        for (final SysMessage item : scannedList) {
            final String key = item.getEventType() + ":" + item.getRefType() + ":" + item.getRefId();
            scannedMessageKeys.add(key);

            // 去重检查
            final SysMessage existing = sysMessageDao.getValidMessageByRef(username, item.getEventType(), item.getRefType(), item.getRefId());
            if (existing == null) {
                sysMessageDao.insert(item);
                log.info("为用户 {} 新增消息: {}", username, item.getTitle());
            }
        }

        // 失效检查 (仅清理本次同步涉及的目标事件类型)
        if (!targetEventTypes.isEmpty()) {
            final List<SysMessage> existingValidList = sysMessageDao.listValidMessagesByTargetUser(username);
            if (existingValidList != null) {
                for (final SysMessage item : existingValidList) {
                    if (targetEventTypes.contains(item.getEventType())) {
                        final String key = item.getEventType() + ":" + item.getRefType() + ":" + item.getRefId();
                        if (!scannedMessageKeys.contains(key)) {
                            // 状态已解决/消除，置为失效
                            sysMessageDao.updateIsValidById(item.getId(), 0);
                            log.info("用户 {} 的消息 {} (ID: {}) 已失效", username, item.getTitle(), item.getId());
                        }
                    }
                }
            }
        }

        // 更新同步时间戳
        lastSyncTimeMap.put(username, now);
    }

    @Override
    @Transactional
    public PageBean<SysMessage> listMessages(final String username, final Integer status, final Integer page, final Integer pageSize) {
        final int limit = pageSize != null ? pageSize : 10;
        final int offset = ((page != null ? page : 1) - 1) * limit;
        final List<SysMessage> rows = sysMessageDao.listByTargetUserAndStatus(username, status, offset, limit);
        final Integer total = sysMessageDao.countByTargetUserAndStatus(username, status);
        return new PageBean<>((long) total, rows);
    }

    @Override
    @Transactional
    public Integer countUnreadMessages(final String username) {
        return sysMessageDao.countUnreadByTargetUser(username);
    }

    @Override
    @Transactional
    public int updateMessageAsRead(final Integer id, final String username) {
        final SysMessage msg = sysMessageDao.getById(id);
        if (msg == null) {
            return 0;
        }
        if (username == null || !username.equals(msg.getTargetUser())) {
            throw new com.weiqiang.exception.ForbiddenException("无权操作他人的消息");
        }
        return sysMessageDao.updateStatusById(id, 1);
    }

    @Override
    @Transactional
    public int updateAllMessagesAsRead(final String username) {
        return sysMessageDao.updateStatusByTargetUser(username, 1);
    }

    @Override
    @Transactional
    public SysMessage getMessageById(final Integer id) {
        return sysMessageDao.getById(id);
    }
}
