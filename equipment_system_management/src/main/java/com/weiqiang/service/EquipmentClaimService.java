package com.weiqiang.service;

import com.weiqiang.pojo.EquipmentClaim;
import com.weiqiang.pojo.PageBean;

/**
 * 设备领用与审批服务接口
 */
public interface EquipmentClaimService {

    /**
     * 申请领用设备
     *
     * @param equipId 设备编号
     * @param remark  领用原因
     * @return 是否申请成功
     */
    boolean applyClaim(String equipId, String remark);

    /**
     * 撤回领用申请
     *
     * @param claimId 申请单号
     * @return 是否撤回成功
     */
    boolean cancelClaim(Integer claimId);

    /**
     * 审批领用申请
     *
     * @param claimId 申请单号
     * @param action  审批动作：1-同意, 2-拒绝
     * @param remark  审批意见
     * @return 是否审批成功
     */
    boolean approveClaim(Integer claimId, Integer action, String remark);

    /**
     * 自主退还设备
     *
     * @param equipId 设备编号
     * @param remark  退还原因/备注
     * @return 是否退还成功
     */
    boolean returnEquipment(String equipId, String remark);

    /**
     * 分页查询领用申请/历史审计列表
     *
     * @param equipId  设备编号
     * @param status   领用状态
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageBean<EquipmentClaim> getClaims(String equipId, Integer status, Integer page, Integer pageSize);
}
