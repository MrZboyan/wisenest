package com.demo.WiseNest.aop.auth.token;

import cn.hutool.core.util.StrUtil;
import com.demo.WiseNest.constant.UserConstant;
import com.demo.WiseNest.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        // 1. 获取 从请求头中获取 Token 如果不携带标准 token 则直接放行
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
            // 2. 如果请求头未携带 Token，尝试从 URL 参数中获取
            token = request.getParameter("token");
            if (StrUtil.isBlank(token)) {
                return true; // 未携带 Token，继续后续验证
            }
        }
        // 2. 携带 解析 Token
        String jwt = token.substring(7);
        Object parseToken = JwtUtils.parseToken(jwt);
        Long userId;
        if (parseToken instanceof Long){
            userId = (Long) parseToken;
            // 小于等于 0 则 Token 无效，继续后续验证
            if (userId <= 0) {
                response.setStatus(415);
                return false;
            }
            log.info("JwtAuthInterceptor.preHandle：userId={}", userId);
        } else {
            String newToken = (String) parseToken;
            response.setHeader("Authorization", "Bearer " + newToken);
            userId = (Long) JwtUtils.parseToken(newToken);
        }
        // 3. 存储 将用户ID存入请求属性 后续逻辑使用
        request.setAttribute(UserConstant.USER_TOKEN, userId);
        return true;
    }
}