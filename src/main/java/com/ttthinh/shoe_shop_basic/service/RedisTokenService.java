package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.entity.RedisToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;


public interface RedisTokenService {
    public void saveRefreshToken(String refreshToken,
                                 String deviceId, HttpServletRequest request);
    public boolean isValidRefreshToken(String jwtId);
    public RedisToken getRefreshTokenInfo(String refreshToken);
    public void revokeRefreshToken(String refreshToken);
    public void revokeAllUserTokens(String userId);
    public List<RedisToken> getUserActiveSessions(String userId);
    public void deleteRefreshToken(String refreshToken);
}
