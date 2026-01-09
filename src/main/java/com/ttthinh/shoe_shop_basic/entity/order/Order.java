package com.ttthinh.shoe_shop_basic.entity.order;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserAccount user;

    @Enumerated(EnumType.STRING)
    OrderStatus status;

    BigDecimal subtotal;
    BigDecimal discountTotal;
    BigDecimal shippingFee;
    BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrderItem> items;
}
