package com.zeebra.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByTossOrderId(String tossOrderId);

	boolean existsByTossOrderId(String tossOrderId);
}