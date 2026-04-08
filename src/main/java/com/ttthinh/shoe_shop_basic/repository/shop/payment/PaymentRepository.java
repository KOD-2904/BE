package com.ttthinh.shoe_shop_basic.repository.shop.payment;

import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findPaymentById(String id);
    List<Payment> findByOrderId(String orderId);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);
    Optional<Payment> findTopByOrderIdAndStatusOrderByCreatedAtDesc(String orderId, PaymentStatus status);
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);

    Payment findByOrder(Order order);
    List<Payment> findPaymentByStatus(PaymentStatus status);
}
