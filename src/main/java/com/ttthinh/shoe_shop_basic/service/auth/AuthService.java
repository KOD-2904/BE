package com.ttthinh.shoe_shop_basic.service.auth;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.RefreshTokenRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) throws JOSEException;

    LogoutResponse logout(LogoutRequest logoutRequest);
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest httpRequest);


}
