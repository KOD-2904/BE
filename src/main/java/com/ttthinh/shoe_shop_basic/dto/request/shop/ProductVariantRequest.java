package com.ttthinh.shoe_shop_basic.dto.request.shop;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantRequest {
    String productId;
    String sku;
    String size;
    String color;
    BigDecimal price;
}
