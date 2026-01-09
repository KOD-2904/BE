package com.ttthinh.shoe_shop_basic.dto.response.auth;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    String accessToken;
    String refreshToken;
    String deviceId;
}
