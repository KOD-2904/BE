package com.ttthinh.shoe_shop_basic.dto.response.shop;

import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {

    private String id;

    private String userId;

    private String status;

    private BigDecimal totalPrice;

    private BigDecimal shippingPrice;

    private BigDecimal discountPrice;

    private List<OrderItemResponse> items;

    //private PaymentMethod paymentMethod;
    //private PaymentStatus paymentStatus;
    private String paymentId; // Cho VNPAY
    private String paymentUrl; // Cho VNPAY

    private LocalDateTime createdAt;

    public BigDecimal getFinalTotalPrice() {
        BigDecimal total = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        BigDecimal shipping = shippingPrice != null ? shippingPrice : BigDecimal.ZERO;
        BigDecimal discount = discountPrice != null ? discountPrice : BigDecimal.ZERO;

        BigDecimal result = total.add(shipping).subtract(discount);

        return result.max(BigDecimal.ZERO); // không cho âm
    }
}
