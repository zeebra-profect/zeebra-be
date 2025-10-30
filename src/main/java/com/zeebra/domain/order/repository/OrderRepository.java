package com.zeebra.domain.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	boolean existsByOrderNumber(String orderNumber);

	Optional<Order> findByIdAndMemberId(Long id, Long memberId);

	Optional<Order> findByIdempotencyKey(String clientRequestId);
}