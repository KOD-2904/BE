package com.ttthinh.shoe_shop_basic.security.oauth2;

import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.security.auth.AuthCookieService;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.service.auth.RedisTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final AuthCookieService authCookieService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");

        if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }

        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email));

        if (user.getProvider() != AuthProvider.GOOGLE) {
            throw new AppException(ErrorCode.INVALID_LOGIN_PROVIDER);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String deviceId = request.getRemoteAddr();
        String refreshToken = jwtService.generateRefreshToken(jwtAuthentication, deviceId);

        redisTokenService.saveRefreshToken(refreshToken, deviceId, request);
        authCookieService.addRefreshTokenCookie(response, refreshToken);

        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }

    private UserAccount createGoogleUser(String email) {
        Role roleUser = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(List.of(roleUser)));

        return userAccountRepository.save(user);
    }
}
