package com.zeebra.domain.payment.service;

import com.zeebra.domain.payment.dto.ApprovePaymentRequest;
import com.zeebra.domain.payment.dto.ApprovePaymentResponse;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.dto.FailPaymentRequest;
import com.zeebra.domain.payment.dto.FailPaymentResponse;

public interface PaymentService {
	CreatePaymentResponse createPayment(CreatePaymentRequest request, Long memberId);

	ApprovePaymentResponse approvePayment(ApprovePaymentRequest request, Long memberId);

	FailPaymentResponse failPayment(Long memberId, FailPaymentRequest request);
}