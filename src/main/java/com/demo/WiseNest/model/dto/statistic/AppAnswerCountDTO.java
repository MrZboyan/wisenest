package com.demo.WiseNest.model.dto.statistic;

import lombok.Data;

@Data
public class AppAnswerCountDTO {

    private Long appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 用户答案统计
     */
    private Long answerCount;

}
