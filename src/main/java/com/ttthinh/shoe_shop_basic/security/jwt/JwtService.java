package com.ttthinh.shoe_shop_basic.security.jwt;


import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.RedisTokenRepository;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class JwtService {
      private final JwtProperties jwtProperties;
      private final RedisTokenRepository redisTokenRepository;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

      public String generateAccessToken(Authentication authentication){
          CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

          assert userDetails != null;
          List<String> roles = userDetails.getAuthorities().stream()
                  .map(GrantedAuthority::getAuthority)
                  .toList();

          return Jwts.builder()
                  .setId(UUID.randomUUID().toString())
                  .setSubject(userDetails.getUsername())
                  .claim("userId", userDetails.getId())
                  .claim("roles", roles)
                  .claim("tokenType", "ACCESS")
                  .setIssuedAt(new Date())
                  .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                  .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                  .compact();
      }
    public String generateRefreshToken(Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        assert userDetails != null;
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .claim("tokenType", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    public String generateRefreshToken(Jws<Claims> claimsJwt){
        var roles = claimsJwt.getBody().get("roles");
        String userId = claimsJwt.getBody().get("userId", String.class);
        String userName = claimsJwt.getBody().getSubject();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userName)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    public String generateAccessToken(Jws<Claims> claimsJwt){
        var roles = claimsJwt.getBody().get("roles");
        String userId = claimsJwt.getBody().get("userId", String.class);
        String userName = claimsJwt.getBody().getSubject();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userName)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }

    public String getTokenType(Jws<Claims> claims) {
        try {
            return claims.getBody().get("tokenType", String.class);
        } catch (JwtException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
    public String getTokenType(String token) {
        try {
            Jws<Claims> claimsJws = parseToken(token);
            return claimsJws.getBody().get("tokenType", String.class);
        } catch (JwtException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }


}
