package com.ttthinh.shoe_shop_basic.dto.request.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddCartItemRequest {
    @NotNull
    private String variantId;

    @Min(1)
    private int quantity;
}
