package com.weiqiang.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtils {

    // 签名密钥，避免魔法值
    private static final String SIGN_KEY = "weiqiang_equipment_system_management_secret_key_1234567890";
    // 过期时间：12小时，单位为毫秒
    private static final long EXPIRE_TIME = 43200000L;

    /**
     * 生成 JWT 令牌
     *
     * @param claims 载荷信息
     * @return JWT 字符串
     */
    public static String generateToken(final Map<String, Object> claims) {
        final long currentTime = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY)
                .setExpiration(new Date(currentTime + EXPIRE_TIME))
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 字符串
     * @return 包含的载荷 Claims 对象
     */
    public static Claims parseToken(final String token) {
        return Jwts.parser()
                .setSigningKey(SIGN_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
