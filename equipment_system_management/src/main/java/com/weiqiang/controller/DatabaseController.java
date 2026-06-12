package com.weiqiang.controller;

import com.weiqiang.anno.RequiresRoles;
import com.weiqiang.config.DBBackupProperties;
import com.weiqiang.dao.UserDao;
import com.weiqiang.pojo.Result;
import com.weiqiang.pojo.User;
import com.weiqiang.utils.BaseContext;
import lombok.extern.slf4j.Slf4j;
import com.weiqiang.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 数据库备份与恢复控制器
 */
@Slf4j
@RestController
@RequestMapping("/system/db")
@RequiresRoles(3) // 全局限系统管理员
public class DatabaseController {

    @Autowired
    private DBBackupProperties backupProperties;

    @Autowired
    private UserDao userDao;

    @Autowired
    private OperationLogService operationLogService;

    @PostMapping("/backup")
    public Result backup() {
        String fileName = "backup_" + System.currentTimeMillis() + ".sql";
        String backupPath = backupProperties.getPath();
        if (backupPath != null && !backupPath.endsWith("/") && !backupPath.endsWith("\\")) {
            backupPath += "/";
        }
        File dir = new File(backupPath);
        if (!dir.exists()) dir.mkdirs();

        String cmd = String.format("mysqldump -u%s -p%s %s -r %s",
                backupProperties.getUser(),
                backupProperties.getPassword(),
                backupProperties.getDatabase(),
                backupPath + fileName);

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process.waitFor() == 0) {
                operationLogService.record("数据库备份", "database", fileName, "数据库备份成功，文件名：" + fileName, 1, null);
                return Result.success("备份成功，文件名：" + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            operationLogService.record("数据库备份", "database", fileName, "数据库备份失败，文件名：" + fileName + "，异常：" + e.getMessage(), 0, e.getMessage());
        }
        return Result.error("备份执行失败，请检查数据库配置");
    }

    @PostMapping("/restore")
    public Result restore(@RequestParam("fileName") String fileName) {
        // 文件名白名单校验，防止路径穿越
        if (fileName == null || !fileName.matches("^[\\w\\-]+\\.sql$")) {
            return Result.error("非法的文件名格式");
        }

        // 1. 防御会话失效：提前备份当前正在操作的管理员信息
        final String currentUsername = BaseContext.getCurrentName();
        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userDao.getByUsername(currentUsername);
        }

        // 构造完整的备份文件路径
        String backupPath = backupProperties.getPath();
        if (backupPath != null && !backupPath.endsWith("/") && !backupPath.endsWith("\\")) {
            backupPath += "/";
        }
        String filePath = backupPath + fileName;

        // Windows 环境下需要调用 cmd /c 才能执行带 < 的重定向命令
        String cmd = String.format("cmd /c mysql -u%s -p%s %s < %s",
                backupProperties.getUser(), backupProperties.getPassword(), backupProperties.getDatabase(), filePath);

        try {
            Process process = Runtime.getRuntime().exec(cmd);

            // 关键点：如果不读取错误流，进程可能会阻塞，导致你一直等不到返回
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            // 设置一个超时保护，防止进程挂死
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return Result.error("恢复操作超时，请检查备份文件是否正确");
            }

            if (process.exitValue() == 0) {
                // 2. 防御会话失效：还原完成后强行写回或恢复管理员账户
                if (currentUser != null) {
                    final User restoredUser = userDao.getByUsername(currentUsername);
                    if (restoredUser == null) {
                        userDao.insert(currentUser);
                        log.info("还原数据库后发现当前管理员账号丢失，已自动插回防御失效：{}", currentUsername);
                    } else {
                        // 强制覆盖密码、角色和单位，确保与还原前一致，防止登录凭证或权限失效
                        final String updateSql = "UPDATE sys_user SET password = ?, role = ?, real_name = ?, unit_code = ? WHERE username = ?";
                        userDao.update(updateSql, 
                                currentUser.getPassword(), 
                                currentUser.getRole(), 
                                currentUser.getRealName(), 
                                currentUser.getUnitCode(), 
                                currentUsername);
                        log.info("还原数据库后已强行同步当前管理员账户密码与角色防失效：{}", currentUsername);
                    }
                }
                operationLogService.record("数据库恢复", "database", fileName, "数据库恢复成功，恢复的文件：" + fileName, 1, null);
                return Result.success("数据已成功恢复");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.error("恢复失败，请检查数据库权限或路径");
    }

    // 获取所有备份文件列表
    @GetMapping("/files")
    public Result listFiles() {
        File dir = new File(backupProperties.getPath());
        if (!dir.exists()) return Result.success(new ArrayList<>());

        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));

        List<Map<String, Object>> fileInfoList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", file.getName()); // 文件名
                map.put("size", file.length());   // 文件大小
                map.put("lastModified", file.lastModified()); // 修改时间
                fileInfoList.add(map);
            }
        }
        fileInfoList.sort((a, b) -> Long.compare((Long)b.get("lastModified"), (Long)a.get("lastModified")));

        return Result.success(fileInfoList);
    }

    // 动态获取当前备份文件目录路径
    @GetMapping("/config")
    public Result getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("path", backupProperties.getPath());
        return Result.success(config);
    }
}
