package com.demo.WiseNest.api.QWen;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.ai")
@Data
public class AIConfigQWen {

    /**
     * AI apiKey
     */
    private String apiKey;

    /**
     * AI appId 生成题目
     */
    private String generateQuestionAppId;

    /**
     * AI appId 生成答案
     */
    private String generateAnswerAppId;
}
