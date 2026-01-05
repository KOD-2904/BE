package com.ttthinh.shoe_shop_basic.security.jwt;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties (prefix = "jwt")
public class JwtProperties {
    @Value("$(jwt.secret)")
    private String secret;
    private long accessTokenExpiration = 3600000; //? ngày
    private long refreshTokenExpiration = 604800000;
}
