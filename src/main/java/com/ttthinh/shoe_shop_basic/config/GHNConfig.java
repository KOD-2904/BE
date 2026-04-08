package com.ttthinh.shoe_shop_basic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class GHNConfig {
    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private Integer shopId;

    @Value("${ghn.from-district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    @Value("${ghn.api-url}")
    private String apiUrl;
}
