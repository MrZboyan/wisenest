package com.demo.WiseNest.api.ChatGLM;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIConfigGLM {

    /*
     * AI API key
     */
    private String apiKey;

    @Bean
    public ClientV4 getClientV4() {
        return new ClientV4.Builder(apiKey)
                .enableTokenCache()
                .networkConfig(10,10,10,10,TimeUnit.MINUTES)
                .build();
    }
}
