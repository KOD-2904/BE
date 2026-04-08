package com.ttthinh.shoe_shop_basic.security.jwt;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.security.user.UserDetailServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailServiceImpl userDetailService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = parseJwt(request);
        String requestPath = request.getServletPath();

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var claims = jwtService.parseToken(jwt).getBody();

            String jti = claims.getId();

            String tokenType = claims.get("tokenType", String.class);

            if ("/auth/refresh".equals(requestPath) && "POST".equalsIgnoreCase(request.getMethod())) {
                // Endpoint refresh chỉ chấp nhận refresh token
                if (!"REFRESH".equals(tokenType)) {
                    log.info("Access token used for refresh endpoint");
                    throw new AppException(ErrorCode.EMAIL_EXIST);
                }
                // Cho refresh token qua mà không set authentication
                filterChain.doFilter(request, response);
                return;
            } else {
                // Các endpoint khác chỉ chấp nhận access token
                if (!"ACCESS".equals(tokenType)) {
                    log.info("Refresh token used for non-refresh endpoint");
//                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                    response.getWriter().write("Invalid token type");
                    throw new AppException(ErrorCode.NOT_VALID_TOKEN);
                }
                // Set authentication cho access token
                String username = claims.getSubject();
                UserDetails userDetails = userDetailService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                //SecurityContextHolder.getContext().setAuthentication(authToken);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            handleJwtError(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
            handleJwtError(response, ErrorCode.NOT_VALID_TOKEN);
            return;
        } catch (AppException e) {
            handleJwtError(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }
    private void handleJwtError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String parseJwt(HttpServletRequest request) {
        String Header = request.getHeader("Authorization");
        if (Header != null && Header.startsWith("Bearer ")) {
            return Header.substring(7);
        }
        return null;
    }
}
