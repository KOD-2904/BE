package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.dto.response.order.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingOrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final GHNShippingService ghnShippingService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createGHNOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getShippingStatus() != null && order.getShippingStatus() != ShippingStatus.NOT_CREATED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = getDisplayPayment(order);
        String orderCode = ghnShippingService.createOrder(order, payment);
        order.setShippingProvider("GHN");
        order.setShippingOrderCode(orderCode);
        order.setShippingStatus(ShippingStatus.CREATED);
        order.setShippingCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPING);

        Order savedOrder = orderRepository.save(order);
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);
        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setPaymentMethod(payment.getMethod());
            response.setPaymentStatus(payment.getStatus());
            response.setPaymentUrl(payment.getPaymentUrl());
        }
        return response;
    }

    @Transactional
    public void handleGHNWebhook(Map<String, Object> payload) {
        String orderCode = getString(payload, "order_code");
        String status = getString(payload, "status");
        if (orderCode == null || status == null) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Order order = orderRepository.findByShippingOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        ShippingStatus shippingStatus = mapGHNStatus(status);
        order.setShippingStatus(shippingStatus);

        if (shippingStatus == ShippingStatus.PICKED || shippingStatus == ShippingStatus.DELIVERING) {
            order.setStatus(OrderStatus.SHIPPING);
            if (order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
            }
        } else if (shippingStatus == ShippingStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            markCodPaid(order);
        } else if (shippingStatus == ShippingStatus.DELIVERY_FAILED) {
            order.setStatus(OrderStatus.FAILED);
        } else if (shippingStatus == ShippingStatus.RETURNED) {
            order.setStatus(OrderStatus.RETURNED);
        } else if (shippingStatus == ShippingStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
    }

    private void markCodPaid(Order order) {
        Payment payment = getDisplayPayment(order);
        if (payment != null && payment.getMethod() == PaymentMethod.COD && payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
    }

    private Payment getDisplayPayment(Order order) {
        return paymentRepository.findDisplayPayments(order.getId()).stream()
                .findFirst()
                .orElse(null);
    }

    private ShippingStatus mapGHNStatus(String ghnStatus) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick", "created" -> ShippingStatus.CREATED;
            case "picking" -> ShippingStatus.PICKING;
            case "picked" -> ShippingStatus.PICKED;
            case "delivering", "transporting", "sorting" -> ShippingStatus.DELIVERING;
            case "delivered" -> ShippingStatus.DELIVERED;
            case "delivery_fail", "delivery_failed", "waiting_to_return" -> ShippingStatus.DELIVERY_FAILED;
            case "return", "returning" -> ShippingStatus.RETURNING;
            case "returned" -> ShippingStatus.RETURNED;
            case "cancel", "cancelled", "canceled" -> ShippingStatus.CANCELLED;
            default -> throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        };
    }

    private String getString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
}
