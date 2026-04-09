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
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hashBytes = hmac.doFinal(data.getBytes());
            StringBuilder hash = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b & 0xff));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error while hashing", ex);
        }
    }

    public static String buildQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII);

                query.append(encodedName).append("=").append(encodedValue).append("&");
                hashData.append(fieldName).append("=").append(encodedValue).append("&");
            }
        }

        // remove last "&"
        query.setLength(query.length() - 1);
        hashData.setLength(hashData.length() - 1);

        return hashData + "||" + query;
    }

    // Thêm method mới để tạo chuỗi hash (không có & ở đầu)
    public static String createHashString(Map<String, String> params) {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        StringBuilder hashStr = new StringBuilder();
        for (String key : sortedKeys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (!hashStr.isEmpty()) {
                    hashStr.append("&");
                }
                hashStr.append(key).append("=").append(value);
            }
        }
        return hashStr.toString();
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
