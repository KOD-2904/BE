package com.ttthinh.shoe_shop_basic.service.impl.authImpl;

import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.RefreshTokenRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.RedisToken;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.auth.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.security.user.UserDetailServiceImpl;
import com.ttthinh.shoe_shop_basic.service.auth.AuthService;
import com.ttthinh.shoe_shop_basic.service.auth.RedisTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedisTokenService redisTokenService;
    private final UserAccountRepository userAccountRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailServiceImpl userDetailsService;

//    private String generateDeviceId(HttpServletRequest request) {
//        // Tạo deviceId từ user-agent + ip
//        String userAgent = request.getHeader("User-Agent");
//        String ip = request.getRemoteAddr();
//        return DigestUtils.md5DigestAsHex((userAgent + "|" + ip).getBytes());
//    }
@Override
public AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) {
    try {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(),
                                loginRequest.getPassword()
                        )
                );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assert userDetails != null;
        String username = userDetails.getUsername();
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

//        if (Boolean.FALSE.equals(userAccount)) {
//            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
//        }

        // ✅ (tuỳ chọn) check status
        // nếu m đang dùng status INACTIVE cho user chưa verify:
         if (userAccount.getStatus() == UserStatus.INACTIVE) {
             throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
         }

        //String deviceId = httpRequest.getHeader("X-Device-ID");
        String deviceId = httpRequest.getRemoteAddr();

        String accessToken = jwtService.generateAccessToken(authentication, deviceId);
        String refreshToken = jwtService.generateRefreshToken(authentication, deviceId);

        redisTokenService.saveRefreshToken(refreshToken, deviceId, httpRequest);

        return AuthResponse.builder()
                .username(authentication.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();

    } catch (BadCredentialsException ex) {
        throw new AppException(ErrorCode.USERNAME_PASSWORD_NOT_MATCH);
    }
}

    @Override
    public TokenResponse refreshToken (RefreshTokenRequest refreshTokenRequest, HttpServletRequest httpRequest){
        var claim = jwtService.parseToken(refreshTokenRequest.getRefreshToken());
        if (!Objects.equals(claim.getBody().get("tokenType", String.class), "REFRESH")) {
            log.warn("Token type");
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        String jwtId = claim.getBody().getId();
        //String deviceIdFromRequest = httpRequest.getHeader("X-Device-ID");
        String deviceIdFromRequest = httpRequest.getRemoteAddr();

        // ✅ Thêm kiểm tra deviceId
        RedisToken storedToken = redisTokenService.getRefreshTokenInfoById(jwtId);
        log.warn("DEVICE ID: {}", storedToken.getDeviceId());
        log.warn("Exprired at: {}", storedToken.getExpiresAt());
        if (storedToken == null) {
            log.warn("Refresh token is null");
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }
        if (!storedToken.isActive()) {
            log.warn("Not active");
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }
        if (storedToken == null || !storedToken.isActive()) {
            log.warn("Token k co or het han");
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        // Kiểm tra deviceId có khớp không
        if (!storedToken.getDeviceId().equals(deviceIdFromRequest)) {
            log.warn("Device mismatch! Token device: {}, Request device: {}",
                    storedToken.getDeviceId(), deviceIdFromRequest);
            throw new AppException(ErrorCode.UNAUTHORIZED_DEVICE);
        }

        // Kiểm tra tính hợp lệ (method này đang dùng claim.getBody().getId() - nên dùng jwtId)
        if (!redisTokenService.isValidRefreshToken(jwtId)) {  // ✅ Sửa thành jwtId
            log.warn("Invalid refresh token: {}", jwtId);
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);  // ✅ Đổi từ EMAIL_EXIST sang đúng error code
        }

        String userId = claim.getBody().get("userId", String.class);
        String username = claim.getBody().getSubject();

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);



        String accessToken = jwtService.generateAccessTokenFromUserDetails(userDetails, deviceIdFromRequest);
        String refreshToken = jwtService.generateRefreshTokenFromUserDetails(userDetails, deviceIdFromRequest);

        redisTokenService.saveRefreshToken(refreshToken, deviceIdFromRequest, httpRequest);
        redisTokenService.revokeRefreshToken(claim.getBody().getId());


        log.info("deviceId: {}", deviceIdFromRequest);
        log.info("refresh token: {}", refreshToken);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceId(deviceIdFromRequest)
                .build();
    }


    @Override
    public LogoutResponse logout(LogoutRequest logoutRequest) {
        // 1. Parse refresh token
        Jws<Claims> claimsJws;
        try {
            claimsJws = jwtService.parseToken(logoutRequest.getToken());
        } catch (JwtException e) {
            log.error("Invalid refresh token during logout: {}", e.getMessage());
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        var claims = claimsJws.getBody();
        String jwtId = claims.getId();
        String userId = claims.get("userId", String.class);
        String tokenType = claims.get("tokenType", String.class);

        // 2. Kiểm tra đây có phải refresh token không
        if (!"REFRESH".equals(tokenType)) {
            log.warn("Invalid token type for logout: {}, expected REFRESH", tokenType);
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        // 3. Xử lý theo yêu cầu
        if (logoutRequest.isLogoutAllDevices()) {
            // TH2: Logout tất cả thiết bị
            redisTokenService.revokeAllUserTokens(userId);
            log.info("Logged out all devices for user: {}", userId);

        } else {
            // TH1: Logout 1 thiết bị
            // Kiểm tra token có tồn tại không trước khi revoke
            if (!redisTokenService.isValidRefreshToken(jwtId)) {
                log.warn("Attempt to revoke invalid or already revoked token: {}", jwtId);
                throw new AppException(ErrorCode.NOT_VALID_TOKEN);
            }

            redisTokenService.revokeRefreshToken(jwtId);
            log.info("Revoked refresh token for user: {}, device: {}",
                    userId, claims.get("deviceId", String.class));
        }

        return LogoutResponse.builder()
                .message("Logout successful")
                .success(true)
                .build();
    }
}
