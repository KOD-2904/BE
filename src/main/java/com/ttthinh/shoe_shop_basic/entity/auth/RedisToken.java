package com.ttthinh.shoe_shop_basic.entity.auth;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refresh_tokens", timeToLive = 3600) // 7 ngày = 604800 giây
public class RedisToken {

    @Id
    private String id;  // Đây sẽ là refreshToken value hoặc jti
    @Indexed
    private String userId;
    private String username;

    @Indexed
    private String deviceId;
    //private String userAgent;

    @Indexed
    private String ipAddress;


    private String tokenType; // "REFRESH"

    private Date createdAt;
    private Date expiresAt;
    private boolean active;
}
