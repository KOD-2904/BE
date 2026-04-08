package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shop.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.Address;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.cart.Cart;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.AddressRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.CartItemRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.CartRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.InventoryRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.order.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.payment.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.payment.VNPayService;
import com.ttthinh.shoe_shop_basic.service.shop.OrderService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.ttthinh.shoe_shop_basic.enums.OrderStatus.PENDING;


// ... imports ...

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl
        implements OrderService
{
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

        var orderResponse =  orderMapper.toOrderResponse(savedOrder);
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
                .findByVariantId(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (request.getQuantity() > inventory.getQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        // 2. Lấy địa chỉ
        Address address = getAndValidateAddress(user, request.getAddressId());

        // 3. Tính thông số shipping cho 1 sản phẩm
        InventoryInfo info = new InventoryInfo();
        info.totalWeight = (inventory.getVariant().getWeight() != 0 ?
                inventory.getVariant().getWeight() : 200) * request.getQuantity();
        info.totalPrice = inventory.getVariant().getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        info.maxLength = inventory.getVariant().getLength() != 0 ?
                inventory.getVariant().getLength() : 0;
        info.maxWidth = inventory.getVariant().getWidth() != 0 ?
                inventory.getVariant().getWidth() : 0;
        info.totalHeight = (inventory.getVariant().getHeight() != 0 ?
                inventory.getVariant().getHeight() : 0) * request.getQuantity();

        // 4. Tính phí ship
        BigDecimal shippingFee = calculateShippingFee(address, info);

        // 5. Tạo order
        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setAddressId(address.getId());
        order.setShippingAddress(address.getFullAddress());
        order.setPhoneNumber(user.getPhone());
        order.setReceiverName(user.getFirstName() + " " + user.getLastName());
        order.setNote(request.getNote());

        // Trừ kho
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());

        // Tạo order item
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variant(inventory.getVariant())
                .quantity(request.getQuantity())
                .unitPrice(inventory.getVariant().getPrice())
                .lineTotal(inventory.getVariant().getPrice()
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
        var orderResponse =  orderMapper.toOrderResponse(savedOrder);
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
                    .findByVariant(item.getVariant())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));

            if (item.getQuantity() > inventory.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            totalPrice = totalPrice.add(
                    item.getVariant().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );

            // Tính weight (mặc định 200g nếu null)
            Integer itemWeight = item.getVariant().getWeight();
            totalWeight += itemWeight * item.getQuantity();

            // Tính kích thước (lấy max)
            maxLength = Math.max(maxLength,
                    item.getVariant().getLength() != 0 ? item.getVariant().getLength() : 0);
            maxWidth = Math.max(maxWidth,
                    item.getVariant().getWidth() != 0 ? item.getVariant().getWidth() : 0);
            totalHeight += (item.getVariant().getHeight() != 0 ?
                    item.getVariant().getHeight() : 0) * item.getQuantity();
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
        order.setReceiverName(user.getFirstName() + " " + user.getLastName());
        order.setNote(note);
        order.setTotalPrice(info.totalPrice);
        order.setShippingFee(shippingFee);
        order.setFinalTotal(info.totalPrice.add(shippingFee));

        // Tạo order items
        for (CartItem item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(item.getVariant())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getVariant().getPrice())
                    .lineTotal(item.getVariant().getPrice()
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
                    .findByVariant(item.getVariant())
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
//@Service
//@RequiredArgsConstructor
//public class OrderServiceImpl implements OrderService {
//    private final OrderRepository orderRepository;
//    private final InventoryRepository inventoryRepository;
//    private final CartRepository cartRepository;
//    private final CartItemRepository cartItemRepository;
//    private final PaymentRepository paymentRepository;
//    private final OrderMapper orderMapper;
//
//
//    @Override
//    @Transactional
//    public OrderResponse createOrder(UserAccount user, PaymentMethod paymentMethod) {
//        //When user created their account, you also should create their cart
//        Cart cart = cartRepository.findByUser(user)
//                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));
//
//        if (cart.getItems().isEmpty()) {
//            throw new AppException(ErrorCode.CART_EMPTY);
//        }
//
//        Order order = new Order();
//        order.setUser(user);
//        order.setStatus(PENDING);
//
//        BigDecimal total = BigDecimal.ZERO;
//
//        //doan nay de uoc tinh phi ship
//        Integer totalWeight = 0;
//        Integer totalHeight = 0;
//        Integer maxLenght = 0;
//        Integer maxWidth = 0;
//
//        //-------
//
//
//        for (CartItem item : cart.getItems()) {
//
//            Inventory inventory = inventoryRepository
//                    .findByVariant(item.getVariant())
//                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
//
//            if (item.getQuantity() > inventory.getQuantity()) {
//                throw new AppException(ErrorCode.OUT_OF_STOCK);
//            }
//
//            inventory.setQuantity(
//                    inventory.getQuantity() - item.getQuantity()
//            );
//
//            OrderItem orderItem = OrderItem.builder()
//                    .order(order)
//                    .variant(item.getVariant())
//                    .quantity(item.getQuantity())
//                    .unitPrice(item.getVariant().getPrice())
//                    .lineTotal(item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                    .build();
//
//            order.getItems().add(orderItem);
//
//            total = total.add(
//                    orderItem.getUnitPrice()
//                            .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
//            );
//        }
//
//        order.setTotalPrice(total);
//
//        Order savedOrder = orderRepository.save(order);
//
//        //ĐOẠN MNAYF XỬ LÍ VNPAY
////        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
////            String paymentUrl = paymentGatewayService.createPaymentUrl(payment);
////            payment.setPaymentUrl(paymentUrl);
////            payment.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // VNPAY timeout 15 phút
////        }
//
//
//        Payment payment = createPaymentForOrder(savedOrder, PaymentMethod.COD);
//        paymentRepository.save(payment);
//        //cartItemRepository.deleteAll(cart.getItems());
//        cart.getItems().clear(); // ✅ là đủ, KHÔNG cần deleteAll
//        return orderMapper.toOrderResponse(savedOrder);
//    }
//    private Payment createPaymentForOrder(Order order, PaymentMethod paymentMethod) {
//        return Payment.builder()
//                .order(order)
//                .status(PaymentStatus.PENDING)
//                .method(paymentMethod)
//                .amount(order.getTotalPrice())
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getMyOrders(UserAccount user) {
//        List<Order> orders =
//                orderRepository.findByUserOrderByCreatedAtDesc(user);
//
//        return orders.stream()
//                .map(orderMapper::toOrderResponse)
//                .toList();
//    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public OrderResponse getOrderDetail(UserAccount user, String orderId) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//
//        if (!order.getUser().getId().equals(user.getId())) {
//            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
//        }
//
//        return orderMapper.toOrderResponse(order);
//    }
//
//    @Override
//    @Transactional
//    public OrderResponse userCancelOrder(UserAccount user, String orderId) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//
//        if (!order.getUser().getId().equals(user.getId())) {
//            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
//        }
//        if (!order.getStatus().userCanCancel()) {
//            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
//        }
////        if (order.getStatus() != PENDING) {
////            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
////        }
//
//        // rollback inventory
//        for (OrderItem item : order.getItems()) {
//            Inventory inventory = inventoryRepository
//                    .findByVariant(item.getVariant())
//                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
//
//            inventory.setQuantity(
//                    inventory.getQuantity() + item.getQuantity()
//            );
//        }
//
//        order.setStatus(OrderStatus.CANCELED);
//
//        return orderMapper.toOrderResponse(order);
//    }
//
//    @Override
//    @Transactional
//    public OrderResponse buyNow(UserAccount user, BuyNowRequest request) {
//
//        Inventory inventory = inventoryRepository
//                .findByVariantId(request.getVariantId())
//                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
//
//        if (request.getQuantity() > inventory.getQuantity()) {
//            throw new AppException(ErrorCode.OUT_OF_STOCK);
//        }
//
//        Order order = new Order();
//        order.setUser(user);
//        order.setStatus(PENDING);
//
//        // trừ kho
//        inventory.setQuantity(
//                inventory.getQuantity() - request.getQuantity()
//        );
//
//        BigDecimal unitPrice = inventory.getVariant().getPrice();
//
//        OrderItem orderItem = OrderItem.builder()
//                .order(order)
//                .variant(inventory.getVariant())
//                .quantity(request.getQuantity())
//                .lineTotal(inventory.getVariant().getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
//                .unitPrice(unitPrice)
//                .build();
//
//        order.getItems().add(orderItem);
//
//        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
//
//        order.setTotalPrice(
//                total
//        );
//
//        Order savedOrder = orderRepository.save(order);
//        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
//        paymentRepository.save(payment);
//
//        return orderMapper.toOrderResponse(savedOrder);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getAllOrders() {
//        return orderRepository.findAllByOrderByCreatedAtDesc()
//                .stream()
//                .map(orderMapper::toOrderResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public OrderResponse getOrderDetailForAdmin(String orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//        return orderMapper.toOrderResponse(order);
//    }
//
//    @Override
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    @Transactional
//    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//        // rule chuyển trạng thái (optional nhưng xịn)
//        if (!order.getStatus().canTransitionTo(status)) {
//            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
//        }
//
//        order.setStatus(status);
//
//        return orderMapper.toOrderResponse(orderRepository.save(order));
//    }
//
//    @Override
//    @Transactional
//    public OrderResponse adminCancelOrder(String orderId) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//
//        if (!order.getStatus().adminCanCancel()) {
//            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
//        }
//
//        if (order.getStatus() == OrderStatus.CANCELED) {
//            return orderMapper.toOrderResponse(order);
//        }
//
//        // trả kho
//        for (OrderItem item : order.getItems()) {
//            Inventory inventory = inventoryRepository
//                    .findByVariant(item.getVariant())
//                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
//
//            inventory.setQuantity(
//                    inventory.getQuantity() + item.getQuantity()
//            );
//        }
//
//        order.setStatus(OrderStatus.CANCELED);
//
//        return orderMapper.toOrderResponse(order);
//    }
////    // Xử lý callback từ VNPAY
////    public void processPaymentCallback(PaymentCallbackRequest callback) {
////        log.info("Processing payment callback for order: {}", callback.getOrderCode());
////
////        Payment payment = paymentService.findByPaymentCode(callback.getPaymentCode())
////                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
////
////        Order order = payment.getOrder();
////
////        // Kiểm tra nếu đã có payment success
////        if (paymentService.hasSuccessfulPayment(order.getId())) {
////            log.warn("Order {} already has successful payment", order.getOrderCode());
////            updateFailedPayment(payment, callback);
////            return;
////        }
////
////        // Xử lý theo status callback
////        switch (callback.getStatus()) {
////            case SUCCESS:
////                processSuccessfulPayment(order, payment, callback);
////                break;
////            case FAILED:
////                processFailedPayment(order, payment, callback);
////                break;
////            default:
////                log.warn("Unexpected payment status: {}", callback.getStatus());
////        }
////    }
//
////đoạn này t chua có class response nên chưa viết mapper cho nó nên để tạm null
////với cả t vãn muốn tối ưu hơn ở chỗ là mình sẽ chọn một list<cartItem> ròi truyền vào hàm này
////sau đó sau khi tạo order xong sẽ xóa cacds item được chọn tỏng cart ra khỏi cart
////Còn giwof mày check xem t viết hàm này tối ưu hơn không
//
////    @Override
////    @Transactional
////    public OrderResponse createOrder(UserAccount user, List<Long> cartItemIds) {
////
////        List<CartItem> selectedItems =
////                cartItemRepository.findAllById(cartItemIds);
////
////        if (selectedItems.isEmpty()) {
////            throw new AppException(ErrorCode.CART_EMPTY);
////        }
////
////        // 1. Validate cart ownership
////        for (CartItem item : selectedItems) {
////            if (!item.getCart().getUser().getId().equals(user.getId())) {
////                throw new AppException(ErrorCode.FORBIDDEN);
////            }
////        }
////
////        // 2. Check inventory
////        for (CartItem item : selectedItems) {
////            Inventory inventory = inventoryRepository
////                    .findByVariant(item.getVariant())
////                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
////
////            if (item.getQuantity() > inventory.getQuantity()) {
////                throw new AppException(ErrorCode.OUT_OF_STOCK);
////            }
////        }
////
////        // 3. Create order
////        Order order = new Order();
////        order.setUser(user);
////        order.setStatus(PENDING);
////        order.setItems(new ArrayList<>());
////
////        BigDecimal total = BigDecimal.ZERO;
////
////        for (CartItem item : selectedItems) {
////
////            Inventory inventory = inventoryRepository
////                    .findByVariant(item.getVariant())
////                    .get();
////
////            inventory.setQuantity(
////                    inventory.getQuantity() - item.getQuantity()
////            );
////
////            OrderItem orderItem = OrderItem.builder()
////                    .order(order)
////                    .variant(item.getVariant())
////                    .quantity(item.getQuantity())
////                    .unitPrice(item.getVariant().getPrice())
////                    .build();
////
////            order.getItems().add(orderItem);
////
////            total = total.add(
////                    item.getVariant().getPrice()
////                            .multiply(BigDecimal.valueOf(item.getQuantity()))
////            );
////        }
////
////        order.setTotalPrice(total);
////
////        orderRepository.save(order);
////
////        // 4. Remove only selected items from cart
////        cartItemRepository.deleteAll(selectedItems);
////
////        return null; // sau này gắn mapper
////    }
//
//
//
//
//}
