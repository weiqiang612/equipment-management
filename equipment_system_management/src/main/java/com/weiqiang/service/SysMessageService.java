package com.weiqiang.service;

import com.weiqiang.common.PageBean;
import com.weiqiang.entity.SysMessage;

/**
 * 系统消息核心业务接口
 */
public interface SysMessageService {

    /**
     * 同步与拉取当前用户的增量消息
     *
     * @param username 用户名
     * @param role 角色 (0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员)
     * @param unitCode 隔离单位编码
     */
    void syncMessages(final String username, final Integer role, final String unitCode);

    /**
     * 分页查询当前用户的系统消息列表
     *
     * @param username 用户名
     * @param status 状态 (0-未读, 1-已读, 传入 null 表示查询全部)
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 分页包装的消息列表
     */
    PageBean<SysMessage> listMessages(final String username, final Integer status, final Integer page, final Integer pageSize);

    /**
     * 获取未读消息总数
     *
     * @param username 用户名
     * @return 未读数
     */
    Integer countUnreadMessages(final String username);

    /**
     * 标记单条消息为已读
     *
     * @param id 消息 ID
     * @param username 当前用户，用于越权验证
     * @return 影响行数
     */
    int updateMessageAsRead(final Integer id, final String username);

    /**
     * 一键标记所有消息为已读
     *
     * @param username 用户名
     * @return 影响行数
     */
    int updateAllMessagesAsRead(final String username);

    /**
     * 根据消息 ID 获取消息详情
     *
     * @param id 消息 ID
     * @return 消息详情
     */
    SysMessage getMessageById(final Integer id);
}
