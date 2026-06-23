package com.ttthinh.shoe_shop_basic.entity.order;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.HashSet;
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

    BigDecimal discountPrice;

    BigDecimal totalPrice;  //tong gia san pham

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrderItem> items = new HashSet<>();

    private String addressId;           // ID địa chỉ giao hàng
    private String shippingAddress;   // full address text (backup)
    private BigDecimal shippingFee;   // phí ship
    private BigDecimal finalTotal;    // tổng = totalPrice + shippingFee
    private String note;              // ghi chú đơn hàng
    private String phoneNumber;       // số điện thoại nhận hàng
    private String receiverName;
}
