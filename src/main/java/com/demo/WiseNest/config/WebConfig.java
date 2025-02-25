package com.demo.WiseNest.config;

import com.demo.WiseNest.aop.auth.token.JwtAuthInterceptor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     *
     * @param registry cors 配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("https://mrz.ztmiaowu.top/", "http://localhost:8081/","https://localhost:8081/",
                        "http://localhost:8080/", "https://localhost:8080/",
                        "https://springboot-3oa6-140310-4-1331347804.sh.run.tcloudbase.com")
                // .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Origin", "Accept")
                .exposedHeaders("Authorization", "Content-Disposition", "X-Custom-Header");
    }

    /**
     * 拦截器配置
     * @param registry 拦截器注册
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/user/register","/user/logout");
    }
}
