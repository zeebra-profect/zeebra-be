package com.zeebra.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.order.entity.OrderItem;


public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}