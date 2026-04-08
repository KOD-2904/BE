package com.ttthinh.shoe_shop_basic.service.impl.shopImpl.payment;

import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.dto.request.shop.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.VNPayCallbackResponse;
import com.ttthinh.shoe_shop_basic.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {
    private final VNPayConfig vnPayConfig;

    /**
     * Tạo URL thanh toán VNPAY
     */
    public String createPaymentUrl(VNPayPaymentRequest request, String ipAddress) {
        // Chuyển amount sang đơn vị VND * 100 (theo yêu cầu VNPAY)
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Tạo mã giao dịch duy nhất nếu chưa có
        String txnRef = request.getOrderId() != null
                ? request.getOrderId()
                : VNPayUtil.generateTransactionCode();

        // Build params
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountInCents));
        params.put("vnp_CurrCode", vnPayConfig.getCurrency());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", request.getOrderInfo());
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", request.getReturnUrl() != null ?
                request.getReturnUrl() : vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);  // Cần lấy IP thực tế của request
        params.put("vnp_CreateDate", getCurrentDate());

        // Sắp xếp params và tạo chữ ký
        String queryString = VNPayUtil.buildQueryString(params);
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        // URL hoàn chỉnh
        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác thực callback từ VNPAY
     */
    public boolean validateCallback(VNPayCallbackResponse callback) {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TmnCode", callback.getVnp_TmnCode());
        fields.put("vnp_Amount", callback.getVnp_Amount());
        fields.put("vnp_BankCode", callback.getVnp_BankCode());
        fields.put("vnp_BankTranNo", callback.getVnp_BankTranNo());
        fields.put("vnp_CardType", callback.getVnp_CardType());
        fields.put("vnp_OrderInfo", callback.getVnp_OrderInfo());
        fields.put("vnp_PayDate", callback.getVnp_PayDate());
        fields.put("vnp_ResponseCode", callback.getVnp_ResponseCode());
        fields.put("vnp_TxnRef", callback.getVnp_TxnRef());
        fields.put("vnp_TransactionNo", callback.getVnp_TransactionNo());
        fields.put("vnp_TransactionStatus", callback.getVnp_TransactionStatus());

        String queryString = VNPayUtil.buildQueryString(fields);
        String expectedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return expectedHash.equals(callback.getVnp_SecureHash());
    }

    /**
     * Lấy thời gian hiện tại theo format VNPAY (yyyyMMddHHmmss)
     */
    private String getCurrentDate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return now.format(formatter);
    }

    /**
     * Lấy IP của client (cần truyền từ request)
     */
    private String getIpAddress() {
        // Tạm thời return localhost
        // Trong controller sẽ lấy từ HttpServletRequest
        return "127.0.0.1";
    }
}
