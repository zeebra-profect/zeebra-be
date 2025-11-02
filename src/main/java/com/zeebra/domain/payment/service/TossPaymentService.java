package com.zeebra.domain.payment.service;

import java.math.BigDecimal;

import com.zeebra.domain.payment.dto.TossApprovalResponse;

public interface TossPaymentService {
	TossApprovalResponse approve(String paymentKey, String tossOrderId, BigDecimal amount);
}