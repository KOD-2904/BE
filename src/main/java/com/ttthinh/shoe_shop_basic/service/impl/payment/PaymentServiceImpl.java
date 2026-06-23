package com.ttthinh.shoe_shop_basic.service.impl.payment;

import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryLockService inventoryLockService;

    @Override
    public void updatePaymentStatus(String id, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findPaymentById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        PaymentStatus oldStatus = payment.getStatus();
        // Validate status transition
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition payment from %s to %s", oldStatus, newStatus)
            );
        }

        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment {} status updated from {} to {}", id, oldStatus, newStatus);
        //return updatedPayment;
    }

    @Override
    public Optional<Payment> findSuccessfulPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID);
    }

    @Override
    public boolean hasSuccessfulPayment(String orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID);
    }

    @Override
    public long countFailedPayments(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                .count();
    }

    @Override
    public Optional<Payment> findByPaymentId(String id) {
        return paymentRepository.findPaymentById(id);
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    private boolean isValidStatusTransition(PaymentStatus oldStatus, PaymentStatus newStatus) {
        // Define valid transitions
        return switch (oldStatus) {
            case UNPAID -> newStatus == PaymentStatus.PAID ||
                    newStatus == PaymentStatus.FAILED;
            case PAID -> newStatus == PaymentStatus.REFUNDED; // Only can refund
            case FAILED -> false; // Failed payments cannot change
            case REFUNDED -> false;
        };
    }
    @Scheduled(fixedDelay = 120000) // Run every 2 minute
    @Transactional
    public void handleExpiredPayments() {
        log.info("Checking for expired payments");

        List<Payment> pendingPayments = paymentRepository.findPaymentByStatus(PaymentStatus.UNPAID);

        for (Payment payment : pendingPayments) {
            if (payment.isExpired()) {
                handleExpiredPayment(payment);
            }
        }
    }
    private void handleExpiredPayment(Payment payment) {
        log.info("Payment {} expired", payment.getId());

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment timeout");
        paymentRepository.save(payment);

        // Check if we should cancel the order
        Order order = payment.getOrder();
        if (!hasSuccessfulPayment(order.getId())) {
            // Nếu không còn payment nào active, cancel order
            boolean hasActivePayment = paymentRepository.existsByOrderIdAndStatusIn(
                    order.getId(),
                    List.of(PaymentStatus.UNPAID)
            );

            if (!hasActivePayment) {
                inventoryLockService.releaseLocked(order);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                // Cần orderRepository.save(order) - có thể inject OrderRepository
            }
        }
    }
}
