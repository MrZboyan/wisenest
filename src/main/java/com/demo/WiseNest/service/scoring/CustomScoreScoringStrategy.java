package com.demo.WiseNest.service.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.WiseNest.annotation.ScoringStrategyConfig;
import com.demo.WiseNest.model.dto.question.QuestionContentDTO;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.Question;
import com.demo.WiseNest.model.entity.ScoringResult;
import com.demo.WiseNest.model.entity.UserAnswer;
import com.demo.WiseNest.model.vo.QuestionVO;
import com.demo.WiseNest.service.QuestionService;
import com.demo.WiseNest.service.ScoringResultService;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Optional;

/**
 * 打分类应用评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    /**
     * 执行评分
     */
    @Override
    public UserAnswer doScore(List<String> choices, App app) {
        Long appId = app.getId();
        //获取题目 根据questionService的getOne方法查询题库中和当前题目相同的id的题目
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId));

        //获取评分结果 根据scoringResultService的list方法查询评分结果表中appId为当前题目的appId的评分结果
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange));

        //统计用户总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        // 遍历题目内容并与用户选择进行匹配
        for (int i = 0; i < questionContent.size(); i++) {
            QuestionContentDTO questionContentDTO = questionContent.get(i);
            // 遍历用户的选择
            if (i < choices.size()) {
                String userChoice = choices.get(i);
                // 遍历选项
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    // 确保按题目顺序比对，并且选项匹配
                    if (option.getKey().equals(userChoice)) {
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                        break; // 只匹配一次，防止重复加分
                    }
                }
            }
        }

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoringResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);

        return userAnswer;
    }
}
