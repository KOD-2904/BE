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
@RedisHash(value = "refresh_tokens", timeToLive = 604800) // 7 ngày = 604800 giây
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

    public boolean isNotExpired() {
        return expiresAt != null && expiresAt.after(new Date());
    }

    // Helper method: Kiểm tra token có hợp lệ không
    public boolean isValid() {
        return active && isNotExpired();
    }

    // Helper method: Tạo mới refresh token
    public static RedisToken create(String jwtId, String userId, String username,
                                    String deviceId, String ipAddress, long ttlSeconds) {
        Date now = new Date();
        return RedisToken.builder()
                .id(jwtId)
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .tokenType("REFRESH")
                .createdAt(now)
                .expiresAt(new Date(now.getTime() + ttlSeconds * 1000))
                .active(true)
                .build();
    }

    // Revoke token
    public void revoke() {
        this.active = false;
    }
}
