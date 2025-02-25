package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.scoringResult.ScoringResultQueryRequest;
import com.demo.WiseNest.model.entity.ScoringResult;
import com.demo.WiseNest.model.vo.ScoringResultVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 评分结果服务
 */
public interface ScoringResultService extends IService<ScoringResult> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validScoringResult(ScoringResult scoringResult, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<ScoringResult> getQueryWrapper(ScoringResultQueryRequest scoringResultQueryRequest);
    
    /**
     * 获取评分结果封装
     */
    ScoringResultVO getScoringResultVO(ScoringResult scoringResult, HttpServletRequest request);

    /**
     * 分页获取评分结果封装
     */
    Page<ScoringResultVO> getScoringResultVOPage(Page<ScoringResult> scoringResultPage, HttpServletRequest request);
}
