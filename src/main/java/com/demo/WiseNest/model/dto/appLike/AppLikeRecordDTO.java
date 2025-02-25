package com.demo.WiseNest.model.dto.appLike;

import com.demo.WiseNest.model.vo.AppVO;
import com.demo.WiseNest.model.vo.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户点赞记录 DTO
 */
@Data
public class AppLikeRecordDTO implements Serializable {

    /**
     * 应用信息列表 视图
     */
    private List<AppVO> appVOList;

    /**
     * 用户信息
     */
    private UserVO user;

    @Serial
    private static final long serialVersionUID = 1L;
}
