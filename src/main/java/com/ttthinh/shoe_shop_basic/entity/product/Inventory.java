package com.ttthinh.shoe_shop_basic.entity.product;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "inventories")
public class Inventory extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    ProductVariant variant;

    Integer quantity;

    @Version
    Long version;
}
