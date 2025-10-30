package com.zeebra.domain.payment.service;

import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;

import jakarta.validation.Valid;

public interface PaymentService {
	CreatePaymentResponse createPayment(@Valid CreatePaymentRequest request, Long memberId);
}