package com.zeebra.domain.payment.service;

import com.zeebra.domain.payment.dto.ApprovePaymentRequest;
import com.zeebra.domain.payment.dto.ApprovePaymentResponse;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;

public interface PaymentService {
	CreatePaymentResponse createPayment(CreatePaymentRequest request, Long memberId);

	ApprovePaymentResponse approvePayment(ApprovePaymentRequest request, Long memberId);

}