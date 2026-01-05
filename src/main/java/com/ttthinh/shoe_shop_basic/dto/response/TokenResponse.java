package com.ttthinh.shoe_shop_basic.dto.response;

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
