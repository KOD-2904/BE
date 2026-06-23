package com.ttthinh.shoe_shop_basic.entity.payment;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "payments")
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String transactionId; // Mã giao dịch từ VNPAY

    @Column
    private String paymentUrl; // URL thanh toán cho VNPAY
    @Column
    private LocalDateTime paidAt;
    @Column
    private String failureReason;
    @Column
    private LocalDateTime expiredAt;

    public boolean isSuccess() {
        return PaymentStatus.PAID.equals(this.status);
    }
    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }
}
