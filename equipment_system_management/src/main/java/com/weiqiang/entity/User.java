package com.weiqiang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息实体类
 * 对应数据库表 sys_user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id; // 主键ID
    private String username; // 登录账号
    private String password; // 登录密码(MD5)
    private String realName; // 真实姓名
    private Integer role; // 角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String unitCode; // 所属单位代码
}
