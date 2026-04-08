package com.ttthinh.shoe_shop_basic.dto.response.shop;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
    private String id;

    private String productId;
    private String productName;

    private String variantId;
    private String variantName;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
