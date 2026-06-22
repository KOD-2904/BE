package com.ttthinh.shoe_shop_basic.dto.request.catalog;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantRequest {
    String productId;
    String color;
    Boolean active;
    List<VariantSizeRequest> sizes;
}
