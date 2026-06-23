package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;
import com.ttthinh.shoe_shop_basic.dto.request.checkout.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNShippingService {
    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final GHNCacheService ghnCacheService;

    @Cacheable(value = "shippingFee", key = "#request.toDistrictId + '_' + #request.toWardCode + '_' + #request.weight")
    public Integer calculateShippingFee(ShippingFeeRequest request) {
        if (ghnConfig.isMockEnabled()) {
            return 30000;
        }

        String url = ghnConfig.getApiUrl() + "/shipping-order/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("service_type_id", 2);  // 2: hàng nhẹ, 5: hàng nặng
        payload.put("from_district_id", ghnConfig.getFromDistrictId());
        payload.put("from_ward_code", ghnConfig.getFromWardCode());
        payload.put("to_district_id", request.getToDistrictId());
        payload.put("to_ward_code", request.getToWardCode());
        payload.put("weight", request.getWeight());

        // Thêm kích thước nếu có
        if (request.getLength() != null && request.getLength() > 0) {
            payload.put("length", request.getLength());
            payload.put("width", request.getWidth());
            payload.put("height", request.getHeight());
        }

        // Bảo hiểm (nếu có)
        if (request.getInsuranceValue() != null && request.getInsuranceValue() > 0) {
            payload.put("insurance_value", request.getInsuranceValue());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && (Integer) response.getBody().get("code") == 200) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                Integer total = (Integer) data.get("total");
                log.info("Shipping fee calculated: {}", total);
                return total;
            } else {
                log.error("GHN API error: {}", response.getBody());
                throw new AppException(ErrorCode.CAN_NOT_SOLVE_SHIPPING_FEE);
                //throw new RuntimeException("Không thể tính phí vận chuyển");
            }
        } catch (Exception e) {
            log.error("Error calling GHN API", e);
            throw new AppException(ErrorCode.CAN_NOT_SOLVE_SHIPPING_FEE);
            //throw new RuntimeException("Lỗi kết nối tới GHN: " + e.getMessage());
        }
    }

    public void createOrder(){

    }
}
