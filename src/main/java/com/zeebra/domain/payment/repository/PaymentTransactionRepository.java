package com.zeebra.domain.payment.repository;

import com.zeebra.domain.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}