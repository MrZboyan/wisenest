package com.demo.WiseNest.service.scoring;

import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略接口
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}

