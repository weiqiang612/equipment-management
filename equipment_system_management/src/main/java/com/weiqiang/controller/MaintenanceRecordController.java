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
}
