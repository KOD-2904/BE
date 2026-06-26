package com.ttthinh.shoe_shop_basic.controller.admin;

import com.ttthinh.shoe_shop_basic.dto.request.order.UpdateOrderStatusRequest;
import com.ttthinh.shoe_shop_basic.dto.response.order.OrderResponse;
import com.ttthinh.shoe_shop_basic.service.OrderService;
import com.ttthinh.shoe_shop_basic.service.shipping.ShippingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final OrderService orderService;
    private final ShippingOrderService shippingOrderService;
    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Admin xem chi tiết order
     */
    @GetMapping("/{orderId}")
    public OrderResponse getOrderDetail(
            @PathVariable String orderId
    ) {
        return orderService.getOrderDetailForAdmin(orderId);
    }

    /**
     * Admin cập nhật trạng thái order
     */
    @PutMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(orderId, request.getStatus());
    }

    /**
     * Admin hủy order
     */
    @PutMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable String orderId
    ) {
        return orderService.adminCancelOrder(orderId);
    }

    @PostMapping("/{orderId}/shipping/ghn")
    public OrderResponse createGHNShippingOrder(@PathVariable String orderId) {
        return shippingOrderService.createGHNOrder(orderId);
    }
}
