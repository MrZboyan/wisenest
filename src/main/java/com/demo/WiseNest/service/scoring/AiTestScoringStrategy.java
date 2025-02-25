package com.demo.WiseNest.service.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.WiseNest.annotation.ScoringStrategyConfig;
import com.demo.WiseNest.api.ChatGLM.AIGeneralMessage;
import com.demo.WiseNest.api.ChatGLM.AiManager;
import com.demo.WiseNest.api.QWen.QWenAIClient;
import com.demo.WiseNest.model.dto.question.QuestionAnswerDTO;
import com.demo.WiseNest.model.dto.question.QuestionContentDTO;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.Question;
import com.demo.WiseNest.model.entity.UserAnswer;
import com.demo.WiseNest.model.vo.QuestionVO;
import com.demo.WiseNest.service.QuestionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private QWenAIClient qWenAIClient;

    /**
     * AI评分结果本地缓存
     */
    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    // 缓存10分钟移除
                    .expireAfterAccess(10L, TimeUnit.MINUTES)
                    .build();

    /**
     * ai评分结果缓存
     *
     * @param choices 用户选项
     * @param app 应用信息
     * @return 评分结果描述
     * @throws InterruptedException 异常
     */
    @Override
    public UserAnswer doScore(List<String> choices, App app) throws InterruptedException {
        Long appId = app.getId();

        // 首先把选项转为json
        String choicesStr = JSONUtil.toJsonStr(choices);
        // 构造生成每个id 对应的 唯一的缓存 key
        String cacheKey = buildCacheKey(appId, choicesStr);
        // 从缓存中获取答案
        String answerJson = answerCacheMap.getIfPresent(cacheKey);

        // 命中缓存则直接返回结果
        if (StrUtil.isNotBlank(answerJson)) {
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(choicesStr);
            userAnswer.setResultPicture(app.getAppIcon());
            return userAnswer;
        }
        // 未命中缓存则继续常规流程 创建锁 此时可能缓存刚好过期 所以需要加锁
        RLock lock = redissonClient.getLock(AIGeneralMessage.AI_ANSWER_LOCK + cacheKey);
        try {
            //竞争锁
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            //没抢到锁强行返回
            if (!res) {
                return null;
            }
            //抢到锁执行后续逻辑
            // 1. 根据 id 查询到题目
            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId));
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

            // 2. 调用 AI 获取结果
            // 封装 Prompt
            String userMessage = getAiTestScoringUserMessage(app, questionContent, choices);
            // todo 调试信息
            // System.out.println(userMessage);

            // AI 生成
            String result = aiManager.doSyncStableRequest(AIGeneralMessage.AI_TEST_SCORING_SYSTEM_MESSAGE, userMessage);
            // String result = qWenAIClient.generateAnswer(userMessage);
            // todo 调试信息
            // System.out.println(result);

            // 截取需要的 JSON 信息
            int start = result.indexOf("{");
            int end = result.lastIndexOf("}");
            String json = result.substring(start, end + 1);
            // todo 调试信息
            // System.out.println(json);

            // 3. 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(choicesStr);
            userAnswer.setResultPicture(app.getAppIcon());

            // 4. 将结果添加到缓存
            answerCacheMap.put(cacheKey, json);

            return userAnswer;
        } finally {
            //首先判断 lock不能为空 且必须是被锁状态
            if (lock != null && lock.isLocked()) {
                // 该锁必须是当前线程自己的锁才能释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * AI 评分用户消息封装
     *
     * @param app 应用信息
     * @param questionContentDTOList 题目列表
     * @param choices 用户选项
     */
    private String getAiTestScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }

    /**
     * 缓存 key 构造
     *
     * @param appId 应用 id
     * @param choicesStr 答案选项
     */
    private String buildCacheKey(Long appId, String choicesStr) {
        return DigestUtil.md5Hex(appId + ":" + choicesStr);
    }

}
