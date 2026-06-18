package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.entity.EquipmentClaim;
import com.weiqiang.common.PageBean;
import com.weiqiang.common.Result;
import com.weiqiang.service.EquipmentClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备领用与审批控制器
 */
@Slf4j
@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
public class EquipmentClaimController {

    private final EquipmentClaimService equipmentClaimService;

    // 提交领用申请
    @PostMapping("/apply")
    @RequiresRoles({0, 2, 3})
    public Result applyClaim(@RequestBody Map<String, String> params) {
        String equipId = params.get("equipId");
        String remark = params.get("remark");
        if (equipId == null || equipId.trim().isEmpty()) {
            return Result.error("设备编号不能为空");
        }
        log.info("提交领用申请，设备编号: {}, 原因: {}", equipId, remark);
        boolean success = equipmentClaimService.applyClaim(equipId, remark);
        return success ? Result.success() : Result.error("提交申请失败");
    }

    // 撤回领用申请
    @PutMapping("/{claimId}/cancel")
    @RequiresRoles({0, 2, 3})
    public Result cancelClaim(@PathVariable("claimId") Integer claimId) {
        log.info("撤回领用申请，申请单号: {}", claimId);
        boolean success = equipmentClaimService.cancelClaim(claimId);
        return success ? Result.success() : Result.error("撤回申请失败");
    }

    // 审批领用申请
    @PutMapping("/{claimId}/approve")
    @RequiresRoles({2, 3})
    public Result approveClaim(@PathVariable("claimId") Integer claimId, @RequestBody Map<String, Object> params) {
        Integer action = (Integer) params.get("action");
        String remark = (String) params.get("remark");
        if (action == null) {
            return Result.error("审批动作不能为空");
        }
        log.info("审批领用申请，申请单号: {}, 动作: {}, 审批意见: {}", claimId, action, remark);
        boolean success = equipmentClaimService.approveClaim(claimId, action, remark);
        return success ? Result.success() : Result.error("审批操作失败");
    }

    // 主动退还设备
    @PostMapping("/return")
    @RequiresRoles({0, 2, 3})
    public Result returnEquipment(@RequestBody Map<String, String> params) {
        String equipId = params.get("equipId");
        String remark = params.get("remark");
        if (equipId == null || equipId.trim().isEmpty()) {
            return Result.error("设备编号不能为空");
        }
        log.info("主动退还设备，设备编号: {}, 备注: {}", equipId, remark);
        boolean success = equipmentClaimService.returnEquipment(equipId, remark);
        return success ? Result.success() : Result.error("退还设备失败");
    }

    // 查询领用申请列表（支持分页）
    @GetMapping
    @RequiresRoles({0, 2, 3})
    public Result getClaims(
            @RequestParam(value = "equipId", required = false) String equipId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        log.info("查询领用列表，设备: {}, 状态: {}, 页码: {}, 大小: {}", equipId, status, page, pageSize);
        PageBean<EquipmentClaim> claims = equipmentClaimService.getClaims(equipId, status, page, pageSize);
        return Result.success(claims);
    }
}
