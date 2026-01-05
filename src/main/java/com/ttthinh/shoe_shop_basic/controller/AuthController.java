package com.ttthinh.shoe_shop_basic.controller;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.IntrospecRequest;
import com.ttthinh.shoe_shop_basic.dto.request.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.RefreshTokenRequest;
import com.ttthinh.shoe_shop_basic.dto.response.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.IntrospectResponse;
import com.ttthinh.shoe_shop_basic.dto.response.TokenResponse;
import com.ttthinh.shoe_shop_basic.service.AuthService;
import com.ttthinh.shoe_shop_basic.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@CrossOrigin("http://127.0.0.1:5500/")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) throws JOSEException {
        var authenticated = authService.login(loginRequest, httpRequest);
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .result(authenticated)
                .build();

    }

//    @PostMapping("/introspect")
//    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospecRequest introspecRequest) throws ParseException, JOSEException {
//        var result  = authService.introspect(introspecRequest);
//        return ApiResponse.<IntrospectResponse>builder()
//                .result(result)
//                .build();
//    }

    @PostMapping("/log-out")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        authService.logout(logoutRequest);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("logged out successfully")
                .build();
    }
    @PostMapping("/refreshToken")
    public ApiResponse<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest httpRequest) throws ParseException, JOSEException {
        return ApiResponse.<TokenResponse>builder()
                .code(200)
                .result(authService.refreshToken(refreshTokenRequest, httpRequest))
                .message("refresh token successfully")
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse verifyResgiterToken(@RequestParam String token){
        mailService.verifyEmail(token);
        return ApiResponse.builder()
                .code(200)
                .message("email verified successfully")
                .build();
    }
}
