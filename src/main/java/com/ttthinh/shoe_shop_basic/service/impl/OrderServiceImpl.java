package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.checkout.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.dto.request.order.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.request.order.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.dto.response.order.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.checkout.ShippingFeeSnapshot;
import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.CartItemRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ShippingFeeSnapshotRepository;
import com.ttthinh.shoe_shop_basic.service.CheckoutService;
import com.ttthinh.shoe_shop_basic.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.service.OrderService;
import com.ttthinh.shoe_shop_basic.service.shipping.GHNShippingService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.ttthinh.shoe_shop_basic.enums.OrderStatus.PENDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final GHNShippingService ghnShippingService;
    private final OrderMapper orderMapper;
    private final InventoryLockService inventoryLockService;
    private final CheckoutService checkoutService;
    private final ShippingFeeSnapshotRepository shippingFeeSnapshotRepository;

    @Override
    @Transactional
    public OrderResponse createOrderFromCart(UserAccount user, CreateOrderRequest request) {
        List<CartItem> selectedItems = checkoutService.validateAndGetSelectedItems(user, request.getCartItemIds());
        Address address = checkoutService.resolveAddress(user, request.getAddressId());
        InventoryInfo inventoryInfo = validateInventoryAndCalculate(selectedItems);
        ShippingFeeSnapshot snapshot = consumeSnapshot(
                user,
                request.getShippingFeeSnapshotId(),
                address,
                selectedItems,
                inventoryInfo
        );

        Order order = buildOrderEntity(
                user,
                address,
                selectedItems,
                inventoryInfo,
                snapshot.getShippingFee(),
                request.getNote()
        );

        inventoryLockService.lockCartItems(selectedItems);
        Order savedOrder = orderRepository.save(order);

        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);
        cartItemRepository.deleteAll(selectedItems);

        snapshot.setUsedAt(LocalDateTime.now());
        shippingFeeSnapshotRepository.save(snapshot);

        return toOrderResponse(savedOrder, payment);
    }

    @Override
    @Transactional
    public OrderResponse buyNow(UserAccount user, BuyNowRequest request) {
        Inventory inventory = inventoryRepository
                .findLockedByVariantSizeId(request.getVariantSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }
        if (request.getQuantity() > inventory.getAvailableQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        Address address = checkoutService.resolveAddress(user, request.getAddressId());
        inventoryLockService.lockVariantSize(request.getVariantSizeId(), request.getQuantity());

        InventoryInfo info = InventoryInfo.builder()
                .totalWeight(200 * request.getQuantity())
                .totalPrice(inventory.getVariantSize().getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .build();

        BigDecimal shippingFee = calculateShippingFee(address, info);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setShippingStatus(ShippingStatus.NOT_CREATED);
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setPhoneNumber(address.getPhoneNumber() != null ? address.getPhoneNumber() : user.getPhone());
        order.setReceiverName(address.getReceiverName() != null ? address.getReceiverName() : user.getEmail());
        order.setNote(request.getNote());

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variantSize(inventory.getVariantSize())
                .quantity(request.getQuantity())
                .unitPrice(inventory.getVariantSize().getPrice())
                .lineTotal(info.getTotalPrice())
                .build();

        order.getItems().add(orderItem);
        order.setTotalPrice(info.getTotalPrice());
        order.setShippingFee(shippingFee);
        order.setFinalTotal(info.getTotalPrice().add(shippingFee));

        Order savedOrder = orderRepository.save(order);
        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);

        return toOrderResponse(savedOrder, payment);
    }

    @Override
    @Transactional
    public OrderResponse adminCancelOrder(String orderId) {
        Order order = getOrder(orderId);
        if (!order.getStatus().adminCanCancel()) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        Payment payment = getDisplayPayment(order);
        if (order.getStatus() == PENDING) {
            inventoryLockService.releaseLocked(order);
            if (payment != null && payment.getStatus() == PaymentStatus.UNPAID) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Order cancelled by admin");
            }
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            inventoryLockService.restoreDeducted(order);
            if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toOrderResponse(orderRepository.save(order), payment);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        if (!order.getStatus().canTransitionTo(status)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = getDisplayPayment(order);
        if (status == OrderStatus.CONFIRMED) {
            confirmOrder(order, payment);
        } else {
            order.setStatus(status);
        }

        return toOrderResponse(orderRepository.save(order), payment);
    }

    @Override
    public OrderResponse getOrderDetailForAdmin(String orderId) {
        Order order = getOrder(orderId);
        return toOrderResponse(order, getDisplayPayment(order));
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(order -> toOrderResponse(order, getDisplayPayment(order)))
                .toList();
    }

    @Override
    public List<OrderResponse> getMyOrders(UserAccount user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(order -> toOrderResponse(order, getDisplayPayment(order)))
                .toList();
    }

    @Override
    public OrderResponse getOrderDetail(UserAccount user, String orderId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return toOrderResponse(order, getDisplayPayment(order));
    }

    @Override
    @Transactional
    public OrderResponse userCancelOrder(UserAccount user, String orderId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (!order.getStatus().userCanCancel()) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        Payment payment = getDisplayPayment(order);
        inventoryLockService.releaseLocked(order);
        if (payment != null && payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Order cancelled by user");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toOrderResponse(orderRepository.save(order), payment);
    }

    private void confirmOrder(Order order, Payment payment) {
        if (payment == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getMethod() == PaymentMethod.COD) {
            inventoryLockService.deductLocked(order);
            order.setStatus(OrderStatus.CONFIRMED);
            return;
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.CONFIRMED);
    }

    private Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Payment getDisplayPayment(Order order) {
        return paymentRepository.findDisplayPayments(order.getId()).stream()
                .findFirst()
                .orElse(null);
    }

    private ShippingFeeSnapshot consumeSnapshot(
            UserAccount user,
            String snapshotId,
            Address address,
            List<CartItem> selectedItems,
            InventoryInfo inventoryInfo
    ) {
        ShippingFeeSnapshot snapshot = shippingFeeSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND));
        if (!snapshot.getUserId().equals(user.getId()) || snapshot.isUsed()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.isExpired()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_EXPIRED);
        }
        if (!snapshot.getAddressId().equals(address.getId())) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (!snapshot.getCartSignature().equals(checkoutService.buildCartSignature(selectedItems))) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.getProductTotal().compareTo(inventoryInfo.getTotalPrice()) != 0) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        return snapshot;
    }

    private InventoryInfo validateInventoryAndCalculate(List<CartItem> items) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalWeight = 0;

        for (CartItem item : items) {
            Inventory inventory = inventoryRepository
                    .findLockedByVariantSizeId(item.getVariantSize().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
            if (item.getQuantity() > inventory.getAvailableQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
            totalPrice = totalPrice.add(
                    item.getVariantSize().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
            totalWeight += 200 * item.getQuantity();
        }

        return InventoryInfo.builder()
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .totalWeight(totalWeight)
                .totalPrice(totalPrice)
                .build();
    }

    private BigDecimal calculateShippingFee(Address address, InventoryInfo info) {
        if (info.totalWeight == 0) {
            return BigDecimal.ZERO;
        }
        ShippingFeeRequest request = ShippingFeeRequest.builder()
                .toDistrictId(address.getDistrictId())
                .toWardCode(address.getWardCode())
                .weight(info.totalWeight)
                .length(info.maxLength)
                .width(info.maxWidth)
                .height(info.totalHeight)
                .insuranceValue(0)
                .build();
        try {
            return BigDecimal.valueOf(ghnShippingService.calculateShippingFee(request));
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee", e);
            int baseFee = 30000;
            int extraWeight = Math.max(0, info.totalWeight - 1000);
            int extraFee = (extraWeight / 500) * 5000;
            return BigDecimal.valueOf(baseFee + extraFee);
        }
    }

    private Order buildOrderEntity(
            UserAccount user,
            Address address,
            List<CartItem> items,
            InventoryInfo info,
            BigDecimal shippingFee,
            String note
    ) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setShippingStatus(ShippingStatus.NOT_CREATED);
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setPhoneNumber(address.getPhoneNumber() != null ? address.getPhoneNumber() : user.getPhone());
        order.setReceiverName(address.getReceiverName() != null ? address.getReceiverName() : user.getEmail());
        order.setNote(note);
        order.setTotalPrice(info.totalPrice);
        order.setShippingFee(shippingFee);
        order.setFinalTotal(info.totalPrice.add(shippingFee));

        for (CartItem item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variantSize(item.getVariantSize())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getVariantSize().getPrice())
                    .lineTotal(item.getVariantSize().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
            order.getItems().add(orderItem);
        }

        return order;
    }

    private Payment createPaymentForOrder(Order order, PaymentMethod paymentMethod) {
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.UNPAID)
                .method(paymentMethod)
                .amount(orderTotal(order))
                .build();

        if (paymentMethod == PaymentMethod.VNPAY) {
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        }

        return payment;
    }

    private BigDecimal orderTotal(Order order) {
        if (order.getFinalTotal() != null) {
            return order.getFinalTotal();
        }
        BigDecimal productTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountPrice() != null ? order.getDiscountPrice() : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }

    private OrderResponse toOrderResponse(Order order, Payment payment) {
        OrderResponse response = orderMapper.toOrderResponse(order);
        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setPaymentUrl(payment.getPaymentUrl());
            response.setPaymentMethod(payment.getMethod());
            response.setPaymentStatus(payment.getStatus());
            if (payment.getMethod() == PaymentMethod.VNPAY
                    && payment.getStatus() == PaymentStatus.UNPAID
                    && order.getStatus() != PENDING
                    && order.getStatus() != OrderStatus.CANCELLED) {
                response.setPaymentStatus(PaymentStatus.PAID);
            }
        }
        return response;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class InventoryInfo {
        BigDecimal totalPrice;
        Integer totalWeight;
        Integer maxLength;
        Integer maxWidth;
        Integer totalHeight;
    }
}
