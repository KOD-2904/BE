package com.ttthinh.shoe_shop_basic.repository.shop.order;

import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserOrderByCreatedAtDesc(UserAccount user);
    //List<Order> findOrderByCreatedAtDesc();
    List<Order> findAllByOrderByCreatedAtDesc();

    boolean existsByIdAndUser(String id, UserAccount user);

    Optional<Order> findOrderById(String id);
    List<Order> findByStatus(OrderStatus status);


}
