package com.ttthinh.shoe_shop_basic.entity.order;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
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
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    ProductVariant variant;

    /**
     * Snapshot giá tại thời điểm mua
     */
    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false)
    Integer quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal lineTotal;
}
