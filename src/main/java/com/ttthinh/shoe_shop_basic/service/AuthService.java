package com.ttthinh.shoe_shop_basic.service;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.*;
import com.ttthinh.shoe_shop_basic.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.text.ParseException;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) throws JOSEException;

    LogoutResponse logout(LogoutRequest logoutRequest);
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest httpRequest);


}
