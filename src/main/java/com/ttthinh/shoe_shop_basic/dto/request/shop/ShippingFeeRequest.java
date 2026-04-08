package com.ttthinh.shoe_shop_basic.dto.request.shop;

import lombok.Builder;

@lombok.Data
@Builder
public class ShippingFeeRequest {
    private Integer toDistrictId;
    private String toWardCode;
    private Integer weight;             // tổng cân nặng (gram)
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer insuranceValue; // gia tri bao hiem
}
