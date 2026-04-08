package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.CheckoutRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.entity.Address;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.AddressService;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.GHNShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final GHNShippingService ghnShippingService;
    private final AddressService addressService;

    @PostMapping("/calculate-shipping")
    public ApiResponse<?> calculateShipping(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody CheckoutRequest checkoutRequest) {
//        Address address;
//        if(checkoutRequest.getAddressId() != null) {
//            address = addressService.getAddressById(checkoutRequest.getAddressId());
//        }
//        else {
//            address = addressService.getDefaultAddress(user);
//        }
//        if(address == null) {
//            return ApiResponse.builder()
//                    .code(400)
//                    .message("Chua co dia chi giao hang")
//                    .build();
//        }
        Integer shippingFee = ghnShippingService.calculateShippingFee(checkoutRequest.getShippingFeeRequest());
        int totalAmount = shippingFee + checkoutRequest.getTotalProductPrice();

        Map<String, Object> response = new HashMap<>();
        response.put("shipping_fee", shippingFee);
        response.put("product_total", checkoutRequest.getTotalProductPrice());
        response.put("total_amount", totalAmount);
        //response.put("address", address);
        return ApiResponse.builder()
                .result(response)
                .message("Calculate Fee Success")
                .code(200)
                .build();
    }
}
