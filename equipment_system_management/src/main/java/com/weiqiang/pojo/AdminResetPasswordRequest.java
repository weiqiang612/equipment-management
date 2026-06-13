package com.weiqiang.pojo;

import lombok.Data;

/**
 * 管理员重置密码请求
 */
@Data
public class AdminResetPasswordRequest {

    private String newPassword;
}
