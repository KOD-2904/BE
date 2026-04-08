package com.ttthinh.shoe_shop_basic.dto.response.shop;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    private String cartItemId;
    private String variantId;

    private String productName;
    private String brand;
    private String category;
    private String size;
    private String color;

    private BigDecimal price;
    private int quantity;
    private BigDecimal lineTotal;
}
