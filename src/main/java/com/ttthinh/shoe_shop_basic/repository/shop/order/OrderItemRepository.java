package com.ttthinh.shoe_shop_basic.repository.shop.order;

import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
}
