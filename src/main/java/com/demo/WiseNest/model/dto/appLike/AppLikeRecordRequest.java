package com.demo.WiseNest.model.dto.appLike;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 应用点赞请求
 */
@Data
public class AppLikeRecordRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否取消点赞（0-有效，1-取消）
     */
    private Integer isLike;

    @Serial
    private static final long serialVersionUID = 1L;
}
