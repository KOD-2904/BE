package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.VNPayCallbackResponse;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.repository.shop.order.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.payment.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.impl.shopImpl.payment.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * API tạo payment URL (FE gọi khi user chọn thanh toán VNPAY)
     */
    @PostMapping("/vnpay/create")
    public ApiResponse<?> createVNPayPayment(
            @RequestParam String orderId,
            HttpServletRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Tạo payment URL
        VNPayPaymentRequest paymentRequest = VNPayPaymentRequest.builder()
                .orderId(orderId)
                .amount(order.getFinalTotal())  // Dùng final total (đã bao gồm ship)
                .orderInfo("Thanh toan don hang " + orderId)
                .build();

        String paymentUrl = vnPayService.createPaymentUrl(paymentRequest, request.getRequestURI());

        // Lưu payment URL vào payment record nếu cần
        Payment payment = paymentRepository.findByOrder(order);
                //.orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setPaymentUrl(paymentUrl);
        payment.setExpiredAt(LocalDateTime.from(LocalDateTime.now().plusMinutes(15)));
        paymentRepository.save(payment);
        return ApiResponse.builder()
                .result(Map.of("paymentUrl", paymentUrl))
                .message("Payment created successfully")
                .code(200)
                .build();
        //return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    /**
     * Callback URL - VNPAY redirect về sau khi thanh toán (GET)
     * Dùng cho redirect từ trình duyệt
     */
    @GetMapping("/vnpay-callback")
    public ApiResponse<?> paymentCallback(HttpServletRequest request) {
        // Lấy tất cả params từ VNPAY
        Map<String, String> params = extractParams(request);

        // Build callback response
        VNPayCallbackResponse callback = VNPayCallbackResponse.builder()
                .vnp_TmnCode(params.get("vnp_TmnCode"))
                .vnp_Amount(params.get("vnp_Amount"))
                .vnp_BankCode(params.get("vnp_BankCode"))
                .vnp_BankTranNo(params.get("vnp_BankTranNo"))
                .vnp_CardType(params.get("vnp_CardType"))
                .vnp_OrderInfo(params.get("vnp_OrderInfo"))
                .vnp_PayDate(params.get("vnp_PayDate"))
                .vnp_ResponseCode(params.get("vnp_ResponseCode"))
                .vnp_TxnRef(params.get("vnp_TxnRef"))
                .vnp_TransactionNo(params.get("vnp_TransactionNo"))
                .vnp_TransactionStatus(params.get("vnp_TransactionStatus"))
                .vnp_SecureHash(params.get("vnp_SecureHash"))
                .build();

        // Validate chữ ký
        boolean isValid = vnPayService.validateCallback(callback);

        if (!isValid) {
            log.error("Invalid signature from VNPAY callback");
            return ApiResponse.builder()
                    .result(Map.of("error", "Invalid signature"))
                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }

        String responseCode = callback.getVnp_ResponseCode();
        String orderId = callback.getVnp_TxnRef();

        // Tìm order và payment
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        Payment payment = paymentRepository.findByOrder(order);
                //.orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(callback.getVnp_TransactionNo());
            payment.setPaidAt(parsePayDate(callback.getVnp_PayDate()));

            // Cập nhật trạng thái order (nếu cần)
            // order.setStatus(OrderStatus.PAID);

            orderRepository.save(order);
            paymentRepository.save(payment);

            log.info("Payment successful for order: {}", orderId);

            // Redirect về frontend success page
            return ApiResponse.builder()
                    .result(Map.of(
                    "code", "00",
                    "message", "Thanh toán thành công",
                    "orderId", orderId))
                    .message("Payment successful")
                    .code(400)
                    .build();
//            return ResponseEntity.ok(Map.of(
//                    "code", "00",
//                    "message", "Thanh toán thành công",
//                    "orderId", orderId
//            ));
        } else {
            // Thanh toán thất bại
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            log.warn("Payment failed for order: {}, responseCode: {}", orderId, responseCode);

            // Redirect về frontend failure page
            return ApiResponse.builder()
                    .result(Map.of(
                            "code", responseCode,
                            "message", getResponseCodeMessage(responseCode),
                            "orderId", orderId))
                    .message("Payment failed")
                    .code(400)
                    .build();
//            return ResponseEntity.ok(Map.of(
//                    "code", responseCode,
//                    "message", getResponseCodeMessage(responseCode),
//                    "orderId", orderId
//            ));
        }
    }

    /**
     * IPN URL - VNPAY gửi thông báo từ server (không redirect)
     * Dùng để xác nhận thanh toán bất kể user có đóng browser hay không
     */
    @PostMapping("/vnpay-ipn")
    public ApiResponse<?> paymentIPN(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);

        VNPayCallbackResponse callback = VNPayCallbackResponse.builder()
                .vnp_TmnCode(params.get("vnp_TmnCode"))
                .vnp_Amount(params.get("vnp_Amount"))
                .vnp_ResponseCode(params.get("vnp_ResponseCode"))
                .vnp_TxnRef(params.get("vnp_TxnRef"))
                .vnp_TransactionNo(params.get("vnp_TransactionNo"))
                .vnp_SecureHash(params.get("vnp_SecureHash"))
                .build();

        // Validate signature
        if (!vnPayService.validateCallback(callback)) {
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "97", "Message", "Invalid signature"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.ok(
             //       Map.of("RspCode", "97", "Message", "Invalid signature"));
        }

        String orderId = callback.getVnp_TxnRef();
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "01", "Message", "Order not found"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        Payment payment = paymentRepository.findByOrder(order);
        if (payment == null) {
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "01", "Message", "Payment not found"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Payment not found"));
        }

        // Kiểm tra nếu đã xử lý rồi thì bỏ qua
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "02", "Message", "Order already confirmed"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
        }

        String responseCode = callback.getVnp_ResponseCode();

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(callback.getVnp_TransactionNo());
            paymentRepository.save(payment);

            // TODO: Cập nhật order status, gửi email xác nhận, tạo đơn GHN...
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "00", "Message", "Success"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
            //return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Success"));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return ApiResponse.builder()
                    .result(Map.of("RspCode", "00", "Message", "Payment failed, order updated"))
//                    .message("Invalid signature")
                    .code(400)
                    .build();
           // return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Payment failed, order updated"));
        }
    }

    // Helper: extract all params from request
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            try {
                String decoded = URLDecoder.decode(values[0], StandardCharsets.UTF_8);
                params.put(key, decoded);
            } catch (Exception e) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    // Helper: parse VNPAY date format (yyyyMMddHHmmss)
    private LocalDateTime parsePayDate(String payDate) {
        try {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(payDate, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    // Helper: get message for response code
    private String getResponseCodeMessage(String code) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Giao dịch thành công");
        messages.put("01", "Giao dịch đã tồn tại");
        messages.put("02", "Merchant không hợp lệ");
        messages.put("03", "Dữ liệu gửi sai định dạng");
        messages.put("04", "Chữ ký không hợp lệ");
        messages.put("05", "Yêu cầu không hợp lệ");
        messages.put("06", "Lỗi sai tham số");
        messages.put("07", "Lỗi trùng lặp giao dịch");
        messages.put("08", "Lỗi giao dịch không thành công");
        messages.put("09", "Giao dịch bị nghi ngờ (fraud)");
        messages.put("10", "Không thực hiện được do lỗi kỹ thuật");
        messages.put("11", "Giao dịch quá thời hạn thanh toán");
        messages.put("12", "Giao dịch bị hủy");
        messages.put("24", "Khách hàng hủy giao dịch");
        return messages.getOrDefault(code, "Lỗi không xác định");
    }
}
