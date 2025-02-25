package com.demo.WiseNest.api.QWen;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QWenAIClient {

    @Resource
    private AIConfigQWen aiConfig;

    /**
     * 生成答案 非流式
     */
    public String generateAnswer(String prompt) {
        String apiKey = aiConfig.getApiKey();
        String appId = aiConfig.getGenerateAnswerAppId();
        return this.getAiGenerate(prompt,apiKey,appId);
    }

    /**
     * 生成题目
     * @param prompt 用户输入的提示语
     */
    public String generateQuestion(String prompt) {
        String apiKey = aiConfig.getApiKey();
        String appId = aiConfig.getGenerateQuestionAppId();
        return this.getAiGenerate(prompt,apiKey,appId);
    }

    /**
     * 调用 QWen API
     *
     * @param prompt 用户输入的提示语
     * @param apiKey 密钥
     * @param appId  应用 id
     */
    public String getAiGenerate(String prompt, String apiKey, String appId) {
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(apiKey)
                .appId(appId)
                .prompt(prompt)
                .build();
        Application application = new Application();
        try {
            return application.call(param).getOutput().getText();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请检查传入参数");
        }
    }

    /**
     * 生成题目流式调用
     *
     * @param prompt 用户输入的提示语
     */
    public Flowable<ApplicationResult> generateQuestionStream(String prompt) {
        String apiKey = aiConfig.getApiKey();
        String appId = aiConfig.getGenerateQuestionAppId();
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(apiKey)
                .appId(appId)
                .prompt(prompt)
                // 增量输出
                .incrementalOutput(true)
                .build();
        // 构建响应
        Application application = new Application();
        try {
            return application.streamCall(param);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
    }
}
