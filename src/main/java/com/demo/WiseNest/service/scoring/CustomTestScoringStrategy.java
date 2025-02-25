package com.demo.WiseNest.service.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.WiseNest.annotation.ScoringStrategyConfig;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.ScoringResult;
import com.demo.WiseNest.model.entity.UserAnswer;
import com.demo.WiseNest.service.ScoringResultService;
import jakarta.annotation.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private ScoringResultService scoringResultService;

    public UserAnswer doScore(List<String> choices, App app) {
        // 获取应用的 ID
        Long appId = app.getId();
        // 获取评分结果
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId));
        // 计数用户选择的答案
        Map<String, Integer> optionCount = new HashMap<>();  // 用来统计用户选择的答案选项
        // 统计用户选择的答案
        // 通过循环统计每个选择的出现次数
        for (String choice : choices) {
            optionCount.put(choice, optionCount.getOrDefault(choice, 0) + 1);
        }
        // 获取评分结果中得分最高的结果
        ScoringResult maxScoreResult = null;  // 存储得分最高的结果
        int maxScore = 0;  // 记录最大得分
        // 遍历评分结果列表
        for (ScoringResult scoringResult : scoringResultList) {
            // 将评分结果的属性字符串转换成列表
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            // 计算当前评分结果的分数
            int score = 0;
            for (String prop : resultProp) {
                // 获取每个结果选项的统计值，若没有则默认为0
                score += optionCount.getOrDefault(prop, 0);
            }
            // 比较当前评分结果的得分，更新最大得分
            if (score > maxScore) {
                maxScore = score;
                maxScoreResult = scoringResult;
            }
        }

        // 创建用户回答对象
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);  // 设置应用 ID
        userAnswer.setAppType(app.getAppType());  // 设置应用类型
        userAnswer.setScoringStrategy(app.getScoringStrategy());  // 设置评分策略
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));  // 设置用户选择的答案列表（JSON 字符串）
        userAnswer.setResultId(maxScoreResult.getId());  // 设置得分最高的结果的 ID
        userAnswer.setResultName(maxScoreResult.getResultName());  // 设置得分最高的结果的名称
        userAnswer.setResultDesc(maxScoreResult.getResultDesc());  // 设置得分最高的结果的描述
        userAnswer.setResultPicture(maxScoreResult.getResultPicture());  // 设置得分最高的结果的图片

        // 返回用户回答对象
        return userAnswer;
    }

}
