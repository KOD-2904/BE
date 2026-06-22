package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.order.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.request.order.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.dto.request.checkout.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.dto.response.order.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.AddressRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CartItemRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CartRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.shipping.GHNShippingService;
import com.ttthinh.shoe_shop_basic.service.impl.payment.VNPayService;
import com.ttthinh.shoe_shop_basic.service.OrderService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.ttthinh.shoe_shop_basic.enums.OrderStatus.PENDING;


// ... imports ...

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl
        implements OrderService {
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final AddressRepository addressRepository;      // thêm
    private final GHNShippingService ghnShippingService;    // thêm
    private final OrderMapper orderMapper;
    private final VNPayService vnPayService;

    // ==================== PHƯƠNG THỨC PUBLIC ====================

    /**
     * Tạo order từ các item được chọn trong cart
     * Đây là method chính, thay thế method createOrder cũ
     */
    @Override
    @Transactional
    public OrderResponse createOrderFromCart(
            UserAccount user,
            CreateOrderRequest request
    ) {
        // 1. Validate và lấy cart items được chọn
        List<CartItem> selectedItems = validateAndGetSelectedItems(
                user,
                request.getCartItemIds()
        );

        // 2. Lấy địa chỉ giao hàng
        Address address = getAndValidateAddress(user, request.getAddressId());

        // 3. Validate inventory và tính toán thông số
        InventoryInfo inventoryInfo = validateInventoryAndCalculate(selectedItems);

        // 4. Tính phí ship từ GHN
        BigDecimal shippingFee = calculateShippingFee(address, inventoryInfo);
        //log.warn("Shipping fee: ", shippingFee);
        log.warn("Shipping fee calculated: {}", shippingFee);

        // 5. Tạo order entity
        Order order = buildOrderEntity(
                user,
                address,
                selectedItems,
                inventoryInfo,
                shippingFee,
                request.getNote()
        );

        // 6. Trừ kho
        deductInventory(selectedItems);

        // 7. Lưu order
        Order savedOrder = orderRepository.save(order);

        // 8. Tạo payment record
        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);

        // 9. Xóa các cart item đã chọn
        cartItemRepository.deleteAll(selectedItems);

        var orderResponse = orderMapper.toOrderResponse(savedOrder);
        orderResponse.setPaymentId(payment.getId());
        // 10. Xử lý VNPAY nếu cần
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            // TODO: Gọi VNPAY service để tạo payment URL
//            VNPayPaymentRequest vnPayPaymentRequest = VNPayPaymentRequest.builder()
//                    .amount(savedOrder.getFinalTotal())
//                    .orderInfo("Thanh toan don hang " + savedOrder.getId())
//                    .orderId(savedOrder.getId())
//                    .build();
//             String paymentUrl = vnPayService.createPaymentUrl(vnPayPaymentRequest);
//             orderResponse.setPaymentUrl(paymentUrl);
//             log.info("Payment URL created: {}", paymentUrl);
//            private String orderId;           // Mã đơn hàng của merchant
//            private BigDecimal amount;        // Số tiền (VNĐ)
//            private String orderInfo;         // Thông tin đơn hàng
//            private String returnUrl;         // URL trả về sau thanh toán
//            private String ipnUrl;            // URL IPN
        }
        return orderResponse;
    }

    /**
     * Mua ngay (đã thêm addressId)
     */
    @Override
    @Transactional
    public OrderResponse buyNow(UserAccount user, BuyNowRequest request) {
        // 1. Kiểm tra inventory
        Inventory inventory = inventoryRepository
                .findByVariantSizeId(request.getVariantSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (request.getQuantity() > inventory.getQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        // 2. Lấy địa chỉ
        Address address = getAndValidateAddress(user, request.getAddressId());

        // 3. Tính thông số shipping cho 1 sản phẩm
        InventoryInfo info = new InventoryInfo();
        info.totalWeight = 200 * request.getQuantity();
        info.totalPrice = inventory.getVariantSize().getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        info.maxLength = 0;
        info.maxWidth = 0;
        info.totalHeight = 0;

        // 4. Tính phí ship
        BigDecimal shippingFee = calculateShippingFee(address, info);

        // 5. Tạo order
        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setPhoneNumber(user.getPhone());
        order.setReceiverName(user.getEmail());
        order.setNote(request.getNote());

        // Trừ kho
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());

        // Tạo order item
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variantSize(inventory.getVariantSize())
                .quantity(request.getQuantity())
                .unitPrice(inventory.getVariantSize().getPrice())
                .lineTotal(inventory.getVariantSize().getPrice()
                        .multiply(BigDecimal.valueOf(request.getQuantity())))
                .build();

        order.getItems().add(orderItem);
        order.setTotalPrice(info.totalPrice);
        order.setShippingFee(shippingFee);
        order.setFinalTotal(info.totalPrice.add(shippingFee));

        Order savedOrder = orderRepository.save(order);

        // Tạo payment
        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);
        var orderResponse = orderMapper.toOrderResponse(savedOrder);
        orderResponse.setPaymentId(payment.getId());
        // Xử lý VNPAY
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
//            // TODO: Gọi VNPAY service để tạo payment URL
//            VNPayPaymentRequest vnPayPaymentRequest = VNPayPaymentRequest.builder()
//                    .amount(savedOrder.getFinalTotal())
//                    .orderInfo("Thanh toan don hang " + savedOrder.getId())
//                    .orderId(savedOrder.getId())
//                    .build();
//            String paymentUrl = vnPayService.createPaymentUrl(vnPayPaymentRequest);
//            orderResponse.setPaymentUrl(paymentUrl);
//            log.info("Payment URL created: {}", paymentUrl);
        }

        return orderResponse;
    }

    @Override
    public OrderResponse adminCancelOrder(String orderId) {
        return null;
    }

    @Override
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
        return null;
    }

    @Override
    public OrderResponse getOrderDetailForAdmin(String orderId) {
        return null;
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return List.of();
    }

    /**
     * Giữ method cũ cho backward compatibility, nhưng đánh dấu deprecated
     */
