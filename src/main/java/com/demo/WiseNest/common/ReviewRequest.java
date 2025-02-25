package com.demo.WiseNest.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审核请求
 */
@Data
public class ReviewRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 审核状态 0:待审核  1:审核通过  2:审核不通过
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMassage;

    @Serial
    private static final long serialVersionUID = 1L;
}