package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.entity.RedisToken;
import com.ttthinh.shoe_shop_basic.repository.RedisTokenRepository;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.service.RedisTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisTokenServiceImpl implements RedisTokenService {
    private final JwtService jwtService;
    private final RedisTokenRepository redisTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveRefreshToken(String refreshToken,
                                 String deviceId, HttpServletRequest request) {
        var claims = jwtService.parseToken(refreshToken).getBody();
        String jwtId = claims.getId();
        String userId = claims.get("userId", String.class);
        String userName = claims.getSubject();
        String ipAddress = request.getRemoteAddr();
        String tokenType = claims.get("tokenType", String.class);
        Date createTime = claims.getIssuedAt();
        Date expiryTime = claims.getExpiration();
        RedisToken redisToken = RedisToken.builder()
                .deviceId(deviceId)
                .id(jwtId)
                .ipAddress(ipAddress)
                .tokenType(tokenType)
                .userId(userId)
                .username(userName)
                .createdAt(createTime)
                .expiresAt(expiryTime)
                .active(true)
                .build();
        redisTokenRepository.save(redisToken);
        log.info("Save refresh token success");
    }

    public boolean isValidRefreshToken(String jwtId) {
        RedisToken token = redisTokenRepository.findById(jwtId)
                .orElse(null);

        if (token == null) return false;

        return token.isActive();
    }


    @Override
    public RedisToken getRefreshTokenInfo(String refreshToken) {
        return null;
    }

    @Override
    public void revokeRefreshToken(String jwtId) {
        redisTokenRepository.findById(jwtId).ifPresent(token -> {
            token.setActive(false);
            redisTokenRepository.save(token);
            log.debug("Revoked refresh token: {}", jwtId.substring(0, 20) + "...");
        });
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        List<RedisToken> tokens = redisTokenRepository.findByUserId(userId);
        for (RedisToken token : tokens) {
            token.setActive(false);
            redisTokenRepository.save(token);
        }
    }

    @Override
    public List<RedisToken> getUserActiveSessions(String userId) {
        return redisTokenRepository.findByUserIdAndActiveTrue(userId);
       // return List.of();
    }

    @Override
    public void deleteRefreshToken(String userId) {
//        var tokens = redisTokenRepository.findByUserId(userId);
//        log.info(" refresh token: {}",userId);
//        for(RedisToken token : tokens) {
//            log.info("Delete refresh token: {}", token.getUserId());
//
//            log.info("Delete refresh token: {}", token.getId());
//        }
//        if (tokens.isEmpty()) {
//            log.info("No refresh tokens found for userId {}", userId);
//            return;
//        }
//        redisTokenRepository.deleteAll(tokens);
//        log.info("Delete token success");
        redisTokenRepository.deleteAll(
                redisTokenRepository.findByUserId(userId)
        );

    }



}
