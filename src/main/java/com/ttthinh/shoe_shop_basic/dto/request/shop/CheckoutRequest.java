package com.ttthinh.shoe_shop_basic.dto.request.shop;

import java.util.List;

@lombok.Data
public class CheckoutRequest {
    private String addressId;
    private Integer totalProductPrice;
    private ShippingFeeRequest shippingFeeRequest;
}
