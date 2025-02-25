package com.demo.WiseNest.model.dto.appLike;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppLikeCountCreateRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 点赞数
     */
    private Long likeCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
