package com.ttthinh.shoe_shop_basic.util;

import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class VNPayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            throw new AppException(ErrorCode.CAN_NOT_HASH);
        }
    }

    // Tạo URL query string từ params
    public static String buildQueryString(Map<String, String> params) {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        StringBuilder query = new StringBuilder();
        for (String key : sortedKeys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (!query.isEmpty()) {
                    query.append("&");
                }
                query.append(key).append("=").append(value);
            }
        }
        return query.toString();
    }
    // Tạo mã hóa đơn ngẫu nhiên (duy nhất trong ngày)
    public static String generateTransactionCode() {
        Random rand = new Random();
        String randomNum = String.format("%06d", rand.nextInt(1000000));
        return System.currentTimeMillis() + randomNum;
    }

    // Convert bytes sang hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
