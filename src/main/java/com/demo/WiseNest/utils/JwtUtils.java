package com.demo.WiseNest.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT 密钥和有效期 单位 ms
     */
    @Value("${jwt.SECRET_KEY}")
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_MS = 1000 * 3600 * 24;

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @return token
     */
    public static String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 验证 JWT Token
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public static Object parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.error("JWT Token 已过期, 尝试刷新 Token");
            String subject = e.getClaims().getSubject();
            String refreshToken = refreshToken(subject);
            log.info("JwtUtils.ExpiredJwt, 刷新成功：{}", refreshToken);
            return refreshToken;
        } catch (JwtException e) {
            log.error("JWT Token 解析失败, 请重新登录获取 Token");
            return 0L;
        }
    }

    /**
     * 刷新 JWT Token
     */
    private static String refreshToken(String token) {
        long userId = Long.parseLong(token);
        return generateToken(userId);
    }
}
