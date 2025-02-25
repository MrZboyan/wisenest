package com.demo.WiseNest.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * TableName app_like_count
 */
@TableName(value ="app_like_count")
@Data
public class AppLikeCount implements Serializable {

    /**
     * 应用ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long appId;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}