//    //@Override
//    @Transactional
//    @Deprecated   OLD METHOD
//    public OrderResponse createOrder(UserAccount user, PaymentMethod paymentMethod) {
//        // Lấy tất cả cart items
//        Cart cart = cartRepository.findByUser(user)
//                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));
//
//        if (cart.getItems().isEmpty()) {
//            throw new AppException(ErrorCode.CART_EMPTY);
//        }
//
//        List<String> allCartItemIds = cart.getItems().stream()
//                .map(CartItem::getId)
//                .toList();
//
//        CreateOrderRequest request = new CreateOrderRequest();
//        request.setCartItemIds(allCartItemIds);
//        request.setPaymentMethod(paymentMethod);
//        // TODO: Cần lấy address mặc định của user
//        // request.setAddressId(user.getDefaultAddressId());
//
//        return createOrderFromCart(user, request);
//    }
    @Override
    public List<OrderResponse> getMyOrders(UserAccount user) {
        return List.of();
    }

    @Override
    public OrderResponse getOrderDetail(UserAccount user, String orderId) {
        return null;
    }

    @Override
    public OrderResponse userCancelOrder(UserAccount user, String orderId) {
        return null;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate và lấy danh sách cart item được chọn
     */
    private List<CartItem> validateAndGetSelectedItems(UserAccount user, List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        List<CartItem> selectedItems = cartItemRepository.findAllById(cartItemIds);

        if (selectedItems.size() != cartItemIds.size()) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // Validate ownership
        for (CartItem item : selectedItems) {
            if (!item.getCart().getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        }

        return selectedItems;
    }

    /**
     * Lấy và validate địa chỉ
     */
    private Address getAndValidateAddress(UserAccount user, String addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return address;
    }

    /**
     * Validate inventory và tính toán thông số cho shipping
     */
    private InventoryInfo validateInventoryAndCalculate(List<CartItem> items) {

        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalWeight = 0;
        int maxLength = 0;
        int maxWidth = 0;
        int totalHeight = 0;

        for (CartItem item : items) {
            Inventory inventory = inventoryRepository
                    .findByVariantSize(item.getVariantSize())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));

            if (item.getQuantity() > inventory.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            totalPrice = totalPrice.add(
                    item.getVariantSize().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );

            // Tính weight (mặc định 200g nếu null)
            totalWeight += 200 * item.getQuantity();

            // Tính kích thước (lấy max)
        }
        return InventoryInfo.builder()
                .maxLength(maxLength)
                .maxWidth(maxWidth)
                .totalHeight(totalHeight)
                .totalWeight(totalWeight)
                .totalPrice(totalPrice)
                .build();
    }

    /**
     * Tính phí ship từ GHN
     */
    private BigDecimal calculateShippingFee(Address address, InventoryInfo info) {
        // Nếu không có weight thì trả về 0 tạm thời
        if (info.totalWeight == 0) {
            log.warn("Total weight is 0, returning 0 shipping fee");
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
//        ShippingFeeRequest request = new ShippingFeeRequest();
//        request.setToDistrictId(address.getDistrictId());
//        request.setToWardCode(address.getWardCode());
//        request.setWeight(info.totalWeight);
//        request.setLength(info.maxLength);
//        request.setWidth(info.maxWidth);
//        request.setHeight(info.totalHeight);
//        request.setInsuranceValue(info.totalPrice.intValue());

        try {
            Integer fee = ghnShippingService.calculateShippingFee(request);
            return BigDecimal.valueOf(fee);
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee", e);
            // Fallback: tính phí ship tạm thời theo weight (30k cho 1kg đầu, 5k mỗi 500g tiếp)
            int baseFee = 30000;
            int extraWeight = Math.max(0, info.totalWeight - 1000);
            int extraFee = (extraWeight / 500) * 5000;
            return BigDecimal.valueOf(baseFee + extraFee);
        }
    }

    /**
     * Build order entity từ các thông tin đã tính
     */
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
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setPhoneNumber(user.getPhone());
        order.setReceiverName(user.getEmail());
        order.setNote(note);
        order.setTotalPrice(info.totalPrice);
        order.setShippingFee(shippingFee);
        order.setFinalTotal(info.totalPrice.add(shippingFee));

        // Tạo order items
        for (CartItem item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variantSize(item.getVariantSize())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getVariantSize().getPrice())
                    .lineTotal(item.getVariantSize().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
            order.getItems().add(orderItem);
        }

        return order;
    }

    /**
     * Trừ kho
     */
    private void deductInventory(List<CartItem> items) {
        for (CartItem item : items) {
            Inventory inventory = inventoryRepository
                    .findByVariantSize(item.getVariantSize())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));

            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
        }
    }

    /**
     * Tạo payment record
     */
    private Payment createPaymentForOrder(Order order, PaymentMethod paymentMethod) {
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.PENDING)
                .method(paymentMethod)
                .amount(order.getFinalTotal())  // Dùng final total (đã bao gồm ship)
                .build();

        // Nếu là VNPAY, thêm expired time
        if (paymentMethod == PaymentMethod.VNPAY) {
            //payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));
            payment.setExpiredAt(LocalDateTime.now().plusSeconds(900));
        }

        return payment;
    }

    // ==================== INNER CLASS ====================

    @Data
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    private static class InventoryInfo {
        private BigDecimal totalPrice;
        private Integer totalWeight;
        private Integer maxLength;
        private Integer maxWidth;
        private Integer totalHeight;
    }
}
