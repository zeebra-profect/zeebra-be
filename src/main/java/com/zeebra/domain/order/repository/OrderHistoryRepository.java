package com.zeebra.domain.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.order.entity.OrderHistory;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
	Optional<OrderHistory> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);
}