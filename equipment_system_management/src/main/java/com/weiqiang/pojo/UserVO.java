package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息值对象 (Value Object)
 * 用于接口响应，安全防范（不暴露密码哈希）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Integer id; // 用户唯一标识ID
    private String username; // 登录账号
    private String realName; // 真实姓名
    private Integer role; // 角色: 0-设备操作员, 1-维修工程师, 2-资产管理员, 3-系统管理员
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String unitCode; // 所属单位代码
}
