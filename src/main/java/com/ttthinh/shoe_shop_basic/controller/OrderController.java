package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.payment.VNPayService;
import com.ttthinh.shoe_shop_basic.service.shop.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

    @PostMapping
    public ApiResponse<?> createOrder(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody CreateOrderRequest req,
            HttpServletRequest httpServletRequest
    ) {

        var orderResponse = orderService.createOrderFromCart(user, req);
        if (req.getPaymentMethod() == PaymentMethod.VNPAY){
            VNPayPaymentRequest vnPayPaymentRequest = VNPayPaymentRequest.builder()
                    .amount(orderResponse.getFinalTotalPrice())
                    .orderInfo("Thanh toan don hang " + orderResponse.getId())
                    .orderId(orderResponse.getId())
                    .build();
            String paymentUrl = vnPayService.createPaymentUrl(vnPayPaymentRequest, httpServletRequest.getRemoteAddr());
            orderResponse.setPaymentUrl(paymentUrl);
            log.info("Payment URL created: {}", paymentUrl);
        }
        log.info("Shipping Fee: {}", orderResponse.getShippingPrice());
        return ApiResponse.builder()
                .result(orderResponse)
                .code(200)
                .message("Order created")
                .build();
    }

    @GetMapping("/me")
    public List<OrderResponse> myOrders(
            @AuthenticationPrincipal(expression = "user") UserAccount user
    ) {
        return orderService.getMyOrders(user);
    }

    @GetMapping("/{id}")
    public OrderResponse orderDetail(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String id
    ) {
        return orderService.getOrderDetail(user, id);
    }
//    @PostMapping("/{orderId}/cancel")
//    public OrderResponse cancelOrder(
//            @AuthenticationPrincipal(expression = "user") UserAccount user,
//            @PathVariable String orderId
//    ) {
//        return orderService.cancelOrder(user, orderId);
//    }

    @PostMapping("/buy-now")
    public OrderResponse buyNow(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody BuyNowRequest request
    ) {
        return orderService.buyNow(user, request);
    }
}

