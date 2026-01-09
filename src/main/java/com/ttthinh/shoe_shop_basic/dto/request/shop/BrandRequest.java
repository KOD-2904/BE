package com.ttthinh.shoe_shop_basic.dto.request.shop;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandRequest {
    String name;
    String logoUrl; // tạm
}
