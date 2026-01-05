package com.ttthinh.shoe_shop_basic.security.jwt;

import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.security.user.UserDetailServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailServiceImpl userDetailService;

//    private final InvalidatedTokenRepository tokenRepository;


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
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            }

        } catch (Exception e) {
            log.error("JWT error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }


    private String parseJwt(HttpServletRequest request) {
        String Header = request.getHeader("Authorization");
        if (Header != null && Header.startsWith("Bearer ")) {
            return Header.substring(7);
        }
        return null;
    }
}

//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE) // Chạy TRƯỚC Spring Security filters
//public class BlacklistFilter extends OncePerRequestFilter {
//
//    @Autowired
//    private InvalidatedTokenRepository invalidatedTokenRepository;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        String token = extractToken(request);
//
//        if (token != null && isTokenBlacklisted(token)) {
//            // Token đã logout → từ chối ngay
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write("{\"error\": \"Token has been logged out\"}");
//            return;
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private boolean isTokenBlacklisted(String token) {
//        try {
//            // Extract jti từ token (không cần verify signature)
//            String jti = extractJtiFromToken(token);
//            return jti != null && invalidatedTokenRepository.existsById(jti);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private String extractJtiFromToken(String token) {
//        try {
//            String[] parts = token.split("\\.");
//            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
//            JsonNode payload = new ObjectMapper().readTree(payloadJson);
//            return payload.has("jti") ? payload.get("jti").asText() : null;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private String extractToken(HttpServletRequest request) {
//        String bearer = request.getHeader("Authorization");
//        if (bearer != null && bearer.startsWith("Bearer ")) {
//            return bearer.substring(7);
//        }
//        return null;
//    }
//}