package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.userAnswer.UserAnswerQueryRequest;
import com.demo.WiseNest.model.entity.UserAnswer;
import com.demo.WiseNest.model.vo.UserAnswerVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户答案服务
 *
 */
public interface UserAnswerService extends IService<UserAnswer> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validUserAnswer(UserAnswer userAnswer, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<UserAnswer> getQueryWrapper(UserAnswerQueryRequest userAnswerQueryRequest);
    
    /**
     * 获取用户答案封装
     */
    UserAnswerVO getUserAnswerVO(UserAnswer userAnswer, HttpServletRequest request);

    /**
     * 分页获取用户答案封装
     */
    Page<UserAnswerVO> getUserAnswerVOPage(Page<UserAnswer> userAnswerPage, HttpServletRequest request);
}
