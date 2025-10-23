package com.zeebra.domain.order.repository;

import com.zeebra.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}