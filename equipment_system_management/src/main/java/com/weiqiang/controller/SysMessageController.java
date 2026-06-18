package com.weiqiang.controller;

import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.common.PageBean;
import com.weiqiang.common.Result;
import com.weiqiang.entity.SysMessage;
import com.weiqiang.service.SysMessageService;
import com.weiqiang.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageService sysMessageService;

    /**
     * 获取分页消息列表
     * 拉取时会自动触发消息扫描同步
     */
    @GetMapping
    public Result getMessages(
            @RequestParam(value = "status", required = false) final Integer status,
            @RequestParam(value = "page", defaultValue = "1") final Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") final Integer pageSize
    ) {
        final String currentUsername = BaseContext.getCurrentName();
        final Integer role = BaseContext.getCurrentRole();
        final String unitCode = BaseContext.getCurrentUnitCode();

        log.info("用户 {} 获取消息列表. status: {}, page: {}, pageSize: {}", currentUsername, status, page, pageSize);

        // 触发拉取式同步扫描
        sysMessageService.syncMessages(currentUsername, role, unitCode);

        // 分页查询
        final PageBean<SysMessage> messagePage = sysMessageService.listMessages(currentUsername, status, page, pageSize);
        return Result.success(messagePage);
    }

    /**
     * 获取未读消息数量
     * 拉取时会自动触发消息扫描同步
     */
    @GetMapping("/unread-count")
    public Result getUnreadCount() {
        final String currentUsername = BaseContext.getCurrentName();
        final Integer role = BaseContext.getCurrentRole();
        final String unitCode = BaseContext.getCurrentUnitCode();

        log.info("用户 {} 获取未读消息数量", currentUsername);

        // 触发拉取式同步扫描
        sysMessageService.syncMessages(currentUsername, role, unitCode);

        final Integer unreadCount = sysMessageService.countUnreadMessages(currentUsername);
        return Result.success(unreadCount);
    }

    /**
     * 标记单条消息为已读
     */
    @PutMapping("/{id}/read")
    public Result readMessage(@PathVariable("id") final Integer id) {
        final String currentUsername = BaseContext.getCurrentName();
        log.info("用户 {} 尝试标记消息 {} 为已读", currentUsername, id);

        final SysMessage msg = sysMessageService.getMessageById(id);
        if (msg == null) {
            return Result.error("该消息不存在");
        }

        // 越权校验：非本人消息禁止操作
        if (!currentUsername.equals(msg.getTargetUser())) {
            log.warn("用户 {} 越权尝试修改用户 {} 的消息 (ID: {})", currentUsername, msg.getTargetUser(), id);
            throw new ForbiddenException("越权访问：您无权操作他人的消息");
        }

        sysMessageService.updateMessageAsRead(id, currentUsername);
        return Result.success();
    }

    /**
     * 一键标记所有消息为已读
     */
    @PutMapping("/read-all")
    public Result readAllMessages() {
        final String currentUsername = BaseContext.getCurrentName();
        log.info("用户 {} 一键标记所有消息为已读", currentUsername);

        sysMessageService.updateAllMessagesAsRead(currentUsername);
        return Result.success();
    }
}
