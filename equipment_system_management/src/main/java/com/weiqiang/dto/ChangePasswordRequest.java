package com.weiqiang.dto;

import lombok.Data;

/**
 * 当前登录用户修改本人密码请求
 */
@Data
public class ChangePasswordRequest {

    private String oldPassword;

    private String newPassword;
}
