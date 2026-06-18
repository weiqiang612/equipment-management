package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.common.PageBean;
import com.weiqiang.common.Result;
import com.weiqiang.entity.Equipment;
import com.weiqiang.entity.MaintenanceRecord;
import com.weiqiang.entity.ScrapRecord;
import com.weiqiang.entity.TransferRecord;
import com.weiqiang.service.EquipmentService;
import com.weiqiang.service.MaintenanceRecordService;
import com.weiqiang.service.ScrapRecordService;
import com.weiqiang.service.TransferRecordService;
import com.weiqiang.vo.EquipmentDepreciationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.weiqiang.vo.EquipmentDetailVO;
import com.weiqiang.exception.ForbiddenException;
import com.weiqiang.utils.BaseContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    private final MaintenanceRecordService maintenanceRecordService;

    private final ScrapRecordService scrapRecordService;

    private final TransferRecordService transferRecordService;

    // 根据ID查询设备信息
    @GetMapping("/{equipId}")
    public Result getEquipmentById(@PathVariable("equipId") final String equipId) {
        final Equipment equipment = equipmentService.getEquipmentById(equipId);
        log.info("进行了根据ID查询设备的操作，结果为：{}", equipment);
        return equipment != null ? Result.success(equipment) : Result.error("未查询到结果！");
    }

    // 动态SQL查询设备列表
    @GetMapping
    public Result getEquipmentsDynamic(
            @RequestParam(value = "equipName", required = false) final String equipName,
            @RequestParam(value = "unitCode", required = false) final String unitCode,
            @RequestParam(value = "categoryId", required = false) final String categoryId,
            @RequestParam(value = "status", required = false) final String status,
            @RequestParam(value = "custodian", required = false) final String custodian,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam(value = "begin", required = false) final LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam(value = "end", required = false) final LocalDate end,
            @RequestParam(value = "page", defaultValue = "1") final Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") final Integer pageSize
    ) {
        final PageBean equipments =
                equipmentService.getEquipmentsDynamic(equipName, unitCode, categoryId, status, begin, end, custodian, page, pageSize);
        log.info("进行了查询设备的操作，结果数量为：{}", equipments.getRows().size());
        return Result.success(equipments);
    }

    // 将动态SQL查询结果导出
    @GetMapping("/export")
    public Result getEquipmentsDynamicForExport(
            @RequestParam(value = "equipName", required = false) final String equipName,
            @RequestParam(value = "unitCode", required = false) final String unitCode,
            @RequestParam(value = "categoryId", required = false) final String categoryId,
            @RequestParam(value = "status", required = false) final String status,
            @RequestParam(value = "custodian", required = false) final String custodian,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam(value = "begin", required = false) final LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam(value = "end", required = false) final LocalDate end
    ) {
        final List<EquipmentDepreciationVO> equipments =
                equipmentService.getEquipmentsDynamicForExport(equipName, unitCode, categoryId, status, begin, end, custodian);
        log.info("进行了导出设备的操作，结果数量为：{}", equipments.size());
        return Result.success(equipments);
    }

    // 添加设备 (限资产管理员)
    @PostMapping
    @RequiresRoles(2)
    public Result addEquipment(@RequestBody final Equipment equipment) {
        final int i = equipmentService.addEquipment(equipment);
        return i > 0 ? Result.success() : Result.error("添加设备失败!");
    }

    // 根据ID更新设备 (限资产管理员)
    @PutMapping("/{equipId}")
    @RequiresRoles(2)
    public Result updateEquipment(@PathVariable final String equipId, @RequestBody final Equipment equipment) {
        equipment.setEquipId(equipId);
        final int i = equipmentService.updateEquipment(equipment);
        return i > 0 ? Result.success() : Result.error("更新设备失败!");
    }

    // 删除设备 (限资产管理员)
    @DeleteMapping("/{equipId}")
    @RequiresRoles(2)
    public Result deleteEquipment(@PathVariable("equipId") final String equipId) {
        final boolean success = equipmentService.deleteEquipment(equipId);
        return success ? Result.success() : Result.error("删除设备失败!");
    }

    // 报修设备 (操作员/管理员皆可发起)
    @PostMapping("/maint/{equipId}")
    public Result maintenanceEquip(@PathVariable("equipId") final String equipId, @RequestBody final MaintenanceRecord maintenanceRecord) {
        final boolean success = maintenanceRecordService.maintenanceEquip(equipId, maintenanceRecord);
        return success ? Result.success() : Result.error("将设备添加到维修表中失败!");
    }

    // 报废设备 (限资产管理员)
    @PostMapping("/scrap/{equipId}")
    @RequiresRoles(2)
    public Result scrapEquip(@PathVariable("equipId") final String equipId, @RequestBody final ScrapRecord scrapRecord) {
        final boolean success = scrapRecordService.scrapEquip(equipId, scrapRecord);
        return success ? Result.success() : Result.error("将设备添加到报废表中失败!");
    }

    // 调拨设备 (限资产管理员)
    @PostMapping("/transfer/{equipId}")
    @RequiresRoles(2)
    public Result transferEquip(@PathVariable("equipId") final String equipId, @RequestBody final TransferRecord transferRecord){
        final boolean success = transferRecordService.transferEquip(equipId, transferRecord);
        return success ? Result.success() : Result.error("将设备添加到调拨表中失败!");
    }

    // 查看某台设备的折旧信息
    @GetMapping("/calculateAccumulated/{equipId}")
    public Result calculateAccumulated(@PathVariable("equipId") final String equipId) {
        final List<EquipmentDepreciationVO> list = equipmentService.getEquipmentsDynamicForExport(equipId, null, null, null, null, null, null);
        if (list != null && !list.isEmpty()) {
            return Result.success(list.get(0));
        }
        return Result.error("未找到该设备的价值信息");
    }

    // 设备全生命周期聚合详情查询
    @GetMapping("/detail/{equipId}")
    public Result getEquipmentDetail(@PathVariable("equipId") final String equipId) {
        final EquipmentDetailVO detail = equipmentService.getEquipmentDetail(equipId);
        if (detail == null) {
            return Result.error("未查询到设备详情");
        }

        // RBAC 水平与垂直越权校验
        final Integer role = BaseContext.getCurrentRole();
        final String currentUnitCode = BaseContext.getCurrentUnitCode();
        final String currentUsername = BaseContext.getCurrentName();

        if (role == null) {
            throw new ForbiddenException("越权访问：权限不足");
        }

        if (role == 0) {
            // 普通用户（Role 0）只能查看其保管的设备详情
            if (detail.getCustodian() == null || !detail.getCustodian().equals(currentUsername)) {
                throw new ForbiddenException("越权访问：普通用户只能查看自己保管的设备详情");
            }
        } else if (role == 1 || role == 2) {
            // 维修工程师（Role 1）和资产管理员（Role 2）只能查看本单位（unitCode 匹配）的设备详情
            if (detail.getUnitCode() == null || !detail.getUnitCode().equals(currentUnitCode)) {
                throw new ForbiddenException("越权访问：只能查看本单位的设备详情");
            }
        }
        // 系统管理员（Role 3）可以看全部，直接放行

        log.info("查询了设备详情，equipId: {}", equipId);
        return Result.success(detail);
    }
}
