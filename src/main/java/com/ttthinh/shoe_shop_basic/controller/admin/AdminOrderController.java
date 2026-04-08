package com.ttthinh.shoe_shop_basic.controller.admin;

import com.ttthinh.shoe_shop_basic.dto.request.shop.UpdateOrderStatusRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.OrderResponse;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.OrderServiceImpl;
import com.ttthinh.shoe_shop_basic.service.shop.OrderService;
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
}
