package com.weiqiang.dto;

import lombok.Data;

/**
 * 后台用户资料更新请求
 */
@Data
public class UserProfileUpdateRequest {

    private String realName;

    private Integer role;

    private String unitCode;
}
