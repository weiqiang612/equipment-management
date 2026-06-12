package com.weiqiang.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:weiqiang_equipment_system_management_secret_key_1234567890}")
    private String signKey;

    @Value("${jwt.expire:43200000}")
    private long expireTime;

    /**
     * 生成 JWT 令牌
     *
     * @param claims 载荷信息
     * @return JWT 字符串
     */
    public String generateToken(final Map<String, Object> claims) {
        final long currentTime = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, signKey)
                .setExpiration(new Date(currentTime + expireTime))
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 字符串
     * @return 包含的载荷 Claims 对象
     */
    public Claims parseToken(final String token) {
        return Jwts.parser()
                .setSigningKey(signKey)
                .parseClaimsJws(token)
                .getBody();
    }
}

