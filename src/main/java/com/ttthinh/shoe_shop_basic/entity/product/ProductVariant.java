package com.ttthinh.shoe_shop_basic.entity.product;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    String sku;
    String size;
    String color;
    BigDecimal price;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL)
    Inventory inventory;
}
