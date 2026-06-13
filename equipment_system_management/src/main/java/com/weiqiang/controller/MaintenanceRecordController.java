package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.MaintenanceRecord;
import com.weiqiang.service.MaintenanceRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检修记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/maintenanceRecords")
public class MaintenanceRecordController {

    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    // 获取检修列表（操作员仅看自己的）
    @GetMapping
    public Result getMaintenanceRecords(){
        List<MaintenanceRecord> mr = maintenanceRecordService.getMaintenanceRecords();
        log.info("进行了查询所有检修记录的操作，结果数量为：{}", mr.size());
        return Result.success(mr);
    }

    // 发起报修
    @PostMapping("/{equipId}")
    public Result maintenanceEquip(@PathVariable("equipId") String equipId, @RequestBody MaintenanceRecord maintenanceRecord){
        boolean success = maintenanceRecordService.maintenanceEquip(equipId, maintenanceRecord);
        return success ? Result.success() : Result.error("将设备添加到维修表中失败!");
    }

    // 删除检修记录 (限资产管理员)
    @DeleteMapping("/{maintId}")
    @RequiresRoles(2)
    public Result deleteMaintenanceRecords(
            @PathVariable("maintId") Integer maintId,
            @RequestParam("equipId") String equipId
    ){
        boolean success = maintenanceRecordService.deleteMaintenanceRecords(equipId, maintId);
        return success ? Result.success() : Result.error("将设备从维修表中删除失败！");
    }

    // 登记维保结果 (限维修工程师 role=1 或资产管理员 role=2)
    @PutMapping("/{maintId}")
    @RequiresRoles({1, 2})
    public Result putMaintenanceRecords(@PathVariable("maintId") Integer maintId, @RequestBody MaintenanceRecord maintenanceRecord){
        int i = maintenanceRecordService.putMaintenanceRecords(maintId, maintenanceRecord);
        return i > 0 ? Result.success() : Result.error("更新维修表失败！");
    }

    // 指派工单 (限资产管理员 2)
    @PutMapping("/assign/{maintId}")
    @RequiresRoles(2)
    public Result assignMaintenance(@PathVariable("maintId") Integer maintId, @RequestBody MaintenanceRecord record) {
        int rows = maintenanceRecordService.assignMaintenance(maintId, record.getMaintPersonId());
        return rows > 0 ? Result.success() : Result.error("指派工单失败！");
    }

    // 完工登记 (限维修工 1 或资产管理员 2)
    @PutMapping("/complete/{maintId}")
    @RequiresRoles({1, 2})
    public Result completeMaintenance(@PathVariable("maintId") Integer maintId, @RequestBody MaintenanceRecord record) {
        int rows = maintenanceRecordService.completeMaintenance(maintId, record);
        return rows > 0 ? Result.success() : Result.error("登记完工失败！");
    }

    // 维保复核 (限资产管理员 2)
    @PutMapping("/review/{maintId}")
    @RequiresRoles(2)
    public Result reviewMaintenance(@PathVariable("maintId") Integer maintId, @RequestBody MaintenanceRecord record) {
        String reviewer = com.weiqiang.utils.BaseContext.getCurrentName();
        String reviewComments = record.getReviewComments();
        Integer maintStatus = record.getMaintStatus(); // 3-已复核可用, 4-已复核转报废

        if (maintStatus == null) {
            return Result.error("复核结论状态不能为空");
        }

        if (maintStatus == 3) {
            boolean success = maintenanceRecordService.reviewMaintenance(maintId, reviewer, reviewComments);
            return success ? Result.success() : Result.error("复核确认在用失败！");
        } else if (maintStatus == 4) {
            String scrapNo = record.getScrapNo();
            boolean success = maintenanceRecordService.reviewToScrap(maintId, reviewer, reviewComments, scrapNo);
            return success ? Result.success() : Result.error("复核转报废失败！");
        } else {
            return Result.error("无效的复核结论状态！");
        }
    }
}
