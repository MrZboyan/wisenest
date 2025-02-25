package com.demo.WiseNest.service.scoring;

import com.demo.WiseNest.annotation.ScoringStrategyConfig;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.UserAnswer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScoringStrategyExecutor {

    // 注入评分策略列表，将所有应用对应的策略注入
    @Resource
    private List<ScoringStrategy> scoringStrategyList;

    // 初始化阶段构建映射（应用启动时执行一次）
    // 使用 ConcurrentHashMap 以确保线程安全
    private static final Map<String, ScoringStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 初始化评分策略映射
     * 在应用启动时执行一次，将所有的评分策略根据其配置信息注入到 strategyMap 中
     */
    @PostConstruct
    public void initStrategies() {
        for (ScoringStrategy strategy : scoringStrategyList) {
            // 获取策略类上的 ScoringStrategyConfig 注解
            ScoringStrategyConfig config = strategy.getClass().getAnnotation(ScoringStrategyConfig.class);
            if (config == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少注解信息");
            }
            // 构建键值对，键为 appType 和 scoringStrategy 的组合
            String key = config.appType() + ":" + config.scoringStrategy();
            // 将策略对象放入映射中
            strategyMap.put(key, strategy);
        }
    }

    /**
     * 根据应用类型和评分策略执行评分
     *
     * @param choiceList 用户选择的答案列表
     * @param app 应用信息
     * @return 用户回答对象
     * @throws Exception 如果找不到匹配的策略或评分过程中发生异常
     */
    public UserAnswer doScore(List<String> choiceList, App app) throws Exception {
        // 构建查找策略的键
        String key = app.getAppType() + ":" + app.getScoringStrategy();
        // 从映射中获取对应的策略
        ScoringStrategy strategy = strategyMap.get(key);
        if (strategy == null) {
            // 如果找不到匹配的策略，抛出业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 执行评分策略
        return strategy.doScore(choiceList, app);
    }

    @Deprecated
    public UserAnswer doScoreDeprecated(List<String> choiceList, App app) throws Exception {
        // 首先校验app类型 和 id
        // 获取应用类型和评分策略配置
        Integer appType = app.getAppType();
        Integer appScoringStrategy = app.getScoringStrategy();

        // 校验应用类型和评分策略是否为空
        if (appType == null || appScoringStrategy == null) {
            // 如果应用类型或评分策略为空，抛出异常提示配置错误
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }

        // 根据注解获取策略 核心代码
        // 遍历应用评分策略列表，尝试根据注解匹配评分策略
        for (ScoringStrategy strategy : scoringStrategyList) {
            // 检查每个策略类是否有 @ScoringStrategyConfig 注解，如果有，则获取该注解的配置
            // 获取当前策略类的 Class 对象，并检查是否有 @ScoringStrategyConfig 注解
            if (strategy.getClass().isAnnotationPresent(ScoringStrategyConfig.class)) {
                // 从策略类上获取 @ScoringStrategyConfig 注解的配置
                ScoringStrategyConfig scoringStrategyConfig =
                        strategy.getClass().getAnnotation(ScoringStrategyConfig.class);

                // 判断获取到的注解中的 appType 和 scoringStrategy 是否与当前应用的配置匹配
                // 如果注解中的配置与当前应用的配置一致，则执行该评分策略
                if (scoringStrategyConfig.appType() == appType
                        && scoringStrategyConfig.scoringStrategy() == appScoringStrategy) {
                    // 如果匹配，则调用该策略的 doScore 方法执行评分，并返回评分结果
                    return strategy.doScore(choiceList, app);
                }
            }
        }

        // 如果没有找到匹配的评分策略，则抛出异常
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }

}

