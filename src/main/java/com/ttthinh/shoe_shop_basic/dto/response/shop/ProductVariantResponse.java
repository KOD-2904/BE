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
public class ProductVariantResponse {
    String id;
    String productId;
    String sku;
    String color;
    String size;
    BigDecimal price;
    Boolean active;
}
