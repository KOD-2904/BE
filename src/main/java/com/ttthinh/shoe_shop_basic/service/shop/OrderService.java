package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.util.List;

public interface OrderService {

    OrderResponse createOrderFromCart(UserAccount user, CreateOrderRequest createOrderRequest);

    List<OrderResponse> getMyOrders(UserAccount user);

    OrderResponse getOrderDetail(UserAccount user, String orderId);

    OrderResponse userCancelOrder(UserAccount user, String orderId);
    OrderResponse buyNow(UserAccount user, BuyNowRequest request);

    OrderResponse adminCancelOrder(String orderId);

    OrderResponse updateOrderStatus(String orderId, OrderStatus status);

    OrderResponse getOrderDetailForAdmin(String orderId);

    List<OrderResponse> getAllOrders();
    //OrderResponse updateOrderStatus(String orderId, OrderStatus status);
}

