package com.demo.WiseNest.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新个人信息请求
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    @Serial
    private static final long serialVersionUID = 1L;
}