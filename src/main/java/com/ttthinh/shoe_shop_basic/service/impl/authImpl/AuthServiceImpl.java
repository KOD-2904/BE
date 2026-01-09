package com.ttthinh.shoe_shop_basic.service.impl.authImpl;

import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.RefreshTokenRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.auth.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.service.auth.AuthService;
import com.ttthinh.shoe_shop_basic.service.auth.RedisTokenService;
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

        String deviceId = httpRequest.getHeader("X-Device-ID");

        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);

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
        // Kiểm tra tính hợp lệ
        if (!redisTokenService.isValidRefreshToken(claim.getBody().getId())) {
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        if (!Objects.equals(claim.getBody().get("tokenType", String.class), "REFRESH")) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }
        redisTokenService.revokeRefreshToken(claim.getBody().getId());

        String accessToken = jwtService.generateAccessToken(claim);
        String refreshToken = jwtService.generateRefreshToken(claim);
        String deviceId = httpRequest.getHeader("X-Device-ID");

        redisTokenService.saveRefreshToken(refreshToken, deviceId, httpRequest);
        log.info("deviceId: {}", deviceId);
        log.info("refresh token: {}", refreshToken);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceId(deviceId)
                .build();
    }


    @Override
    public LogoutResponse logout(LogoutRequest logoutRequest) {
        var claims = jwtService.parseToken(logoutRequest.getToken()).getBody();
        String jwtId = claims.getId();
        if(logoutRequest.isLogoutAllDevices()){
            redisTokenService.revokeAllUserTokens(claims.get("userId", String.class));
        }
        else{
            redisTokenService.revokeRefreshToken(jwtId);
        }
        return LogoutResponse.builder()
                .message("logout")
                .success(true)
                .build();
    }
}
