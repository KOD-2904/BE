package com.ttthinh.shoe_shop_basic.controller;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.RefreshTokenRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import com.ttthinh.shoe_shop_basic.service.auth.AuthService;
import com.ttthinh.shoe_shop_basic.service.auth.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
//@CrossOrigin("http://127.0.0.1:5173/")
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
    public ApiResponse<LogoutResponse> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {

        return ApiResponse.<LogoutResponse>builder()
                .code(200)
                .message("logged out successfully")
                .result(authService.logout(logoutRequest))
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
