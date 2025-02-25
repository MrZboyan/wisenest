package com.demo.WiseNest.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * TableName app_like_record
 */
@TableName(value ="app_like_record")
@Data
public class AppLikeRecord implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long appId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否取消点赞（0-有效，1-取消）
     */
    private Integer isLike;

    /**
     * 点赞时间
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